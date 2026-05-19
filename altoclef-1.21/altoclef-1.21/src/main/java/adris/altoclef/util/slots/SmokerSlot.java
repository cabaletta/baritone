package adris.altoclef.util.slots;

public class SmokerSlot extends Slot {
    public static final SmokerSlot INPUT_SLOT_FUEL = new SmokerSlot(1);
    public static final SmokerSlot INPUT_SLOT_MATERIALS = new SmokerSlot(0);
    public static final SmokerSlot OUTPUT_SLOT = new SmokerSlot(2);

    public SmokerSlot(int windowSlot) {
        this(windowSlot, false);
    }

    protected SmokerSlot(int slot, boolean inventory) {
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
        return "Smoker";
    }
}
