package baritone.combat;

import baritone.api.utils.IPlayerContext;
import baritone.api.utils.input.Input;
import baritone.awareness.model.ThreatEntry;
import baritone.utils.InputOverrideHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Fires entity attacks when legit-hit conditions are met.
 *
 * Uses Minecraft.gameMode.attack() directly so the attack always lands on the
 * correct entity regardless of what the crosshair hitResult points at (crosshair
 * and tick-set yaw/pitch can desync by one frame when using InputOverrideHandler).
 *
 * Attack conditions:
 *   1. Target within 3.0 m
 *   2. Line-of-sight confirmed by EntitySensor
 *   3. Attack cooldown ≥ 0.9 (no reduced-damage swings)
 *
 * Crit sequence (bonus — tried first, but not required):
 *   - Initiates JUMP when cooldown ≥ 0.85 and player is on the ground.
 *   - Fires the attack once the player is falling (y-velocity < 0) and cooldown ≥ 0.9
 *     → 1.5× damage crit.
 *   - Falls back to a plain ground hit at cooldown ≥ 0.98 if the crit never lands,
 *     or if the jump is aborted (stuck under ceiling, timer expired, etc.).
 */
public final class AttackValidator {

    private static final float MIN_COOLDOWN   = 0.9f;
    private static final float FORCE_COOLDOWN = 0.98f;
    private static final float JUMP_COOLDOWN  = 0.85f;
    private static final float MAX_RANGE      = 3.0f;
    private static final int   MAX_JUMP_WAIT  = 14;

    private final IPlayerContext ctx;
    private final SpacingController spacing;

    private boolean jumpedForCrit = false;
    private int     jumpTimer     = 0;

    public AttackValidator(IPlayerContext ctx, SpacingController spacing) {
        this.ctx     = ctx;
        this.spacing = spacing;
    }

    public void tick(InputOverrideHandler input, ThreatEntry target) {
        Player player = ctx.player();
        if (player == null) return;

        float cooldown = player.getAttackStrengthScale(0f);
        float distance = (float) target.tracked.distance;

        // Reset jump state whenever target leaves melee range so the next entry is fresh
        if (distance > MAX_RANGE || !target.tracked.hasLineOfSight) {
            jumpedForCrit = false;
            jumpTimer     = 0;
            return;
        }
        if (cooldown < MIN_COOLDOWN) return;

        Vec3    delta   = player.getDeltaMovement();
        boolean falling = !player.isOnGround()
                       && !player.onClimbable()
                       && !player.isInWater()
                       && delta.y < 0;

        // Initiate crit jump when on ground and cooldown is nearly ready
        if (!jumpedForCrit && player.isOnGround() && cooldown >= JUMP_COOLDOWN) {
            input.setInputForceState(Input.JUMP, true);
            jumpedForCrit = true;
            jumpTimer     = 0;
        }
        if (jumpedForCrit) jumpTimer++;

        boolean shouldHit =
            falling                                              // crit hit (best case)
            || (jumpedForCrit && jumpTimer > MAX_JUMP_WAIT)     // jump failed — give up and hit
            || (!jumpedForCrit && cooldown >= FORCE_COOLDOWN);  // target just entered range, no jump yet

        if (!shouldHit && cooldown >= FORCE_COOLDOWN) {
            // Catch-all: cooldown maxed but still waiting for a crit that may never come
            shouldHit = true;
        }

        if (shouldHit) {
            spacing.scheduleWTap();
            // Direct entity attack — bypasses the crosshair/hitResult pipeline which
            // may not have updated yet after we set player rotation this tick.
            Minecraft mc = ctx.minecraft();
            if (mc.gameMode != null) {
                mc.gameMode.attack(ctx.player(), target.tracked.entity);
            }
            jumpedForCrit = false;
            jumpTimer     = 0;
        }
    }
}
