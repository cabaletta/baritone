package adris.altoclef.tasks.slot;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.slots.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class ClickSlotTask extends Task {

    private final Slot _slot;
    private final int _mouseButton;
    private final SlotActionType _type;

    private boolean _clicked = false;

    public ClickSlotTask(Slot slot, int mouseButton, SlotActionType type) {
        _slot = slot;
        _mouseButton = mouseButton;
        _type = type;
    }

    public ClickSlotTask(Slot slot, SlotActionType type) {
        this(slot, 0, type);
    }

    public ClickSlotTask(Slot slot, int mouseButton) {
        this(slot, mouseButton, SlotActionType.PICKUP);
    }

    public ClickSlotTask(Slot slot) {
        this(slot, SlotActionType.PICKUP);
    }

    @Override
    protected void onStart(AltoClef mod) {
        _clicked = false;
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (mod.getSlotHandler().canDoSlotAction()) {
            mod.getSlotHandler().clickSlot(_slot, _mouseButton, _type);
            mod.getSlotHandler().registerSlotAction();
            _clicked = true;
        }
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof ClickSlotTask task) {
            return task._mouseButton == _mouseButton && task._type == _type && task._slot.equals(_slot);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Clicking " + _slot.toString();
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return _clicked;
    }
}
