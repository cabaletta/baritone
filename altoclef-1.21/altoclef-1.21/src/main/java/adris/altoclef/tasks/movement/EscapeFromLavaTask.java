package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.api.pathing.goals.Goal;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.MovementHelper;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class EscapeFromLavaTask extends CustomBaritoneGoalTask {

    private final float _strength;

    public EscapeFromLavaTask(float strength) {
        _strength = strength;
    }

    public EscapeFromLavaTask() {
        this(100);
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getFoodChain().shouldStop(true);
        mod.getBehaviour().push();
        mod.getClientBaritone().getExploreProcess().onLostControl();
        mod.getClientBaritone().getCustomGoalProcess().onLostControl();
        mod.getBehaviour().allowSwimThroughLava(true);
        // Encourage placing of all blocks!
        mod.getBehaviour().setBlockPlacePenalty(0);
        mod.getBehaviour().setBlockBreakAdditionalPenalty(0); // Normally 2
        // do NOT ever wander
        _checker = new MovementProgressChecker((int) Float.POSITIVE_INFINITY);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Sprint through lava + jump, it's faster
        if (mod.getPlayer().isInLava() || mod.getWorld().getBlockState(mod.getPlayer().getBlockPos().down()).getBlock() == Blocks.LAVA) {
            mod.getInputControls().hold(Input.JUMP);
            mod.getInputControls().hold(Input.SPRINT);
            mod.getInputControls().hold(Input.MOVE_FORWARD);
        }
        return super.onTick(mod);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getFoodChain().shouldStop(false);
        mod.getBehaviour().pop();
        mod.getInputControls().release(Input.JUMP);
        mod.getInputControls().release(Input.SPRINT);
        mod.getInputControls().release(Input.MOVE_FORWARD);
    }

    @Override
    protected Goal newGoal(AltoClef mod) {
        return new EscapeFromLavaGoal();
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof EscapeFromLavaTask;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return !mod.getPlayer().isInLava() && !mod.getPlayer().isOnFire();
    }

    @Override
    protected String toDebugString() {
        return "Escaping lava";
    }

    private class EscapeFromLavaGoal implements Goal {

        private static boolean isLava(int x, int y, int z) {
            if (MinecraftClient.getInstance().world == null) return false;
            return MovementHelper.isLava(MinecraftClient.getInstance().world.getBlockState(new BlockPos(x, y, z)));
        }

        private static boolean isLavaAdjacent(int x, int y, int z) {
            return isLava(x + 1, y, z) || isLava(x - 1, y, z) || isLava(x, y, z + 1) || isLava(x, y, z - 1)
                    || isLava(x + 1, y, z - 1) || isLava(x + 1, y, z + 1) || isLava(x - 1, y, z - 1)
                    || isLava(x - 1, y, z + 1);
        }

        private static boolean isWater(int x, int y, int z) {
            if (MinecraftClient.getInstance().world == null) return false;
            return MovementHelper.isWater(MinecraftClient.getInstance().world.getBlockState(new BlockPos(x, y, z)));
        }

        @Override
        public boolean isInGoal(int x, int y, int z) {
            return !isLava(x, y, z) && !isLavaAdjacent(x, y, z);
        }

        @Override
        public double heuristic(int x, int y, int z) {
            if (isLava(x, y, z)) {
                return _strength;
            } else if (isLavaAdjacent(x, y, z)) {
                return _strength * 0.5f;
            }
            if (isWater(x, y, z)) {
                return -100;
            }
            return 0;
        }
    }
}
