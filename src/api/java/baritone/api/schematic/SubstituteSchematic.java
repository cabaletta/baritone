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

import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubstituteSchematic extends AbstractSchematic {

    private final ISchematic schematic;
    private final Map<Block, List<Block>> substitutions;
    private final Map<BlockState, Map<Block, BlockState>> blockStateCache = new HashMap<>();

    public SubstituteSchematic(ISchematic schematic, Map<Block, List<Block>> substitutions) {
        super(schematic.widthX(), schematic.heightY(), schematic.lengthZ());
        this.schematic = schematic;
        this.substitutions = substitutions;
    }

    @Override
    public boolean inSchematic(int x, int y, int z, BlockState currentState) {
        return schematic.inSchematic(x, y, z, currentState);
    }

    @Override
    public BlockState desiredState(int x, int y, int z, BlockState current, List<BlockState> approxPlaceable) {
        BlockState desired = schematic.desiredState(x, y, z, current, approxPlaceable);
        Block desiredBlock = desired.getBlock();
        if (!substitutions.containsKey(desiredBlock)) {
            return desired;
        }
        List<Block> substitutes = substitutions.get(desiredBlock);
        if (substitutes.contains(current.getBlock()) && !(current.getBlock() instanceof AirBlock)) {// don't preserve air, it's almost always there and almost never wanted
            return withBlock(desired, current.getBlock());
        }
        for (Block substitute : substitutes) {
            if (substitute instanceof AirBlock) {
                return current.getBlock() instanceof AirBlock ? current : Blocks.AIR.defaultBlockState(); // can always "place" air
            }
            for (BlockState placeable : approxPlaceable) {
                if (substitute.equals(placeable.getBlock())) {
                    return withBlock(desired, placeable.getBlock());
                }
            }
        }
        return substitutes.get(0).defaultBlockState();
    }

    private BlockState withBlock(BlockState state, Block block) {
        if (blockStateCache.containsKey(state) && blockStateCache.get(state).containsKey(block)) {
            return blockStateCache.get(state).get(block);
        }
        Collection<Property<?>> properties = state.getProperties();
        BlockState newState = block.defaultBlockState();
        for (Property<?> property : properties) {
            try {
                newState = copySingleProp(state, newState, property);
            } catch (IllegalArgumentException e) { //property does not exist for target block
            }
        }
        blockStateCache.computeIfAbsent(state, s -> new HashMap<Block, BlockState>()).put(block, newState);
        return newState;
    }

    private <T extends Comparable<T>> BlockState copySingleProp(BlockState fromState, BlockState toState, Property<T> prop) {
        return toState.setValue(prop, fromState.getValue(prop));
    }
}
