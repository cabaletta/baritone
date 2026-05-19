package adris.altoclef.util.slots;

public class BrewingStandSlot extends Slot {
    public static final BrewingStandSlot LEFT_POTION = new BrewingStandSlot(0);
    public static final BrewingStandSlot MIDDLE_POTION = new BrewingStandSlot(1);
    public static final BrewingStandSlot RIGHT_POTION = new BrewingStandSlot(2);
    public static final BrewingStandSlot INGREDIENT = new BrewingStandSlot(3);
    public static final BrewingStandSlot FUEL = new BrewingStandSlot(4);

    public BrewingStandSlot(int slot) {
        this(slot, false);
    }

    protected BrewingStandSlot(int slot, boolean inventory) {
        super(slot, inventory);
    }

    @Override
    public int inventorySlotToWindowSlot(int inventorySlot) {
        if (inventorySlot < 9) {
            return inventorySlot + 32;
        }
        return inventorySlot - 4;
    }

    @Override
    protected int windowSlotToInventorySlot(int windowSlot) {
        if (windowSlot >= 32) {
            return windowSlot - 32;
        }
        return windowSlot + 4;
    }

    @Override
    protected String getName() {
        return "Brewing Stand";
    }
}
