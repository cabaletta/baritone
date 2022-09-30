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

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import net.minecraft.util.math.Vec3i;

import java.io.File;
import java.util.List;

public final class LitematicaHelper {
    public static boolean isLitematicaPresent() {
        try {
            Class.forName(Litematica.class.getName());
            return true;
        } catch (ClassNotFoundException | NoClassDefFoundError ex) {
            return false;
        }
    }
    //TODO compact into one line when debugging is done
    public static boolean hasLoadedSchematic() {
        System.out.println("start checking for schematic"); //TODO debug line remove
        SchematicPlacementManager a = DataManager.getSchematicPlacementManager();
        System.out.println("manager aquired"); //TODO debug line remove
        List< SchematicPlacement> b = a.getAllSchematicPlacements();
        System.out.println("list aquired"); //TODO debug line remove
        return b.size()>0;
    }
    public static String getName(int i) {
        return DataManager.getSchematicPlacementManager().getAllSchematicPlacements().get(i).getName();
    }
    public static Vec3i getOrigin(int i) {
        int x,y,z;
        x=DataManager.getSchematicPlacementManager().getAllSchematicPlacements().get(i).getOrigin().getX();
        y=DataManager.getSchematicPlacementManager().getAllSchematicPlacements().get(i).getOrigin().getY();
        z=DataManager.getSchematicPlacementManager().getAllSchematicPlacements().get(i).getOrigin().getZ();
        return new Vec3i(x,y,z);
    }
    public static File getSchematicFile(int i) {
        return DataManager.getSchematicPlacementManager().getAllSchematicPlacements().get(i).getSchematicFile();
    }
}
























