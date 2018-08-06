package baritone.bot.behavior.impl;

import baritone.bot.behavior.Behavior;
import baritone.bot.event.events.TickEvent;
import baritone.bot.utils.Rotation;

import java.util.Optional;

public class LookBehavior extends Behavior {

    public static final LookBehavior INSTANCE = new LookBehavior();

    private LookBehavior() {
        target = Optional.empty();
    }

    /**
     * Target's values are as follows:
     *
     * getFirst() -> yaw
     * getSecond() -> pitch
     */
    private Optional<Rotation> target;

    public void updateTarget(Rotation target) {
        this.target = Optional.of(target);
    }

    @Override
    public void onTick(TickEvent event) {
        this.onPlayerUpdate();
    }

    @Override
    public void onPlayerUpdate() {
        if(target.isPresent()) {
            player().setPositionAndRotation(player().posX, player().posY, player().posZ,
                    target.get().getFirst(), target.get().getSecond());
            target = Optional.empty();
        }
    }

}
