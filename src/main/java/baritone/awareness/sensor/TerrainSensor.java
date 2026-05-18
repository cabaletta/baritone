package baritone.awareness.sensor;

import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.IPlayerContext;
import baritone.awareness.model.TerrainSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Analyses the terrain around the player on two schedules:
 * - Light scan (every tick): escape directions and fall danger in a 3-block radius.
 * - Heavy scan (every 10 ticks): hazards, safe positions, blast-resistant blocks in 8-block radius.
 */
public final class TerrainSensor {

    private static final int HEAVY_SCAN_INTERVAL = 10;

    private final IPlayerContext ctx;
    private int tickCount = 0;
    private TerrainSnapshot snapshot = new TerrainSnapshot();

    public TerrainSensor(IPlayerContext ctx) {
        this.ctx = ctx;
    }

    public void update() {
        tickCount++;
        BetterBlockPos feet = ctx.playerFeet();
        snapshot = (tickCount % HEAVY_SCAN_INTERVAL == 0) ? heavyScan(feet) : lightScan(feet);
    }

    private TerrainSnapshot lightScan(BetterBlockPos feet) {
        TerrainSnapshot s = new TerrainSnapshot();
        s.escapeDirections = findEscapeDirections(feet, 3);
        s.fallDanger = isFallDanger(feet);
        return s;
    }

    private TerrainSnapshot heavyScan(BetterBlockPos feet) {
        TerrainSnapshot s = new TerrainSnapshot();
        s.escapeDirections = findEscapeDirections(feet, 8);
        s.safePositions = findSafePositions(feet, 8);
        s.fallDanger = isFallDanger(feet);

        float nearestDist = 32f;
        TerrainSnapshot.HazardType nearestType = TerrainSnapshot.HazardType.NONE;
        int blastCount = 0;

        for (int dx = -8; dx <= 8; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -8; dz <= 8; dz++) {
                    BlockPos pos = new BlockPos(feet.x + dx, feet.y + dy, feet.z + dz);
                    float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

                    if (ctx.world().getBlockState(pos).getFluidState().is(Fluids.LAVA)
                        || ctx.world().getBlockState(pos).getFluidState().is(Fluids.FLOWING_LAVA)) {
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nearestType = TerrainSnapshot.HazardType.LAVA;
                        }
                    }

                    if (dy == 0 && ctx.world().getBlockState(pos).getBlock().getExplosionResistance() > 100f) {
                        blastCount++;
                    }
                }
            }
        }

        s.nearestHazardDistance = nearestDist;
        s.nearestHazardType = nearestType;
        s.nearbyBlastResistantCount = blastCount;
        return s;
    }

    private boolean isFallDanger(BetterBlockPos feet) {
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : dirs) {
            BlockPos adj = new BlockPos(feet.x + d[0], feet.y, feet.z + d[1]);
            if (ctx.world().getBlockState(adj).isAir()) {
                int drop = 0;
                for (int dy = 1; dy <= 4; dy++) {
                    if (ctx.world().getBlockState(
                        new BlockPos(adj.getX(), feet.y - dy, adj.getZ())).isAir()) {
                        drop++;
                    } else {
                        break;
                    }
                }
                if (drop >= 3) return true;
            }
        }
        return false;
    }

    private List<Vec3> findEscapeDirections(BetterBlockPos feet, int dist) {
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
        List<Vec3> result = new ArrayList<>();
        for (int[] d : dirs) {
            int nx = feet.x + d[0] * dist;
            int nz = feet.z + d[1] * dist;
            for (int dy = -1; dy <= 1; dy++) {
                BlockPos ground = new BlockPos(nx, feet.y + dy, nz);
                BlockPos body1 = new BlockPos(nx, feet.y + dy + 1, nz);
                BlockPos body2 = new BlockPos(nx, feet.y + dy + 2, nz);
                if (!ctx.world().getBlockState(ground).isAir()
                    && ctx.world().getBlockState(body1).isAir()
                    && ctx.world().getBlockState(body2).isAir()) {
                    result.add(new Vec3(d[0], 0, d[1]).normalize());
                    break;
                }
            }
        }
        return result;
    }

    private List<BlockPos> findSafePositions(BetterBlockPos feet, int radius) {
        List<BlockPos> safe = new ArrayList<>();
        for (int dx = -radius; dx <= radius && safe.size() < 8; dx += 2) {
            for (int dz = -radius; dz <= radius && safe.size() < 8; dz += 2) {
                int nx = feet.x + dx;
                int nz = feet.z + dz;
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos ground = new BlockPos(nx, feet.y + dy, nz);
                    BlockPos body1 = new BlockPos(nx, feet.y + dy + 1, nz);
                    BlockPos body2 = new BlockPos(nx, feet.y + dy + 2, nz);
                    if (!ctx.world().getBlockState(ground).isAir()
                        && ctx.world().getBlockState(body1).isAir()
                        && ctx.world().getBlockState(body2).isAir()) {
                        safe.add(body1);
                        break;
                    }
                }
            }
        }
        return safe;
    }

    public TerrainSnapshot getSnapshot() {
        return snapshot;
    }
}
