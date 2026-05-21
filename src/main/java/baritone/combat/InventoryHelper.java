package baritone.combat;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;

final class InventoryHelper {

    private static final Field SELECTED;
    static {
        Field f = null;
        try {
            f = Inventory.class.getDeclaredField("selected");
            f.setAccessible(true);
        } catch (ReflectiveOperationException ignored) {}
        SELECTED = f;
    }

    static int getSelected(Player player) {
        if (SELECTED == null) return 0;
        try { return (int) SELECTED.get(player.getInventory()); }
        catch (ReflectiveOperationException e) { return 0; }
    }

    static void setSelected(Player player, int slot) {
        if (SELECTED == null) return;
        try { SELECTED.setInt(player.getInventory(), slot); }
        catch (ReflectiveOperationException ignored) {}
    }

    private InventoryHelper() {}
}
