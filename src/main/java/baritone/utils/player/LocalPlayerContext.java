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

package baritone.utils.player;

import baritone.Baritone;
import baritone.api.cache.IWorldData;
import baritone.api.utils.IPlayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.world.World;

/**
 * Implementation of {@link IPlayerContext} that provides information about the local player.
 *
 * @author Brady
 * @since 11/12/2018
 */
public final class LocalPlayerContext implements IPlayerContext {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static final LocalPlayerContext INSTANCE = new LocalPlayerContext();

    private LocalPlayerContext() {}

    @Override
    public EntityPlayerSP player() {
        return mc.player;
    }

    @Override
    public PlayerControllerMP playerController() {
        return mc.playerController;
    }

    @Override
    public World world() {
        return mc.world;
    }

    @Override
    public IWorldData worldData() {
        return Baritone.INSTANCE.getWorldProvider().getCurrentWorld();
    }
}
