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

package baritone.api.utils;

import baritone.api.cache.IWorldData;
import net.minecraft.block.BlockSlab;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * @author Brady
 * @since 11/12/2018
 */
public interface IPlayerContext {

    EntityPlayerSP player();

    PlayerControllerMP playerController();

    World world();

    IWorldData worldData();

    default BetterBlockPos playerFeet() {
        // TODO find a better way to deal with soul sand!!!!!
        BetterBlockPos feet = new BetterBlockPos(player().posX, player().posY + 0.1251, player().posZ);
        if (world().getBlockState(feet).getBlock() instanceof BlockSlab) {
            return feet.up();
        }
        return feet;
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
}
