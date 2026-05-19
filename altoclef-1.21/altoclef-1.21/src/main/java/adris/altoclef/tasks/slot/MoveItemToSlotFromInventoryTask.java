package adris.altoclef.tasks.slot;

import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.slots.Slot;

public class MoveItemToSlotFromInventoryTask extends MoveItemToSlotTask {
    public MoveItemToSlotFromInventoryTask(ItemTarget toMove, Slot destination) {
        super(toMove, destination, mod -> mod.getItemStorage().getSlotsWithItemPlayerInventory(false, toMove.getMatches()));
    }
}
