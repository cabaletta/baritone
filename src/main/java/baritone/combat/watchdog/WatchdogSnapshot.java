package baritone.combat.watchdog;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable per-tick state capture for the rolling 20-tick Watchdog window.
 */
public final class WatchdogSnapshot {

    // ── Self ─────────────────────────────────────────────────────────────────
    public final long   gameTick;
    public final float  health;
    /** How many hearts were lost vs the previous tick (always >= 0). */
    public final float  damageTaken;
    /** getDeltaMovement() */
    public final Vec3   velocity;
    public final Vec3   position;
    public final boolean onGround;
    public final boolean horizontalCollision;
    /** sqrt(vx² + vz²) */
    public final double  horizontalSpeed;

    // ── Primary threat ───────────────────────────────────────────────────────
    @Nullable public final LivingEntity primaryTarget;
    /** Float.NaN when no target. */
    public final float   targetY;
    /** target.getDeltaMovement().y; Float.NaN when no target. */
    public final float   targetVelocityY;
    /** Target holds a Mace. */
    public final boolean targetHasMace;
    /** targetVelocityY < -0.1 (actively falling, not just air-borne). */
    public final boolean targetFalling;
    /** target.getMainHandItem() is obsidian or end crystal. */
    public final boolean targetDeployingObsidian;
    /** -1 when no target. */
    public final float   targetDist;

    // ── Crystal proximity ────────────────────────────────────────────────────
    @Nullable public final Entity nearestCrystal;
    /** -1 when none within 8 m. */
    public final float nearestCrystalDist;

    // ── Surround state ───────────────────────────────────────────────────────
    /** Count of cardinal (N/S/E/W) faces at head-height blocked by obsidian/bedrock/ender-chest. */
    public final int blockedFaces;

    public WatchdogSnapshot(
            long gameTick,
            float health, float damageTaken,
            Vec3 velocity, Vec3 position,
            boolean onGround, boolean horizontalCollision, double horizontalSpeed,
            @Nullable LivingEntity primaryTarget,
            float targetY, float targetVelocityY,
            boolean targetHasMace, boolean targetDeployingObsidian, float targetDist,
            @Nullable Entity nearestCrystal, float nearestCrystalDist,
            int blockedFaces) {
        this.gameTick              = gameTick;
        this.health                = health;
        this.damageTaken           = damageTaken;
        this.velocity              = velocity;
        this.position              = position;
        this.onGround              = onGround;
        this.horizontalCollision   = horizontalCollision;
        this.horizontalSpeed       = horizontalSpeed;
        this.primaryTarget         = primaryTarget;
        this.targetY               = targetY;
        this.targetVelocityY       = targetVelocityY;
        this.targetHasMace         = targetHasMace;
        this.targetFalling         = targetVelocityY < -0.1f;
        this.targetDeployingObsidian = targetDeployingObsidian;
        this.targetDist            = targetDist;
        this.nearestCrystal        = nearestCrystal;
        this.nearestCrystalDist    = nearestCrystalDist;
        this.blockedFaces          = blockedFaces;
    }
}
