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

import baritone.api.BaritoneAPI;
import baritone.api.cache.IWorldData;
import baritone.api.utils.Helper;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.IPlayerController;
import baritone.api.utils.RayTraceUtils;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

/**
 * Implementation of {@link IPlayerContext} that provides information about the primary player.
 *
 * @author Brady
 * @since 11/12/2018
 */
public enum PrimaryPlayerContext implements IPlayerContext, Helper {

    INSTANCE;

    @Override
    public EntityPlayerSP player() {
        return mc.player;
    }

    @Override
    public IPlayerController playerController() {
        return PrimaryPlayerController.INSTANCE;
    }

    @Override
    public World world() {
        return mc.world;
    }

    @Override
    public IWorldData worldData() {
        return BaritoneAPI.getProvider().getPrimaryBaritone().getWorldProvider().getCurrentWorld();
    }

    @Override
    public RayTraceResult objectMouseOver() {
        return RayTraceUtils.rayTraceTowards(player(), playerRotations(), playerController().getBlockReachDistance());
    }
}
