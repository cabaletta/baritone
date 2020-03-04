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

import baritone.api.bot.IBaritoneUser;
import baritone.api.cache.IWorldData;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.IPlayerController;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.world.World;

public final class BotPlayerContext implements IPlayerContext {

    /**
     * The backing {@link IBaritoneUser}
     */
    private final IBaritoneUser bot;

    public BotPlayerContext(IBaritoneUser bot) {
        this.bot = bot;
    }

    @Override
    public EntityPlayerSP player() {
        if (bot.getEntity() == null) {
            return null;
        }
        return bot.getEntity();
    }

    @Override
    public IPlayerController playerController() {
        if (bot.getEntity() == null) {
            return null;
        }
        return bot.getPlayerController();
    }

    @Override
    public World world() {
        if (bot.getEntity() == null) {
            return null;
        }
        return bot.getEntity().world;
    }

    @Override
    public IWorldData worldData() {
        return bot.getBaritone().getWorldProvider().getCurrentWorld();
    }
}
