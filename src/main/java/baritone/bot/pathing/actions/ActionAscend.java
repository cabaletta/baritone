package baritone.bot.pathing.actions;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public class ActionAscend extends Action {

    ActionAscend(BlockPos dest) {
        super(dest);
    }

    @Override
    public ActionState calcState() {
        return null;
    }
}
