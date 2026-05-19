package adris.altoclef.tasks.construction;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Optional;

public class PlaceSignTask extends Task {

    private final BlockPos _target;
    private final String _message;

    private boolean _finished;

    public PlaceSignTask(BlockPos pos, String message) {
        _target = pos;
        _message = message;
    }

    public PlaceSignTask(String message) {
        this(null, message);
    }

    private static boolean isSign(Block block) {
        for (Block check : ItemHelper.WOOD_SIGNS_ALL) {
            if (check == block) return true;
        }
        return false;
    }

    @Override
    protected void onStart(AltoClef mod) {
        _finished = false;
    }

    @Override
    protected Task onTick(AltoClef mod) {

        if (editingSign()) {
            return editSign(mod);
        }

        // Make sure we have a sign to place
        if (!StorageHelper.hasCataloguedItem(mod, "sign")) {
            return TaskCatalogue.getItemTask("sign", 1);
        }

        // Place sign
        if (placeAnywhere()) {
            return new PlaceBlockNearbyTask(ItemHelper.WOOD_SIGNS_ALL);
        } else {

            assert MinecraftClient.getInstance().world != null;
            BlockState b = MinecraftClient.getInstance().world.getBlockState(_target);

            if (!isSign(b.getBlock()) && !b.isAir() && b.getBlock() != Blocks.WATER && b.getBlock() != Blocks.LAVA) {
                return new DestroyBlockTask(_target);
            }

            return new InteractWithBlockTask(new ItemTarget("sign", 1), Direction.UP, _target.down(), true);
        }
    }

    private Task editSign(AltoClef mod) {
        SignEditScreen screen = (SignEditScreen) MinecraftClient.getInstance().currentScreen;
        assert screen != null;

        StringBuilder currentLine = new StringBuilder();

        int lines = 0;

        final int SIGN_TEXT_MAX_WIDTH = 90;

        for (char c : _message.toCharArray()) {
            currentLine.append(c);

            if (c == '\n' || MinecraftClient.getInstance().textRenderer.getWidth(currentLine.toString()) > SIGN_TEXT_MAX_WIDTH) {
                currentLine.delete(0, currentLine.length());
                if (c != '\n') {
                    currentLine.append(c);
                }
                lines++;
                if (lines >= 4) {
                    Debug.logWarning("Too much text to fit on sign! Got Cut off.");
                    break;
                }

                // Add newline
                screen.keyPressed(257, 36, 0);
                //Debug.logMessage("NEW LINE ADDED BEFORE: " + c);
            }
            // keycode don't matter
            //int keyCode = java.awt.event.KeyEvent.getExtendedKeyCodeForChar(c);
            screen.charTyped(c, -1);
            //screen.keyPressed(keyCode, -1, )
        }
        screen.close();
        _finished = true;

        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
        if (!cursorStack.isEmpty()) {
            Optional<Slot> moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
            moveTo.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));
            if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            }
            Optional<Slot> garbage = StorageHelper.getGarbageSlot(mod);
            // Try throwing away cursor slot if it's garbage
            garbage.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));
            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
        } else {
            StorageHelper.closeScreen();
        }
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return _finished;
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof PlaceSignTask task) {
            if (!task._message.equals(_message)) return false;
            if ((task._target == null) != (_target == null)) return false;
            if (task._target != null) {
                return task._target.equals(_target);
            }
            return true;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        if (placeAnywhere()) {
            return "Place Sign Anywhere";
        }
        return "Place Sign at " + _target.toShortString();
    }

    private boolean placeAnywhere() {
        return _target == null;
    }

    private boolean editingSign() {
        return MinecraftClient.getInstance().currentScreen instanceof SignEditScreen;
    }
}
