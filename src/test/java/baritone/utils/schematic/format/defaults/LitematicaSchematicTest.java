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

import junit.framework.TestCase;

import net.minecraft.core.Vec3i;
import net.minecraft.nbt.ListTag;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.mockito.Mock;

//@RunWith(Parameterized.class)
public class LitematicaSchematicTest extends TestCase {

    class LitematicaSchematicTestDef {

    }

    @Parameterized.Parameters
    public static Collection<Object[]> configs() {
        Map<String, String> map1 = new HashMap<String, String>();
        map1.put("Name", "Bill");
        map1.put("Favourite Color", "Blue");

        Map<String, String> map2 = new HashMap<String, String>();
        map2.put("Name", "Sam");
        map2.put("Favourite Color", "Plaid");

        return Arrays.asList(new Object[][]{
                {map1},
                {map2}
        });
    }

    @Mock
    private ListTag blockStatePalette;

    @Before
    public void setup() {
        System.out.println("running setup");

        // Mock BuiltInRegistries and other game classes
        //BuiltInRegistries mockRegistries = mock(BuiltInRegistries.class);
        // Setup your mock behavior as needed
//        when(mockRegistries.get(any(ResourceKey.class))).thenReturn(mock(Item.class));
    }

    @Test
    public void testShouldReturnCorrectOffsetMinCorner() {

        LitematicaSchematic schematic = new LitematicaSchematic(new CompoundTag(), false);

        Vec3i expectedOffsetMinCorner = new Vec3i(2147483647, 2147483647, 2147483647);
        assertEquals(expectedOffsetMinCorner, schematic.getOffsetMinCorner());
    }

    public void testInSchematic() {
    }

    LitematicaSchematic getSchematicForResource(String resourcePath) {
        Path path;
        CompoundTag nbt;
        try {
            path = Paths.get(getClass().getClassLoader().getResource(resourcePath).toURI());
            nbt = NbtIo.readCompressed(Files.newInputStream(path), NbtAccounter.unlimitedHeap());
            return new LitematicaSchematic(nbt, false) {

                BlockState[] getBlockList2(ListTag blockStatePalette) {


                    BlockState[] blockList = new BlockState[blockStatePalette.size()];

                    for (int i = 0; i < blockStatePalette.size(); i++) {
                        ResourceLocation resLoc = new ResourceLocation((((CompoundTag) blockStatePalette.get(i)).getString("Name")));
                    }
                    return blockList;
                }

                @Override
                protected void fillInSchematic() {
                    for (String subReg : getRegions(nbt)) {
                        System.out.println("fillInSchematic for subregion: " + subReg);
                        ListTag usedBlockTypes = nbt.getCompound("Regions").getCompound(subReg).getList("BlockStatePalette", 10);

                        // this requires bootstrapped registry stuff
                        BlockState[] blockList = getBlockList2(usedBlockTypes);

                        //Block dirt = Blocks.DIRT;

                        int bitsPerBlock = getBitsPerBlock(usedBlockTypes.size());
                        long regionVolume = getVolume(nbt, subReg);
                        long[] blockStateArray = getBlockStates(nbt, subReg);

                        LitematicaBitArray bitArray = new LitematicaBitArray(bitsPerBlock, regionVolume, blockStateArray);

                        writeSubregionIntoSchematic(nbt, subReg, blockList, bitArray);
                    }
                }
            }
                    ;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Test of getBlockList method, of class LitematicaSchematic.
     * This schematic has 2 regions of size 1 separated by a block
     */
    @Test
    public void testGetXTest1() {


        LitematicaSchematic schematic = getSchematicForResource("schematics/litematica/subregion test1.litematic");

        // origin is in a region
        assertTrue(schematic.inSchematic(0, 0, 0, null));
        assertTrue(schematic.inSchematic(0, 0, 2, null));
        assertFalse(schematic.inSchematic(0, 0, 1, null));
        assertFalse(schematic.inSchematic(0, 1, 1, null));
        assertFalse(schematic.inSchematic(99, 1, 1, null));
    }

    /**
     * Test of getBlockList method, of class LitematicaSchematic.
     * This schematic has 2 regions separated by a block
     * origin is not in a region
     */
    @Test
    public void testGetXTest2() {


        LitematicaSchematic schematic = getSchematicForResource("schematics/litematica/subregion test2.litematic");

        // origin is not in a region
        assertFalse(schematic.inSchematic(0, 0, 0, null));
        assertFalse(schematic.inSchematic(0, 0, 1, null));
        assertTrue(schematic.inSchematic(0, 0, 2, null));
        assertFalse(schematic.inSchematic(0, 1, 1, null));
        assertFalse(schematic.inSchematic(99, 1, 1, null));
    }

    @Test
    public void testGetXTest3() {


        LitematicaSchematic schematic = getSchematicForResource("schematics/litematica/subregion test3.litematic");

        // origin is not in a region
        assertFalse(schematic.inSchematic(0, 0, 0, null));
        assertTrue(schematic.inSchematic(-4, 0, -1, null));
        assertFalse(schematic.inSchematic(-4, 0, -5, null));
//        assertFalse(schematic.inSchematic(0,0,1,null));

//        assertFalse(schematic.inSchematic(0,1,1,null));
//        assertFalse(schematic.inSchematic(99,1,1,null));
    }
    @Test
    public void testGetXTest8() {


        LitematicaSchematic schematic = getSchematicForResource("schematics/litematica/subregion test8.litematic");

        // origin is not in a region
        assertFalse(schematic.inSchematic(0, 0, 0, null));
        assertFalse(schematic.inSchematic(1, 0, 0, null));
        assertFalse(schematic.inSchematic(2, 0, 0, null));
        assertTrue(schematic.inSchematic(3, 0, 0, null));
        assertTrue(schematic.inSchematic(3, 1, 0, null));
        assertFalse(schematic.inSchematic(3, 2, 0, null));
        assertFalse(schematic.inSchematic(3, 0, 2, null));
        assertFalse(schematic.inSchematic(3, 1, 2, null));
//        assertFalse(schematic.inSchematic(0,0,1,null));

//        assertFalse(schematic.inSchematic(0,1,1,null));
//        assertFalse(schematic.inSchematic(99,1,1,null));
    }

    public void testGetY() {
    }

    public void testGetZ() {
    }
}
