package baritone.combat;

import baritone.api.utils.IPlayerContext;
import baritone.api.utils.input.Input;
import baritone.awareness.AwarenessContext;
import baritone.awareness.model.SelfState;
import baritone.awareness.model.ThreatEntry;
import baritone.utils.InputOverrideHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Ender pearl escape logic.
 *
 * Decides to throw when:
 *   • Surrounded: 3+ threats within 5 m, OR
 *   • Own shield just broken AND HP < 30%
 *
 * Safety gates before throwing:
 *   • Pearl in hotbar
 *   • HP > 3 hearts (fall damage on landing = 2.5 hearts)
 *   • Valid landing spot (solid ground, 2-block head clearance)
 *   • 20-tick cooldown between throws
 *
 * Physics simulation (matches vanilla ender pearl):
 *   Each tick: pos += vel; vel = vel * 0.99 + (0, -0.03, 0)
 *   Throw speed: 2.5 blocks/tick
 *   Elevation: 15° above horizontal for ~18-20 block escape range
 */
public final class PearlController {

    private static final float PEARL_SPEED       = 2.5f;
    private static final float GRAVITY           = 0.03f;
    private static final float AIR_RESISTANCE    = 0.99f;
    private static final float THROW_ELEVATION   = 15f;   // degrees above horizontal
    private static final float FALL_DAMAGE       = 2.5f;  // hearts
    private static final float MIN_HP_TO_THROW   = FALL_DAMAGE + 0.5f; // 3-heart buffer
    private static final int   SIM_MAX_TICKS     = 80;
    private static final int   THROW_COOLDOWN    = 20;
    private static final int   SURROUND_COUNT    = 3;
    private static final float SURROUND_RADIUS   = 5f;

    private final IPlayerContext ctx;
    private int     cooldown  = 0;
    private boolean threw     = false;

    public PearlController(IPlayerContext ctx) {
        this.ctx = ctx;
    }

    public void tick(InputOverrideHandler input, AwarenessContext awarenessCtx,
                     boolean ownShieldBroken) {
        if (cooldown > 0) cooldown--;
        threw = false;

        SelfState self = awarenessCtx.getSelf();
        if (!self.hasPearl || self.health < MIN_HP_TO_THROW || cooldown > 0) return;

        Player player = ctx.player();
        if (player == null) return;

        int   surrounded = countNearThreats(awarenessCtx, SURROUND_RADIUS);
        float hpFrac     = self.maxHealth > 0 ? self.health / self.maxHealth : 1f;

        boolean shouldThrow = surrounded >= SURROUND_COUNT
            || (ownShieldBroken && hpFrac < 0.30f);
        if (!shouldThrow) return;

        // Find escape direction (away from threat centroid)
        Vec3 escapeHorizontal = computeEscapeDir(awarenessCtx, player);
        if (escapeHorizontal == null) return;

        // Elevate throw for distance
        Vec3 throwDir = elevate(escapeHorizontal, THROW_ELEVATION);

        // Simulate trajectory and find landing
        Vec3 landing = simulate(player.getEyePosition(1f), throwDir, ctx.world());
        if (landing == null) return;

        int pearlSlot = InventoryLayout.findPearlSlot(player);
        if (pearlSlot < 0) return;

        // Select pearl, aim toward landing, throw
        player.getInventory().selected = pearlSlot;
        aimAt(player, landing);
        input.setInputForceState(Input.CLICK_RIGHT, true);

        cooldown = THROW_COOLDOWN;
        threw    = true;
    }

    /** True on the exact tick a pearl was thrown. */
    public boolean justThrew() { return threw; }

    // ── helpers ─────────────────────────────────────────────────────────────────────

    private Vec3 computeEscapeDir(AwarenessContext ctx, Player player) {
        List<ThreatEntry> threats = ctx.getThreats();
        if (threats.isEmpty()) return null;
        Vec3 sum = Vec3.ZERO;
        int  n   = 0;
        for (ThreatEntry t : threats) {
            if (t.tracked.entity.isAlive()) { sum = sum.add(t.tracked.entity.position()); n++; }
        }
        if (n == 0) return null;
        Vec3 centroid = sum.scale(1.0 / n);
        Vec3 away     = player.position().subtract(centroid);
        if (away.lengthSqr() < 0.01) return null;
        return new Vec3(away.x, 0, away.z).normalize();
    }

    private Vec3 elevate(Vec3 horizontal, float degrees) {
        double r    = Math.toRadians(degrees);
        double cosA = Math.cos(r), sinA = Math.sin(r);
        return new Vec3(horizontal.x * cosA, sinA, horizontal.z * cosA).normalize();
    }

    private Vec3 simulate(Vec3 origin, Vec3 dir, Level world) {
        Vec3 pos = origin;
        Vec3 vel = dir.normalize().scale(PEARL_SPEED);

        for (int t = 0; t < SIM_MAX_TICKS; t++) {
            pos = pos.add(vel);
            vel = new Vec3(vel.x * AIR_RESISTANCE,
                           vel.y * AIR_RESISTANCE - GRAVITY,
                           vel.z * AIR_RESISTANCE);

            BlockPos bp = new BlockPos((int) Math.floor(pos.x),
                                       (int) Math.floor(pos.y),
                                       (int) Math.floor(pos.z));

            if (!world.getBlockState(bp).isAir()) {
                // Candidate landing: one block above the collision point
                BlockPos land  = bp.above();
                BlockPos head1 = land.above();
                BlockPos head2 = head1.above();
                if (world.getBlockState(land).isAir()
                        && world.getBlockState(head1).isAir()
                        && world.getBlockState(head2).isAir()) {
                    return Vec3.atCenterOf(land);
                }
                return null; // blocked, no valid landing
            }
        }
        return null; // no ground found within range
    }

    private void aimAt(Player player, Vec3 target) {
        Vec3  eye   = player.getEyePosition(1f);
        Vec3  dir   = target.subtract(eye).normalize();
        float yaw   = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float pitch = (float) -Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, dir.y))));
        player.setYRot(yaw);   player.yRotO = yaw;
        player.setXRot(pitch); player.xRotO = pitch;
    }

    private int countNearThreats(AwarenessContext ctx, float radius) {
        int c = 0;
        for (ThreatEntry t : ctx.getThreats()) {
            if (t.tracked.distance <= radius && t.tracked.entity.isAlive()) c++;
        }
        return c;
    }
}
