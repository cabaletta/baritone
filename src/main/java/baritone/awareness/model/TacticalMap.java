package baritone.awareness.model;

import baritone.api.utils.BetterBlockPos;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;

/**
 * Sparse per-block safety scores keyed by packed BlockPos longs.
 * Positive values are safe; negative values are danger zones.
 * Used by future Avoidance integration.
 */
public final class TacticalMap {

    private final Long2FloatOpenHashMap map = new Long2FloatOpenHashMap();

    public TacticalMap() {
        map.defaultReturnValue(0f);
    }

    public void set(int x, int y, int z, float score) {
        map.put(BetterBlockPos.longHash(x, y, z), score);
    }

    public float get(int x, int y, int z) {
        return map.get(BetterBlockPos.longHash(x, y, z));
    }

    public boolean isSafe(int x, int y, int z) {
        return get(x, y, z) >= 0f;
    }

    public void markDanger(int x, int y, int z, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz <= radius * radius) {
                        set(x + dx, y + dy, z + dz, -1f);
                    }
                }
            }
        }
    }

    public void clear() {
        map.clear();
    }
}
