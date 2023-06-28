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

package baritone.bot.impl;

import it.unimi.dsi.fastutil.ints.*;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Brady
 * @since 11/7/2018
 */
public final class BotWorld extends WorldClient {

    private static Profiler BOT_WORLD_PROFILER = new Profiler();
    private final Map<ChunkPos, IntSet> loadedChunksMap;

    public BotWorld(WorldSettings settings, int dimension) {
        super(null, settings, dimension, EnumDifficulty.EASY, BOT_WORLD_PROFILER);
        this.loadedChunksMap = new HashMap<>();
    }

    @Override
    public void playSound(double x, double y, double z, SoundEvent soundIn, SoundCategory category, float volume, float pitch, boolean distanceDelay) {
        // Do nothing
    }

    @Override
    public void addEntityToWorld(int entityID, Entity entity) {
        this.removeEntityFromWorld(entityID);
        this.spawnEntity(entity);
        this.entitiesById.addKey(entityID, entity);
    }

    @Override
    public Entity removeEntityFromWorld(int entityID) {
        Entity entity = this.entitiesById.lookup(entityID);
        if (entity != null && !(entity instanceof BotPlayer)) {
            this.removeEntity(entity);
            this.entitiesById.removeObject(entityID);
        }
        return entity;
    }

    @Nullable
    @Override
    public Entity getEntityByID(int id) {
        return this.entitiesById.lookup(id);
    }

    /**
     * @param bot    The bot requesting the chunk
     * @param chunkX The chunk X position
     * @param chunkZ The chunk Z position
     * @param load   {@code true} if the chunk is being loaded, {@code false} if the chunk is being unloaded.
     * @return Whether or not the chunk needs to be loaded or unloaded accordingly.
     */
    public boolean handlePreChunk(BotPlayer bot, int chunkX, int chunkZ, boolean load) {
        IntSet bots = this.loadedChunksMap.computeIfAbsent(new ChunkPos(chunkX, chunkZ), $ -> new IntArraySet());
        if (load) {
            boolean wasEmpty = bots.isEmpty();
            bots.add(bot.getEntityId());
            return wasEmpty;
        } else {
            bots.remove(bot.getEntityId());
            return bots.isEmpty();
        }
    }

    public void handleWorldRemove(BotPlayer bot) {
        // Remove Bot from world
        this.removeEntity(bot);
        this.entitiesById.removeObject(bot.getEntityId());

        // Unload all chunks that are no longer loaded by the removed Bot
        this.loadedChunksMap.entrySet().stream()
                .peek(entry -> entry.getValue().remove(bot.getEntityId()))
                .filter(entry -> entry.getValue().isEmpty())
                .forEach(entry -> this.doPreChunk(entry.getKey().x, entry.getKey().z, false));
    }
}
