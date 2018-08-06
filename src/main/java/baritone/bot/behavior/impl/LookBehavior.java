package baritone.bot.behavior.impl;

import baritone.bot.behavior.Behavior;
import baritone.bot.utils.Rotation;

public class LookBehavior extends Behavior {

    public static final LookBehavior INSTANCE = new LookBehavior();

    private LookBehavior() {}

    /**
     * Target's values are as follows:
     *
     * getFirst() -> yaw
     * getSecond() -> pitch
     */
    private Rotation target;

    public void updateTarget(Rotation target) {
        this.target = target;
    }

    @Override
    public void onPlayerUpdate() {
        if (target != null) {
            player().rotationYaw = target.getFirst();
            player().rotationPitch = target.getSecond();
            target = null;
        }
    }
}
