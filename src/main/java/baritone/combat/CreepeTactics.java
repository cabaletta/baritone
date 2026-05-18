package baritone.combat;

import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.input.Input;
import baritone.awareness.AwarenessContext;
import baritone.awareness.model.ThreatEntry;
import baritone.utils.InputOverrideHandler;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles fusing Creepers independently of the main combat loop.
 *
 * Fuse mechanics (standard, 30 ticks):
 *   Tick  0–19  (swell 0.00–0.667)  — fusing, approach and pre-position
 *   Tick 20–26  (swell 0.667–0.867) — HIT WINDOW: knock into mob cluster, sprint perpendicular
 *   Tick 27+    (swell > 0.867)     — DANGER: sprint perpendicular, do NOT try to hit
 *
 * Fast-fuse detection (fall damage >= 16 blocks reduces fuse to ~5 ticks):
 *   Normal fuse increments ~1 tick/tick.  If our counter jumps, we flag it and
 *   treat the creeper as immediately dangerous.
 *
 * Knockback direction: player faces the creeper, and we fire CLICK_LEFT.
 *   Minecraft's knockback pushes the creeper away from the attacker (toward any
 *   mob cluster behind it from the player's perspective).
 */
public final class CreepeTactics {

    // Standard fuse: 30 ticks total
    private static final float FUSE_TICKS       = 30f;
    private static final float HIT_WINDOW_START = 20f / FUSE_TICKS; // 0.667
    private static final float HIT_WINDOW_END   = 26f / FUSE_TICKS; // 0.867
    // Fast-fuse: normal rate is 1/30 per tick; flag if delta exceeds 2 ticks/tick
    private static final float FAST_FUSE_DELTA  = 2f / FUSE_TICKS;

    private final IPlayerContext ctx;

    // Own fuse-tick counters keyed by entity ID (avoids relying on getSwell() mapping)
    private final Map<Integer, Float> fuseProgress = new HashMap<>();
    private final Map<Integer, Boolean> hitFired   = new HashMap<>();

    public CreepeTactics(IPlayerContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Returns a PathingCommand if creeper logic takes over this tick, null otherwise.
     */
    public PathingCommand tick(InputOverrideHandler input, AwarenessContext awarenessCtx) {
        // Find most-advanced fusing creeper
        Creeper fusing = null;
        int fusingId   = -1;
        float maxProg  = 0f;

        for (ThreatEntry t : awarenessCtx.getThreats()) {
            if (!(t.tracked.entity instanceof Creeper)) continue;
            Creeper c = (Creeper) t.tracked.entity;
            int id    = c.getId();

            if (c.getSwellDir() > 0) {
                float prev  = fuseProgress.getOrDefault(id, 0f);
                float next  = prev + (1f / FUSE_TICKS);
                fuseProgress.put(id, next);
                if (next > maxProg) {
                    maxProg  = next;
                    fusing   = c;
                    fusingId = id;
                }
            } else {
                // Fuse cancelled or creeper not yet aggroed
                fuseProgress.remove(id);
                hitFired.remove(id);
            }
        }

        if (fusing == null) return null;

        // Fast-fuse check: if progress jumped more than expected this tick, treat as danger
        float prevProg   = fuseProgress.getOrDefault(fusingId, 0f) - (1f / FUSE_TICKS);
        boolean fastFuse = (maxProg - prevProg) > FAST_FUSE_DELTA && prevProg > 0.05f;

        if (fastFuse || maxProg >= HIT_WINDOW_END) {
            runPerpendicular(input, fusing);
            return pause();
        }

        if (maxProg >= HIT_WINDOW_START) {
            // Fire one hit aimed at the creeper (knockback sends it toward any mob cluster behind it)
            if (!hitFired.getOrDefault(fusingId, false)) {
                aimAt(fusing);
                input.setInputForceState(Input.CLICK_LEFT, true);
                hitFired.put(fusingId, true);
            }
            runPerpendicular(input, fusing);
            return pause();
        }

        // Pre-window: let normal combat continue but do not engage the creeper
        return null;
    }

    private void runPerpendicular(InputOverrideHandler input, Creeper creeper) {
        net.minecraft.world.entity.player.Player player = ctx.player();
        if (player == null) return;
        Vec3 toCreeper = creeper.position().subtract(player.position());
        if (toCreeper.lengthSqr() < 0.001) return;
        toCreeper = toCreeper.normalize();
        // Rotate 90° in XZ plane
        Vec3 perp = new Vec3(-toCreeper.z, 0, toCreeper.x);
        input.setInputForceState(Input.SPRINT, true);
        if (perp.x >= 0) {
            input.setInputForceState(Input.MOVE_RIGHT, true);
        } else {
            input.setInputForceState(Input.MOVE_LEFT, true);
        }
    }

    private void aimAt(net.minecraft.world.entity.Entity target) {
        net.minecraft.world.entity.player.Player player = ctx.player();
        if (player == null) return;
        Vec3 eye    = player.getEyePosition(1f);
        Vec3 tEye   = target.getEyePosition(1f);
        Vec3 dir    = tEye.subtract(eye).normalize();
        float yaw   = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float pitch = (float) -Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, dir.y))));
        player.setYRot(yaw);   player.yRotO = yaw;
        player.setXRot(pitch); player.xRotO = pitch;
    }

    private static PathingCommand pause() {
        return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
    }
}
