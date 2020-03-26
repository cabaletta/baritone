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

import net.minecraft.util.EnumFacing;

public class SpaceRequest {
    private static final int REQUEST_TOP = 1;
    private static final int REQUEST_BOTTOM = 2;
    private static final int REQUEST_NORTH = 4;
    private static final int REQUEST_EAST = 8;
    private static final int REQUEST_SOUTH = 16;
    private static final int REQUEST_WEST = 32;
    private static final int REQUEST_NO_SUFFOCATION = 64;
    private static final int REQUEST_PLAYER_SPACE = 128;

    private SpaceRequest() {
    }

    public static int none() {
        return 0;
    }

    public static int addFaces(int v, EnumFacing... faces) {
        for (EnumFacing face : faces) {
            switch (face) {
                case UP:
                    v |= REQUEST_TOP;
                    break;
                case DOWN:
                    v |= REQUEST_BOTTOM;
                    break;
                case NORTH:
                    v |= REQUEST_NORTH;
                    break;
                case EAST:
                    v |= REQUEST_EAST;
                    break;
                case SOUTH:
                    v |= REQUEST_SOUTH;
                    break;
                case WEST:
                    v |= REQUEST_WEST;
                    break;
                default:
                    break;
            }
        }
        return v;
    }

    public static int fromFaces(EnumFacing... faces) {
        return addFaces(SpaceRequest.none(), faces);
    }

    public static int greedyRequest() {
        return Integer.MAX_VALUE;
    }

    public static boolean requires(int v, EnumFacing face) {
        switch (face) {
            case UP:
                return (v & REQUEST_TOP) != 0;
            case DOWN:
                return (v & REQUEST_BOTTOM) != 0;
            case NORTH:
                return (v & REQUEST_NORTH) != 0;
            case EAST:
                return (v & REQUEST_EAST) != 0;
            case SOUTH:
                return (v & REQUEST_SOUTH) != 0;
            case WEST:
                return (v & REQUEST_WEST) != 0;
            default:
                return false;
        }
    }

    public static boolean isRequestNoSuffocation(int v) {
        return (v & REQUEST_NO_SUFFOCATION) != 0;
    }

    public static boolean isRequestPlayerSpace(int v) {
        return (v & REQUEST_PLAYER_SPACE) != 0;
    }

    public static int withNoSuffocation(int v) {
        return v | REQUEST_NO_SUFFOCATION;
    }

    public static int withLowerPlayerSpace(int v) {
        return v | REQUEST_PLAYER_SPACE | REQUEST_TOP;
    }

    public static int withUpperPlayerSpace(int v) {
        return v | REQUEST_PLAYER_SPACE | REQUEST_BOTTOM;
    }

    public static int withAllPlayerSpace(int v) {
        return v | REQUEST_PLAYER_SPACE | REQUEST_TOP | REQUEST_BOTTOM;
    }
}