package adris.altoclef.util.slots;

public class ChestSlot extends Slot {

    private final boolean _big;

    public ChestSlot(int slot, boolean big) {
        this(slot, big, false);
    }

    public ChestSlot(int slot, boolean big, boolean inventory) {
        super(slot, inventory);
        _big = big;
    }

    @Override
    public int inventorySlotToWindowSlot(int inventorySlot) {
        if (inventorySlot < 9) {
            return inventorySlot + (_big ? 81 : 54);
        }
        return (inventorySlot - 9) + (_big ? 54 : 27);
    }

    @Override
    protected int windowSlotToInventorySlot(int windowSlot) {
        int bottomStart = (_big ? 81 : 54);
        if (windowSlot >= bottomStart) {
            return windowSlot - bottomStart;
        }
        return (windowSlot + 9) - (_big ? 54 : 27);
    }

    @Override
    protected String getName() {
        return "Chest";
    }
}
