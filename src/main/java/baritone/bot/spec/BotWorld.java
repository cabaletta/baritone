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

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;

import javax.annotation.Nullable;

/**
 * @author Brady
 * @since 11/7/2018
 */
public final class BotWorld extends WorldClient {

    private static Profiler BOT_WORLD_PROFILER = new Profiler();

    public BotWorld(WorldSettings settings, int dimension) {
        super(null, settings, dimension, EnumDifficulty.EASY, BOT_WORLD_PROFILER);
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
        if (entity != null && !(entity instanceof EntityBot)) {
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
}
