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

import baritone.api.schematic.CompositeSchematic;
import baritone.api.schematic.IStaticSchematic;
import baritone.utils.schematic.StaticSchematic;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.HashMap;
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

    private static Vec3i transform(Vec3i in, Mirror mirror, Rotation rotation) {
        int x = in.getX();
        int z = in.getZ();
        if (mirror == Mirror.LEFT_RIGHT) {
            z = -z;
        } else if (mirror == Mirror.FRONT_BACK) {
            x = -x;
        }
        switch (rotation) {
            case CLOCKWISE_90:
                return new Vec3i(-z, in.getY(), x);
            case CLOCKWISE_180:
                return new Vec3i(-x, in.getY(), -z);
            case COUNTERCLOCKWISE_90:
                return new Vec3i(z, in.getY(), -x);
            default:
                return new Vec3i(x, in.getY(), z);
        }
    }

    /**
     * @param i   index of the Schematic in the schematic placement list.
     * @return    The transformed schematic and the position of its minimum corner
     */
    public static Tuple<IStaticSchematic, Vec3i> getSchematic(int i) {
        SchematicPlacement placement = getPlacement(i);
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        HashMap<Vec3i, StaticSchematic> subRegions = new HashMap<>();
        WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();
        for (Map.Entry<String, SubRegionPlacement> entry : placement.getEnabledRelativeSubRegionPlacements().entrySet()) {
            SubRegionPlacement subPlacement = entry.getValue();
            Vec3i pos = transform(subPlacement.getPos(), placement.getMirror(), placement.getRotation());
            Vec3i size = placement.getSchematic().getAreaSize(entry.getKey());
            size = transform(size, placement.getMirror(), placement.getRotation());
            size = transform(size, subPlacement.getMirror(), subPlacement.getRotation());
            int mx = Math.min(size.getX() + 1, 0);
            int my = Math.min(size.getY() + 1, 0);
            int mz = Math.min(size.getZ() + 1, 0);
            minX = Math.min(minX, pos.getX() + mx);
            minY = Math.min(minY, pos.getY() + my);
            minZ = Math.min(minZ, pos.getZ() + mz);
            BlockPos origin = placement.getOrigin().offset(pos).offset(mx, my, mz);
            BlockState[][][] states = new BlockState[Math.abs(size.getX())][Math.abs(size.getZ())][Math.abs(size.getY())];
            for (int x = 0; x < states.length; x++) {
                for (int z = 0; z < states[x].length; z++) {
                    for (int y = 0; y < states[x][z].length; y++) {
                        states[x][z][y] = schematicWorld.getBlockState(origin.offset(x, y, z));
                    }
                }
            }
            StaticSchematic schematic = new StaticSchematic(states);
            subRegions.put(pos.offset(mx, my, mz), schematic);
        }
        LitematicaPlacementSchematic composite = new LitematicaPlacementSchematic(placement.getName());
        for (Map.Entry<Vec3i, StaticSchematic> entry : subRegions.entrySet()) {
            Vec3i pos = entry.getKey().offset(-minX, -minY, -minZ);
            composite.put(entry.getValue(), pos.getX(), pos.getY(), pos.getZ());
        }
        return new Tuple<>(composite, placement.getOrigin().offset(minX, minY, minZ));
    }

    private static class LitematicaPlacementSchematic extends CompositeSchematic implements IStaticSchematic {
        private final String name;

        public LitematicaPlacementSchematic(String name) {
            super(0, 0, 0);
            this.name = name;
        }

        @Override
        public BlockState getDirect(int x, int y, int z) {
            if (inSchematic(x, y, z, null)) {
                return desiredState(x, y, z, null, Collections.emptyList());
            }
            return null;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}