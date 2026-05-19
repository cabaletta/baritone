package adris.altoclef.tasks.speedrun;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.GetToXZTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.WorldHelper;
import baritone.api.utils.input.Input;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.Phase;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class KillEnderDragonWithBedsTask extends Task {
    private final Task _whenNotPerchingTask;

    private BlockPos _endPortalTop;
    private Task _positionTask;

    private static boolean isDragonDead;
    private static boolean isDragonPresent;

    public KillEnderDragonWithBedsTask(IDragonWaiter notPerchingOverride) {
        _whenNotPerchingTask = (Task) notPerchingOverride;
    }

    private static BlockPos locateExitPortalTop(AltoClef mod) {
        if (!mod.getChunkTracker().isChunkLoaded(new BlockPos(0, 64, 0))) return null;
        int height = WorldHelper.getGroundHeight(mod, 0, 0, Blocks.BEDROCK);
        if (height != -1) return new BlockPos(0, height, 0);
        return null;
    }

    @Override
    protected void onStart(AltoClef mod) {
        isDragonDead = false;
        isDragonPresent = false;
    }

    @Override
    protected Task onTick(AltoClef mod) {
        /*
            If dragon is perching:
                If we're not in position (XZ):
                    Get in position (XZ)
                If there's no bed:
                    If we can't "reach" the top of the pillar:
                        Jump
                    Place a bed
                If the dragon's head hitbox is close enough to the bed:
                    Right click the bed
            Else:
                // Perform "Default Wander" mode and avoid dragon breath.
         */
        Optional<Entity> dragon = mod.getEntityTracker().getClosestEntity(EnderDragonEntity.class);
        if (dragon.isEmpty() && !isDragonPresent) {
            setDebugState("Waiting for dragon to spawn.");
            return null;
        }
        if (!isDragonPresent) {
            isDragonPresent = true;
        }
        if (_endPortalTop == null) {
            _endPortalTop = locateExitPortalTop(mod);
            if (_endPortalTop != null) {
                ((IDragonWaiter) _whenNotPerchingTask).setExitPortalTop(_endPortalTop);
            }
        }

        if (_endPortalTop == null) {
            setDebugState("Searching for end portal top.");
            return new GetToXZTask(0, 0);
        }

        if (isDragonDead) {
            setDebugState("Waiting for overworld portal to spawn.");
            if (mod.getPlayer().getPitch() != -90) {
                mod.getPlayer().setPitch(-90);
            }
            return null;
        }

        if (!mod.getEntityTracker().entityFound(EnderDragonEntity.class)) {
            setDebugState("No dragon found.");

            if (!WorldHelper.inRangeXZ(mod.getPlayer(), _endPortalTop, 0.25)) {
                setDebugState("Going to end portal top at " + _endPortalTop.toString() + ".");
                return new GetToXZTask(_endPortalTop.getX(), _endPortalTop.getZ());
            }
            isDragonDead = true;
        }
        if (dragon.isPresent()) {
            EnderDragonEntity dragonEntity = (EnderDragonEntity) dragon.get();
            Phase dragonPhase = dragonEntity.getPhaseManager().getCurrent();

            boolean perching = dragonPhase.getType() == PhaseType.LANDING || dragonPhase.isSittingOrHovering() || dragonPhase.getType() == PhaseType.LANDING_APPROACH;
            if (dragonEntity.getY() < _endPortalTop.getY() + 2) {
                // Dragon is already perched.
                perching = false;
            }
            ((IDragonWaiter) _whenNotPerchingTask).setPerchState(perching);
            // When the dragon is not perching...
            if (_whenNotPerchingTask.isActive() && !_whenNotPerchingTask.isFinished(mod)) {
                setDebugState("Dragon not perching, performing special behavior...");
                return _whenNotPerchingTask;
            }
            if (perching) {
                mod.getFoodChain().shouldStop(true);
                BlockPos targetStandPosition = _endPortalTop.add(-1, -1, 0);
                BlockPos playerPosition = mod.getPlayer().getBlockPos();
                // If we're not positioned (above is OK), go there and make sure we're at the right height.
                if (_positionTask != null && _positionTask.isActive() && !_positionTask.isFinished(mod)) {
                    setDebugState("Going to position for bed cycle...");
                    return _positionTask;
                }
                if ((!WorldHelper.inRangeXZ(WorldHelper.toVec3d(targetStandPosition), mod.getPlayer().getPos(), 0.50))
//                            && mod.getPlayer().getVelocity().getX() == 0 && mod.getPlayer().getVelocity().getY() == 0 && mod.getPlayer().getVelocity().getZ() == 0
                ) {
                    _positionTask = new GetToBlockTask(targetStandPosition);
                    Debug.logMessage("Going to position for bed cycle...");
                    setDebugState("Moving to target stand position");
                    return _positionTask;
                }
                // We're positioned. Perform bed strats!
                BlockPos bedTargetPosition = _endPortalTop.up();
                boolean bedPlaced = mod.getBlockTracker().blockIsValid(bedTargetPosition, ItemHelper.itemsToBlocks(ItemHelper.BED));
                if (!bedPlaced) {
                    setDebugState("Placing bed");
                    // If no bed, place bed.
                    // Fire messes up our "reach" so we just assume we're good when we're above a height.
                    boolean canPlace = LookHelper.getCameraPos(mod).y > bedTargetPosition.getY();
                    //Optional<Rotation> placeReach = LookHelper.getReach(bedTargetPosition.down(), Direction.UP);
                    if (canPlace) {
                        // Look at and place!
                        if (mod.getSlotHandler().forceEquipItem(ItemHelper.BED, true)) {
                            LookHelper.lookAt(mod, bedTargetPosition.down(), Direction.UP, true);
                            //mod.getClientBaritone().getLookBehavior().updateTarget(placeReach.get(), true);
                            //if (mod.getClientBaritone().getPlayerContext().isLookingAt(bedTargetPosition.down())) {
                            // There could be fire so eh place right away
                            mod.getInputControls().tryPress(Input.CLICK_RIGHT);
                            //}
                        }
                    } else {
                        if (mod.getPlayer().isOnGround()) {
                            // Jump
                            mod.getInputControls().tryPress(Input.JUMP);
                        }
                    }
                } else {
                    setDebugState("Wait for it...");
                    // Make sure we're standing on the ground so we don't blow ourselves up lmfao
                    if (!mod.getPlayer().isOnGround()) {
                        // Wait to fall
                        return null;
                    }
                    // Wait for dragon head to be close enough to the bed's head...
                    BlockPos bedfoot = WorldHelper.getBedFoot(mod, bedTargetPosition);
                    assert bedfoot != null;
                    Vec3d headPos = dragonEntity.head.getBoundingBox().getCenter(); // dragon.head.getPos();
                    double dist = headPos.distanceTo(WorldHelper.toVec3d(bedfoot));
                    Debug.logMessage("Dist: " + dist + " Health: " + dragonEntity.getHealth());

                    if (dist < BeatMinecraft2Task.getConfig().dragonHeadCloseEnoughClickBedRange) {
                        // Interact with the bed.
                        return new InteractWithBlockTask(bedTargetPosition);
                    }
                    // Wait for it...
                }
                return null;
            }
        }
        mod.getFoodChain().shouldStop(false);
        // Start our "Not perching task"
        return _whenNotPerchingTask;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getFoodChain().shouldStop(false);
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return super.isFinished(mod);
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof KillEnderDragonWithBedsTask;
    }

    @Override
    protected String toDebugString() {
        return "Bedding the Ender Dragon";
    }
}