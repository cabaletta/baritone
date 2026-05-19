package adris.altoclef.util.slots;

public class FurnaceSlot extends Slot {
    public static final FurnaceSlot INPUT_SLOT_FUEL = new FurnaceSlot(1);
    public static final FurnaceSlot INPUT_SLOT_MATERIALS = new FurnaceSlot(0);
    public static final FurnaceSlot OUTPUT_SLOT = new FurnaceSlot(2);

    public FurnaceSlot(int windowSlot) {
        this(windowSlot, false);
    }

    protected FurnaceSlot(int slot, boolean inventory) {
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
        return "Furnace";
    }
}
