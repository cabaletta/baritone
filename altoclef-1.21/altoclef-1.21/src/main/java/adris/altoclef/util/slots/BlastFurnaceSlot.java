package adris.altoclef.util.slots;

public class BlastFurnaceSlot extends Slot {
    public static final BlastFurnaceSlot INPUT_SLOT_FUEL = new BlastFurnaceSlot(1);
    public static final BlastFurnaceSlot INPUT_SLOT_MATERIALS = new BlastFurnaceSlot(0);
    public static final BlastFurnaceSlot OUTPUT_SLOT = new BlastFurnaceSlot(2);

    public BlastFurnaceSlot(int windowSlot) {
        this(windowSlot, false);
    }

    protected BlastFurnaceSlot(int slot, boolean inventory) {
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
        return "Blast Furnace";
    }
}
