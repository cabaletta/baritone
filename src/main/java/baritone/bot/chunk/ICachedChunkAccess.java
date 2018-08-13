/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.bot.chunk;

import baritone.bot.utils.pathing.IBlockTypeAccess;

import java.util.BitSet;

/**
 * @author Brady
 * @since 8/4/2018 1:10 AM
 */
public interface ICachedChunkAccess extends IBlockTypeAccess {

    void updateCachedChunk(int chunkX, int chunkZ, BitSet data);
}
