package baritone.bot.behavior.impl;

import baritone.bot.behavior.Behavior;
import baritone.bot.event.events.RenderEvent;
import baritone.bot.pathing.path.IPath;
import baritone.bot.pathing.path.PathExecutor;

public class PathingBehavior extends Behavior {

    public static final PathingBehavior INSTANCE = new PathingBehavior();

    private PathingBehavior() {}

    private PathExecutor current;

    @Override
    public void onTick() {
        System.out.println("Ticking");
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
        if (current == null) {
            return null;
        }
        return current.getPath();
    }

    @Override
    public void onRenderPass(RenderEvent event) {
        System.out.println("Render passing");
        System.out.println(event.getPartialTicks());
    }

}
