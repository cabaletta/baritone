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
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.data.DataManager;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
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
    public static Rotation getRotation(int i) {
        return DataManager.getSchematicPlacementManager().getAllSchematicsPlacements().get(i).getRotation();
    }
    public static Mirror getMirror(int i) {
        return DataManager.getSchematicPlacementManager().getAllSchematicsPlacements().get(i).getMirror();
    }
    public static Vec3i getCorrectedOrigin(Vec3i origin, Vec3i correction) {
        return new Vec3i(origin.getX()+ correction.getX(), origin.getY() + correction.getY(), origin.getZ() + correction.getZ());
    }
    public static Vec3i doMirroring(Vec3i in, int sizeX, int sizeZ, Mirror mirror) {
        int xOut = in.getX();
        int zOut = in.getZ();
        if(mirror == Mirror.LEFT_RIGHT) {
            zOut = sizeZ - in.getZ();
        } else if (mirror == Mirror.FRONT_BACK) {
            xOut = sizeX - in.getX();
        }
        return new Vec3i(xOut, in.getY(), zOut);
    }
    public static Vec3i rotate(Vec3i in, int sizeX, int sizeZ) {
        return new  Vec3i(sizeX - (sizeX - sizeZ) - in.getZ(), in.getY(), in.getX());
    }
    public static LitematicaSchematic blackMagicFuckery(LitematicaSchematic schemIn, int i) {
        LitematicaSchematic tempSchem = schemIn.getCopy(LitematicaHelper.getRotation(i).ordinal()%2==1);
        for (int yCounter=0; yCounter<schemIn.getY(); yCounter++) {
            for (int zCounter=0; zCounter<schemIn.getZ(); zCounter++) {
                for (int xCounter=0; xCounter<schemIn.getX(); xCounter++) {
                    Vec3i xyzHolder = new Vec3i(xCounter, yCounter, zCounter);
                    //System.out.println(String.format("In: %s, sizeX=%S, sizeZ=%s",xyzHolder,schemIn.getX(),schemIn.getZ()));
                    xyzHolder = LitematicaHelper.doMirroring(xyzHolder, schemIn.getX() - 1, schemIn.getZ() - 1, LitematicaHelper.getMirror(i));
                    //System.out.println(String.format("Mirror: %s, sizeX=%S, sizeZ=%s",xyzHolder,schemIn.getX(),schemIn.getZ()));
                    for (int turns = 0; turns < LitematicaHelper.getRotation(i).ordinal(); turns++) {
                        if ((turns%2)==0) {
                            xyzHolder = LitematicaHelper.rotate(xyzHolder, schemIn.getX() - 1, schemIn.getZ() - 1);
                        } else {
                            xyzHolder = LitematicaHelper.rotate(xyzHolder, schemIn.getZ() - 1, schemIn.getX() - 1);
                        }
                        //System.out.println(String.format("Turned: %s, sizeX=%S, sizeZ=%s",xyzHolder,schemIn.getX(),schemIn.getZ()));
                    }
                    tempSchem.setDirect(xyzHolder.getX(), xyzHolder.getY(), xyzHolder.getZ(), schemIn.getDirect(xCounter, yCounter, zCounter));
                }
            }
        }
        return tempSchem;
    }
}