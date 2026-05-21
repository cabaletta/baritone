package baritone.combat;

import baritone.api.utils.IPlayerContext;
import baritone.api.utils.input.Input;
import baritone.awareness.model.ThreatEntry;
import baritone.utils.InputOverrideHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

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

        if (distance > MAX_RANGE || !target.tracked.hasLineOfSight) {
            jumpedForCrit = false;
            jumpTimer     = 0;
            return;
        }
        if (cooldown < MIN_COOLDOWN) return;

        Vec3    delta    = player.getDeltaMovement();
        boolean onGround = player.onGround();
        boolean falling  = !onGround && !player.onClimbable()
                        && !player.isInWater() && delta.y < 0;

        if (!jumpedForCrit && onGround && cooldown >= JUMP_COOLDOWN) {
            input.setInputForceState(Input.JUMP, true);
            jumpedForCrit = true;
            jumpTimer     = 0;
        }
        if (jumpedForCrit) jumpTimer++;

        net.minecraft.world.entity.EntityType<?> etype = target.tracked.entity.getType();
        boolean isSkeleton = etype == EntityType.SKELETON
            || etype == EntityType.STRAY
            || etype == EntityType.WITHER_SKELETON;
        boolean shouldHit;

        if (isSkeleton) {
            shouldHit = falling || (jumpedForCrit && jumpTimer > MAX_JUMP_WAIT);
        } else {
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
