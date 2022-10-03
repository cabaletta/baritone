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
import net.minecraft.block.state.IBlockState;
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
    public static Vec3i getCorrectedOrigin(LitematicaSchematic schematic, int i) {
        int x = LitematicaHelper.getOrigin(i).getX();
        int y = LitematicaHelper.getOrigin(i).getY();
        int z = LitematicaHelper.getOrigin(i).getZ();
        int mx = schematic.getMinimumCorner().getX();
        int my = schematic.getMinimumCorner().getY();
        int mz = schematic.getMinimumCorner().getZ();
        int sx = (schematic.getX() - 1) * -1;
        int sz = (schematic.getZ() - 1) * -1;
        Vec3i correctedOrigin;
        Mirror mirror = LitematicaHelper.getMirror(i);
        Rotation rotation = LitematicaHelper.getRotation(i);

        //todo there has to be a better way to do this but i cant finde it atm
        switch (mirror) {
            case FRONT_BACK:
            case LEFT_RIGHT:
                switch ((mirror.ordinal()*2+rotation.ordinal())%4) {
                    case 1:
                        correctedOrigin = new Vec3i(x + (sz - mz), y + my, z + (sx - mx));
                        break;
                    case 2:
                        correctedOrigin = new Vec3i(x + mx, y + my, z + (sz - mz));
                        break;
                    case 3:
                        correctedOrigin = new Vec3i(x + mz, y + my, z + mx);
                        break;
                    default:
                        correctedOrigin = new Vec3i(x + (sx - mx), y + my, z + mz);
                        break;
                }
                break;
            default:
                switch (rotation) {
                    case CLOCKWISE_90:
                        correctedOrigin = new Vec3i(x + (sz - mz), y + my, z + mz);
                        break;
                    case CLOCKWISE_180:
                        correctedOrigin = new Vec3i(x + (sx - mx), y + my, z + (sz - mz));
                        break;
                    case COUNTERCLOCKWISE_90:
                        correctedOrigin = new Vec3i(x + mz, y + my, z + (sx - mx));
                        break;
                    default:
                        correctedOrigin = new Vec3i(x + mx, y + my, z + mz);
                        break;
                }
        }
        return correctedOrigin;
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
                    System.out.println(String.format("In: %s, sizeX=%S, sizeZ=%s",xyzHolder,schemIn.getX(),schemIn.getZ()));
                    xyzHolder = LitematicaHelper.doMirroring(xyzHolder, schemIn.getX() - 1, schemIn.getZ() - 1, LitematicaHelper.getMirror(i));
                    System.out.println(String.format("Mirror: %s, sizeX=%S, sizeZ=%s",xyzHolder,schemIn.getX(),schemIn.getZ()));
                    for (int turns = 0; turns < LitematicaHelper.getRotation(i).ordinal(); turns++) {
                        if ((turns%2)==0) {
                            xyzHolder = LitematicaHelper.rotate(xyzHolder, schemIn.getX() - 1, schemIn.getZ() - 1);
                        } else {
                            xyzHolder = LitematicaHelper.rotate(xyzHolder, schemIn.getZ() - 1, schemIn.getX() - 1);
                        }
                        System.out.println(String.format("Turned: %s, sizeX=%S, sizeZ=%s",xyzHolder,schemIn.getX(),schemIn.getZ()));
                    }
                    IBlockState state = schemIn.getDirect(xCounter, yCounter, zCounter);
                    try {
                        state.withMirror(LitematicaHelper.getMirror(i)).withRotation(LitematicaHelper.getRotation(i));
                    } catch (NullPointerException e) {
                        System.out.println("nullpointerexception aka schemblock that is void");
                    }
                    tempSchem.setDirect(xyzHolder.getX(), xyzHolder.getY(), xyzHolder.getZ(), state);
                }
            }
        }
        return tempSchem;
    }
}