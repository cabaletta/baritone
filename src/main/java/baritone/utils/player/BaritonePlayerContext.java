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
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

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
    public LocalPlayer player() {
        return this.mc.player;
    }

    @Override
    public IPlayerController playerController() {
        return this.playerController;
    }

    @Override
    public Level world() {
        return this.mc.level;
    }

    @Override
    public IWorldData worldData() {
        return this.baritone.getWorldProvider().getCurrentWorld();
    }

    @Override
    public BetterBlockPos viewerPos() {
        final Entity entity = this.mc.getCameraEntity();
        return entity == null ? this.playerFeet() : BetterBlockPos.from(entity.blockPosition());
    }

    @Override
    public Rotation playerRotations() {
        return this.baritone.getLookBehavior().getEffectiveRotation().orElseGet(IPlayerContext.super::playerRotations);
    }

    @Override
    public HitResult objectMouseOver() {
        return RayTraceUtils.rayTraceTowards(player(), playerRotations(), playerController().getBlockReachDistance());
    }
}
