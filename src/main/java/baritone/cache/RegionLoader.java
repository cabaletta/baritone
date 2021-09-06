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

package baritone.cache;

import baritone.Baritone;
import baritone.api.event.events.RenderEvent;
import baritone.api.event.events.TickEvent;
import baritone.api.event.listener.AbstractGameEventListener;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import static baritone.api.utils.Helper.mc;

/**
 * Loads an area around the player and renderViewEntity to the cache.
 */
public class RegionLoader implements AbstractGameEventListener {

    private final Baritone baritone;
    private final WorldProvider worldProvider;
    /**
     * The maximum length from the player to the edge of the loaded region.
     * A radius of 0 does no caching. A radius of 1 only loads the region the player is on.
     * A radius of 2 load a 3 by three grid around the player.
     */
    private final int radius;

    private int lastRegionX;
    private int lastRegionZ;

    public RegionLoader(Baritone baritone, int radius) {
        this.baritone = baritone;
        this.worldProvider = baritone.getWorldProvider();
        this.radius = radius;
    }

    @Override
    public void onTick(TickEvent event) {
        this.loadRegions();
    }

    @Override
    public void onRenderPass(RenderEvent event) {
        this.loadRegions();
    }

    private void loadRegions() {
        EntityPlayerSP player = mc.player;
        Entity renderViewEntity = mc.getRenderViewEntity();
        // Don't do anything before the player is loaded.
        if (player == null || renderViewEntity == null) {
            return;
        }
        WorldData worldData = this.worldProvider.getCurrentWorld();
        // Don't do anything if the world doesn't exist. Like in the main menu.
        if (worldData == null) {
            return;
        }

        int playerRegionX = player.chunkCoordX >> 5;
        int playerRegionZ = player.chunkCoordZ >> 5;
        // Only load if the player has moved.
        if (playerRegionX != this.lastRegionX || playerRegionZ != this.lastRegionZ) {
            loadAroundRegion(playerRegionX, playerRegionZ);
            this.lastRegionX = playerRegionX;
            this.lastRegionZ = playerRegionZ;
        }

        // Only load around the renderViewEntity if it's in a different region.
        int renderViewEntityRegionX = renderViewEntity.chunkCoordX >> 5;
        int renderViewEntityRegionZ = renderViewEntity.chunkCoordZ >> 5;
        if (playerRegionX != renderViewEntityRegionX || playerRegionZ != renderViewEntityRegionZ) {
            loadAroundRegion(renderViewEntityRegionX, renderViewEntityRegionZ);
        }
    }

    private void loadAroundRegion(int regionX, int regionZ) {
        CachedWorld cachedWorld = (CachedWorld) this.worldProvider.getCurrentWorld().getCachedWorld();
        // If the radius is zero do nothing.
        if (this.radius == 0) {
            return;
        }
        // Load the regions.
        for (int xOffset = -(this.radius - 1); xOffset < this.radius; xOffset++) {
            for (int zOffset = -(this.radius - 1); zOffset < this.radius; zOffset++) {
                cachedWorld.tryLoadFromDisk(regionX + xOffset, regionZ + zOffset);
            }
        }
    }
}
