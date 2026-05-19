package adris.altoclef.tasks.entity;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.movement.FollowPlayerTask;
import adris.altoclef.tasks.movement.RunAwayFromPositionTask;
import adris.altoclef.tasks.squashed.CataloguedResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class GiveItemToPlayerTask extends Task {

    private final String _playerName;
    private final ItemTarget[] _targets;

    private final CataloguedResourceTask _resourceTask;
    private final List<ItemTarget> _throwTarget = new ArrayList<>();
    private boolean _droppingItems;

    private Task _throwTask;

    public GiveItemToPlayerTask(String player, ItemTarget... targets) {
        _playerName = player;
        _targets = targets;
        _resourceTask = TaskCatalogue.getSquashedItemTask(targets);
    }

    @Override
    protected void onStart(AltoClef mod) {
        _droppingItems = false;
        _throwTarget.clear();
    }

    @Override
    protected Task onTick(AltoClef mod) {

        if (_throwTask != null && _throwTask.isActive() && !_throwTask.isFinished(mod)) {
            setDebugState("Throwing items");
            return _throwTask;
        }

        Optional<Vec3d> lastPos = mod.getEntityTracker().getPlayerMostRecentPosition(_playerName);

        if (lastPos.isEmpty()) {
            setDebugState("No player found/detected. Doing nothing until player loads into render distance.");
            return null;
        }
        Vec3d targetPos = lastPos.get().add(0, 0.2f, 0);

        if (_droppingItems) {
            // THROW ITEMS
            setDebugState("Throwing items");
            LookHelper.lookAt(mod, targetPos);
            for (int i = 0; i < _throwTarget.size(); ++i) {
                ItemTarget target = _throwTarget.get(i);
                if (target.getTargetCount() > 0) {
                    Optional<Slot> has = mod.getItemStorage().getSlotsWithItemPlayerInventory(false, target.getMatches()).stream().findFirst();
                    if (has.isPresent()) {
                        Slot currentlyPresent = has.get();
                        if (Slot.isCursor(currentlyPresent)) {
                            ItemStack stack = StorageHelper.getItemStackInSlot(currentlyPresent);
                            // Update target
                            target = new ItemTarget(target, target.getTargetCount() - stack.getCount());
                            _throwTarget.set(i, target);
                            Debug.logMessage("THROWING: " + has.get());
                            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                        } else {
                            mod.getSlotHandler().clickSlot(currentlyPresent, 0, SlotActionType.PICKUP);
                        }
                        return null;
                    }
                }
            }

            if (!targetPos.isInRange(mod.getPlayer().getPos(), 4)) {
                mod.log("Finished giving items.");
                stop(mod);
                return null;
            }
            return new RunAwayFromPositionTask(6, WorldHelper.toBlockPos(targetPos));
        }

        if (!StorageHelper.itemTargetsMet(mod, _targets)) {
            setDebugState("Collecting resources...");
            return _resourceTask;
        }

        if (targetPos.isInRange(mod.getPlayer().getPos(), 1.5)) {
            if (!mod.getEntityTracker().isPlayerLoaded(_playerName)) {
                mod.logWarning("Failed to get to player \"" + _playerName + "\". We moved to where we last saw them but now have no idea where they are.");
                stop(mod);
                return null;
            }
            _droppingItems = true;
            _throwTarget.addAll(Arrays.asList(_targets));
        }

        setDebugState("Going to player...");
        return new FollowPlayerTask(_playerName);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof GiveItemToPlayerTask task) {
            if (!task._playerName.equals(_playerName)) return false;
            return Arrays.equals(task._targets, _targets);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Giving items to " + _playerName;
    }
}
