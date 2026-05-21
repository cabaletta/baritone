package baritone.combat;

import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.input.Input;
import baritone.awareness.AwarenessContext;
import baritone.awareness.model.SelfState;
import baritone.awareness.model.ThreatEntry;
import baritone.utils.InputOverrideHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class HealthGate {

    private static final float DISENGAGE_THRESHOLD = 0.5f;
    private static final float REENGAGE_THRESHOLD  = 0.75f;

    private final IPlayerContext ctx;
    private boolean healing     = false;
    private int healCooldown    = 0;

    public HealthGate(IPlayerContext ctx) {
        this.ctx = ctx;
    }

    public boolean shouldHeal(AwarenessContext awarenessCtx) {
        SelfState self = awarenessCtx.getSelf();
        if (self.maxHealth <= 0) return false;
        float hpFrac = self.health / self.maxHealth;
        if (!healing && hpFrac < DISENGAGE_THRESHOLD)  healing = true;
        if (healing  && hpFrac >= REENGAGE_THRESHOLD)  healing = false;
        return healing;
    }

    public PathingCommand tick(InputOverrideHandler input, AwarenessContext awarenessCtx) {
        SelfState self = awarenessCtx.getSelf();
        Player player  = ctx.player();

        if (player != null && healCooldown <= 0) {
            if (tryUseHealItem(player, self)) healCooldown = 40;
        }

        if (healCooldown > 0 || self.shieldEquipped) {
            input.setInputForceState(Input.CLICK_RIGHT, true);
        }
        if (healCooldown > 0) healCooldown--;

        if (!awarenessCtx.getThreats().isEmpty()) {
            ThreatEntry nearest = awarenessCtx.getThreats().get(0);
            aimAt(nearest.tracked.entity);
            input.setInputForceState(Input.MOVE_BACK, true);
        }

        return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
    }

    private boolean tryUseHealItem(Player player, SelfState self) {
        if (self.hasPotion) {
            int slot = InventoryLayout.findPotionSlot(player);
            if (slot >= 0) { InventoryHelper.setSelected(player, slot); return true; }
        }
        if (self.hasGapple) {
            int slot = InventoryLayout.findGappleSlot(player);
            if (slot >= 0) { InventoryHelper.setSelected(player, slot); return true; }
        }
        return false;
    }

    private void aimAt(Entity target) {
        Player player = ctx.player();
        if (player == null) return;
        Vec3 eye    = player.getEyePosition(1f);
        Vec3 tEye   = target.getEyePosition(1f);
        Vec3 dir    = tEye.subtract(eye).normalize();
        float yaw   = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float pitch = (float) -Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, dir.y))));
        player.setYRot(yaw);   player.yRotO = yaw;
        player.setXRot(pitch); player.xRotO = pitch;
    }
}
