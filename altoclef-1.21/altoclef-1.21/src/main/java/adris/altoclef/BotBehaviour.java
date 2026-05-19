package adris.altoclef;

import adris.altoclef.util.slots.Slot;
import baritone.altoclef.AltoClefSettings;
import baritone.api.Settings;
import baritone.api.utils.RayTraceUtils;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Represents the current behaviour/"on the fly settings" of the bot.
 * <p>
 * Use this to change how the bot works for the duration of a task.
 * <p>
 * (for example, "Build this bridge and avoid mining any blocks nearby")
 */
public class BotBehaviour {

    private final AltoClef _mod;
    Deque<State> _states = new ArrayDeque<>();

    public BotBehaviour(AltoClef mod) {
        _mod = mod;

        // Start with one state.
        push();
    }

    // Getter(s)

    /**
     * Returns the current state of Behaviour for escapeLava
     *
     * @return The current state of Behaviour for escapeLava
     */
    public boolean shouldEscapeLava() {
        return current().escapeLava;
    }

    /// Parameters

    /**
     * If the bot should escape lava or not, part of WorldSurvivalChain
     *
     * @param allow True if the bot should escape lava
     */
    public void setEscapeLava(boolean allow) {
        current().escapeLava = allow;
        current().applyState();
    }

    public void setFollowDistance(double distance) {
        current().followOffsetDistance = distance;
        current().applyState();
    }

    public void setMineScanDroppedItems(boolean value) {
        current().mineScanDroppedItems = value;
        current().applyState();
    }


    public boolean exclusivelyMineLogs() {
        return current().exclusivelyMineLogs;
    }

    public void setExclusivelyMineLogs(boolean value) {
        current().exclusivelyMineLogs = value;
        current().applyState();
    }

    public boolean shouldExcludeFromForcefield(Entity entity) {
        if (!current().excludeFromForceField.isEmpty()) {
            for (Predicate<Entity> pred : current().excludeFromForceField) {
                if (pred.test(entity)) return true;
            }
        }
        return false;
    }

    public void addForceFieldExclusion(Predicate<Entity> pred) {
        current().excludeFromForceField.add(pred);
        // Not needed, as excludeFromForceField isn't applied anywhere else.
        // current.applyState();
    }

    public List<Pair<Slot, Predicate<ItemStack>>> getConversionSlots() {
        return current().conversionSlots;
    }

    public void markSlotAsConversionSlot(Slot slot, Predicate<ItemStack> itemBelongsHere) {
        current().conversionSlots.add(new Pair<>(slot, itemBelongsHere));
        // apply not needed
    }

    public void avoidBlockBreaking(BlockPos pos) {
        current().blocksToAvoidBreaking.add(pos);
        current().applyState();
    }

    public void avoidBlockBreaking(Predicate<BlockPos> pred) {
        current().toAvoidBreaking.add(pred);
        current().applyState();
    }

    public void avoidBlockPlacing(Predicate<BlockPos> pred) {
        current().toAvoidPlacing.add(pred);
        current().applyState();
    }

    public void allowWalkingOn(Predicate<BlockPos> pred) {
        current().allowWalking.add(pred);
        current().applyState();
    }

    public void avoidWalkingThrough(Predicate<BlockPos> pred) {
        current().avoidWalkingThrough.add(pred);
        current().applyState();
    }


    public void forceUseTool(BiPredicate<BlockState, ItemStack> pred) {
        current().forceUseTools.add(pred);
        current().applyState();
    }

    public void setRayTracingFluidHandling(RaycastContext.FluidHandling fluidHandling) {
        current().rayFluidHandling = fluidHandling;
        //Debug.logMessage("OOF: " + fluidHandling);
        current().applyState();
    }

    public void setAllowWalkThroughFlowingWater(boolean value) {
        current()._allowWalkThroughFlowingWater = value;
        current().applyState();
    }

    public void setPauseOnLostFocus(boolean pauseOnLostFocus) {
        current().pauseOnLostFocus = pauseOnLostFocus;
        current().applyState();
    }

    public void addProtectedItems(Item... items) {
        Collections.addAll(current().protectedItems, items);
        current().applyState();
    }

    public void removeProtectedItems(Item... items) {
        current().protectedItems.removeAll(Arrays.asList(items));
        current().applyState();
    }

    public boolean isProtected(Item item) {
        // For now nothing is protected.
        return current().protectedItems.contains(item);
    }

    public boolean shouldForceFieldPlayers() {
        return current().forceFieldPlayers;
    }

