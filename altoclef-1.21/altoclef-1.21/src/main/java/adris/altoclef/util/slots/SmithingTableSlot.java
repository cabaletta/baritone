package adris.altoclef.util.slots;

public class SmithingTableSlot extends Slot {
    public static final SmithingTableSlot INPUT_SLOT_TEMPLATE = new SmithingTableSlot(0);
    public static final SmithingTableSlot INPUT_SLOT_TOOL = new SmithingTableSlot(1);
    public static final SmithingTableSlot INPUT_SLOT_MATERIALS = new SmithingTableSlot(2);
    public static final SmithingTableSlot OUTPUT_SLOT = new SmithingTableSlot(3);

    public SmithingTableSlot(int slot) {
        this(slot, false);
    }

    SmithingTableSlot(int slot, boolean inventory) {
        super(slot, inventory);
    }

    @Override
    public int inventorySlotToWindowSlot(int inventorySlot) {
        if (inventorySlot < 9) {
            return inventorySlot + 30;
        }
        return inventorySlot - 6;
    }

    @Override
    protected int windowSlotToInventorySlot(int windowSlot) {
        if (windowSlot >= 30) {
            return windowSlot - 30;
        }
        return windowSlot + 6;
    }

    @Override
    protected String getName() {
        return "Smithing Table";
    }
}
