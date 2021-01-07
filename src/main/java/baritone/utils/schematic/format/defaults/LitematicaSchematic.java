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

package baritone.utils.schematic.format.defaults;

import baritone.utils.schematic.StaticSchematic;
import baritone.utils.schematic.schematica.LitematicaBitArray;
import net.minecraft.block.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.state.Property;
import net.minecraft.util.registry.Registry;

import java.util.*;

/**
 * @author fencingparry4
 * @since 12/27/2020
 */
public final class LitematicaSchematic extends StaticSchematic {
    private String regionName;

    public LitematicaSchematic(CompoundNBT nbt) {
        regionName = (String) nbt.getCompound("Regions").keySet().toArray()[0];
        this.x = Math.abs(nbt.getCompound("Regions").getCompound(regionName).getCompound("Size").getInt("x"));
        this.y = Math.abs(nbt.getCompound("Regions").getCompound(regionName).getCompound("Size").getInt("y"));
        this.z = Math.abs(nbt.getCompound("Regions").getCompound(regionName).getCompound("Size").getInt("z"));
        this.states = new BlockState[this.x][this.z][this.y];


        ListNBT paletteTag = nbt.getCompound("Regions").getCompound(regionName).getList("BlockStatePalette",10);
        if (paletteTag == null) {
            System.out.println("Failed to get palette");
        }

        BlockState[] paletteBlockStates = new BlockState[paletteTag.size()];
        for (int i = 0; i<paletteTag.size(); i++) {
            Block block = Registry.BLOCK.getOrDefault(new ResourceLocation((((CompoundNBT) paletteTag.get(i)).getString("Name"))));
            BlockState blockState = block.getDefaultState();
            CompoundNBT properties = ((CompoundNBT) paletteTag.get(i)).getCompound("Properties");
            Object[] keys = properties.keySet().toArray();
            Map<String, String> propertiesMap = new HashMap<>();
            for (int j = 0; j<keys.length; j++) {
                propertiesMap.put((String) keys[j], (properties.get((String) keys[j])).toString());
            }
            for (int j = 0; j<keys.length; j++) {
                Property<?> property = block.getStateContainer().getProperty(keys[j].toString());
                if (property != null) {
                    //Thank you litematica for putting quotes around the key values
                    blockState = setPropertyValue(blockState, property, propertiesMap.get(keys[j]).substring(1,(propertiesMap.get(keys[j])).length()-1));
                }
            }
            paletteBlockStates[i] = blockState;
        }


        // BlockData is stored as an NBT long[]
        int paletteSize = (int) Math.floor(log2(paletteTag.size()))+1;
        long litematicSize = (long) this.x*this.y*this.z;

        long[] rawBlockData = nbt.getCompound("Regions").getCompound(regionName).getLongArray("BlockStates");

        LitematicaBitArray bitArray = new LitematicaBitArray(paletteSize,litematicSize,rawBlockData);
        if (paletteSize > 32) {
            throw new IllegalStateException("Too many blocks in schematic to handle");
        }

        int[] serializedBlockStates = new int[(int) litematicSize];
        for (int i = 0; i<serializedBlockStates.length; i++) {
            serializedBlockStates[i] = bitArray.getAt(i);
        }

        int counter = 0;
        for (int y = 0; y < this.y; y++) {
            for (int z = 0; z < this.z; z++) {
                for (int x = 0; x < this.x; x++) {
                    BlockState state = paletteBlockStates[serializedBlockStates[counter]];
                    this.states[x][z][y] = state;
                    counter++;
                }
            }
        }
    }
    private static double log2(int N) {
        return (Math.log(N) / Math.log(2));
    }

    private static <T extends Comparable<T>> BlockState setPropertyValue(BlockState state, Property<T> property, String value) {
        Optional<T> parsed = property.parseValue(value);
        if (parsed.isPresent()) {
            return state.with(property, parsed.get());
        } else {
            throw new IllegalArgumentException("Invalid value for property " + property);
        }
    }
}
