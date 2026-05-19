package adris.altoclef.chains;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.construction.PutOutFireTask;
import adris.altoclef.tasks.movement.EnterNetherPortalTask;
import adris.altoclef.tasks.movement.EscapeFromLavaTask;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.SafeRandomShimmyTask;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.time.TimerGame;
import baritone.api.utils.Rotation;
import baritone.api.utils.input.Input;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Optional;

public class WorldSurvivalChain extends SingleTaskChain {

    private final TimerGame _wasInLavaTimer = new TimerGame(1);
    private boolean _wasAvoidingDrowning;
    private TimerGame _portalStuckTimer = new TimerGame(5);

    private BlockPos _extinguishWaterPosition;

    public WorldSurvivalChain(TaskRunner runner) {
        super(runner);
    }

    @Override
    protected void onTaskFinish(AltoClef mod) {

    }

    @Override
    public float getPriority(AltoClef mod) {
        if (!AltoClef.inGame()) return Float.NEGATIVE_INFINITY;

        // Drowning
        handleDrowning(mod);

        // Lava Escape
        if (isInLavaOhShit(mod) && mod.getBehaviour().shouldEscapeLava()) {
            setTask(new EscapeFromLavaTask());
            return 100;
        }

        // Fire escape
        if (isInFire(mod)) {
            setTask(new DoToClosestBlockTask(PutOutFireTask::new, Blocks.FIRE, Blocks.SOUL_FIRE));
            return 100;
        }

        // Extinguish with water
        if (mod.getModSettings().shouldExtinguishSelfWithWater()) {
            if (!(_mainTask instanceof EscapeFromLavaTask && isCurrentlyRunning(mod)) && mod.getPlayer().isOnFire() && !mod.getPlayer().hasStatusEffect(StatusEffects.FIRE_RESISTANCE) && !mod.getWorld().getDimension().ultrawarm()) {
                // Extinguish ourselves
                if (mod.getItemStorage().hasItem(Items.WATER_BUCKET)) {
                    BlockPos targetWaterPos = mod.getPlayer().getBlockPos();
                    if (WorldHelper.isSolid(mod, targetWaterPos.down()) && WorldHelper.canPlace(mod, targetWaterPos)) {
                        Optional<Rotation> reach = LookHelper.getReach(targetWaterPos.down(), Direction.UP);
                        if (reach.isPresent()) {
                            mod.getClientBaritone().getLookBehavior().updateTarget(reach.get(), true);
                            if (mod.getClientBaritone().getPlayerContext().isLookingAt(targetWaterPos.down())) {
                                if (mod.getSlotHandler().forceEquipItem(Items.WATER_BUCKET)) {
                                    _extinguishWaterPosition = targetWaterPos;
                                    mod.getInputControls().tryPress(Input.CLICK_RIGHT);
                                    setTask(null);
                                    return 90;
                                }
                            }
                        }
                    }
                }
                setTask(new DoToClosestBlockTask(GetToBlockTask::new, Blocks.WATER));
                return 90;
            } else if (mod.getItemStorage().hasItem(Items.BUCKET) && _extinguishWaterPosition != null && mod.getBlockTracker().blockIsValid(_extinguishWaterPosition, Blocks.WATER)) {
                // Pick up the water
                setTask(new InteractWithBlockTask(new ItemTarget(Items.BUCKET, 1), Direction.UP, _extinguishWaterPosition.down(), true));
                return 60;
            } else {
                _extinguishWaterPosition = null;
            }
        }

        // Portal stuck
        if (isStuckInNetherPortal(mod)) {
            // We can't break or place while inside a portal (not really)
            mod.getExtraBaritoneSettings().setInteractionPaused(true);
        } else {
            // We're no longer stuck, but we might want to move AWAY from our stuck position.
            _portalStuckTimer.reset();
            mod.getExtraBaritoneSettings().setInteractionPaused(false);
        }
        if (_portalStuckTimer.elapsed()) {
            // We're stuck inside a portal, so get out.
            // Don't allow breaking while we're inside the portal.
            setTask(new SafeRandomShimmyTask());
            return 60;
        }

        return Float.NEGATIVE_INFINITY;
    }

    private void handleDrowning(AltoClef mod) {
        // Swim
        boolean avoidedDrowning = false;
        if (mod.getModSettings().shouldAvoidDrowning()) {
            if (!mod.getClientBaritone().getPathingBehavior().isPathing()) {
                if (mod.getPlayer().isTouchingWater() && mod.getPlayer().getAir() < mod.getPlayer().getMaxAir()) {
                    // Swim up!
                    mod.getInputControls().hold(Input.JUMP);
                    //mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.JUMP, true);
                    avoidedDrowning = true;
                    _wasAvoidingDrowning = true;
                }
            }
        }
        // Stop swimming up if we just swam.
        if (_wasAvoidingDrowning && !avoidedDrowning) {
            _wasAvoidingDrowning = false;
            mod.getInputControls().release(Input.JUMP);
            //mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.JUMP, false);
        }
    }

    private boolean isInLavaOhShit(AltoClef mod) {
        if (mod.getPlayer().isInLava() && !mod.getPlayer().hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
            _wasInLavaTimer.reset();
            return true;
        }
        return mod.getPlayer().isOnFire() && !_wasInLavaTimer.elapsed();
    }

    private boolean isInFire(AltoClef mod) {
        if (mod.getPlayer().isOnFire() && !mod.getPlayer().hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
            for (BlockPos pos : WorldHelper.getBlocksTouchingPlayer(mod)) {
                Block b = mod.getWorld().getBlockState(pos).getBlock();
                if (b instanceof AbstractFireBlock) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isStuckInNetherPortal(AltoClef mod) {
        return WorldHelper.isInNetherPortal(mod) && !mod.getUserTaskChain().getCurrentTask().thisOrChildSatisfies(task -> task instanceof EnterNetherPortalTask);
    }

    @Override
    public String getName() {
        return "Misc World Survival Chain";
    }

    @Override
    public boolean isActive() {
        // Always check for survival.
        return true;
    }

    @Override
    protected void onStop(AltoClef mod) {
        super.onStop(mod);
    }
}
