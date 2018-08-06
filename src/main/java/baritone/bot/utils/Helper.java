package baritone.bot.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

/**
 * @author Brady
 * @since 8/1/2018 12:18 AM
 */
public interface Helper {

    Minecraft mc = Minecraft.getMinecraft();

    default EntityPlayerSP player() {
        return mc.player;
    }

    default WorldClient world() {
        return mc.world;
    }

    default BlockPos playerFeet() {
        return new BlockPos(mc.player.posX, mc.player.posY, mc.player.posZ);
    }

    default void displayChatMessageRaw(String message) {
        mc.ingameGUI.getChatGUI().printChatMessage(new TextComponentString(message));
    }
}
