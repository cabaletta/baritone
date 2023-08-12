/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.bot.impl;

import baritone.api.bot.IBaritoneUser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;

import javax.annotation.Nonnull;

/**
 * @author Brady
 */
public final class BotGuiInventory extends GuiInventory {

    private final IBaritoneUser user;

    public BotGuiInventory(IBaritoneUser user) {
        super(user.getPlayerContext().player());
        this.user = user;
    }

    @Override
    public void setWorldAndResolution(@Nonnull Minecraft primary, int width, int height) {
        final Minecraft mc = this.user.getPlayerContext().minecraft();
        mc.displayWidth = primary.displayWidth;
        mc.displayHeight = primary.displayHeight;
        super.setWorldAndResolution(mc, width, height);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        final BotPlayer player = (BotPlayer) this.user.getPlayerContext().player();
        player.isUser = false;
        super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
        player.isUser = true;
    }
}
