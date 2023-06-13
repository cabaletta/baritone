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

package baritone.bot;

import baritone.bot.impl.BotWorld;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

/**
 * @author Brady
 * @since 11/7/2018
 */
public final class BotWorldProvider {

    /**
     * Generic world settings for a typical survival world.
     */
    private static final WorldSettings GENERIC_WORLD_SETTINGS = new WorldSettings(0L, GameType.SURVIVAL, true, false, WorldType.DEFAULT);

    /**
     * All of the dimensions mapped to their respective worlds.
     */
    private final Int2ObjectMap<BotWorld> worlds = new Int2ObjectArrayMap<>();

    /**
     * Gets or creates the {@link BotWorld} for the specified dimension
     *
     * @param dimension The dimension id
     * @return The world
     */
    public BotWorld getWorld(int dimension) {
        return worlds.computeIfAbsent(dimension, this::createWorldForDim);
    }

    /**
     * Creates a new {@link BotWorld} for the given dimension id.
     *
     * @param dimension The dimension id
     * @return The new world
     */
    private BotWorld createWorldForDim(int dimension) {
        return new BotWorld(GENERIC_WORLD_SETTINGS, dimension);
    }

    public void tick() {
        this.worlds.forEach((dim, world) -> world.updateEntities());
    }
}
