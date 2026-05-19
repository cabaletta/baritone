package baritone.combat;

import baritone.api.utils.IPlayerContext;
import baritone.api.utils.input.Input;
import baritone.awareness.model.ThreatEntry;
import baritone.utils.InputOverrideHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Fires entity attacks when the correct conditions are met, using a mob-specific strategy.
 *
 * SKELETON — crit spam:
 *   Jumps and waits for the DOWNWARD arc (delta.y < 0) before attacking.
 *   This lands a 1.5× damage critical hit every swing.
 *   No catch-all — if the crit can't complete (ceiling, etc.) a fallback fires
 *   after MAX_JUMP_WAIT ticks.
 *
 * ZOMBIE / generic — uppercut combo:
 *   Fires at full cooldown (≥ 0.98) regardless of the player's y-velocity.
 *   Cooldown reaches 0.98 about 2 ticks into the crit-jump, while the player is
 *   still RISING — this is an uppercut, a legitimate high-tier PvP technique that
 *   gives upward knockback and is just as dangerous as a crit in close combat.
 *   W-tap is scheduled before every hit for momentum reset.
 *
 * Both styles use Minecraft.gameMode.attack() directly rather than the CLICK_LEFT
 * input — this guarantees the attack lands on the correct entity rather than
 * whatever the crosshair hitResult happens to contain this frame.
 */
public final class AttackValidator {

    private static final float MIN_COOLDOWN   = 0.9f;
    private static final float FORCE_COOLDOWN = 0.98f;
    private static final float JUMP_COOLDOWN  = 0.85f;
    private static final float MAX_RANGE      = 3.0f;
    private static final int   MAX_JUMP_WAIT  = 14;

    private final IPlayerContext    ctx;
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

        // Reset stale jump state when the target leaves melee range
        if (distance > MAX_RANGE || !target.tracked.hasLineOfSight) {
            jumpedForCrit = false;
            jumpTimer     = 0;
            return;
        }
        if (cooldown < MIN_COOLDOWN) return;

        Vec3    delta   = player.getDeltaMovement();
        boolean onGround = player.isOnGround();
        boolean falling  = !onGround && !player.onClimbable()
                        && !player.isInWater() && delta.y < 0;

        // Initiate a crit jump when grounded and cooldown is nearly ready
        if (!jumpedForCrit && onGround && cooldown >= JUMP_COOLDOWN) {
            input.setInputForceState(Input.JUMP, true);
            jumpedForCrit = true;
            jumpTimer     = 0;
        }
        if (jumpedForCrit) jumpTimer++;

        boolean isSkeleton = target.tracked.entity instanceof AbstractSkeleton;
        boolean shouldHit;

        if (isSkeleton) {
            // SKELETON: crit spam — only fire on the downward arc (true 1.5× crit)
            // Fall back to a plain hit only if the jump is stuck (ceiling / timer expired)
            shouldHit = falling
                     || (jumpedForCrit && jumpTimer > MAX_JUMP_WAIT);
        } else {
            // ZOMBIE / GENERIC: uppercut combo — fire at full cooldown whether rising or falling.
            // Cooldown 0.98 is reached ~2 ticks into the jump while y-vel is still > 0 (rising).
            // Hitting during the upward arc launches the enemy with upward knockback = uppercut.
            shouldHit = falling
                     || (jumpedForCrit && jumpTimer > MAX_JUMP_WAIT)
                     || cooldown >= FORCE_COOLDOWN;
        }

        if (shouldHit) {
            spacing.scheduleWTap();
            Minecraft mc = ctx.minecraft();
            if (mc.gameMode != null) {
                mc.gameMode.attack(ctx.player(), target.tracked.entity);
            }
            jumpedForCrit = false;
            jumpTimer     = 0;
        }
    }
}
