package baritone.awareness.model;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;

/** Result of a terrain analysis pass; updated by TerrainSensor. */
public final class TerrainSnapshot {

    public enum HazardType {
        NONE, LAVA, VOID, FALL, WATER
    }

    public List<Vec3> escapeDirections = Collections.emptyList();
    public List<BlockPos> safePositions = Collections.emptyList();
    public float nearestHazardDistance = 32f;
    public HazardType nearestHazardType = HazardType.NONE;
    /** True when an explosive entity (TNT/crystal) is within 8 blocks. */
    public boolean inBlastZone = false;
    public int nearbyBlastResistantCount = 0;
    public boolean fallDanger = false;
}
