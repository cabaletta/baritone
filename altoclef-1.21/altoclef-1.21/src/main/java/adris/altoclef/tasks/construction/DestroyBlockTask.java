package adris.altoclef.tasks.construction;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.movement.RunAwayFromPositionTask;
import adris.altoclef.tasks.movement.SafeRandomShimmyTask;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.utils.Rotation;
import baritone.api.utils.input.Input;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;
import java.util.Optional;

/**
 * Destroy a block at a position.
 */
public class DestroyBlockTask extends Task implements ITaskRequiresGrounded {
    private final MovementProgressChecker stuckCheck = new MovementProgressChecker();
    private final MovementProgressChecker _moveChecker = new MovementProgressChecker();
    private final BlockPos _pos;
    Block[] annoyingBlocks = new Block[]{
            Blocks.VINE,
            Blocks.NETHER_SPROUTS,
            Blocks.CAVE_VINES,
            Blocks.CAVE_VINES_PLANT,
            Blocks.TWISTING_VINES,
            Blocks.TWISTING_VINES_PLANT,
            Blocks.WEEPING_VINES_PLANT,
            Blocks.LADDER,
            Blocks.BIG_DRIPLEAF,
            Blocks.BIG_DRIPLEAF_STEM,
            Blocks.SMALL_DRIPLEAF,
            Blocks.TALL_GRASS,
            Blocks.GRASS_BLOCK,
            Blocks.SWEET_BERRY_BUSH
    };
    private Task _unstuckTask = null;
    private boolean isMining;

    public DestroyBlockTask(BlockPos pos) {
        _pos = pos;
    }

    /**
     * Generates the surrounding BlockPos based on the given position.
     *
     * @param pos The center BlockPos
     * @return An array of surrounding BlockPos
     */
    private static BlockPos[] generateSides(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        return new BlockPos[]{
                new BlockPos(x + 1, y, z),
                new BlockPos(x - 1, y, z),
                new BlockPos(x, y, z + 1),
                new BlockPos(x, y, z - 1),
                new BlockPos(x + 1, y, z - 1),
                new BlockPos(x + 1, y, z + 1),
                new BlockPos(x - 1, y, z - 1),
                new BlockPos(x - 1, y, z + 1)
        };
    }

    /**
     * Checks if the block at the specified position is annoying.
     *
     * @param mod the AltoClef instance
     * @param pos the position to check
     * @return true if the block is annoying, false otherwise
     */
    private boolean isAnnoying(AltoClef mod, BlockPos pos) {
        for (Block annoyingBlock : annoyingBlocks) {
            boolean isAnnoying = mod.getWorld().getBlockState(pos).getBlock() == annoyingBlock
                    || mod.getWorld().getBlockState(pos).getBlock() instanceof DoorBlock
                    || mod.getWorld().getBlockState(pos).getBlock() instanceof FenceBlock
                    || mod.getWorld().getBlockState(pos).getBlock() instanceof FenceGateBlock
                    || mod.getWorld().getBlockState(pos).getBlock() instanceof FlowerBlock;
            if (isAnnoying) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the position where the player is stuck in a block.
     *
     * @param mod the mod instance
     * @return the position where the player is stuck, or null if not stuck
     */
    private BlockPos stuckInBlock(AltoClef mod) {
        // Check if player is stuck in their current position
        if (isAnnoying(mod, mod.getPlayer().getBlockPos())) {
            return mod.getPlayer().getBlockPos();
        }

        // Check if player is stuck when moving up
        if (isAnnoying(mod, mod.getPlayer().getBlockPos().up())) {
            return mod.getPlayer().getBlockPos().up();
        }

        // Check for stuck positions in the sides of the player's current position
        for (BlockPos check : generateSides(mod.getPlayer().getBlockPos())) {
            if (isAnnoying(mod, check)) {
                return check;
            }
        }

        // Check for stuck positions in the sides of the player's position when moving up
        for (BlockPos check : generateSides(mod.getPlayer().getBlockPos().up())) {
            if (isAnnoying(mod, check)) {
                return check;
            }
        }

        return null; // Player is not stuck
    }

    /**
     * Gets a task to unstick a fence.
     *
     * @return the task to unstick the fence, or null if an exception is caught
     */
    private Task getFenceUnstuckTask() {
        try {
            // Create a safe random shimmy task
            Task task = createSafeRandomShimmyTask();

            // Return the task
            return task;
        } catch (Exception e) {
            e.printStackTrace();
            // Handle the exception or rethrow it as needed
            return null;
        }
    }

    /**
     * Creates and returns a new instance of SafeRandomShimmyTask.
     *
     * @return a new SafeRandomShimmyTask instance
     */
    private Task createSafeRandomShimmyTask() {
        return new SafeRandomShimmyTask();
    }

    /**
     * This method is called when the AltoClef mod starts.
     * It cancels any ongoing pathing behavior, resets move checker and stuck check,
     * and handles the item stack in the cursor slot.
     * If the cursor stack is not empty, it calls handleNonEmptyCursorStack,
     * otherwise, it closes the screen.
     */
    @Override
    protected void onStart(AltoClef mod) {
        // Cancel any ongoing pathing behavior.
        mod.getClientBaritone().getPathingBehavior().forceCancel();

        // Reset move checker and stuck check.
        _moveChecker.reset();
        stuckCheck.reset();

        // Get the item stack in the cursor slot.
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();

        // If the cursor stack is not empty, handle it.
        if (!cursorStack.isEmpty()) {
            handleNonEmptyCursorStack(mod, cursorStack);
        } else {
            // If the cursor stack is empty, close the screen.
            StorageHelper.closeScreen();
        }
    }

    /**
     * Handles the non-empty cursor stack by performing various actions.
     * @param mod the AltoClef mod
     * @param cursorStack the cursor stack
     */
    private void handleNonEmptyCursorStack(AltoClef mod, ItemStack cursorStack) {
        // Get the slot that can fit the cursor stack in the player inventory and click it to pick up the item.
        mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false)
                .ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));

