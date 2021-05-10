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

package baritone.builder;

public enum PlaceAgainstData {
    TOP(Half.TOP, false),
    BOTTOM(Half.BOTTOM, false),
    EITHER(Half.EITHER, false),
    SNEAK_TOP(Half.TOP, true), // only if you need to, like, place a top slab against a crafting table
    SNEAK_BOTTOM(Half.BOTTOM, true), // only if you need to, like, place a top slab against a crafting table
    SNEAK_EITHER(Half.EITHER, true);

    public final Half half; // like if its a slab
    public final boolean mustSneak; // like if its a crafting table

    PlaceAgainstData(Half half, boolean mustSneak) {
        this.half = half;
        this.mustSneak = mustSneak;
    }

    public static PlaceAgainstData get(Half half, boolean mustSneak) {
        // oh, the things i do, out of fear of the garbage collector
        switch (half) {
            case TOP:
                return mustSneak ? SNEAK_TOP : TOP;
            case BOTTOM:
                return mustSneak ? SNEAK_BOTTOM : BOTTOM;
            case EITHER:
                return mustSneak ? SNEAK_EITHER : EITHER;
            default:
                throw new IllegalArgumentException();
        }
    }
}
