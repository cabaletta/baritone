/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.utils;

import baritone.Baritone;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/**
 * @author Brady
 * @since 8/1/2018 12:18 AM
 */
public interface Helper {

    ITextComponent MESSAGE_PREFIX = new TextComponentString("§5[§dBaritone§5]§7");

    Minecraft mc = Minecraft.getMinecraft();

    default EntityPlayerSP player() {
        return mc.player;
    }

    default WorldClient world() {
        return mc.world;
    }

    default BlockPos playerFeet() {
        // TODO find a better way to deal with soul sand!!!!!
        return new BlockPos(player().posX, player().posY + 0.1251, player().posZ);
        /*if (BlockStateInterface.get(feet).getBlock().equals(Blocks.SOUL_SAND) && player().posY > feet.getY() + 0.874999) {
            return feet.up();
        }*/
    }

    default Vec3d playerFeetAsVec() {
        return new Vec3d(player().posX, player().posY, player().posZ);
    }

    default Vec3d playerHead() {
        return new Vec3d(player().posX, player().posY + player().getEyeHeight(), player().posZ);
    }

    default Rotation playerRotations() {
        return new Rotation(player().rotationYaw, player().rotationPitch);
    }

    default void displayChatMessageRaw(String message) {
        if (!Baritone.settings().chatDebug.get()) {
            System.out.println("Suppressed debug message:");
            System.out.println(message);
            return;
        }

        ITextComponent component = MESSAGE_PREFIX.createCopy();
        component.getStyle().setColor(TextFormatting.GRAY);
        component.appendSibling(new TextComponentString(" " + message));
        mc.ingameGUI.getChatGUI().printChatMessage(component);
    }
}
