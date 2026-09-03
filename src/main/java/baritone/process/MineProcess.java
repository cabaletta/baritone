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
import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.*;
import baritone.api.process.IMineProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.*;
import baritone.api.utils.input.Input;
import baritone.cache.CachedChunk;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.MovementHelper;
import baritone.utils.BaritoneProcessHelper;
import baritone.utils.BlockStateInterface;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.*;
import java.util.stream.Collectors;

import static baritone.api.pathing.movement.ActionCosts.COST_INF;

/**
 * Mine blocks of a certain type
 *
 * @author leijurv
 */
public final class MineProcess extends BaritoneProcessHelper implements IMineProcess {

    private BlockOptionalMetaLookup filter;
    private List<BlockPos> knownOreLocations;
    private List<BlockPos> blacklist; // inaccessible
    private Map<BlockPos, Long> anticipatedDrops;
    private BlockPos branchPoint;
    private GoalRunAway branchPointRunaway;
    private int desiredQuantity;
    private int tickCount;
    private ShulkerDepositHandler shulkerDepositHandler;
    private long shulkerDepositCooldownUntil;

    public MineProcess(Baritone baritone) {
        super(baritone);
    }

    @Override
    public boolean isActive() {
        return filter != null;
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        if (desiredQuantity > 0) {
            int curr = ctx.player().getInventory().getNonEquipmentItems().stream()
                    .filter(stack -> filter.has(stack))
                    .mapToInt(ItemStack::getCount).sum();
            if (curr >= desiredQuantity) {
                logDirect("Have " + curr + " valid items");
                cancel();
                return null;
            }
        }
        PathingCommand depositCommand = handleActiveDeposit(isSafeToCancel);
        if (depositCommand != null) {
            return depositCommand;
        }
        if (shouldStartDeposit()) {
            if (tryStartShulkerDeposit()) {
                depositCommand = handleActiveDeposit(isSafeToCancel);
                if (depositCommand != null) {
                    return depositCommand;
                }
            }
        }
        if (calcFailed) {
            if (!knownOreLocations.isEmpty() && Baritone.settings().blacklistClosestOnFailure.value) {
                logDirect("Unable to find any path to " + filter + ", blacklisting presumably unreachable closest instance...");
                if (Baritone.settings().notificationOnMineFail.value) {
                    logNotification("Unable to find any path to " + filter + ", blacklisting presumably unreachable closest instance...", true);
                }
                knownOreLocations.stream().min(Comparator.comparingDouble(ctx.playerFeet()::distSqr)).ifPresent(blacklist::add);
                knownOreLocations.removeIf(blacklist::contains);
            } else {
                logDirect("Unable to find any path to " + filter + ", canceling mine");
                if (Baritone.settings().notificationOnMineFail.value) {
                    logNotification("Unable to find any path to " + filter + ", canceling mine", true);
                }
                cancel();
                return null;
            }
        }

        updateLoucaSystem();
        int mineGoalUpdateInterval = Baritone.settings().mineGoalUpdateInterval.value;
        List<BlockPos> curr = new ArrayList<>(knownOreLocations);
        if (mineGoalUpdateInterval != 0 && tickCount++ % mineGoalUpdateInterval == 0) { // big brain
            CalculationContext context = new CalculationContext(baritone, true);
            Baritone.getExecutor().execute(() -> rescan(curr, context));
        }
        if (Baritone.settings().legitMine.value) {
            if (!addNearby()) {
                cancel();
                return null;
            }
        }
        Optional<BlockPos> shaft = curr.stream()
                .filter(pos -> pos.getX() == ctx.playerFeet().getX() && pos.getZ() == ctx.playerFeet().getZ())
                .filter(pos -> pos.getY() >= ctx.playerFeet().getY())
                .filter(pos -> !(BlockStateInterface.get(ctx, pos).getBlock() instanceof AirBlock)) // after breaking a block, it takes mineGoalUpdateInterval ticks for it to actually update this list =(
                .min(Comparator.comparingDouble(ctx.playerFeet().above()::distSqr));
        baritone.getInputOverrideHandler().clearAllKeys();
        if (shaft.isPresent() && ctx.player().onGround()) {
            BlockPos pos = shaft.get();
            BlockState state = baritone.bsi.get0(pos);
            if (!MovementHelper.avoidBreaking(baritone.bsi, pos.getX(), pos.getY(), pos.getZ(), state)) {
                Optional<Rotation> rot = RotationUtils.reachable(ctx, pos);
                if (rot.isPresent() && isSafeToCancel) {
                    baritone.getLookBehavior().updateTarget(rot.get(), true);
                    MovementHelper.switchToBestToolFor(ctx, ctx.world().getBlockState(pos));
                    if (ctx.isLookingAt(pos) || ctx.playerRotations().isReallyCloseTo(rot.get())) {
                        baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_LEFT, true);
                    }
                    return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
                }
            }
        }
        PathingCommand command = updateGoal();
        if (command == null) {
            // none in range
            // maybe say something in chat? (ahem impact)
            cancel();
            return null;
        }
        return command;
    }

    private boolean shouldStartDeposit() {
        if (filter == null || shulkerDepositHandler != null) {
            return false;
        }
        if (ctx.player() == null || ctx.world() == null) {
            return false;
        }
        if (ctx.world().getGameTime() < shulkerDepositCooldownUntil) {
            return false;
        }
        if (!inventoryIsFull()) {
            return false;
        }
        return hasDepositableStacks();
    }

    private boolean tryStartShulkerDeposit() {
        if (shulkerDepositHandler != null) {
            return false;
        }
        BlockOptionalMetaLookup activeFilter = filterFilter();
        if (activeFilter == null) {
            return false;
        }
        shulkerDepositHandler = new ShulkerDepositHandler(activeFilter);
        return true;
    }

    private PathingCommand handleActiveDeposit(boolean isSafeToCancel) {
        if (shulkerDepositHandler == null) {
            return null;
        }
        PathingCommand command = shulkerDepositHandler.tick(isSafeToCancel);
        if (shulkerDepositHandler.isFinished()) {
            if (!shulkerDepositHandler.succeeded() && ctx.world() != null) {
                shulkerDepositCooldownUntil = ctx.world().getGameTime() + 200;
            }
            shulkerDepositHandler = null;
        }
        return command;
    }

    private boolean inventoryIsFull() {
        for (int i = 0; i < 36; i++) {
            if (ctx.player().getInventory().getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean hasDepositableStacks() {
        BlockOptionalMetaLookup activeFilter = filterFilter();
        if (activeFilter == null) {
            return false;
        }
        for (int i = 0; i < 36; i++) {
            ItemStack stack = ctx.player().getInventory().getItem(i);
            if (!stack.isEmpty() && activeFilter.has(stack)) {
                return true;
            }
        }
        return false;
    }

    private final class ShulkerDepositHandler {

        private final BlockOptionalMetaLookup activeFilter;
        private final RegistryOps<Tag> registryOps;
        private Stage stage;
        private Placement placement;
        private int shulkerInventorySlot = -1;
        private int shulkerHotbarSlot = -1;
        private ItemStack shulkerReference = ItemStack.EMPTY;

        ShulkerDepositHandler(BlockOptionalMetaLookup activeFilter) {
            this.activeFilter = activeFilter;
            this.registryOps = ctx.world() == null
                    ? null
                    : RegistryOps.create(NbtOps.INSTANCE, ctx.world().registryAccess());
            this.stage = Stage.SEARCH;
            locateDepositShulker();
        }

        boolean isFinished() {
            return stage == Stage.COMPLETE || stage == Stage.FAILED;
        }

        boolean succeeded() {
            return stage == Stage.COMPLETE;
        }

        PathingCommand tick(boolean isSafeToCancel) {
            switch (stage) {
                case SEARCH:
                    locateDepositShulker();
                    return requestPause();
                case MOVE_TO_HOTBAR:
                    return moveShulkerToHotbar();
                case WAIT_FOR_HOTBAR:
                    if (ensureHotbarSlot()) {
                        stage = Stage.FIND_PLACEMENT;
                    }
                    return requestPause();
                case FIND_PLACEMENT:
                    findPlacement();
                    return requestPause();
                case PLACE:
                    return placeShulker(isSafeToCancel);
                case OPEN:
                    return openShulker(isSafeToCancel);
                case DEPOSIT:
                    return depositItems();
                case CLOSE:
                    return closeContainer();
                case BREAK:
                    return breakPlacedShulker(isSafeToCancel);
                case COMPLETE:
                case FAILED:
                default:
                    return null;
            }
        }

        private void locateDepositShulker() {
            if (stage != Stage.SEARCH) {
                return;
            }
            for (int slot = 0; slot < 36; slot++) {
                ItemStack stack = ctx.player().getInventory().getItem(slot);
                if (isDepositShulker(stack)) {
                    shulkerInventorySlot = slot;
                    shulkerReference = stack.copy();
                    stage = Stage.MOVE_TO_HOTBAR;
                    return;
                }
            }
            fail("No shulker with available space");
        }

        private PathingCommand moveShulkerToHotbar() {
            if (shulkerInventorySlot < 0) {
                fail("Unable to locate shulker slot");
                return null;
            }
            if (shulkerInventorySlot < 9) {
                shulkerHotbarSlot = shulkerInventorySlot;
                stage = Stage.FIND_PLACEMENT;
                return requestPause();
            }
            baritone.getInventoryBehavior().attemptToPutOnHotbar(shulkerInventorySlot, i -> false);
            stage = Stage.WAIT_FOR_HOTBAR;
            return requestPause();
        }

        private boolean ensureHotbarSlot() {
            if (shulkerHotbarSlot >= 0 && shulkerHotbarSlot < 9) {
                return true;
            }
            int located = locateHotbarShulker();
            if (located != -1) {
                shulkerHotbarSlot = located;
                return true;
            }
            return false;
        }

        private void findPlacement() {
            if (!ensureHotbarSlot()) {
                stage = Stage.WAIT_FOR_HOTBAR;
                return;
            }
            if (shulkerReference.isEmpty() || !(shulkerReference.getItem() instanceof BlockItem)) {
                fail("Shulker reference lost");
                return;
            }
            BlockState state = ((BlockItem) shulkerReference.getItem()).getBlock().defaultBlockState();
            Optional<Placement> candidate = findPlacementNearPlayer(state);
            if (!candidate.isPresent()) {
                fail("No safe location to deploy shulker");
                return;
            }
            Placement base = candidate.get();
            placement = new Placement(shulkerHotbarSlot >= 0 ? shulkerHotbarSlot : base.hotbarSelection,
                    base.placePos, base.placeAgainst, base.side, base.rot);
            stage = Stage.PLACE;
        }

        private PathingCommand placeShulker(boolean isSafeToCancel) {
            if (placement == null) {
                stage = Stage.FIND_PLACEMENT;
                return requestPause();
            }
            if (ctx.world().getBlockState(placement.placePos).getBlock() instanceof ShulkerBoxBlock) {
                stage = Stage.OPEN;
                return requestPause();
            }
            if (isSafeToCancel && ctx.player().onGround()) {
                Rotation rot = placement.rot;
                baritone.getLookBehavior().updateTarget(rot, true);
                ctx.player().getInventory().setSelectedSlot(placement.hotbarSelection);
                baritone.getInputOverrideHandler().setInputForceState(Input.SNEAK, true);
                if ((ctx.isLookingAt(placement.placeAgainst) && ((BlockHitResult) ctx.objectMouseOver()).getDirection().equals(placement.side))
                        || ctx.playerRotations().isReallyCloseTo(rot)) {
                    baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
                }
                return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
            }
            return new PathingCommand(new GoalBlock(placement.placePos), PathingCommandType.FORCE_REVALIDATE_GOAL_AND_PATH);
        }

        private PathingCommand openShulker(boolean isSafeToCancel) {
            if (!(ctx.world().getBlockState(placement.placePos).getBlock() instanceof ShulkerBoxBlock)) {
                stage = Stage.FIND_PLACEMENT;
                return requestPause();
            }
            if (ctx.player().containerMenu instanceof ShulkerBoxMenu) {
                stage = Stage.DEPOSIT;
                return requestPause();
            }
            Optional<Rotation> rot = RotationUtils.reachable(ctx, placement.placePos, ctx.playerController().getBlockReachDistance());
            if (rot.isPresent() && isSafeToCancel) {
                baritone.getLookBehavior().updateTarget(rot.get(), true);
                baritone.getInputOverrideHandler().setInputForceState(Input.SNEAK, true);
                if (ctx.isLookingAt(placement.placePos) || ctx.playerRotations().isReallyCloseTo(rot.get())) {
                    baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
                }
                return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
            }
            return new PathingCommand(new GoalBlock(placement.placePos.above()), PathingCommandType.FORCE_REVALIDATE_GOAL_AND_PATH);
        }

        private PathingCommand depositItems() {
            if (!(ctx.player().containerMenu instanceof ShulkerBoxMenu)) {
                stage = Stage.OPEN;
                return requestPause();
            }
            AbstractContainerMenu menu = ctx.player().containerMenu;
            boolean movedAny = false;
            for (int slotIndex = 27; slotIndex < menu.slots.size(); slotIndex++) {
                Slot slot = menu.getSlot(slotIndex);
                ItemStack stack = slot.getItem();
                if (stack.isEmpty() || !activeFilter.has(stack)) {
                    continue;
                }
                ctx.playerController().windowClick(menu.containerId, slotIndex, 0, ClickType.QUICK_MOVE, ctx.player());
                movedAny = true;
            }
            if (!movedAny) {
                logDebug("Shulker deposit had no matching stacks to move");
                stage = Stage.CLOSE;
                return requestPause();
            }
            stage = Stage.CLOSE;
            return requestPause();
        }

        private PathingCommand closeContainer() {
            if (ctx.player().containerMenu instanceof ShulkerBoxMenu) {
                ctx.player().closeContainer();
            }
            stage = Stage.BREAK;
            return requestPause();
        }

        private PathingCommand breakPlacedShulker(boolean isSafeToCancel) {
            BlockState state = ctx.world().getBlockState(placement.placePos);
            if (!(state.getBlock() instanceof ShulkerBoxBlock)) {
                stage = Stage.COMPLETE;
                return requestPause();
            }
            Optional<Rotation> rot = RotationUtils.reachable(ctx, placement.placePos, ctx.playerController().getBlockReachDistance());
            if (rot.isPresent() && isSafeToCancel) {
                baritone.getLookBehavior().updateTarget(rot.get(), true);
                MovementHelper.switchToBestToolFor(ctx, state);
                if (ctx.isLookingAt(placement.placePos) || ctx.playerRotations().isReallyCloseTo(rot.get())) {
                    baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_LEFT, true);
                }
                return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
            }
            return new PathingCommand(new GoalBlock(placement.placePos), PathingCommandType.FORCE_REVALIDATE_GOAL_AND_PATH);
        }

        private boolean isDepositShulker(ItemStack stack) {
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
                return false;
            }
            if (!(((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock)) {
                return false;
            }
            return shulkerHasCapacity(stack);
        }

        private boolean shulkerHasCapacity(ItemStack stack) {
            CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
            if (blockEntityData == null || !blockEntityData.contains("Items")) {
                return true;
            }
            if (registryOps == null) {
                return false;
            }
            ListTag items = blockEntityData.copyTag().getListOrEmpty("Items");
            if (items.isEmpty()) {
                return true;
            }
            if (items.size() < 27) {
                return true;
            }
            for (int i = 0; i < items.size(); i++) {
                CompoundTag entry = items.getCompoundOrEmpty(i);
                ItemStack contained = ItemStack.CODEC.parse(registryOps, entry)
                        .result()
                        .orElse(ItemStack.EMPTY);
                if (contained.isEmpty()) {
                    continue;
                }
                if (activeFilter.has(contained) && contained.getCount() < contained.getMaxStackSize()) {
                    return true;
                }
            }
            return false;
        }

        private int locateHotbarShulker() {
            if (shulkerReference.isEmpty()) {
                return -1;
            }
            for (int i = 0; i < 9; i++) {
                ItemStack stack = ctx.player().getInventory().getItem(i);
                if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, shulkerReference)) {
                    return i;
                }
            }
            return -1;
        }

        private Optional<Placement> findPlacementNearPlayer(BlockState state) {
            BlockStateInterface bsi = new BlockStateInterface(ctx);
            BetterBlockPos center = ctx.playerFeet();
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = -1; dy <= 2; dy++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        int x = center.x + dx;
                        int y = center.y + dy;
                        int z = center.z + dz;
                        BlockState current = bsi.get0(x, y, z);
                        if (!MovementHelper.isReplaceable(x, y, z, current, bsi)) {
                            continue;
                        }
                        Optional<Placement> candidate = possibleToPlace(state, x, y, z, bsi);
                        if (candidate.isPresent()) {
                            return candidate;
                        }
                    }
                }
            }
            return Optional.empty();
        }

        private Optional<Placement> possibleToPlace(BlockState toPlace, int x, int y, int z, BlockStateInterface bsi) {
            BetterBlockPos target = new BetterBlockPos(x, y, z);
            for (Direction against : Direction.values()) {
                BetterBlockPos placeAgainstPos = target.relative(against);
                BlockState placeAgainstState = bsi.get0(placeAgainstPos);
                if (MovementHelper.isReplaceable(placeAgainstPos.x, placeAgainstPos.y, placeAgainstPos.z, placeAgainstState, bsi)) {
                    continue;
                }
                if (!toPlace.canSurvive(ctx.world(), target)) {
                    continue;
                }
                if (!placementPlausible(target, toPlace)) {
                    continue;
                }
                VoxelShape shape = placeAgainstState.getShape(ctx.world(), placeAgainstPos);
                if (shape.isEmpty()) {
                    continue;
                }
                AABB bounds = shape.bounds();
                for (Vec3 placementMultiplier : aabbSideMultipliers(against)) {
                    double placeX = placeAgainstPos.x + bounds.minX * placementMultiplier.x + bounds.maxX * (1 - placementMultiplier.x);
                    double placeY = placeAgainstPos.y + bounds.minY * placementMultiplier.y + bounds.maxY * (1 - placementMultiplier.y);
                    double placeZ = placeAgainstPos.z + bounds.minZ * placementMultiplier.z + bounds.maxZ * (1 - placementMultiplier.z);
                    Rotation rot = RotationUtils.calcRotationFromVec3d(RayTraceUtils.inferSneakingEyePosition(ctx.player()), new Vec3(placeX, placeY, placeZ), ctx.playerRotations());
                    Rotation actualRot = baritone.getLookBehavior().getAimProcessor().peekRotation(rot);
                    HitResult result = RayTraceUtils.rayTraceTowards(ctx.player(), actualRot, ctx.playerController().getBlockReachDistance(), true);
                    if (result instanceof BlockHitResult) {
                        BlockHitResult hit = (BlockHitResult) result;
                        if (hit.getBlockPos().equals(placeAgainstPos) && hit.getDirection() == against.getOpposite()) {
                            return Optional.of(new Placement(shulkerHotbarSlot, target, placeAgainstPos, against.getOpposite(), rot));
                        }
                    }
                }
            }
            return Optional.empty();
        }

        private boolean placementPlausible(BlockPos pos, BlockState state) {
            VoxelShape voxelshape = state.getCollisionShape(ctx.world(), pos);
            return voxelshape.isEmpty() || ctx.world().isUnobstructed(null, voxelshape.move(pos.getX(), pos.getY(), pos.getZ()));
        }

        private Vec3[] aabbSideMultipliers(Direction side) {
            switch (side) {
                case UP:
                    return new Vec3[]{new Vec3(0.5, 1, 0.5), new Vec3(0.1, 1, 0.5), new Vec3(0.9, 1, 0.5), new Vec3(0.5, 1, 0.1), new Vec3(0.5, 1, 0.9)};
                case DOWN:
                    return new Vec3[]{new Vec3(0.5, 0, 0.5), new Vec3(0.1, 0, 0.5), new Vec3(0.9, 0, 0.5), new Vec3(0.5, 0, 0.1), new Vec3(0.5, 0, 0.9)};
                case EAST:
                    return new Vec3[]{new Vec3(1, 0.5, 0.5), new Vec3(1, 0.1, 0.5), new Vec3(1, 0.9, 0.5), new Vec3(1, 0.5, 0.1), new Vec3(1, 0.5, 0.9)};
                case WEST:
                    return new Vec3[]{new Vec3(0, 0.5, 0.5), new Vec3(0, 0.1, 0.5), new Vec3(0, 0.9, 0.5), new Vec3(0, 0.5, 0.1), new Vec3(0, 0.5, 0.9)};
                case SOUTH:
                    return new Vec3[]{new Vec3(0.5, 0.5, 1), new Vec3(0.1, 0.5, 1), new Vec3(0.9, 0.5, 1), new Vec3(0.5, 0.1, 1), new Vec3(0.5, 0.9, 1)};
                case NORTH:
                default:
                    return new Vec3[]{new Vec3(0.5, 0.5, 0), new Vec3(0.1, 0.5, 0), new Vec3(0.9, 0.5, 0), new Vec3(0.5, 0.1, 0), new Vec3(0.5, 0.9, 0)};
            }
        }

        private PathingCommand requestPause() {
            return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
        }

        private void fail(String reason) {
            logDebug("Shulker deposit failed: " + reason);
            stage = Stage.FAILED;
        }

        private final class Placement {

            private final int hotbarSelection;
            private final BetterBlockPos placePos;
            private final BlockPos placeAgainst;
            private final Direction side;
            private final Rotation rot;

            private Placement(int hotbarSelection, BetterBlockPos placePos, BlockPos placeAgainst, Direction side, Rotation rot) {
                this.hotbarSelection = hotbarSelection;
                this.placePos = placePos;
                this.placeAgainst = placeAgainst;
                this.side = side;
                this.rot = rot;
            }
        }

        private enum Stage {
            SEARCH,
            MOVE_TO_HOTBAR,
            WAIT_FOR_HOTBAR,
            FIND_PLACEMENT,
            PLACE,
            OPEN,
            DEPOSIT,
            CLOSE,
            BREAK,
            COMPLETE,
            FAILED
        }
    }


    private void updateLoucaSystem() {
        Map<BlockPos, Long> copy = new HashMap<>(anticipatedDrops);
        ctx.getSelectedBlock().ifPresent(pos -> {
            if (knownOreLocations.contains(pos)) {
                copy.put(pos, System.currentTimeMillis() + Baritone.settings().mineDropLoiterDurationMSThanksLouca.value);
            }
        });
        // elaborate dance to avoid concurrentmodificationexcepption since rescan thread reads this
        // don't want to slow everything down with a gross lock do we now
        for (BlockPos pos : anticipatedDrops.keySet()) {
            if (copy.get(pos) < System.currentTimeMillis()) {
                copy.remove(pos);
            }
        }
        anticipatedDrops = copy;
    }

    @Override
    public void onLostControl() {
        mine(0, (BlockOptionalMetaLookup) null);
    }

    @Override
    public String displayName0() {
        return "Mine " + filter;
    }

    private PathingCommand updateGoal() {
        BlockOptionalMetaLookup filter = filterFilter();
        if (filter == null) {
            return null;
        }

        boolean legit = Baritone.settings().legitMine.value;
        List<BlockPos> locs = knownOreLocations;
        if (!locs.isEmpty()) {
            CalculationContext context = new CalculationContext(baritone);
            List<BlockPos> locs2 = prune(context, new ArrayList<>(locs), filter, Baritone.settings().mineMaxOreLocationsCount.value, blacklist, droppedItemsScan());
            // can't reassign locs, gotta make a new var locs2, because we use it in a lambda right here, and variables you use in a lambda must be effectively final
            Goal goal = new GoalComposite(locs2.stream().map(loc -> coalesce(loc, locs2, context)).toArray(Goal[]::new));
            knownOreLocations = locs2;
            return new PathingCommand(goal, legit ? PathingCommandType.FORCE_REVALIDATE_GOAL_AND_PATH : PathingCommandType.REVALIDATE_GOAL_AND_PATH);
        }
        // we don't know any ore locations at the moment
        if (!legit && !Baritone.settings().exploreForBlocks.value) {
            return null;
        }
        // only when we should explore for blocks or are in legit mode we do this
        int y = Baritone.settings().legitMineYLevel.value;
        if (branchPoint == null) {
            /*if (!baritone.getPathingBehavior().isPathing() && playerFeet().y == y) {
                // cool, path is over and we are at desired y
                branchPoint = playerFeet();
                branchPointRunaway = null;
            } else {
                return new GoalYLevel(y);
            }*/
            branchPoint = ctx.playerFeet();
        }
        // TODO shaft mode, mine 1x1 shafts to either side
        // TODO also, see if the GoalRunAway with maintain Y at 11 works even from the surface
        if (branchPointRunaway == null) {
            branchPointRunaway = new GoalRunAway(1, y, branchPoint) {
                @Override
                public boolean isInGoal(int x, int y, int z) {
                    return false;
                }

                @Override
                public double heuristic() {
                    return Double.NEGATIVE_INFINITY;
                }
            };
        }
        return new PathingCommand(branchPointRunaway, PathingCommandType.REVALIDATE_GOAL_AND_PATH);
    }

    private void rescan(List<BlockPos> already, CalculationContext context) {
        BlockOptionalMetaLookup filter = filterFilter();
        if (filter == null) {
            return;
        }
        if (Baritone.settings().legitMine.value) {
            return;
        }
        List<BlockPos> dropped = droppedItemsScan();
        List<BlockPos> locs = searchWorld(context, filter, Baritone.settings().mineMaxOreLocationsCount.value, already, blacklist, dropped);
        locs.addAll(dropped);
        if (locs.isEmpty() && !Baritone.settings().exploreForBlocks.value) {
            logDirect("No locations for " + filter + " known, cancelling");
            if (Baritone.settings().notificationOnMineFail.value) {
                logNotification("No locations for " + filter + " known, cancelling", true);
            }
            cancel();
            return;
        }
        knownOreLocations = locs;
    }

    private boolean internalMiningGoal(BlockPos pos, CalculationContext context, List<BlockPos> locs) {
        // Here, BlockStateInterface is used because the position may be in a cached chunk (the targeted block is one that is kept track of)
        if (locs.contains(pos)) {
            return true;
        }
        BlockState state = context.bsi.get0(pos);
        if (Baritone.settings().internalMiningAirException.value && state.getBlock() instanceof AirBlock) {
            return true;
        }
        return filter.has(state) && plausibleToBreak(context, pos);
    }

    private Goal coalesce(BlockPos loc, List<BlockPos> locs, CalculationContext context) {
        boolean assumeVerticalShaftMine = !(baritone.bsi.get0(loc.above()).getBlock() instanceof FallingBlock);
        if (!Baritone.settings().forceInternalMining.value) {
            if (assumeVerticalShaftMine) {
                // we can get directly below the block
                return new GoalThreeBlocks(loc);
            } else {
                // we need to get feet or head into the block
                return new GoalTwoBlocks(loc);
            }
        }
        boolean upwardGoal = internalMiningGoal(loc.above(), context, locs);
        boolean downwardGoal = internalMiningGoal(loc.below(), context, locs);
        boolean doubleDownwardGoal = internalMiningGoal(loc.below(2), context, locs);
        if (upwardGoal == downwardGoal) { // symmetric
            if (doubleDownwardGoal && assumeVerticalShaftMine) {
                // we have a checkerboard like pattern
                // this one, and the one two below it
                // therefore it's fine to path to immediately below this one, since your feet will be in the doubleDownwardGoal
                // but only if assumeVerticalShaftMine
                return new GoalThreeBlocks(loc);
            } else {
                // this block has nothing interesting two below, but is symmetric vertically so we can get either feet or head into it
                return new GoalTwoBlocks(loc);
            }
        }
        if (upwardGoal) {
            // downwardGoal known to be false
            // ignore the gap then potential doubleDownward, because we want to path feet into this one and head into upwardGoal
            return new GoalBlock(loc);
        }
        // upwardGoal known to be false, downwardGoal known to be true
        if (doubleDownwardGoal && assumeVerticalShaftMine) {
            // this block and two below it are goals
            // path into the center of the one below, because that includes directly below this one
            return new GoalTwoBlocks(loc.below());
        }
        // upwardGoal false, downwardGoal true, doubleDownwardGoal false
        // just this block and the one immediately below, no others
        return new GoalBlock(loc.below());
    }

    private static class GoalThreeBlocks extends GoalTwoBlocks {

        public GoalThreeBlocks(BlockPos pos) {
            super(pos);
        }

        @Override
        public boolean isInGoal(int x, int y, int z) {
            return x == this.x && (y == this.y || y == this.y - 1 || y == this.y - 2) && z == this.z;
        }

        @Override
        public double heuristic(int x, int y, int z) {
            int xDiff = x - this.x;
            int yDiff = y - this.y;
            int zDiff = z - this.z;
            return GoalBlock.calculate(xDiff, yDiff < -1 ? yDiff + 2 : yDiff == -1 ? 0 : yDiff, zDiff);
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o);
        }

        @Override
        public int hashCode() {
            return super.hashCode() * 393857768;
        }

        @Override
        public String toString() {
            return String.format(
                    "GoalThreeBlocks{x=%s,y=%s,z=%s}",
                    SettingsUtil.maybeCensor(x),
                    SettingsUtil.maybeCensor(y),
                    SettingsUtil.maybeCensor(z)
            );
        }
    }

    public List<BlockPos> droppedItemsScan() {
        if (!Baritone.settings().mineScanDroppedItems.value) {
            return Collections.emptyList();
        }
        List<BlockPos> ret = new ArrayList<>();
        for (Entity entity : ((ClientLevel) ctx.world()).entitiesForRendering()) {
            if (entity instanceof ItemEntity) {
                ItemEntity ei = (ItemEntity) entity;
                if (filter.has(ei.getItem())) {
                    ret.add(entity.blockPosition());
                }
            }
        }
        ret.addAll(anticipatedDrops.keySet());
        return ret;
    }

    public static List<BlockPos> searchWorld(CalculationContext ctx, BlockOptionalMetaLookup filter, int max, List<BlockPos> alreadyKnown, List<BlockPos> blacklist, List<BlockPos> dropped) {
        List<BlockPos> locs = new ArrayList<>();
        List<Block> untracked = new ArrayList<>();
        for (BlockOptionalMeta bom : filter.blocks()) {
            Block block = bom.getBlock();
            if (CachedChunk.BLOCKS_TO_KEEP_TRACK_OF.contains(block)) {
                BetterBlockPos pf = ctx.baritone.getPlayerContext().playerFeet();

                // maxRegionDistanceSq 2 means adjacent directly or adjacent diagonally; nothing further than that
                locs.addAll(ctx.worldData.getCachedWorld().getLocationsOf(
                        BlockUtils.blockToString(block),
                        Baritone.settings().maxCachedWorldScanCount.value,
                        pf.x,
                        pf.z,
                        2
                ));
            } else {
                untracked.add(block);
            }
        }

        locs = prune(ctx, locs, filter, max, blacklist, dropped);

        if (!untracked.isEmpty() || (Baritone.settings().extendCacheOnThreshold.value && locs.size() < max)) {
            locs.addAll(BaritoneAPI.getProvider().getWorldScanner().scanChunkRadius(
                    ctx.getBaritone().getPlayerContext(),
                    filter,
                    max,
                    10,
                    32
            )); // maxSearchRadius is NOT sq
        }

        locs.addAll(alreadyKnown);

        return prune(ctx, locs, filter, max, blacklist, dropped);
    }

    private boolean addNearby() {
        List<BlockPos> dropped = droppedItemsScan();
        knownOreLocations.addAll(dropped);
        BlockPos playerFeet = ctx.playerFeet();
        BlockStateInterface bsi = new BlockStateInterface(ctx);


        BlockOptionalMetaLookup filter = filterFilter();
        if (filter == null) {
            return false;
        }

        int searchDist = 10;
        double fakedBlockReachDistance = 20; // at least 10 * sqrt(3) with some extra space to account for positioning within the block
        for (int x = playerFeet.getX() - searchDist; x <= playerFeet.getX() + searchDist; x++) {
            for (int y = playerFeet.getY() - searchDist; y <= playerFeet.getY() + searchDist; y++) {
                for (int z = playerFeet.getZ() - searchDist; z <= playerFeet.getZ() + searchDist; z++) {
                    // crucial to only add blocks we can see because otherwise this
                    // is an x-ray and it'll get caught
                    if (filter.has(bsi.get0(x, y, z))) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if ((Baritone.settings().legitMineIncludeDiagonals.value && knownOreLocations.stream().anyMatch(ore -> ore.distSqr(pos) <= 2 /* sq means this is pytha dist <= sqrt(2) */)) || RotationUtils.reachable(ctx, pos, fakedBlockReachDistance).isPresent()) {
                            knownOreLocations.add(pos);
                        }
                    }
                }
            }
        }
        knownOreLocations = prune(new CalculationContext(baritone), knownOreLocations, filter, Baritone.settings().mineMaxOreLocationsCount.value, blacklist, dropped);
        return true;
    }

    private static List<BlockPos> prune(CalculationContext ctx, List<BlockPos> locs2, BlockOptionalMetaLookup filter, int max, List<BlockPos> blacklist, List<BlockPos> dropped) {
        dropped.removeIf(drop -> {
            for (BlockPos pos : locs2) {
                if (pos.distSqr(drop) <= 9 && filter.has(ctx.get(pos.getX(), pos.getY(), pos.getZ())) && MineProcess.plausibleToBreak(ctx, pos)) { // TODO maybe drop also has to be supported? no lava below?
                    return true;
                }
            }
            return false;
        });
        List<BlockPos> locs = locs2
                .stream()
                .distinct()

                // remove any that are within loaded chunks that aren't actually what we want
                .filter(pos -> !ctx.bsi.worldContainsLoadedChunk(pos.getX(), pos.getZ()) || filter.has(ctx.get(pos.getX(), pos.getY(), pos.getZ())) || dropped.contains(pos))

                // remove any that are implausible to mine (encased in bedrock, or touching lava)
                .filter(pos -> MineProcess.plausibleToBreak(ctx, pos))

                .filter(pos -> {
                    if (Baritone.settings().allowOnlyExposedOres.value) {
                        return isNextToAir(ctx, pos);
                    } else {
                        return true;
                    }
                })

                .filter(pos -> pos.getY() >= Baritone.settings().minYLevelWhileMining.value + ctx.world.dimensionType().minY())

                .filter(pos -> pos.getY() <= Baritone.settings().maxYLevelWhileMining.value)

                .filter(pos -> !blacklist.contains(pos))

                .sorted(Comparator.comparingDouble(ctx.getBaritone().getPlayerContext().player().blockPosition()::distSqr))
                .collect(Collectors.toList());

        if (locs.size() > max) {
            return locs.subList(0, max);
        }
        return locs;
    }

    public static boolean isNextToAir(CalculationContext ctx, BlockPos pos) {
        int radius = Baritone.settings().allowOnlyExposedOresDistance.value;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) <= radius
                            && MovementHelper.isTransparent(ctx.getBlock(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    public static boolean plausibleToBreak(CalculationContext ctx, BlockPos pos) {
        BlockState state = ctx.bsi.get0(pos);
        if (MovementHelper.getMiningDurationTicks(ctx, pos.getX(), pos.getY(), pos.getZ(), state, true) >= COST_INF) {
            return false;
        }
        if (MovementHelper.avoidBreaking(ctx.bsi, pos.getX(), pos.getY(), pos.getZ(), state)) {
            return false;
        }

        // bedrock above and below makes it implausible, otherwise we're good
        return !(ctx.bsi.get0(pos.above()).getBlock() == Blocks.BEDROCK && ctx.bsi.get0(pos.below()).getBlock() == Blocks.BEDROCK);
    }

    @Override
    public void mineByName(int quantity, String... blocks) {
        mine(quantity, new BlockOptionalMetaLookup(blocks));
    }

    @Override
    public void mine(int quantity, BlockOptionalMetaLookup filter) {
        this.filter = filter;
        if (this.filterFilter() == null) {
            this.filter = null;
        }
        this.desiredQuantity = quantity;
        this.knownOreLocations = new ArrayList<>();
        this.blacklist = new ArrayList<>();
        this.branchPoint = null;
        this.branchPointRunaway = null;
        this.anticipatedDrops = new HashMap<>();
        this.shulkerDepositHandler = null;
        this.shulkerDepositCooldownUntil = 0L;
        if (filter != null) {
            rescan(new ArrayList<>(), new CalculationContext(baritone));
        }
    }

    private BlockOptionalMetaLookup filterFilter() {
        if (this.filter == null) {
            return null;
        }
        if (!Baritone.settings().allowBreak.value) {
            BlockOptionalMetaLookup f = new BlockOptionalMetaLookup(this.filter.blocks()
                    .stream()
                    .filter(e -> Baritone.settings().allowBreakAnyway.value.contains(e.getBlock()))
                    .toArray(BlockOptionalMeta[]::new));
            if (f.blocks().isEmpty()) {
                logDirect("Unable to mine when allowBreak is false and target block is not in allowBreakAnyway!");
                return null;
            }
            return f;
        }
        return filter;
    }
}
