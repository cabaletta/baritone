package baritone.bot.behavior.impl;

import baritone.bot.behavior.Behavior;
import net.minecraft.util.Tuple;

import java.util.Optional;

public class LookBehavior extends Behavior {

    public static final LookBehavior INSTANCE = new LookBehavior();

    public LookBehavior() {
        target = Optional.empty();
    }

    /**
     * Target's values are as follows:
     *
     * getFirst() -> yaw
     * getSecond() -> pitch
     */
    private Optional<Tuple<Float, Float>> target;

    public void updateTarget(Tuple<Float, Float> target) {
        this.target = Optional.of(target);
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
