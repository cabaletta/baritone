package adris.altoclef.util.slots;

import adris.altoclef.Debug;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;

import java.util.Iterator;
import java.util.Objects;

// Very helpful links
// Container Window Slots (used to move stuff around all containers, including player):
//      https://wiki.vg/Inventory
// Player Inventory Slots (used to grab inventory items only):
//      https://minecraft.gamepedia.com/Inventory
public abstract class Slot {

    // -1 means cursor slot, the slot of the cursor when it holds an item.
    public static final int CURSOR_SLOT_INDEX = -1;
    private static final int UNDEFINED_SLOT_INDEX = -999;
    @SuppressWarnings("StaticInitializerReferencesSubClass")
    public static Slot UNDEFINED = new PlayerSlot(UNDEFINED_SLOT_INDEX);
    private final int _inventorySlot;
    private final int _windowSlot;
    private final boolean _isInventory;

    public Slot(int slot, boolean inventory) {
        _isInventory = inventory;
        if (inventory) {
            _inventorySlot = slot;
            _windowSlot = UNDEFINED_SLOT_INDEX;
            //_windowSlot = inventorySlotToWindowSlot(slot);
        } else {
            //_inventorySlot = windowSlotToInventorySlot(slot);
            _inventorySlot = UNDEFINED_SLOT_INDEX;
            _windowSlot = slot;
        }
    }

    private static Slot getFromCurrentScreenAbstract(int slot, boolean inventory) {
        switch (getCurrentType()) {
            case PLAYER:
                return new PlayerSlot(slot, inventory);
            case CRAFTING_TABLE:
                return new CraftingTableSlot(slot, inventory);
            case FURNACE_OR_SMITH_OR_SMOKER_OR_BLAST:
                return new FurnaceSlot(slot, inventory);
            case CHEST_LARGE:
                return new ChestSlot(slot, true, inventory);
            case CHEST_SMALL:
                return new ChestSlot(slot, false, inventory);
            default:
                Debug.logWarning("Unhandled slot for inventory check: " + getCurrentType());
                return null;
        }
    }

    public static Slot getFromCurrentScreen(int windowSlot) {
        return getFromCurrentScreenAbstract(windowSlot, false);
    }

    public static Slot getFromCurrentScreenInventory(int inventorySlot) {
        return getFromCurrentScreenAbstract(inventorySlot, true);
    }

    private static ContainerType getCurrentType() {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen instanceof FurnaceScreen || screen instanceof SmithingScreen || screen instanceof SmokerScreen ||
                screen instanceof BlastFurnaceScreen) {
            return ContainerType.FURNACE_OR_SMITH_OR_SMOKER_OR_BLAST;
        }
        if (screen instanceof GenericContainerScreen) {
            GenericContainerScreenHandler handler = ((GenericContainerScreen) screen).getScreenHandler();
            boolean big = (handler.getRows() == 6);
            return big ? ContainerType.CHEST_LARGE : ContainerType.CHEST_SMALL;
        }
        if (screen instanceof CraftingScreen) {
            return ContainerType.CRAFTING_TABLE;
        }
        return ContainerType.PLAYER;
    }

    public static boolean isCursor(Slot slot) {
        return slot instanceof CursorSlot;
    }

    public static Iterable<Slot> getCurrentScreenSlots() {
        return () -> new Iterator<>() {
            final ClientPlayerEntity player = MinecraftClient.getInstance().player;
            final ScreenHandler handler = player != null ? player.currentScreenHandler : null;
            final int MAX = handler != null ? handler.slots.size() : 0;
            int i = -1;

            @Override
            public boolean hasNext() {
                return i < MAX;
            }

            @Override
            public Slot next() {
                if (i == -1) {
                    ++i;
                    return new CursorSlot();
                }
                return Slot.getFromCurrentScreen(i++);
            }
        };
    }

    public int getInventorySlot() {
        if (!_isInventory) {
            return windowSlotToInventorySlot(_windowSlot);
        }
        return _inventorySlot;
    }

    public int getWindowSlot() {
        if (_isInventory) {
            return inventorySlotToWindowSlot(_inventorySlot);
        }
        return _windowSlot;
    }

    protected abstract int inventorySlotToWindowSlot(int inventorySlot);

    protected abstract int windowSlotToInventorySlot(int windowSlot);

    protected abstract String getName();

    @Override
    public String toString() {
        return getName() + (_isInventory ? "InventorySlot" : "Slot") + "{" +
                "inventory slot = " + getInventorySlot() +
                ", window slot = " + getWindowSlot() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (o instanceof Slot slot) {
            return getInventorySlot() == slot.getInventorySlot() && getWindowSlot() == slot.getWindowSlot();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInventorySlot(), getWindowSlot());
    }

    /**
     * @return Whether this slot exists within the player's inventory or in a container that's disconnected from the player's inventory.
     */
    public boolean isSlotInPlayerInventory() {
        ScreenHandler handler = MinecraftClient.getInstance().player != null ? MinecraftClient.getInstance().player.currentScreenHandler : null;
        int windowSlot = getWindowSlot();
        if (handler instanceof PlayerScreenHandler) {
            // Everything visible is player inventory.
            return true;
        }
        int slotCount = handler != null ? handler.slots.size() : 0;
        return windowSlot >= (slotCount - (4 * 9));
    }

    enum ContainerType {
        PLAYER,
        CRAFTING_TABLE,
        CHEST_SMALL,
        CHEST_LARGE,
        FURNACE_OR_SMITH_OR_SMOKER_OR_BLAST
    }
}
