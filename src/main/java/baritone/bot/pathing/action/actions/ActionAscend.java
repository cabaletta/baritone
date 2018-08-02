package baritone.bot.pathing.action.actions;

import baritone.bot.pathing.action.Action;
import baritone.bot.pathing.action.ActionState;
import net.minecraft.util.math.BlockPos;

public class ActionAscend extends Action {

    ActionAscend(BlockPos dest) {
        super(dest);
    }

    @Override
    public ActionState calcState() {
        return null;
    }
}
