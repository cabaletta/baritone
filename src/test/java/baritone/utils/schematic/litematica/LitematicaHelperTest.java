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

package baritone.utils.schematic.litematica;

import baritone.utils.schematic.format.defaults.LitematicaSchematic;
import junit.framework.TestCase;
import fi.dy.masa.litematica.data.DataManager;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LitematicaHelperTest extends TestCase {

    public void testGetSchematicFormat() {
        System.out.println(LitematicaHelper.isLitematicaPresent());
        //assertFalse(LitematicaHelper.isLitematicaPresent());
    }

    public void testFngThis() {
        DataManager.getSchematicPlacementManager();
        System.out.println(DataManager.getSchematicPlacementManager().getAllSchematicsPlacements());
        Path resourcePath;
//        try {
//            resourcePath = Paths.get(getClass().getClassLoader().getResource("schematics/litematica/subregion test1.litematic").toURI());
//            // Use the resourcePath here
//        //    LitematicaSchematic schematic1 = new LitematicaSchematic(NbtIo.readCompressed(Files.newInputStream(resourcePath), NbtAccounter.unlimitedHeap()), false);
//        } catch (URISyntaxException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

        System.out.println(getClass().getClassLoader().getResource("schematics/litematica/subregion test1.litematic").getPath());
    }
}
