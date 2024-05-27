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
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.io.File;

/**
 * Helper class that provides access or processes data related to Litmatica schematics.
 *
 * @author rycbar
 * @since 28.09.2022
 */
public final class LitematicaHelper {

    /**
     * @return if Litmatica is installed.
     */
    public static boolean isLitematicaPresent() {
        try {
            Class.forName(Litematica.class.getName());
            return true;
        } catch (ClassNotFoundException | NoClassDefFoundError ex) {
            return false;
        }
    }

    /**
     * @return if there are loaded schematics.
     */
    public static boolean hasLoadedSchematic() {
        return DataManager.getSchematicPlacementManager().getAllSchematicsPlacements().size() > 0;
    }

    /**
     * @param i index of the Schematic in the schematic placement list.
     * @return the name of the requested schematic.
     */
    public static String getName(int i) {
        return DataManager.getSchematicPlacementManager().getAllSchematicsPlacements().get(i).getName();
    }

    /**
     * @param i index of the Schematic in the schematic placement list.
     * @return the world coordinates of the schematic origin. This can but does not have to be the minimum corner.
     */
    public static Vec3i getOrigin(int i) {
        return DataManager.getSchematicPlacementManager().getAllSchematicsPlacements().get(i).getOrigin();
    }

    /**
     * @param i index of the Schematic in the schematic placement list.
     * @return Filepath of the schematic file.
     */
    public static File getSchematicFile(int i) {
        return DataManager.getSchematicPlacementManager().getAllSchematicsPlacements().get(i).getSchematicFile();
    }

    /**
     * @param i index of the Schematic in the schematic placement list.
     * @return rotation of the schematic placement.
     */
    public static Rotation getRotation(int i) {
        return DataManager.getSchematicPlacementManager().getAllSchematicsPlacements().get(i).getRotation();
    }

    /**
     * @param i index of the Schematic in the schematic placement list.
     * @return the mirroring of the schematic placement.
     */
    public static Mirror getMirror(int i) {
        return DataManager.getSchematicPlacementManager().getAllSchematicsPlacements().get(i).getMirror();
    }

    /**
     * @param schematic original schematic.
     * @param i         index of the Schematic in the schematic placement list.
     * @return the minimum corner coordinates of the schematic, after the original schematic got rotated and mirrored.
     */
    public static Vec3i getCorrectedOrigin(LitematicaSchematic schematic, int i) {

        System.out.println("in get Corrected origin");

        int x = LitematicaHelper.getOrigin(i).getX();
        int y = LitematicaHelper.getOrigin(i).getY();
        int z = LitematicaHelper.getOrigin(i).getZ();
        int mx = schematic.getOffsetMinCorner().getX();
        int my = schematic.getOffsetMinCorner().getY();
        int mz = schematic.getOffsetMinCorner().getZ();
        int sx = (schematic.getX() - 1) * -1;
        int sz = (schematic.getZ() - 1) * -1;

        Vec3i correctedOrigin;
        Mirror mirror = LitematicaHelper.getMirror(i);
        Rotation rotation = LitematicaHelper.getRotation(i);

        //todo there has to be a better way to do this but i cant finde it atm
        switch (mirror) {
            case FRONT_BACK:
            case LEFT_RIGHT:
                switch ((mirror.ordinal() * 2 + rotation.ordinal()) % 4) {
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
                        correctedOrigin = new Vec3i(x + (sz - mz), y + my, z + mx);
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

    /**
     * @param in     the xyz offsets of the block relative to the schematic minimum corner.
     * @param sizeX  size of the schematic in the x-axis direction.
     * @param sizeZ  size of the schematic in the z-axis direction.
     * @param mirror the mirroring of the schematic placement.
     * @return the corresponding xyz coordinates after mirroring them according to the given mirroring.
     */
    public static Vec3i doMirroring(Vec3i in, int sizeX, int sizeZ, Mirror mirror) {
        int xOut = in.getX();
        int zOut = in.getZ();
        if (mirror == Mirror.LEFT_RIGHT) {
            zOut = sizeZ - in.getZ();
        } else if (mirror == Mirror.FRONT_BACK) {
            xOut = sizeX - in.getX();
        }
        return new Vec3i(xOut, in.getY(), zOut);
    }

    /**
     * @param in    the xyz offsets of the block relative to the schematic minimum corner.
     * @param sizeX size of the schematic in the x-axis direction.
     * @param sizeZ size of the schematic in the z-axis direction.
     * @return the corresponding xyz coordinates after rotation them 90° clockwise.
     */
    public static Vec3i rotate(Vec3i in, int sizeX, int sizeZ) {
        return new Vec3i(sizeX - (sizeX - sizeZ) - in.getZ(), in.getY(), in.getX());
    }

    /**
     * IDFK this just grew and it somehow works. If you understand how, pls tell me.
     *
     * @param schemIn give in the original schematic.
     * @param i       index of the Schematic in the schematic placement list.
     * @return get it out rotated and mirrored.
     */
    public static LitematicaSchematic blackMagicFuckery(LitematicaSchematic schemIn, int i) {
        LitematicaSchematic tempSchem = schemIn.getCopy(LitematicaHelper.getRotation(i).ordinal() % 2 == 1);
        for (int yCounter = 0; yCounter < schemIn.getY(); yCounter++) {
            for (int zCounter = 0; zCounter < schemIn.getZ(); zCounter++) {
                for (int xCounter = 0; xCounter < schemIn.getX(); xCounter++) {
                    Vec3i xyzHolder = new Vec3i(xCounter, yCounter, zCounter);
                    xyzHolder = LitematicaHelper.doMirroring(xyzHolder, schemIn.getX() - 1, schemIn.getZ() - 1, LitematicaHelper.getMirror(i));
                    for (int turns = 0; turns < LitematicaHelper.getRotation(i).ordinal(); turns++) {
                        if ((turns % 2) == 0) {
                            xyzHolder = LitematicaHelper.rotate(xyzHolder, schemIn.getX() - 1, schemIn.getZ() - 1);
                        } else {
                            xyzHolder = LitematicaHelper.rotate(xyzHolder, schemIn.getZ() - 1, schemIn.getX() - 1);
                        }
                    }
                    BlockState state = schemIn.getDirect(xCounter, yCounter, zCounter);
                    try {
                        state = state.mirror(LitematicaHelper.getMirror(i)).rotate(LitematicaHelper.getRotation(i));
                    } catch (NullPointerException e) {
                        //nothing to worry about it's just a hole in the schematic.
                    }
                    tempSchem.setDirect(xyzHolder.getX(), xyzHolder.getY(), xyzHolder.getZ(), state);
                }
            }
        }
        return tempSchem;
    }
}
