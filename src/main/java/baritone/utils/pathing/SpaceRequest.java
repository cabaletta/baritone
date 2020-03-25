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
    private boolean requestTop;
    private boolean requestBottom;
    private boolean requestNorth;
    private boolean requestEast;
    private boolean requestSouth;
    private boolean requestWest;
    private boolean requestNoSuffocation;
    private boolean requestPlayerSpace;

    public SpaceRequest(EnumFacing... faces) {
        for (EnumFacing face : faces) {
            switch (face) {
                case UP:
                    requestTop = true;
                    break;
                case DOWN:
                    requestBottom = true;
                    break;
                case NORTH:
                    requestNorth = true;
                    break;
                case EAST:
                    requestEast = true;
                    break;
                case SOUTH:
                    requestSouth = true;
                    break;
                case WEST:
                    requestWest = true;
                    break;
            }
        }
    }

    public static SpaceRequest greedyRequest() {
        SpaceRequest s = new SpaceRequest();
        s.requestTop = true;
        s.requestBottom = true;
        s.requestNorth = true;
        s.requestEast = true;
        s.requestSouth = true;
        s.requestWest = true;
        s.requestNoSuffocation = true;
        return s;
    }

    public boolean requires(EnumFacing face) {
        switch (face) {
            case UP:
                return requestTop;
            case DOWN:
                return requestBottom;
            case NORTH:
                return requestNorth;
            case EAST:
                return requestEast;
            case SOUTH:
                return requestSouth;
            case WEST:
                return requestWest;
            default:
                return false;
        }
    }

    public SpaceRequest withNoSuffocation() {
        requestNoSuffocation = true;
        return this;
    }

    public SpaceRequest withLowerPlayerSpace() {
        requestTop = true;
        requestPlayerSpace = true;
        return this;
    }

    public SpaceRequest withUpperPlayerSpace() {
        requestBottom = true;
        requestPlayerSpace = true;
        return this;
    }
}