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

package baritone.api.schematic;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Rotation;

import java.util.List;
import java.util.stream.Collectors;

public class RotatedSchematic implements ISchematic {

    private final ISchematic schematic;
    private final Rotation rotation;
    private final Rotation inverseRotation;

    public RotatedSchematic(ISchematic schematic, Rotation rotation) {
        this.schematic = schematic;
        this.rotation = rotation;
        // I don't think a 14 line switch would improve readability
        this.inverseRotation = rotation.getRotated(rotation).getRotated(rotation);
    }

    @Override
    public boolean inSchematic(int x, int y, int z, BlockState currentState) {
        return schematic.inSchematic(
                rotateX(x, z, widthX(), lengthZ(), inverseRotation),
                y,
                rotateZ(x, z, widthX(), lengthZ(), inverseRotation),
                rotate(currentState, inverseRotation)
        );
    }

    @Override
    public BlockState desiredState(int x, int y, int z, BlockState current, List<BlockState> approxPlaceable) {
        return rotate(schematic.desiredState(
                rotateX(x, z, widthX(), lengthZ(), inverseRotation),
                y,
                rotateZ(x, z, widthX(), lengthZ(), inverseRotation),
                rotate(current, inverseRotation),
                rotate(approxPlaceable, inverseRotation)
        ), rotation);
    }

    @Override
    public void reset() {
        schematic.reset();
    }

    @Override
    public int widthX() {
        return flipsCoordinates(rotation) ? schematic.lengthZ() : schematic.widthX();
    }

    @Override
    public int heightY() {
        return schematic.heightY();
    }

    @Override
    public int lengthZ() {
        return flipsCoordinates(rotation) ? schematic.widthX() : schematic.lengthZ();
    }

    /**
     * Wether {@code rotation} swaps the x and z components
     */
    private static boolean flipsCoordinates(Rotation rotation) {
        return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
    }

    /**
     * The x component of x,y after applying the rotation
     */
    private static int rotateX(int x, int z, int sizeX, int sizeZ, Rotation rotation) {
        switch (rotation) {
            case NONE:
                return x;
            case CLOCKWISE_90:
                return sizeZ - z - 1;
            case CLOCKWISE_180:
                return sizeX - x - 1;
            case COUNTERCLOCKWISE_90:
                return z;
        }
        throw new IllegalArgumentException("Unknown rotation");
    }

    /**
     * The z component of x,y after applying the rotation
     */
    private static int rotateZ(int x, int z, int sizeX, int sizeZ, Rotation rotation) {
        switch (rotation) {
            case NONE:
                return z;
            case CLOCKWISE_90:
                return x;
            case CLOCKWISE_180:
                return sizeZ - z - 1;
            case COUNTERCLOCKWISE_90:
                return sizeX - x - 1;
        }
        throw new IllegalArgumentException("Unknown rotation");
    }

    private static BlockState rotate(BlockState state, Rotation rotation) {
        if (state == null) {
            return null;
        }
        return state.rotate(rotation);
    }

    private static List<BlockState> rotate(List<BlockState> states, Rotation rotation) {
        if (states == null) {
            return null;
        }
        return states.stream()
                .map(s -> rotate(s, rotation))
                .collect(Collectors.toList());
    }
}
