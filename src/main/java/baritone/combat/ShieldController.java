package baritone.combat;

import baritone.api.utils.IPlayerContext;
import baritone.awareness.model.SelfState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Shield and off-hand inventory management.
 *
 * Detection:
 *   isOwnShieldBroken()  — true when our shield has the 5-second disable cooldown
 *                            from being hit by an axe.  CombatEngine uses this to
 *                            trigger pearl escapes and suppress shield-raise reflexes.
 *
 * Off-hand management (singleplayer / host only — uses direct inventory access):
 *   Swap off-hand to Totem when HP < 4 hearts so it auto-activates on near-death.
 *   Swap back to Shield when HP recovers above 8 hearts.
 *
 * Reactive shield raising (CLICK_RIGHT) is handled by ReactionSystem, not here.
 */
public final class ShieldController {

    private static final float TOTEM_SWAP_HP    = 4f;   // hearts: equip totem below this
    private static final float SHIELD_RESTORE_HP = 8f;  // hearts: restore shield above this

    private final IPlayerContext ctx;

    public ShieldController(IPlayerContext ctx) {
        this.ctx = ctx;
    }

    /** True when our shield was disabled by an enemy axe (5-second cooldown). */
    public boolean isOwnShieldBroken() {
        Player player = ctx.player();
        return player != null && player.getCooldowns().isOnCooldown(Items.SHIELD);
    }

    /**
     * Manages what lives in the off-hand slot.
     * ⚠️ Direct inventory mutation — works in singleplayer / as world host.
     *    Multiplayer requires ServerboundContainerClickPacket (future work).
     */
    public void manageOffHand(SelfState self) {
        Player player = ctx.player();
        if (player == null) return;

        ItemStack offhand = player.getInventory().offhand.get(0);

        if (self.health < TOTEM_SWAP_HP && self.hasTotem
                && offhand.getItem() != Items.TOTEM_OF_UNDYING) {
            swapToOffhand(player, Items.TOTEM_OF_UNDYING);
        } else if (self.health >= SHIELD_RESTORE_HP
                && offhand.getItem() == Items.TOTEM_OF_UNDYING) {
            swapToOffhand(player, Items.SHIELD);
        }
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────

    private void swapToOffhand(Player player, net.minecraft.world.item.Item targetType) {
        // Find the item in the main inventory (slots 0-35)
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || stack.getItem() != targetType) continue;
            ItemStack current = player.getInventory().offhand.get(0);
            player.getInventory().offhand.set(0, stack.copy());
            player.getInventory().setItem(i, current);
            return;
        }
    }
}
