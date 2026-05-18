package baritone.awareness.sensor;

import baritone.api.utils.IPlayerContext;
import baritone.awareness.model.SelfState;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Reads the local player's health, hunger, armor durability, and inventory every tick. */
public final class SelfSensor {

    private final IPlayerContext ctx;
    private SelfState snapshot = new SelfState();

    public SelfSensor(IPlayerContext ctx) {
        this.ctx = ctx;
    }

    public void update() {
        Player player = ctx.player();
        SelfState s = new SelfState();

        s.health = player.getHealth();
        s.maxHealth = player.getMaxHealth();
        s.foodLevel = player.getFoodData().getFoodLevel();
        s.attackCooldown = player.getAttackStrengthScale(0f);
        s.shieldEquipped = player.getOffhandItem().getItem() == Items.SHIELD
                        || player.getMainHandItem().getItem() == Items.SHIELD;

        Inventory inv = player.getInventory();
        int totems = 0, pearls = 0;
        boolean gapple = false, potion = false;
        float durabilitySum = 0f;
        int armorSlots = 0;

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            if (item == Items.TOTEM_OF_UNDYING)        totems += stack.getCount();
            if (item == Items.ENDER_PEARL)             pearls += stack.getCount();
            if (item == Items.ENCHANTED_GOLDEN_APPLE
                || item == Items.GOLDEN_APPLE)         gapple = true;
            if (item == Items.POTION
                || item == Items.SPLASH_POTION)        potion = true;
        }

        // Armor slots in Inventory are indices 36-39 (boots, leggings, chestplate, helmet)
        for (int i = 36; i < 40; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.isDamageableItem()) {
                durabilitySum += 1f - (float) stack.getDamageValue() / stack.getMaxDamage();
                armorSlots++;
            }
        }

        s.totemCount = totems;
        s.hasTotem = totems > 0;
        s.pearlCount = pearls;
        s.hasPearl = pearls > 0;
        s.hasGapple = gapple;
        s.hasPotion = potion;
        s.totalArmorDurability = armorSlots > 0 ? durabilitySum / armorSlots : 0f;
        s.effectiveHp = s.health + (s.hasTotem ? 20f : 0f);

        this.snapshot = s;
    }

    public SelfState getSnapshot() {
        return snapshot;
    }
}
