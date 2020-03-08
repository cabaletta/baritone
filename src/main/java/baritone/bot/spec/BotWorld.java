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

package baritone.bot.spec;

import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.SaveHandlerMP;
import net.minecraft.world.storage.WorldInfo;

import javax.annotation.Nonnull;

/**
 * @author Brady
 * @since 11/7/2018
 */
public final class BotWorld extends World {

    private static Profiler BOT_WORLD_PROFILER = new Profiler();
    private static int worldNum = 0;

    private ChunkProviderClient chunkProviderClient;

    public BotWorld(WorldSettings settings, int dimension) {
        super(
                new SaveHandlerMP(),
                new WorldInfo(settings, "BotWorld" + ++worldNum),
                DimensionType.getById(dimension).createDimension(),
                BOT_WORLD_PROFILER,
                true
        );
        this.provider.setWorld(this);
        this.chunkProvider = this.createChunkProvider();
    }

    @Override
    @Nonnull
    protected IChunkProvider createChunkProvider() {
        return (this.chunkProviderClient = new ChunkProviderClient(this));
    }

    @Override
    protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) {
        return allowEmpty || !this.chunkProviderClient.provideChunk(x, z).isEmpty();
    }

    @Override
    public void playSound(double x, double y, double z, SoundEvent soundIn, SoundCategory category, float volume, float pitch, boolean distanceDelay) {
        // Do nothing
    }

    public void addEntityToWorld(int entityID, EntityBot entity) {
        this.removeEntityFromWorld(entityID);
        this.spawnEntity(entity);
        this.entitiesById.addKey(entityID, entity);
    }

    public void removeEntityFromWorld(int entityID) {
        Entity entity = this.entitiesById.lookup(entityID);
        if (entity != null && !(entity instanceof EntityBot)) {
            this.removeEntity(entity);
            this.entitiesById.removeObject(entityID);
        }
    }

    public void doPreChunk(int chunkX, int chunkZ, boolean loadChunk) {
        if (loadChunk) {
            this.chunkProviderClient.loadChunk(chunkX, chunkZ);
        } else {
            this.chunkProviderClient.unloadChunk(chunkX, chunkZ);
        }
    }
}
