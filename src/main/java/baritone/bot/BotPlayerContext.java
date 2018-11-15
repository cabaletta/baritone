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

package baritone.bot;

import baritone.api.BaritoneAPI;
import baritone.api.cache.IWorldData;
import baritone.api.utils.IPlayerContext;
import baritone.bot.handler.BotNetHandlerPlayClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class BotPlayerContext implements IPlayerContext {
    private final IBaritoneUser bot;

    public BotPlayerContext(IBaritoneUser bot) {
        this.bot = bot;
    }

    @Override
    public EntityPlayerSP player() {
        if (bot.getConnection() == null) {
            return null;
        }
        return ((BotNetHandlerPlayClient) bot.getConnection()).player();
    }

    @Override
    public PlayerControllerMP playerController() {
        return Minecraft.getMinecraft().playerController; // idk LOL
    }

    @Override
    public World world() {
        if (bot.getConnection() == null) {
            return null;
        }
        return ((BotNetHandlerPlayClient) bot.getConnection()).world();
    }

    @Override
    public IWorldData worldData() {
        return BaritoneAPI.getProvider().getBaritoneForPlayer(player()).getWorldProvider().getCurrentWorld();
    }

    @Override
    public RayTraceResult objectMouseOver() {
        return Minecraft.getMinecraft().objectMouseOver; // idk LOL
    }
}
