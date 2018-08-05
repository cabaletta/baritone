package baritone.bot.behavior;

import baritone.bot.pathing.path.PathExecutor;

public class PathingBehavior extends Behavior{
    public static final PathingBehavior INSTANCE=new PathingBehavior();
    private PathingBehavior(){}

    private PathExecutor current;

}
