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
    EntityPlayerSP player = mc.player;
    WorldClient world = mc.world;

    default BlockPos playerFeet() {
        return new BlockPos(player.posX, player.posY, player.posZ);
    }

}
