package baritone.bot.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.math.BlockPos;

/**
 * @author Brady
 * @since 8/1/2018 12:18 AM
 */
public interface Helper {

    Minecraft mc = Minecraft.getMinecraft();

    default BlockPos playerFeet() {
        return new BlockPos(mc.player.posX, mc.player.posY, mc.player.posZ);
    }

}
