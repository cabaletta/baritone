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

import baritone.api.schematic.IStaticSchematic;
import baritone.utils.schematic.StaticSchematic;
import baritone.utils.schematic.format.defaults.LitematicaSchematic;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.nio.file.Files;

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
     * @return if {@code i} is a valid placement index
     */
    public static boolean hasLoadedSchematic(int i) {
        return 0 <= i && i < DataManager.getSchematicPlacementManager().getAllSchematicsPlacements().size();
    }

    private static SchematicPlacement getPlacement(int i) {
        return DataManager.getSchematicPlacementManager().getAllSchematicsPlacements().get(i);
    }

    /**
     * @param i index of the Schematic in the schematic placement list.
     * @return the name of the requested schematic.
     */
    public static String getName(int i) {
        return getPlacement(i).getName();
    }

    /**
     * @param schematic original schematic.
     * @param i         index of the Schematic in the schematic placement list.
     * @return the minimum corner coordinates of the schematic, after the original schematic got rotated and mirrored.
     */
    private static Vec3i getCorrectedOrigin(SchematicPlacement placement, LitematicaSchematic schematic) {
        Vec3i origin = placement.getOrigin();
        Vec3i minCorner = schematic.getOffsetMinCorner();
        int sx = 2 - schematic.widthX(); // this is because the position needs to be adjusted
        int sz = 2 - schematic.lengthZ(); // by widthX/lengthZ after every transformation

        Mirror mirror = placement.getMirror();
        Rotation rotation = placement.getRotation();

        return origin.offset(rotate(doMirroring(minCorner, sx, sz, mirror), sx, sz, rotation));
    }

    /**
     * @param in     the xyz offsets of the block relative to the schematic minimum corner.
     * @param sizeX  size of the schematic in the x-axis direction.
     * @param sizeZ  size of the schematic in the z-axis direction.
     * @param mirror the mirroring of the schematic placement.
     * @return the corresponding xyz coordinates after mirroring them according to the given mirroring.
     */
    private static Vec3i doMirroring(Vec3i in, int sizeX, int sizeZ, Mirror mirror) {
        int xOut = in.getX();
        int zOut = in.getZ();
        if (mirror == Mirror.LEFT_RIGHT) {
            zOut = sizeZ - 1 - in.getZ();
        } else if (mirror == Mirror.FRONT_BACK) {
            xOut = sizeX - 1 - in.getX();
        }
        return new Vec3i(xOut, in.getY(), zOut);
    }

    /**
     * @param in    the xyz offsets of the block relative to the schematic minimum corner.
     * @param sizeX size of the schematic in the x-axis direction.
     * @param sizeZ size of the schematic in the z-axis direction.
     * @param rotation the rotation to apply
     * @return the corresponding xyz coordinates after applying {@code rotation}.
     */
    private static Vec3i rotate(Vec3i in, int sizeX, int sizeZ, Rotation rotation) {
        switch (rotation) {
            case CLOCKWISE_90:
                return new Vec3i(sizeZ - 1 - in.getZ(), in.getY(), in.getX());
            case CLOCKWISE_180:
                return new Vec3i(sizeX - 1 - in.getX(), in.getY(), sizeZ - 1 - in.getZ());
            case COUNTERCLOCKWISE_90:
                return new Vec3i(in.getZ(), in.getY(), sizeX - 1 - in.getX());
            default:
                return in;
        }
    }

    /**
     * @param schemIn give in the original schematic.
     * @param i       index of the Schematic in the schematic placement list.
     * @return get it out rotated and mirrored.
     */
    public static Tuple<IStaticSchematic, Vec3i> getSchematic(int i) throws IOException {
        SchematicPlacement placement = getPlacement(i);
        Rotation rotation = placement.getRotation();
        Mirror mirror = placement.getMirror();
        LitematicaSchematic schemIn = new LitematicaSchematic(NbtIo.readCompressed(Files.newInputStream(placement.getSchematicFile().toPath())));
        Vec3i origin = getCorrectedOrigin(placement, schemIn);
        boolean flip = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
        BlockState[][][] states = new BlockState[flip ? schemIn.lengthZ() : schemIn.widthX()][flip ? schemIn.widthX() : schemIn.lengthZ()][schemIn.heightY()];
        for (int yCounter = 0; yCounter < schemIn.heightY(); yCounter++) {
            for (int zCounter = 0; zCounter < schemIn.lengthZ(); zCounter++) {
                for (int xCounter = 0; xCounter < schemIn.widthX(); xCounter++) {
                    Vec3i xyzHolder = new Vec3i(xCounter, yCounter, zCounter);
                    xyzHolder = LitematicaHelper.doMirroring(xyzHolder, schemIn.widthX(), schemIn.lengthZ(), mirror);
                    xyzHolder = rotate(xyzHolder, schemIn.widthX(), schemIn.lengthZ(), rotation);
                    BlockState state = schemIn.getDirect(xCounter, yCounter, zCounter);
                    state = state == null ? null : state.mirror(mirror).rotate(rotation);
                    states[xyzHolder.getX()][xyzHolder.getZ()][xyzHolder.getY()] = state;
                }
            }
        }
        return new Tuple<>(new StaticSchematic(states), origin);
    }
}