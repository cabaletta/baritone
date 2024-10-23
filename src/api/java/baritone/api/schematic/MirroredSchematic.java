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
import net.minecraft.world.level.block.Mirror;

import java.util.List;
import java.util.stream.Collectors;

public class MirroredSchematic implements ISchematic {

    private final ISchematic schematic;
    private final Mirror mirror;

    public MirroredSchematic(ISchematic schematic, Mirror mirror) {
        this.schematic = schematic;
        this.mirror = mirror;
    }

    @Override
    public boolean inSchematic(int x, int y, int z, BlockState currentState) {
        return schematic.inSchematic(
                mirrorX(x, widthX(), mirror),
                y,
                mirrorZ(z, lengthZ(), mirror),
                mirror(currentState, mirror)
        );
    }

    @Override
    public BlockState desiredState(int x, int y, int z, BlockState current, List<BlockState> approxPlaceable) {
        return mirror(schematic.desiredState(
                mirrorX(x, widthX(), mirror),
                y,
                mirrorZ(z, lengthZ(), mirror),
                mirror(current, mirror),
                mirror(approxPlaceable, mirror)
        ), mirror);
    }

    @Override
    public void reset() {
        schematic.reset();
    }

    @Override
    public int widthX() {
        return schematic.widthX();
    }

    @Override
    public int heightY() {
        return schematic.heightY();
    }

    @Override
    public int lengthZ() {
        return schematic.lengthZ();
    }

    private static int mirrorX(int x, int sizeX, Mirror mirror) {
        switch (mirror) {
            case NONE:
            case LEFT_RIGHT:
                return x;
            case FRONT_BACK:
                return sizeX - x - 1;
        }
        throw new IllegalArgumentException("Unknown mirror");
    }

    private static int mirrorZ(int z, int sizeZ, Mirror mirror) {
        switch (mirror) {
            case NONE:
            case FRONT_BACK:
                return z;
            case LEFT_RIGHT:
                return sizeZ - z - 1;
        }
        throw new IllegalArgumentException("Unknown mirror");
    }

    private static BlockState mirror(BlockState state, Mirror mirror) {
        if (state == null) {
            return null;
        }
        return state.mirror(mirror);
    }

    private static List<BlockState> mirror(List<BlockState> states, Mirror mirror) {
        if (states == null) {
            return null;
        }
        return states.stream()
                .map(s -> mirror(s, mirror))
                .collect(Collectors.toList());
    }
}
