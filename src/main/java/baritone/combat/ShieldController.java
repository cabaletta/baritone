package baritone.combat;

import baritone.api.utils.IPlayerContext;
import baritone.awareness.model.SelfState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ShieldController {

    private static final float TOTEM_SWAP_HP    = 4f;
    private static final float SHIELD_RESTORE_HP = 8f;

    private final IPlayerContext ctx;

    public ShieldController(IPlayerContext ctx) {
        this.ctx = ctx;
    }

    public boolean isOwnShieldBroken() {
        Player player = ctx.player();
        return player != null && player.getCooldowns().isOnCooldown(Items.SHIELD.getDefaultInstance());
    }

    public void manageOffHand(SelfState self) {
        Player player = ctx.player();
        if (player == null) return;

        ItemStack offhand = player.getInventory().getItem(40);
        boolean offhandIsTotem  = offhand.getItem() == Items.TOTEM_OF_UNDYING;
        boolean offhandIsShield = offhand.getItem() == Items.SHIELD;

        if (self.health < TOTEM_SWAP_HP && self.hasTotem && !offhandIsTotem) {
            swapToOffhand(player, Items.TOTEM_OF_UNDYING);
        } else if (self.health >= SHIELD_RESTORE_HP && !offhandIsShield) {
            swapToOffhand(player, Items.SHIELD);
        }
    }

    private void swapToOffhand(Player player, net.minecraft.world.item.Item targetType) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || stack.getItem() != targetType) continue;
            ItemStack current = player.getInventory().getItem(40);
            player.getInventory().setItem(40, stack.copy());
            player.getInventory().setItem(i, current);
            return;
        }
    }
}
