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
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Brady
 * @since 11/12/2018
 */
public interface IPlayerContext {

    Minecraft minecraft();

    LocalPlayer player();

    IPlayerController playerController();

    Level world();

    default Iterable<Entity> entities() {
        return ((ClientLevel) world()).entitiesForRendering();
    }

    default Stream<Entity> entitiesStream() {
        return StreamSupport.stream(entities().spliterator(), false);
    }


    IWorldData worldData();

    HitResult objectMouseOver();

    default BetterBlockPos playerToes() {
        // TODO find a better way to deal with soul sand!!!!!
        BetterBlockPos feet = new BetterBlockPos(player().position().x, player().position().y + 0.1251, player().position().z);

        // sometimes when calling this from another thread or while world is null, it'll throw a NullPointerException
        // that causes the game to immediately crash
        //
        // so of course crashing on 2b is horribly bad due to queue times and logout spot
        // catch the NPE and ignore it if it does happen
        //
        // this does not impact performance at all since we're not null checking constantly
        // if there is an exception, the only overhead is Java generating the exception object... so we can ignore it
        try {
            if (world().getBlockState(feet).getBlock() instanceof SlabBlock) {
                return feet.above();
            }
        } catch (NullPointerException ignored) {}

        return feet;
    }

    default Vec3 playerFeetAsVec() {
        return new Vec3(player().position().x, player().position().y, player().position().z);
    }

    default Vec3 playerHead() {
        return new Vec3(player().position().x, player().position().y + player().getEyeHeight(), player().position().z);
    }

    default Vec3 playerMotion() {
        return player().getDeltaMovement();
    }

    BetterBlockPos viewerPos();

    default Rotation playerRotations() {
        return new Rotation(player().getYRot(), player().getXRot());
    }

    static double eyeHeight(boolean ifSneaking) {
        return ifSneaking ? 1.27 : 1.62;
    }

    /**
     * Returns the block that the crosshair is currently placed over. Updated once per tick.
     *
     * @return The position of the highlighted block
     */
    default Optional<BlockPos> getSelectedBlock() {
        HitResult result = objectMouseOver();
        if (result != null && result.getType() == HitResult.Type.BLOCK) {
            return Optional.of(((BlockHitResult) result).getBlockPos());
        }
        return Optional.empty();
    }

    default boolean isLookingAt(BlockPos pos) {
        return getSelectedBlock().equals(Optional.of(pos));
    }
}