        // If the cursor stack can be thrown away, click an undefined slot to pick up the item.
        if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
        }

        // Get the garbage slot and click it to pick up the item.
        StorageHelper.getGarbageSlot(mod)
                .ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));

        // Click an undefined slot to pick up the item.
        mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Check if there is white wool at the specified position
        if (mod.getWorld().getBlockState(_pos).getBlock() == Blocks.WHITE_WOOL) {
            // Iterate over all entities in the world
            Iterable<Entity> entities = mod.getWorld().getEntities();
            for (Entity entity : entities) {
                // Check if the entity is a PillagerEntity and is within a distance of 144 blocks from the position
                if (entity instanceof PillagerEntity && _pos.isWithinDistance(entity.getPos(), 144)) {
                    // Request the block at the position to be marked as unreachable
                    mod.getBlockTracker().requestBlockUnreachable(_pos, 0);
                }
            }
        }

        // Reset the move checker if Baritone is currently pathing
        if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
            _moveChecker.reset();
        }

        // Check if the player is in a Nether portal
        if (WorldHelper.isInNetherPortal(mod)) {
            if (!mod.getClientBaritone().getPathingBehavior().isPathing()) {
                setDebugState("Getting out from nether portal");
                // Hold the sneak and move forward inputs to exit the Nether portal
                mod.getInputControls().hold(Input.SNEAK);
                mod.getInputControls().hold(Input.MOVE_FORWARD);
                return null;
            } else {
                mod.getInputControls().release(Input.SNEAK);
                mod.getInputControls().release(Input.MOVE_BACK);
                mod.getInputControls().release(Input.MOVE_FORWARD);
            }
        } else if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
            mod.getInputControls().release(Input.SNEAK);
            mod.getInputControls().release(Input.MOVE_BACK);
            mod.getInputControls().release(Input.MOVE_FORWARD);
        }

        // Check if there is an active unstuck task and the player is stuck in a block
        if (_unstuckTask != null && _unstuckTask.isActive() && !_unstuckTask.isFinished(mod) && stuckInBlock(mod) != null) {
            setDebugState("Getting unstuck from block.");
            stuckCheck.reset();
            // Release control of Baritone's custom goal process and explore process
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
            mod.getClientBaritone().getExploreProcess().onLostControl();
            return _unstuckTask;
        }

        // Check if the move checker or the stuck check failed
        if (!_moveChecker.check(mod) || !stuckCheck.check(mod)) {
            BlockPos blockStuck = stuckInBlock(mod);
            if (blockStuck != null) {
                _unstuckTask = getFenceUnstuckTask();
                return _unstuckTask;
            }
            stuckCheck.reset();
        }

        // Check if the move checker failed
        if (!_moveChecker.check(mod)) {
            _moveChecker.reset();
            // Request the block at the position to be marked as unreachable
            mod.getBlockTracker().requestBlockUnreachable(_pos);
        }

        // Check if the block above the position is not solid, the player is above the position,
        // and the player is within a distance of 0.89 blocks from the position
        if (!WorldHelper.isSolid(mod, _pos.up()) && mod.getPlayer().getPos().y > _pos.getY() && _pos.isWithinDistance(mod.getPlayer().isOnGround() ? mod.getPlayer().getPos() : mod.getPlayer().getPos().add(0, -1, 0), 0.89)) {
            if (WorldHelper.dangerousToBreakIfRightAbove(mod, _pos)) {
                setDebugState("It's dangerous to break as we're right above it, moving away and trying again.");
                return new RunAwayFromPositionTask(3, _pos.getY(), _pos);
            }
        }

        Optional<Rotation> reach = LookHelper.getReach(_pos);
        if (reach.isPresent() && (mod.getPlayer().isTouchingWater() || mod.getPlayer().isOnGround())
                && !mod.getFoodChain().needsToEat() && !WorldHelper.isInNetherPortal(mod)
                && mod.getClientBaritone().getPathingBehavior().isSafeToCancel()) {
            setDebugState("Block in range, mining...");
            stuckCheck.reset();
            isMining = true;
            mod.getInputControls().release(Input.SNEAK);
            mod.getInputControls().release(Input.MOVE_BACK);
            mod.getInputControls().release(Input.MOVE_FORWARD);
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
            mod.getClientBaritone().getBuilderProcess().onLostControl();
            if (!LookHelper.isLookingAt(mod, reach.get())) {
                LookHelper.lookAt(mod, reach.get());
            }
            BlockState state = mod.getWorld().getBlockState(_pos);
            Optional<Slot> bestToolSlot = StorageHelper.getBestToolSlot(mod, state);
            Slot currentEquipped = PlayerSlot.getEquipSlot();
            // if baritone is running, only accept tools OUTSIDE OF HOTBAR!
            // Baritone will take care of tools inside the hotbar.
            if (bestToolSlot.isPresent() && bestToolSlot.get() != currentEquipped) {
                // ONLY equip if the item class is STRICTLY different (otherwise we swap around a lot)
                if (StorageHelper.getItemStackInSlot(currentEquipped).getItem() != StorageHelper.getItemStackInSlot(bestToolSlot.get()).getItem()) {
                    boolean isAllowedToManage = !mod.getClientBaritone().getPathingBehavior().isPathing()
                            && !mod.getFoodChain().isTryingToEat();
                    if (isAllowedToManage) {
                        Debug.logMessage("Found better tool in inventory, equipping.");
                        ItemStack bestToolItemStack = StorageHelper.getItemStackInSlot(bestToolSlot.get());
                        Item bestToolItem = bestToolItemStack.getItem();
                        mod.getSlotHandler().forceEquipItem(bestToolItem);
                    }
                    return null;
                }
            }
            mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_LEFT, true);
        } else {
            setDebugState("Getting to block...");
            if (isMining && mod.getPlayer().isTouchingWater()) {
                isMining = false;
                mod.getBlockTracker().requestBlockUnreachable(_pos);
            } else {
                isMining = false;
            }
            boolean isCloseToMoveBack = _pos.isWithinDistance(mod.getPlayer().getPos(), 2);
            if (isCloseToMoveBack) {
                if (!mod.getClientBaritone().getPathingBehavior().isPathing() && !mod.getPlayer().isTouchingWater() &&
                        !mod.getFoodChain().needsToEat()) {
                    mod.getInputControls().hold(Input.MOVE_BACK);
                    mod.getInputControls().hold(Input.SNEAK);
                } else {
                    mod.getInputControls().release(Input.MOVE_BACK);
                    mod.getInputControls().release(Input.SNEAK);
                }
            }
            if (!mod.getClientBaritone().getCustomGoalProcess().isActive()) {
                mod.getClientBaritone().getBuilderProcess().onLostControl();
                mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(mod.getWorld().getBlockState(_pos.up()).getBlock() ==
                        Blocks.SNOW ? new GoalBlock(_pos) : new GoalNear(_pos, 1));
            }
        }
        return null;
    }

    /**
     * This method is called when the task is interrupted.
     * It cancels Baritone pathing and releases input controls if in game.
     *
     * @param mod The AltoClef mod instance
     * @param interruptTask The interrupting task
     */
    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        // Cancel Baritone pathing
        mod.getClientBaritone().getPathingBehavior().forceCancel();

        // If not in game, return
        if (!AltoClef.inGame()) {
            return;
        }

        // Release input controls
        mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_LEFT, false);
        mod.getInputControls().release(Input.SNEAK);
        mod.getInputControls().release(Input.MOVE_BACK);
        mod.getInputControls().release(Input.MOVE_FORWARD);
    }

    /**
     * Check if the specified position is finished.
     *
     * @param mod The AltoClef instance
     * @return True if the block at the specified position is air, false otherwise.
     */
    @Override
    public boolean isFinished(AltoClef mod) {
        // Check if the world or position is null
        if (mod.getWorld() == null || _pos == null) {
            return false;
        }
        // Get the block state at the specified position and check if it's air
        BlockState blockState = mod.getWorld().getBlockState(_pos);
        return blockState.isAir();
    }

    /**
     * Overrides the isEqual method to compare with another Task object.
     *
     * @param other The other Task object to compare with.
     * @return true if the other object is a DestroyBlockTask and has the same position, false otherwise.
     */
    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof DestroyBlockTask destroyBlockTask) {
            return Objects.equals(destroyBlockTask._pos, _pos);
        }
        return false;
    }

    /**
     * Returns a debug string describing the block destruction action.
     * If the position is known, it includes the position in the string, otherwise it indicates an unknown position.
     */
    @Override
    protected String toDebugString() {
        if (_pos != null) {
            return "Destroy block at " + _pos.toShortString();
        } else {
            return "Destroy block at unknown position";
        }
    }
}
