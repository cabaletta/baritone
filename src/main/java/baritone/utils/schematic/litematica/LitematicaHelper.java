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

import com.github.lunatrius.schematica.Schematica;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import net.minecraft.util.math.Vec3i;

import java.io.File;
import java.util.List;

public enum LitematicaHelper { ;
    public static boolean isLitematicaPresent() {
        try {
            Class.forName(Schematica.class.getName());
            return true;
        } catch (ClassNotFoundException | NoClassDefFoundError ex) {
            return false;
        }
    }
    public static String getName(List<SchematicPlacement> placementList, int i) {
        return placementList.get(i).getName();
    }
    public static Vec3i getOrigin(List<SchematicPlacement> placementList, int i) {
        int x,y,z;
        x=placementList.get(i).getOrigin().getX();
        y=placementList.get(i).getOrigin().getY();
        z=placementList.get(i).getOrigin().getZ();
        return new Vec3i(x,y,z);
    }
    public static File getSchematicFile(List<SchematicPlacement> placementList, int i) {
        return placementList.get(i).getSchematicFile();
    }
}
























