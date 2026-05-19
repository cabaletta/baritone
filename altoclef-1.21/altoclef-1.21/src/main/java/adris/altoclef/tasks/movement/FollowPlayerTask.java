package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public class FollowPlayerTask extends Task {

    private final String _playerName;

    public FollowPlayerTask(String playerName) {
        _playerName = playerName;
    }

    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {

        Optional<Vec3d> lastPos = mod.getEntityTracker().getPlayerMostRecentPosition(_playerName);

        if (lastPos.isEmpty()) {
            setDebugState("No player found/detected. Doing nothing until player loads into render distance.");
            return null;
        }
        Vec3d target = lastPos.get();

        if (target.isInRange(mod.getPlayer().getPos(), 1) && !mod.getEntityTracker().isPlayerLoaded(_playerName)) {
            mod.logWarning("Failed to get to player \"" + _playerName + "\". We moved to where we last saw them but now have no idea where they are.");
            stop(mod);
            return null;
        }

        Optional<PlayerEntity> player = mod.getEntityTracker().getPlayerEntity(_playerName);
        if (player.isEmpty()) {
            // Go to last location
            return new GetToBlockTask(new BlockPos((int) target.x, (int) target.y, (int) target.z), false);
        }
        return new GetToEntityTask(player.get(), 2);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof FollowPlayerTask task) {
            return task._playerName.equals(_playerName);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Going to player " + _playerName;
    }
}
