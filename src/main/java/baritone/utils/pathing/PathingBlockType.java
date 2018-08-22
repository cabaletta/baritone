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

package baritone.utils.pathing;

/**
 * @author Brady
 * @since 8/4/2018 1:11 AM
 */
public enum PathingBlockType {

    AIR  (0b00),
    WATER(0b01),
    AVOID(0b10),
    SOLID(0b11);

    private final boolean[] bits;

    PathingBlockType(int bits) {
        this.bits = new boolean[] {
                (bits & 0b10) != 0,
                (bits & 0b01) != 0
        };
    }

    public final boolean[] getBits() {
        return this.bits;
    }

    public static PathingBlockType fromBits(boolean b1, boolean b2) {
        for (PathingBlockType type : values())
            if (type.bits[0] == b1 && type.bits[1] == b2)
                return type;

        // This will never happen, but if it does, assume it's just AIR
        return PathingBlockType.AIR;
    }
}
