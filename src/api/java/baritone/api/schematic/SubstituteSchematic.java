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

import baritone.api.utils.BlockOptionalMetaLookup;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import java.util.List;
import java.util.Map;

public class SubstituteSchematic extends AbstractSchematic {

    private final ISchematic schematic;
    private final Map<Block, List<Block>> substitutions;

    public SubstituteSchematic(ISchematic schematic, Map<Block,List<Block>> substitutions) {
        super(schematic.widthX(), schematic.heightY(), schematic.lengthZ());
        this.schematic = schematic;
        this.substitutions = substitutions;
    }

    @Override
    public boolean inSchematic(int x, int y, int z, IBlockState currentState) {
        return schematic.inSchematic(x, y, z, currentState);
    }

    @Override
    public IBlockState desiredState(int x, int y, int z, IBlockState current, List<IBlockState> approxPlaceable) {
        IBlockState desired = schematic.desiredState(x, y, z, current, approxPlaceable);
        Block desiredBlock = desired.getBlock();
        if (!substitutions.containsKey(desiredBlock)) {
            return desired;
        }
        List<Block> substitutes = substitutions.get(desiredBlock);
        if (substitutes.contains(current.getBlock()) && !current.getBlock().equals(Blocks.AIR)) {// don't preserve air, it's almost always there and almost never wanted
            System.out.println(String.format("%s is already placed", current));
            return current;
        }
        for (Block substitute : substitutes) {
            if (substitute.equals(Blocks.AIR)) {
                System.out.println("air, lol");
                return Blocks.AIR.getDefaultState(); // can always "place" air
            }
            for (IBlockState placeable : approxPlaceable) {
                if (substitute.equals(placeable.getBlock())) {
                    System.out.println(String.format("%s can be placed", placeable));
                    return placeable;
                }
            }
            System.out.println(String.format("%s is not an option", substitute));
        }
        System.out.println(String.format("%s fail", substitutes.get(0).getDefaultState()));
        return substitutes.get(0).getDefaultState();
    }
}
