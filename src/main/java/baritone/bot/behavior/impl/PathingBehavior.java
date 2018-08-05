package baritone.bot.behavior.impl;

import baritone.bot.behavior.Behavior;
import baritone.bot.pathing.path.IPath;
import baritone.bot.pathing.path.PathExecutor;

public class PathingBehavior extends Behavior {
    public static final PathingBehavior INSTANCE = new PathingBehavior();

    private PathingBehavior() {
    }

    private PathExecutor current;

    @Override
    public void onTick() {
        if (current == null) {
            return;
        }
        current.onTick();
        if (current.failed() || current.finished()) {
            current = null;
        }
    }

    public PathExecutor getExecutor() {
        return current;
    }

    public IPath getPath() {
        return current.getPath();
    }

}
