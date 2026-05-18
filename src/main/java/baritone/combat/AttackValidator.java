package baritone.combat;

import baritone.api.utils.IPlayerContext;
import baritone.api.utils.input.Input;
import baritone.awareness.model.ThreatEntry;
import baritone.utils.InputOverrideHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Enforces legit-hit rules before pressing ATTACK:
 *
 *   1. Attack cooldown >= 0.9  (no reduced-damage swings)
 *   2. Target within 3.0 m
 *   3. Line-of-sight confirmed by EntitySensor
 *   4. Crit: player must be falling (!onGround, !onClimbable, !inWater, deltaY < 0)
 *
 * Crit sequence:
 *   - Initiates a jump when cooldown crosses 0.85 and player is on the ground.
 *   - Fires CLICK_LEFT once the player is falling and cooldown >= 0.9.
 *   - If a crit never materialises, fires anyway at cooldown >= 0.98 (full charge).
 *   - Schedules a W-tap via SpacingController just before each hit.
 */
public final class AttackValidator {

    private static final float MIN_COOLDOWN   = 0.9f;
    private static final float FORCE_COOLDOWN = 0.98f;
    private static final float JUMP_COOLDOWN  = 0.85f;
    private static final float MAX_RANGE      = 3.0f;

    private final IPlayerContext ctx;
    private final SpacingController spacing;

    private boolean jumpedForCrit = false;
    private int jumpTimer         = 0;
    private static final int MAX_JUMP_WAIT = 14; // ticks; give up crit if falling not detected

    public AttackValidator(IPlayerContext ctx, SpacingController spacing) {
        this.ctx     = ctx;
        this.spacing = spacing;
    }

    public void tick(InputOverrideHandler input, ThreatEntry target) {
        Player player = ctx.player();
        if (player == null) return;

        float cooldown = player.getAttackStrengthScale(0f);
        float distance = target.tracked.distance;

        if (distance > MAX_RANGE)              return;
        if (!target.tracked.hasLineOfSight)    return;
        if (cooldown < MIN_COOLDOWN)           return;

        Vec3 delta   = player.getDeltaMovement();
        boolean falling = !player.onGround()
            && !player.onClimbable()
            && !player.isInWater()
            && delta.y < 0;

        // Initiate crit jump once cooldown is near ready
        if (!jumpedForCrit && player.onGround() && cooldown >= JUMP_COOLDOWN) {
            input.setInputForceState(Input.JUMP, true);
            jumpedForCrit = true;
            jumpTimer     = 0;
        }

        if (jumpedForCrit) jumpTimer++;

        if (falling && cooldown >= MIN_COOLDOWN) {
            // Crit hit
            spacing.scheduleWTap();
            input.setInputForceState(Input.CLICK_LEFT, true);
            jumpedForCrit = false;
            jumpTimer     = 0;
        } else if (jumpedForCrit && jumpTimer > MAX_JUMP_WAIT && cooldown >= FORCE_COOLDOWN) {
            // Crit never landed — hit at full cooldown rather than waste the charge
            spacing.scheduleWTap();
            input.setInputForceState(Input.CLICK_LEFT, true);
            jumpedForCrit = false;
            jumpTimer     = 0;
        } else if (!jumpedForCrit && cooldown >= FORCE_COOLDOWN) {
            // On-ground full-charge hit (e.g. target just came into range)
            spacing.scheduleWTap();
            input.setInputForceState(Input.CLICK_LEFT, true);
        }
    }
}
