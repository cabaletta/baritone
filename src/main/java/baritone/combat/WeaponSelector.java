package baritone.combat;

import baritone.awareness.AwarenessContext;
import baritone.awareness.model.ThreatEntry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Decides which hotbar slot to occupy each combat tick.
 *
 * Default: sword (slot 0 or first found).
 *
 * Switch to axe when primary target is actively blocking — an axe hit disables
 * the shield for 100 ticks (~5 seconds). We detect the break by observing the
 * target transition from blocking to not blocking immediately after we swung.
 *
 * AXE_HOLD_TICKS: stay on axe a few extra ticks after the block drops so the
 * break packet has time to register on the server before we switch back to sword.
 *
 * EXPLOIT_TICKS: count down the vulnerability window.  isInExploitWindow()
 * lets AttackValidator know to be aggressive (don't wait for crits).
 */
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

        // Detect block-drop transition: was blocking → no longer blocking after axe swing
        if (prevTargetBlocking && !targetBlocking && axeHoldTimer > 0) {
            exploitTimer = EXPLOIT_TICKS;
        }
        prevTargetBlocking = targetBlocking;

        if (axeHoldTimer  > 0) axeHoldTimer--;
        if (exploitTimer  > 0) exploitTimer--;

        if (targetBlocking) {
            // Enemy has shield up — switch to axe
            int axeSlot = InventoryLayout.findAxeSlot(player);
            if (axeSlot >= 0) {
                axeHoldTimer = AXE_HOLD_TICKS;
                return axeSlot;
            }
        }

        if (axeHoldTimer > 0) {
            // Hold axe briefly after block drops so break registers
            int axeSlot = InventoryLayout.findAxeSlot(player);
            if (axeSlot >= 0) return axeSlot;
        }

        // Default: best sword
        int swordSlot = InventoryLayout.findSwordSlot(player);
        return swordSlot >= 0 ? swordSlot : player.getInventory().selected;
    }

    /** True during the ~5-second window after we break an enemy's shield. */
    public boolean isInExploitWindow() {
        return exploitTimer > 0;
    }
}
