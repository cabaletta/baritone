package adris.altoclef.util.slots;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;

import java.util.stream.IntStream;

public class PlayerSlot extends Slot {
    public static final PlayerSlot CRAFT_OUTPUT_SLOT = new PlayerSlot(0);
    // Armor slots are not visible in crafting/furnace (they break), and as such are unsafe to use.
    public static final PlayerSlot ARMOR_HELMET_SLOT = new PlayerSlot(5);
    public static final PlayerSlot ARMOR_CHESTPLATE_SLOT = new PlayerSlot(6);
    public static final PlayerSlot ARMOR_LEGGINGS_SLOT = new PlayerSlot(7);
    public static final PlayerSlot ARMOR_BOOTS_SLOT = new PlayerSlot(8);
    public static final PlayerSlot[] ARMOR_SLOTS = new PlayerSlot[]{
            ARMOR_HELMET_SLOT,
            ARMOR_CHESTPLATE_SLOT,
            ARMOR_LEGGINGS_SLOT,
            ARMOR_BOOTS_SLOT
    };
    public static final PlayerSlot OFFHAND_SLOT = new PlayerSlot(45);

    public static final PlayerSlot[] CRAFT_INPUT_SLOTS = IntStream.range(0, 4).mapToObj(PlayerSlot::getCraftInputSlot).toArray(PlayerSlot[]::new);

    public PlayerSlot(int windowSlot) {
        this(windowSlot, false);
    }

    protected PlayerSlot(int slot, boolean inventory) {
        super(slot, inventory);
    }

    public static PlayerSlot getCraftInputSlot(int x, int y) {
        return getCraftInputSlot(y * 2 + x);
    }

    public static PlayerSlot getCraftInputSlot(int index) {
        return new PlayerSlot(index + 1);
    }

    public static Slot getEquipSlot(EquipmentSlot equipSlot) {
        switch (equipSlot) {
            case MAINHAND:
                assert MinecraftClient.getInstance().player != null;
                return Slot.getFromCurrentScreenInventory(MinecraftClient.getInstance().player.getInventory().selectedSlot);
            case OFFHAND:
                return OFFHAND_SLOT;
            case FEET:
                return ARMOR_BOOTS_SLOT;
            case LEGS:
                return ARMOR_LEGGINGS_SLOT;
            case CHEST:
                return ARMOR_CHESTPLATE_SLOT;
            case HEAD:
                return ARMOR_HELMET_SLOT;
        }
        return null;
    }

    public static Slot getEquipSlot() {
        return getEquipSlot(EquipmentSlot.MAINHAND);
    }

    @Override
    public int inventorySlotToWindowSlot(int inventorySlot) {
        if (inventorySlot < 9) {
            return inventorySlot + 36;
        }
        return inventorySlot;
    }

    @Override
    protected int windowSlotToInventorySlot(int windowSlot) {
        if (windowSlot >= 36) {
            return windowSlot - 36;
        }
        return windowSlot;
    }

    @Override
    protected String getName() {
        return "Player";
    }

}