    public void setForceFieldPlayers(boolean forceFieldPlayers) {
        current().forceFieldPlayers = forceFieldPlayers;
        // Not needed, nothing changes.
        // current.applyState()
    }

    public void allowSwimThroughLava(boolean allow) {
        current().swimThroughLava = allow;
        current().applyState();
    }

    public void setPreferredStairs(boolean allow) {
        //current().preferredStairs = allow;
        current().applyState();
    }

    public void setAllowDiagonalAscend(boolean allow) {
        current().allowDiagonalAscend = allow;
        current().applyState();
    }

    public void setBlockPlacePenalty(double penalty) {
        current().blockPlacePenalty = penalty;
        current().applyState();
    }

    public void setBlockBreakAdditionalPenalty(double penalty) {
        current().blockBreakAdditionalPenalty = penalty;
        current().applyState();
    }

    public void avoidDodgingProjectile(Predicate<Entity> whenToDodge) {
        current().avoidDodgingProjectile.add(whenToDodge);
        // Not needed, nothing changes.
        // current().applyState();
    }

    public void addGlobalHeuristic(BiFunction<Double, BlockPos, Double> heuristic) {
        current().globalHeuristics.add(heuristic);
        current().applyState();
    }

    public boolean shouldAvoidDodgingProjectile(Entity entity) {
        if (!current().avoidDodgingProjectile.isEmpty()) {
            for (Predicate<Entity> test : current().avoidDodgingProjectile) {
                if (test.test(entity)) return true;
            }
        }
        return false;
    }

    /// Stack management
    public void push() {
        if (_states.isEmpty()) {
            _states.push(new State());
        } else {
            // Make copy and push that
            _states.push(new State(current()));
        }
    }

    public void push(State customState) {
        _states.push(customState);
    }

    public State pop() {
        if (_states.isEmpty()) {
            Debug.logError("State stack is empty. This shouldn't be happening.");
            return null;
        }
        State popped = _states.pop();
        if (_states.isEmpty()) {
            Debug.logError("State stack is empty after pop. This shouldn't be happening.");
            return null;
        }
        _states.peek().applyState();
        return popped;
    }

    private State current() {
        if (_states.isEmpty()) {
            Debug.logError("STATE EMPTY, UNEMPTIED!");
            push();
        }
        return _states.peek();
    }

    class State {
        /// Baritone Params
        public double followOffsetDistance;
        public List<Item> protectedItems = new ArrayList<>();
        public boolean mineScanDroppedItems;
        public boolean swimThroughLava;
        public boolean allowDiagonalAscend;
        //public boolean preferredStairs;
        public double blockPlacePenalty;
        public double blockBreakAdditionalPenalty;

        // Alto Clef params
        public boolean exclusivelyMineLogs;
        public boolean forceFieldPlayers;
        public List<Predicate<Entity>> avoidDodgingProjectile = new ArrayList<>();
        public List<Predicate<Entity>> excludeFromForceField = new ArrayList<>();
        public List<Pair<Slot, Predicate<ItemStack>>> conversionSlots = new ArrayList<>();

        // Extra Baritone Settings
        public HashSet<BlockPos> blocksToAvoidBreaking = new HashSet<>();
        public List<Predicate<BlockPos>> toAvoidBreaking = new ArrayList<>();
        public List<Predicate<BlockPos>> toAvoidPlacing = new ArrayList<>();
        public List<Predicate<BlockPos>> allowWalking = new ArrayList<>();
        public List<Predicate<BlockPos>> avoidWalkingThrough = new ArrayList<>();
        public List<BiPredicate<BlockState, ItemStack>> forceUseTools = new ArrayList<>();
        public List<BiFunction<Double, BlockPos, Double>> globalHeuristics = new ArrayList<>();
        public boolean _allowWalkThroughFlowingWater = false;

        // Minecraft config
        public boolean pauseOnLostFocus = true;

        // Hard coded stuff
        public RaycastContext.FluidHandling rayFluidHandling;

        // Other necessary stuff
        public boolean escapeLava = true;

        public State() {
            this(null);
        }

        public State(State toCopy) {
            // Read in current state
            readState(_mod.getClientBaritoneSettings());

            readExtraState(_mod.getExtraBaritoneSettings());

            readMinecraftState();

            if (toCopy != null) {
                // Copy over stuff from old one
                exclusivelyMineLogs = toCopy.exclusivelyMineLogs;
                avoidDodgingProjectile.addAll(toCopy.avoidDodgingProjectile);
                excludeFromForceField.addAll(toCopy.excludeFromForceField);
                conversionSlots.addAll(toCopy.conversionSlots);
                forceFieldPlayers = toCopy.forceFieldPlayers;
                escapeLava = toCopy.escapeLava;
            }
        }

