package adris.altoclef.tasks.slot;

import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.slots.Slot;

public class MoveItemToSlotFromContainerTask extends MoveItemToSlotTask {
    public MoveItemToSlotFromContainerTask(ItemTarget toMove, Slot destination) {
        super(toMove, destination, mod -> mod.getItemStorage().getSlotsWithItemContainer(toMove.getMatches()));
    }
}
