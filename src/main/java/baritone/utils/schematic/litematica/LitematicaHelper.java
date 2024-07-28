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

import baritone.api.schematic.AbstractSchematic;
import baritone.api.schematic.IStaticSchematic;
import baritone.api.schematic.ISchematic;
import baritone.api.schematic.CompositeSchematic;
import baritone.api.schematic.MirroredSchematic;
import baritone.api.schematic.RotatedSchematic;
import baritone.utils.schematic.StaticSchematic;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Collections;
import java.util.Map;

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
     * @param in     the xyz offsets of the block relative to the schematic minimum corner.
     * @param sizeX  size of the schematic in the x-axis direction.
     * @param sizeZ  size of the schematic in the z-axis direction.
     * @param mirror the mirroring of the schematic placement.
     * @return the corresponding xyz coordinates after mirroring them according to the given mirroring.
     */
    static Vec3i doMirroring(Vec3i in, int sizeX, int sizeZ, Mirror mirror) {
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
    static Vec3i rotate(Vec3i in, int sizeX, int sizeZ, Rotation rotation) {
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
    public static Tuple<IStaticSchematic, Vec3i> getSchematic(int i) {
        // annoying fun fact: you can't just work in placement coordinates and then apply
        // the placement rotation/mirror to the result because litematica applies the
        // global transforms *before* applying the local transforms
        SchematicPlacement placement = getPlacement(i);
        CompositeSchematic composite = new CompositeSchematic(0, 0, 0);
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        for (Map.Entry<String, SubRegionPlacement> entry : placement.getEnabledRelativeSubRegionPlacements().entrySet()) {
            SubRegionPlacement subPlacement = entry.getValue();
            Vec3i pos = subPlacement.getPos();
            Vec3i size = placement.getSchematic().getAreaSize(entry.getKey());
            minX = Math.min(pos.getX() + Math.min(size.getX() + 1, 0), minX);
            minY = Math.min(pos.getY() + Math.min(size.getY() + 1, 0), minY);
            minZ = Math.min(pos.getZ() + Math.min(size.getZ() + 1, 0), minZ);
        }
        for (Map.Entry<String, SubRegionPlacement> entry : placement.getEnabledRelativeSubRegionPlacements().entrySet()) {
            SubRegionPlacement subPlacement = entry.getValue();
            Vec3i size = placement.getSchematic().getAreaSize(entry.getKey());
            LitematicaBlockStateContainer container = placement.getSchematic().getSubRegionContainer(entry.getKey());
            BlockState[][][] states = new BlockState[Math.abs(size.getX())][Math.abs(size.getZ())][Math.abs(size.getY())];
            for (int x = 0; x < states.length; x++) {
                for (int z = 0; z < states[x].length; z++) {
                    for (int y = 0; y < states[x][z].length; y++) {
                        states[x][z][y] = container.get(x, y, z);
                    }
                }
            }
            ISchematic schematic = new StaticSchematic(states);
            Mirror mirror = subPlacement.getMirror();
            Rotation rotation = subPlacement.getRotation();
            if (placement.getRotation() == Rotation.CLOCKWISE_90 || placement.getRotation() == Rotation.COUNTERCLOCKWISE_90) {
                mirror = mirror == Mirror.LEFT_RIGHT ? Mirror.FRONT_BACK : Mirror.LEFT_RIGHT;
            }
            if (placement.getMirror() != Mirror.NONE) {
                rotation = rotation.getRotated(rotation).getRotated(rotation); // inverse rotation
            }
            schematic = new MirroredSchematic(schematic, mirror);
            schematic = new RotatedSchematic(schematic, rotation);
            int mx = Math.min(size.getX() + 1, 0);
            int my = Math.min(size.getY() + 1, 0);
            int mz = Math.min(size.getZ() + 1, 0);
            int sx = 2 - Math.abs(size.getX()); // this is because the position needs to be adjusted
            int sz = 2 - Math.abs(size.getZ()); // by widthX/lengthZ after every transformation
            Vec3i minCorner = new Vec3i(mx, my, mz);
            minCorner = rotate(doMirroring(minCorner, sx, sz, mirror), sx, sz, rotation);
            Vec3i pos = subPlacement.getPos().offset(minCorner).offset(-minX, -minY, -minZ);
            composite.put(schematic, pos.getX(), pos.getY(), pos.getZ());
        }
        int sx = 2 - composite.widthX(); // this is because the position needs to be adjusted
        int sz = 2 - composite.lengthZ(); // by widthX/lengthZ after every transformation
        Vec3i minCorner = new Vec3i(minX, minY, minZ);
        Mirror mirror = placement.getMirror();
        Rotation rotation = placement.getRotation();
        minCorner = rotate(doMirroring(minCorner, sx, sz, mirror), sx, sz, rotation);
        ISchematic schematic = new MirroredSchematic(composite, mirror);
        schematic = new RotatedSchematic(schematic, rotation);
        return new Tuple<>(new LitematicaPlacementSchematic(schematic), placement.getOrigin().offset(minCorner));
    }

    private static class LitematicaPlacementSchematic extends AbstractSchematic implements IStaticSchematic {
        private final ISchematic schematic;

        public LitematicaPlacementSchematic(ISchematic schematic) {
            super(schematic.widthX(), schematic.heightY(), schematic.lengthZ());
            this.schematic = schematic;
        }

        @Override
        public BlockState getDirect(int x, int y, int z) {
            if (inSchematic(x, y, z, null)) {
                return desiredState(x, y, z, null, Collections.emptyList());
            }
            return null;
        }

        @Override
        public BlockState desiredState(int x, int y, int z, BlockState current, List<BlockState> approxPlaceable) {
            return schematic.desiredState(x, y, z, current, approxPlaceable);
        }

        @Override
        public boolean inSchematic(int x, int y, int z, BlockState current) {
            return schematic.inSchematic(x, y, z, current);
        }
    }
}