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
import baritone.api.utils.*;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

/**
 * Implementation of {@link IPlayerContext} that provides information about the primary player.
 *
 * @author Brady
 * @since 11/12/2018
 */
public final class BaritonePlayerContext implements IPlayerContext {

    private final Baritone baritone;
    private final Minecraft mc;
    private final IPlayerController playerController;

    public BaritonePlayerContext(Baritone baritone, Minecraft mc) {
        this.baritone = baritone;
        this.mc = mc;
        this.playerController = new BaritonePlayerController(mc);
    }

    @Override
    public Minecraft minecraft() {
        return this.mc;
    }

    @Override
    public ClientPlayerEntity player() {
        return this.mc.player;
    }

    @Override
    public IPlayerController playerController() {
        return this.playerController;
    }

    @Override
    public World world() {
        return this.mc.world;
    }

    @Override
    public IWorldData worldData() {
        return this.baritone.getWorldProvider().getCurrentWorld();
    }

    @Override
    public BetterBlockPos viewerPos() {
        final Entity entity = this.mc.getRenderViewEntity();
        return entity == null ? this.playerFeet() : BetterBlockPos.from(new BlockPos(entity));
    }

    @Override
    public Rotation playerRotations() {
        return this.baritone.getLookBehavior().getEffectiveRotation().orElseGet(IPlayerContext.super::playerRotations);
    }

    @Override
    public RayTraceResult objectMouseOver() {
        return RayTraceUtils.rayTraceTowards(player(), playerRotations(), playerController().getBlockReachDistance());
    }
}
