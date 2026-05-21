package baritone.combat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.ItemTags;

public final class InventoryLayout {

    public static final int SLOT_SWORD  = 0;
    public static final int SLOT_AXE    = 1;
    public static final int SLOT_PEARL  = 2;
    public static final int SLOT_GAPPLE = 3;
    public static final int SLOT_POTION = 4;
    public static final int SLOT_FOOD   = 5;
    public static final int SLOT_BLOCKS = 6;
    public static final int SLOT_BOW    = 7;
    public static final int SLOT_TOTEM  = 8;

    public static int findSwordSlot(Player player) {
        if (isSword(player.getInventory().getItem(SLOT_SWORD))) return SLOT_SWORD;
        for (int i = 0; i < 9; i++) {
            if (isSword(player.getInventory().getItem(i))) return i;
        }
        return -1;
    }

    public static int findAxeSlot(Player player) {
        if (isAxe(player.getInventory().getItem(SLOT_AXE))) return SLOT_AXE;
        for (int i = 0; i < 9; i++) {
            if (isAxe(player.getInventory().getItem(i))) return i;
        }
        return -1;
    }

    public static int findPearlSlot(Player player) {
        if (isPearl(player.getInventory().getItem(SLOT_PEARL))) return SLOT_PEARL;
        for (int i = 0; i < 9; i++) {
            if (isPearl(player.getInventory().getItem(i))) return i;
        }
        return -1;
    }

    public static int findGappleSlot(Player player) {
        if (isGapple(player.getInventory().getItem(SLOT_GAPPLE))) return SLOT_GAPPLE;
        for (int i = 0; i < 9; i++) {
            if (isGapple(player.getInventory().getItem(i))) return i;
        }
        return -1;
    }

    public static int findPotionSlot(Player player) {
        if (isPotion(player.getInventory().getItem(SLOT_POTION))) return SLOT_POTION;
        for (int i = 0; i < 9; i++) {
            if (isPotion(player.getInventory().getItem(i))) return i;
        }
        return -1;
    }

    public static boolean isSword(ItemStack s)  { return !s.isEmpty() && s.is(ItemTags.SWORDS); }
    public static boolean isAxe(ItemStack s)    { return !s.isEmpty() && s.getItem() instanceof AxeItem; }
    public static boolean isPearl(ItemStack s)  { return !s.isEmpty() && s.getItem() == Items.ENDER_PEARL; }
    public static boolean isGapple(ItemStack s) {
        return !s.isEmpty() && (s.getItem() == Items.ENCHANTED_GOLDEN_APPLE
                             || s.getItem() == Items.GOLDEN_APPLE);
    }
    public static boolean isPotion(ItemStack s) {
        return !s.isEmpty() && (s.getItem() == Items.POTION
                             || s.getItem() == Items.SPLASH_POTION);
    }

    private InventoryLayout() {}
}
