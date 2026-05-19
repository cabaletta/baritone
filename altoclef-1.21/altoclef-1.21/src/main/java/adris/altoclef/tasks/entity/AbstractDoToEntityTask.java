package adris.altoclef.tasks.entity;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.movement.GetToEntityTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import adris.altoclef.util.slots.Slot;
import baritone.api.pathing.goals.GoalRunAway;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.Optional;

/**
 * Interacts with an entity while maintaining distance.
 * <p>
 * The interaction is abstract.
 */
public abstract class AbstractDoToEntityTask extends Task implements ITaskRequiresGrounded {
    protected final MovementProgressChecker _progress = new MovementProgressChecker();
    private final double _maintainDistance;
    private final double _combatGuardLowerRange;
    private final double _combatGuardLowerFieldRadius;

    public AbstractDoToEntityTask(double maintainDistance, double combatGuardLowerRange, double combatGuardLowerFieldRadius) {
        _maintainDistance = maintainDistance;
        _combatGuardLowerRange = combatGuardLowerRange;
        _combatGuardLowerFieldRadius = combatGuardLowerFieldRadius;
    }

    public AbstractDoToEntityTask(double maintainDistance) {
        this(maintainDistance, 0, Double.POSITIVE_INFINITY);
    }

    public AbstractDoToEntityTask(double combatGuardLowerRange, double combatGuardLowerFieldRadius) {
        this(-1, combatGuardLowerRange, combatGuardLowerFieldRadius);
    }

    @Override
    protected void onStart(AltoClef mod) {
        _progress.reset();
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
        if (!cursorStack.isEmpty()) {
            Optional<Slot> moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
            moveTo.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));
            if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            }
            Optional<Slot> garbage = StorageHelper.getGarbageSlot(mod);
            // Try throwing away cursor slot if it's garbage
            garbage.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));
            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
        } else {
            StorageHelper.closeScreen();
        } // Kinda duct tape but it should be future proof ish
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
            _progress.reset();
        }

        Optional<Entity> checkEntity = getEntityTarget(mod);


        // Oof
        if (checkEntity.isEmpty()) {
            mod.getMobDefenseChain().resetTargetEntity();
            mod.getMobDefenseChain().resetForceField();
        } else {
            mod.getMobDefenseChain().setTargetEntity(checkEntity.get());
        }
        if (checkEntity.isPresent()) {
            Entity entity = checkEntity.get();

            double playerReach = mod.getModSettings().getEntityReachRange();

            // TODO: This is basically useless.
            EntityHitResult result = LookHelper.raycast(mod.getPlayer(), entity, playerReach);

            double sqDist = entity.squaredDistanceTo(mod.getPlayer());

            if (sqDist < _combatGuardLowerRange * _combatGuardLowerRange) {
                mod.getMobDefenseChain().setForceFieldRange(_combatGuardLowerFieldRadius);
            } else {
                mod.getMobDefenseChain().resetForceField();
            }

            // If we don't specify a maintain distance, default to within 1 block of our reach.
            double maintainDistance = _maintainDistance >= 0 ? _maintainDistance : playerReach - 1;

            boolean tooClose = sqDist < maintainDistance * maintainDistance;

            // Step away if we're too close
            if (tooClose) {
                //setDebugState("Maintaining distance");
                if (!mod.getClientBaritone().getCustomGoalProcess().isActive()) {
                    mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(new GoalRunAway(maintainDistance, entity.getBlockPos()));
                }
            }

            if (mod.getControllerExtras().inRange(entity) && result != null &&
                    result.getType() == HitResult.Type.ENTITY && !mod.getFoodChain().needsToEat() &&
                    !mod.getMLGBucketChain().isFallingOhNo(mod) && mod.getMLGBucketChain().doneMLG() &&
                    !mod.getMLGBucketChain().isChorusFruiting() &&
                    mod.getClientBaritone().getPathingBehavior().isSafeToCancel() &&
                    mod.getPlayer().isOnGround()) {
                _progress.reset();
                return onEntityInteract(mod, entity);
            } else if (!tooClose) {
                setDebugState("Approaching target");
                if (!_progress.check(mod)) {
                    _progress.reset();
                    Debug.logMessage("Failed to get to target, blacklisting.");
                    mod.getEntityTracker().requestEntityUnreachable(entity);
                }
                // Move to target
                return new GetToEntityTask(entity, maintainDistance);
            }
        }
        if (!mod.getClientBaritone().getPathingBehavior().isSafeToCancel()) {
            return null;
        }
        return new TimeoutWanderTask();
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof AbstractDoToEntityTask task) {
            if (!doubleCheck(task._maintainDistance, _maintainDistance)) return false;
            if (!doubleCheck(task._combatGuardLowerFieldRadius, _combatGuardLowerFieldRadius)) return false;
            if (!doubleCheck(task._combatGuardLowerRange, _combatGuardLowerRange)) return false;
            return isSubEqual(task);
        }
        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean doubleCheck(double a, double b) {
        if (Double.isInfinite(a) == Double.isInfinite(b)) return true;
        return Math.abs(a - b) < 0.1;
    }

    protected abstract boolean isSubEqual(AbstractDoToEntityTask other);

    protected abstract Task onEntityInteract(AltoClef mod, Entity entity);

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getMobDefenseChain().setTargetEntity(null);
        mod.getMobDefenseChain().resetForceField();
    }

    protected abstract Optional<Entity> getEntityTarget(AltoClef mod);

}
