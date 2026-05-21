package baritone.combat;

import baritone.awareness.AwarenessContext;
import baritone.awareness.model.ThreatEntry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class WeaponSelector {

    private static final int AXE_HOLD_TICKS   = 3;
    public  static final int EXPLOIT_TICKS    = 100;

    private boolean prevTargetBlocking = false;
    private int     axeHoldTimer       = 0;
    private int     exploitTimer       = 0;

    public int select(Player player, AwarenessContext ctx) {
        ThreatEntry primary = ctx.getPrimaryThreat();

        boolean targetBlocking = false;
        if (primary != null && primary.tracked.entity.isAlive()
                && primary.tracked.entity instanceof LivingEntity) {
            targetBlocking = ((LivingEntity) primary.tracked.entity).isBlocking();
        }

        if (prevTargetBlocking && !targetBlocking && axeHoldTimer > 0) {
            exploitTimer = EXPLOIT_TICKS;
        }
        prevTargetBlocking = targetBlocking;

        if (axeHoldTimer  > 0) axeHoldTimer--;
        if (exploitTimer  > 0) exploitTimer--;

        if (targetBlocking) {
            int axeSlot = InventoryLayout.findAxeSlot(player);
            if (axeSlot >= 0) {
                axeHoldTimer = AXE_HOLD_TICKS;
                return axeSlot;
            }
        }

        if (axeHoldTimer > 0) {
            int axeSlot = InventoryLayout.findAxeSlot(player);
            if (axeSlot >= 0) return axeSlot;
        }

        int swordSlot = InventoryLayout.findSwordSlot(player);
        return swordSlot >= 0 ? swordSlot : InventoryHelper.getSelected(player);
    }

    public boolean isInExploitWindow() {
        return exploitTimer > 0;
    }
}
