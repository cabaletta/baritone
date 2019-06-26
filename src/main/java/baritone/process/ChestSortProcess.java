/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.process;

import baritone.Baritone;
import baritone.api.event.events.PacketEvent;
import baritone.api.event.events.type.EventState;
import baritone.api.event.listener.AbstractGameEventListener;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.process.IChestSortProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.utils.BaritoneProcessHelper;
import com.google.common.collect.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketCloseWindow;
import net.minecraft.network.play.server.SPacketWindowItems;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public final class ChestSortProcess extends BaritoneProcessHelper implements IChestSortProcess, AbstractGameEventListener {
    private static final PathingCommand NO_PATH = new PathingCommand(null, PathingCommandType.DEFER);

    private boolean active = false;

    private Phase phase;
    // only one of these can be non null at a time
    // tfw no sum types
    private ScanningChestVisitor scanner;
    private SortingChestVisitor sorter;


    public ChestSortProcess(Baritone baritone) {
        super(baritone);
    }

    private enum Phase {
        SCANNING,
        SORTING
    }

    @Override
    public boolean isActive() {
        return this.active;
    }

    @Override
    public void activate() {
        this.active = true;
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        if (scanner == null && sorter == null) { // uninitialized
            this.scanWorld(this.ctx.world());
        }

        if (isChestOpen(ctx)) {
            if (!this.getVisitor().containerOpenTick((ContainerChest)ctx.player().openContainer)) {
                ctx.player().closeScreenAndDropStack();
            }
        }

        while (this.getVisitor().finished()) {
            if (this.getVisitor() == this.scanner) {
                this.setSorting(SortingChestVisitor.fromScanner(this, this.scanner));
            } else {
                this.onLostControl(); // sorter has finished
                return null;
            }
        }

        final ChestVisitor visitor = getVisitor();
        final UniqueChest target = visitor.getCurrentTarget().get();
        final BlockPos targetPos = target.closestChest(ctx).getPos();

        Goal goal = new GoalNear(targetPos, 2);
        Optional<Rotation> newRotation = getRotationForChest(target); // may be aiming at different chest than targetPos but thats fine
        newRotation.ifPresent(rotation -> {
            baritone.getLookBehavior().updateTarget(rotation, true);
        });
        final RayTraceResult trace = ctx.objectMouseOver();
        if (!(isChestOpen(ctx)) && target.getAllChests().stream().anyMatch(chest -> ctx.isLookingAt(chest.getPos()))) {
            ctx.playerController().processRightClickBlock(ctx.player(), ctx.world(), targetPos, trace.sideHit, trace.hitVec, EnumHand.OFF_HAND);
        }

        return new PathingCommand(goal, PathingCommandType.REVALIDATE_GOAL_AND_PATH);
    }

    @Override
    public void onLostControl() {
        this.active = false;

        // reset the state
        // if onLostControl is called we assume that the state of the chests have changed and we should start from scratch
        this.phase = null;
        this.scanner = null;
        this.sorter = null;
    }

    @Override
    public String displayName0() {
        return "ChestSortProcess";
    }

    private void setScanning(ScanningChestVisitor scanner) {
        this.phase = Phase.SCANNING;
        this.scanner = scanner;
        this.sorter = null;
    }
    private void setSorting(SortingChestVisitor sorter) {
        this.phase = Phase.SORTING;
        this.sorter = sorter;
        this.scanner = null;
    }

    private ChestVisitor getVisitor() {
        switch (this.phase) {
            case SCANNING: return Objects.requireNonNull(this.scanner);
            case SORTING:  return Objects.requireNonNull(this.sorter);

            default: throw new IllegalStateException();
        }
    }

    private void scanWorld(World world) {
        List<TileEntityChest> allChests = world.loadedTileEntityList
            .stream()
            .filter(TileEntityChest.class::isInstance).map(TileEntityChest.class::cast)
            .collect(Collectors.toList());

        this.setScanning(new ScanningChestVisitor(this, getUniqueChests(allChests)));
    }

    private static boolean isChestOpen(IPlayerContext ctx) {
        return ctx.player().openContainer instanceof ContainerChest ;
    }

    private static Comparator<UniqueChest> closestChestToPlayer(EntityPlayerSP player) {
        return Comparator.comparingDouble(unique ->
            unique.getAllChests()
                .stream()
                .mapToDouble(tileEnt -> player.getDistanceSq(tileEnt.getPos()))
                .min()
                .getAsDouble() // UniqueChest should be guaranteed to have at least 1 chest
        );
    }

    private static Comparator<TileEntityChest> closestTileEntToPlayer(EntityPlayerSP player) {
        return Comparator.comparingDouble(tileEnt -> player.getDistanceSq(tileEnt.getPos()));
    }

    // TODO: use this
    private static final class UniqueChest {
        private final Set<TileEntityChest> connectedChests; // always has at least 1 chest

        public UniqueChest(TileEntityChest chest) {
            this(getConnectedChests(chest));
        }

        public UniqueChest(Set<TileEntityChest> connectedChests) {
            if (connectedChests.isEmpty()) throw new IllegalArgumentException("Must have at least 1 chest");
            if (!connectedChests.equals(getConnectedChests(connectedChests.iterator().next()))) throw new IllegalArgumentException("bad"); // lol
            this.connectedChests = Collections.unmodifiableSet(connectedChests);
        }

        public int slots() {
            return connectedChests.size() * (9 * 3);
        }

        public boolean isConnected(TileEntityChest chest) {
            return connectedChests.contains(chest);
        }

        // immutable
        public Set<TileEntityChest> getAllChests() {
            return this.connectedChests;
        }

        public TileEntityChest closestChest(IPlayerContext ctx) {
            return this.getAllChests().stream()
                .min(closestTileEntToPlayer(ctx.player()))
                .get();
        }



        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o.getClass() != this.getClass()) return false;
            UniqueChest that = (UniqueChest) o;
            return connectedChests.equals(that.connectedChests);
        }

        @Override
        public int hashCode() {
            return connectedChests.hashCode();
        }
    }

    private static final class StackLocation {
        final UniqueChest chest; // TODO: use UniqueChest
        final int slot;

        public StackLocation(UniqueChest chest, int slot) {
            this.chest = chest;
            this.slot = slot;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o.getClass() != this.getClass()) return false;
            StackLocation that = (StackLocation) o;
            return slot == that.slot &&
                chest.equals(that.chest);
        }

        @Override
        public int hashCode() {
            return Objects.hash(chest, slot);
        }

        @Override
        public String toString() {
            return String.format("StackLocation(%s, %d)", this.chest.toString(), this.slot);
        }
    }

    private static abstract class ChestVisitor {
        protected final ChestSortProcess parent; // non static inner class does not allow static methods :^(

        @Nullable
        protected UniqueChest currentTarget;

        protected ChestVisitor(ChestSortProcess parent) {
            this.parent = parent;
        }

        public final Optional<UniqueChest> getCurrentTarget() {
            return Optional.ofNullable(this.currentTarget);
        }

        // maybe should just check if target is empty??
        public abstract boolean finished();

        // return true if this visitor must do more work
        public abstract boolean onContainerOpened(ContainerChest container, List<ItemStack> itemStacks);

        // Called when SPacketCloseWindow is received.
        // This may happen unexpectedly while the visitor is doing stuff and this visitor may want to call onLostControl in that case
        public void onContainerClosed(int windowId) {}

        // do some work while the container is open
        // return true if the container should stay open
        public abstract boolean containerOpenTick(ContainerChest container);
    }

    private static class ScanningChestVisitor extends ChestVisitor {
        private final ImmutableSet<UniqueChest> scannedChests;
        private final Set<UniqueChest> visitedChests = new HashSet<>(); // visited chest = chest we have opened and received items for. can probably just use chestData keys
        private final Map<UniqueChest, List<ItemStack>> chestData = new HashMap<>();


        ScanningChestVisitor(ChestSortProcess parent, Set<UniqueChest> chests) {
            super(parent);
            this.scannedChests = ImmutableSet.copyOf(chests);
            super.currentTarget = nextTarget().orElse(null);
        }

        public boolean finished() {
            return Sets.difference(scannedChests, visitedChests).isEmpty();
        }

        @Override
        public boolean onContainerOpened(ContainerChest container, List<ItemStack> itemStacks) {
            Objects.requireNonNull(currentTarget, "null currentTarget");
            this.chestData.put(this.currentTarget, itemStacks);
            this.visitedChests.add(this.currentTarget);
            this.currentTarget = nextTarget().orElse(null);

            return false;
        }

        @Override
        public boolean containerOpenTick(ContainerChest container) {
            // we want to wait until we receive the items
            return !this.visitedChests.contains(this.currentTarget);
        }

        private Optional<UniqueChest> nextTarget() {
            Set<UniqueChest> remaining = Sets.difference(this.scannedChests, this.visitedChests);
            return remaining.stream()
                .min(closestChestToPlayer(parent.ctx.player()));
        }

    }


    private static class SortingChestVisitor extends ChestVisitor {
        private final ImmutableSet<UniqueChest> chestsToSort;
        private final Map<UniqueChest, List<ItemStack>> chestData; // this should be kept updated


        private final BiMap<StackLocation, StackLocation> howToSort; // immutable
        private final Set<StackLocation> sortedSlots = new HashSet<>();

        // temp impl
        static final int WORKING_SLOT = 0;

        private Tuple<StackLocationPair, SortState> currentlyMoving;

        @Nullable
        private ContainerChest openContainer; // not used for functionality


        static SortingChestVisitor fromScanner(ChestSortProcess parent, ScanningChestVisitor scanner) {
            return new SortingChestVisitor(parent, scanner.visitedChests, scanner.chestData);
        }

        public SortingChestVisitor(ChestSortProcess parent, Set<UniqueChest> toSort, Map<UniqueChest, List<ItemStack>> chestData) {
            super(parent);
            this.chestsToSort = ImmutableSet.copyOf(toSort);
            this.chestData = chestData;
            this.howToSort = ImmutableBiMap.copyOf(howToSortChests(chestData));
            this.currentlyMoving = nextTarget(this.howToSort, Collections.emptySet()).map(pair -> new Tuple<>(pair, SortState.FETCHING)).orElse(null);
            super.currentTarget = currentlyMoving != null ? currentlyMoving.getFirst().from.chest : null;
        }

        private enum SortState {
            FETCHING,
            MOVING
        }

        public boolean finished() { // TODO don't create new hashsets from the values
            return Sets.difference(Sets.newHashSet(howToSort.values()), sortedSlots).isEmpty();
        }

        @Override
        public boolean onContainerOpened(ContainerChest container, List<ItemStack> itemStacks) {
            this.openContainer = container;
            return true;
        }

        @Override
        public void onContainerClosed(int windowId) {
            if (this.openContainer != null && this.openContainer.windowId == windowId) {
                parent.logDirect("onLostControl");
                // commented out because seems to sometimes happen when everything is fine
                //this.parent.onLostControl(); // container closed while we were using it :^(
            }
        }


        @Override
        public boolean containerOpenTick(ContainerChest container) {
            if (this.openContainer == null) throw new IllegalStateException();

            switch(currentlyMoving.getSecond()) {
                case FETCHING: {
                    // from chest to inv
                    pickupClick(currentlyMoving.getFirst().from.slot, parent.ctx);
                    pickupClick(getInvSlotIndex(WORKING_SLOT, container), parent.ctx);

                    this.currentlyMoving = new Tuple<>(this.currentlyMoving.getFirst(), SortState.MOVING); // change the state
                    this.currentTarget = this.currentlyMoving.getFirst().to.chest;

                    this.openContainer = null; // close chest
                    return false;
                }
                //break;
                case MOVING: {
                    // from inv to chest
                    pickupClick(getInvSlotIndex(WORKING_SLOT, container), parent.ctx);
                    pickupClick(currentlyMoving.getFirst().to.slot, parent.ctx);

                    this.sortedSlots.add(this.currentlyMoving.getFirst().to);

                    this.currentlyMoving = nextTarget(this.howToSort, this.sortedSlots).map(entry -> new Tuple<>(entry, SortState.FETCHING)).orElse(null);
                    this.currentTarget = this.currentlyMoving != null ? this.currentlyMoving.getFirst().from.chest : null;

                    this.openContainer = null; // close chest
                    return false;
                }
                //break;

                default: throw new IllegalStateException("enum??");
            }
        }


        private static Optional<StackLocationPair> nextTarget(BiMap<StackLocation, StackLocation> howToSort, Set<StackLocation> sortedLocations) {
            return howToSort.entrySet().stream()
                .filter(entry  -> !sortedLocations.contains(entry.getValue()))
                .map(entry -> new StackLocationPair(entry.getKey(), entry.getValue()))
                .findAny();
        }

        private static int getInvSlotIndex(int slot, ContainerChest chest) {
            return chest.getLowerChestInventory().getSizeInventory() + slot;
        }

        private static ItemStack pickupClick(int slotId, IPlayerContext ctx) {
            return ctx.playerController().windowClick(ctx.player().openContainer.windowId, slotId, 0, ClickType.PICKUP, ctx.player());
        }

        // given all the chest data, return a map that says where to move chest slots to
        // number = slot slot
        private static BiMap<StackLocation, StackLocation> howToSortChests(final Map<UniqueChest, List<ItemStack>> chestData) {
            final List<List<ItemStack>> sortedChestList = sortChestData(chestData.values()); // paritioned into groups of no more than size of double chest (need to allow single chests later)
            final Set<UniqueChest> chests = Collections.unmodifiableSet(chestData.keySet());
            if (sortedChestList.size() > chests.size()) throw new IllegalStateException(sortedChestList.size() + " - " + chests.size());

            final List<Tuple<UniqueChest, List<ItemStack>>> pairs =
                combine(
                    sortedChestList.iterator(), chests.iterator(),
                    (stack, chest) -> new Tuple<>(chest, stack)
                ); // TODO: don't choose random chests

            final Map<UniqueChest, List<ItemStack>> sortedChestState = pairs.stream() // how we want the state of the chests to be
                .collect(Collectors.toMap(Tuple::getFirst, Tuple::getSecond));

            return conversion(chestData, sortedChestState);
        }


        private static BiMap<StackLocation, StackLocation> conversion(Map<UniqueChest, List<ItemStack>> unsorted, Map<UniqueChest, List<ItemStack>> sorted) {
            // set of slots that some stack has been decided to be moved to
            final Set<StackLocation> reservedSlots = new HashSet<>();
            final BiMap<StackLocation, StackLocation> out = HashBiMap.create();

            unsorted.forEach((chestA, stacksFrom) -> {
                for (int i = 0; i < stacksFrom.size(); i++) {
                    final int indexFrom = i; // dumb final meme

                    final ItemStack stack = stacksFrom.get(indexFrom);
                    if (stack.isEmpty()) continue;

                    sorted.forEach((chestB, stacksTo) -> {
                        indexMatching(stacksTo, (stackSorted, idx) -> !reservedSlots.contains(new StackLocation(chestB, idx)) && ItemStack.areItemStacksEqual(stack, stackSorted))
                            .ifPresent(indexTo -> {
                                final StackLocation from = new StackLocation(chestA, indexFrom);
                                final StackLocation to = new StackLocation(chestB, indexTo);

                                reservedSlots.add(to);
                                out.put(from, to);
                            });
                    });
                }
            });

            return out;
        }

        private static <T> OptionalInt indexMatching(List<T> list, BiPredicate<T, Integer> predicate) {
            for (int i = 0; i < list.size(); i++) {
                if (predicate.test(list.get(i), i)) return OptionalInt.of(i);
            }
            return OptionalInt.empty();
        }

        /*private static <T> Stream<Tuple<T, Integer>> streamWithIndex(List<T> list) {

        }*/

        // combine 2 iterators into list of pairs
        // second argument must at least have the same number of elements as the first argument
        // any extra elements from the second iterator are unused
        private static <T extends Tuple<?, ?>, A, B> List<T> combine(Iterator<A> iterA, Iterator<B> iterB, BiFunction<A, B, T> toPairFn) {
            final List<T> out = new ArrayList<>();

            while(iterA.hasNext()) {
                if (!iterB.hasNext()) throw new IllegalArgumentException("second argument must not have less elements than the first argument");
                out.add(toPairFn.apply(iterA.next(), iterB.next()));
            }

            return out;
        }

        // TODO: dont partition here
        private static List<List<ItemStack>> sortChestData(Collection<List<ItemStack>> values) {
            List<ItemStack> sorted = values.stream()
                .flatMap(List::stream)
                .sorted(ItemSorter::compare)
                .collect(Collectors.toList());
            final int CHEST_SIZE = 9 * 6; // TODO: dont assume double chests

            return Lists.partition(sorted, CHEST_SIZE); // guava is based
        }

        private static final class StackLocationPair {
            public final StackLocation from;
            public final StackLocation to;

            StackLocationPair(StackLocation from, StackLocation to) {
                this.from = from;
                this.to = to;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                StackLocationPair that = (StackLocationPair) o;
                return from.equals(that.from) &&
                    to.equals(that.to);
            }

            @Override
            public int hashCode() {
                return Objects.hash(from, to);
            }
        }
    }


    @Override
    public void onReceivePacket(PacketEvent event) {
        if (event.getState() != EventState.POST) return;
        if (!this.active) return;

        if (event.getPacket() instanceof SPacketWindowItems) {
            final SPacketWindowItems packet = event.cast();

            Minecraft.getMinecraft().addScheduledTask(() -> { // cant be on netty thread
                final Container openContainer = ctx.player().openContainer;
                if (isChestOpen(ctx) && openContainer.windowId == packet.getWindowId()) {
                    ContainerChest containerChest = (ContainerChest)openContainer;
                    // we just got the items for the chest we have open
                    // the server sends our inventory with the chest inventory but we only care about chest inventory so only use the first 27/54 slots
                    final List<ItemStack> chestItems = packet.getItemStacks().subList(0, containerChest.getLowerChestInventory().getSizeInventory());
                    final boolean stayOpen = this.getVisitor().onContainerOpened((ContainerChest)openContainer, chestItems);
                    if (!stayOpen/*this.getVisitor().wantsContainerOpen()*/) {
                        ctx.player().closeScreenAndDropStack();
                    }
                }
            });
        }
        if (event.getPacket() instanceof SPacketCloseWindow) {
            // TODO: check if chest still exists
            final Field idField = SPacketCloseWindow.class.getDeclaredFields()[0]; // lol
            idField.setAccessible(true);
            try {
                this.getVisitor().onContainerClosed((Integer) idField.get(event.getPacket()));
            } catch (ReflectiveOperationException ex) {
                System.out.println("oyyy vey " + ex.toString());
                throw new RuntimeException(ex);
            }
        }
    }


    private Optional<Rotation> getRotationForChest(UniqueChest chestIn) {
        return chestIn.getAllChests().stream()
            .sorted(closestTileEntToPlayer(ctx.player()))
            .map(chest -> RotationUtils.reachable(ctx, chest.getPos()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
    }



    // takes any list of chests and returns a new list of chests where no 2 chests in the list are connected
    private Set<UniqueChest> getUniqueChests(final Collection<TileEntityChest> chestsIn) {
        final Set<Set<TileEntityChest>> graphs = new HashSet<>(); // set of graphs

        // this code is O(1) but more complicated
        /*Set<TileEntityChest> chestsToAdd = new HashSet<>(chestsIn);
        Iterator<TileEntityChest> iterator = chestsToAdd.iterator();
        while (iterator.hasNext()) {
            final TileEntityChest next = iterator.next();
            if (graphs.stream().noneMatch(set -> set.contains(next))) {
                final Set<TileEntityChest> newGraph = getConnectedChests(next);
                graphs.add(newGraph);
                chestsToAdd = Sets.difference(chestsToAdd, newGraph);
                iterator = chestsToAdd.iterator();
            }
        }*/
        for (TileEntityChest iter : chestsIn) {
            if (graphs.stream().noneMatch(set -> set.contains(iter))) {
                final Set<TileEntityChest> newGraph = getConnectedChests(iter);
                graphs.add(newGraph);
            }
        }

        return graphs.stream().map(UniqueChest::new).collect(Collectors.toSet());
    }


    private static Set<TileEntityChest> getConnectedChests(TileEntityChest root) {
        Set<TileEntityChest> out = new HashSet<>();
        addToGraph(out, root);
        return out;
    }

    private static void addToGraph(Set<TileEntityChest> graph, TileEntityChest node) {
        if (node == null || graph.contains(node)) return;
        graph.add(node);
        addToGraph(graph, node.adjacentChestXNeg);
        addToGraph(graph, node.adjacentChestXPos);
        addToGraph(graph, node.adjacentChestZNeg);
        addToGraph(graph, node.adjacentChestZPos);
    }

    private static class ItemSorter {

        // temporary
        static int compare(ItemStack a, ItemStack b) {
            return Comparator.<ItemStack, String>comparing(stack -> stack.getItem().getItemStackDisplayName(stack), String.CASE_INSENSITIVE_ORDER)
                .compare(a, b);
        }
    }

}