        /**
         * Make the current state match our copy
         */
        public void applyState() {
            applyState(_mod.getClientBaritoneSettings(), _mod.getExtraBaritoneSettings());
        }

        /**
         * Read in a copy of the current state
         */
        private void readState(Settings s) {
            followOffsetDistance = s.followOffsetDistance.value;
            mineScanDroppedItems = s.mineScanDroppedItems.value;
            swimThroughLava = s.assumeWalkOnLava.value;
            allowDiagonalAscend = s.allowDiagonalAscend.value;
            blockPlacePenalty = s.blockPlacementPenalty.value;
            blockBreakAdditionalPenalty = s.blockBreakAdditionalPenalty.value;
            //preferredStairs = s.allowDownward.value;
        }

        private void readExtraState(AltoClefSettings settings) {
            synchronized (settings.getBreakMutex()) {
                synchronized (settings.getPlaceMutex()) {
                    blocksToAvoidBreaking = new HashSet<>(settings.getBlocksToAvoidBreaking());
                    toAvoidBreaking = new ArrayList<>(settings.getBreakAvoiders());
                    toAvoidPlacing = new ArrayList<>(settings.getPlaceAvoiders());
                    protectedItems = new ArrayList<>(settings.getProtectedItems());
                    synchronized (settings.getPropertiesMutex()) {
                        allowWalking = new ArrayList<>(settings.getForceWalkOnPredicates());
                        avoidWalkingThrough = new ArrayList<>(settings.getForceAvoidWalkThroughPredicates());
                        forceUseTools = new ArrayList<>(settings.getForceUseToolPredicates());
                    }
                }
            }
            synchronized (settings.getGlobalHeuristicMutex()) {
                globalHeuristics = new ArrayList<>(settings.getGlobalHeuristics());
            }
            _allowWalkThroughFlowingWater = settings.isFlowingWaterPassAllowed();

            rayFluidHandling = RayTraceUtils.fluidHandling;
        }

        private void readMinecraftState() {
            pauseOnLostFocus = MinecraftClient.getInstance().options.pauseOnLostFocus;
        }

        /**
         * Make the current state match our copy
         */
        private void applyState(Settings s, AltoClefSettings sa) {
            s.followOffsetDistance.value = followOffsetDistance;
            s.mineScanDroppedItems.value = mineScanDroppedItems;
            s.allowDiagonalAscend.value = allowDiagonalAscend;
            s.blockPlacementPenalty.value = blockPlacePenalty;
            s.blockBreakAdditionalPenalty.value = blockBreakAdditionalPenalty;

            // We need an alternrative method to handle this, this method makes navigation much less reliable.
            //s.allowDownward.value = preferredStairs;

            // Kinda jank but it works.
            synchronized (sa.getBreakMutex()) {
                synchronized (sa.getPlaceMutex()) {
                    sa.getBreakAvoiders().clear();
                    sa.getBreakAvoiders().addAll(toAvoidBreaking);
                    sa.getBlocksToAvoidBreaking().clear();
                    sa.getBlocksToAvoidBreaking().addAll(blocksToAvoidBreaking);
                    sa.getPlaceAvoiders().clear();
                    sa.getPlaceAvoiders().addAll(toAvoidPlacing);
                    sa.getProtectedItems().clear();
                    sa.getProtectedItems().addAll(protectedItems);
                    synchronized (sa.getPropertiesMutex()) {
                        sa.getForceWalkOnPredicates().clear();
                        sa.getForceWalkOnPredicates().addAll(allowWalking);
                        sa.getForceAvoidWalkThroughPredicates().clear();
                        sa.getForceAvoidWalkThroughPredicates().addAll(avoidWalkingThrough);
                        sa.getForceUseToolPredicates().clear();
                        sa.getForceUseToolPredicates().addAll(forceUseTools);
                    }
                }
            }
            synchronized (sa.getGlobalHeuristicMutex()) {
                sa.getGlobalHeuristics().clear();
                sa.getGlobalHeuristics().addAll(globalHeuristics);
            }


            sa.setFlowingWaterPass(_allowWalkThroughFlowingWater);
            sa.allowSwimThroughLava(swimThroughLava);

            // Extra / hard coded
            RayTraceUtils.fluidHandling = rayFluidHandling;

            // Minecraft
            MinecraftClient.getInstance().options.pauseOnLostFocus = pauseOnLostFocus;
        }
    }
}
