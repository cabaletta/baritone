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

package baritone.utils.pathing;

import net.minecraft.world.border.WorldBorder;

/**
 * Essentially, a "rule" for the path finder, prevents proposed movements from attempting to venture
 * into the world border, and prevents actual movements from placing blocks in the world border.
 */
public class BetterWorldBorder {

    private final double minX;
    private final double maxX;
    private final double minZ;
    private final double maxZ;

    public BetterWorldBorder(WorldBorder border) {
        this.minX = border.minX();
        this.maxX = border.maxX();
        this.minZ = border.minZ();
        this.maxZ = border.maxZ();
    }

    public boolean entirelyContains(int x, int z) {
        return x + 1 > minX && x < maxX && z + 1 > minZ && z < maxZ;
    }

    public boolean canPlaceAt(int x, int z) {
        // move it in 1 block on all sides
        // because we can't place a block at the very edge against a block outside the border
        // it won't let us right click it
        return x > minX && x + 1 < maxX && z > minZ && z + 1 < maxZ;
    }
}
