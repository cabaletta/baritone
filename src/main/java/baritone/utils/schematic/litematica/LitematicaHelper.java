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
import net.minecraft.util.math.Vec3i;

import java.io.File;

public final class LitematicaHelper {
    public static boolean isLitematicaPresent() {
        try {
            Class.forName(Litematica.class.getName());
            return true;
        } catch (ClassNotFoundException | NoClassDefFoundError ex) {
            return false;
        }
    }
    public static boolean hasLoadedSchematic() {
        return DataManager.getSchematicPlacementManager().getAllSchematicsPlacements().size()>0;
    }
    public static String getName(int i) {
        return DataManager.getSchematicPlacementManager().getAllSchematicsPlacements().get(i).getName();
    }
    public static Vec3i getOrigin(int i) {
        int x,y,z;
        x=DataManager.getSchematicPlacementManager().getAllSchematicsPlacements().get(i).getOrigin().getX();
        y=DataManager.getSchematicPlacementManager().getAllSchematicsPlacements().get(i).getOrigin().getY();
        z=DataManager.getSchematicPlacementManager().getAllSchematicsPlacements().get(i).getOrigin().getZ();
        return new Vec3i(x,y,z);
    }
    public static File getSchematicFile(int i) {
        return DataManager.getSchematicPlacementManager().getAllSchematicsPlacements().get(i).getSchematicFile();
    }
}
























