package adris.altoclef.tasks.construction;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.Subscription;
import adris.altoclef.eventbus.events.BlockPlaceEvent;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import adris.altoclef.util.slots.Slot;
import adris.altoclef.util.time.TimerGame;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.MovementHelper;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Place a type of block nearby, anywhere.
 * <p>
 * Also known as the "bear strats" task.
 */
public class PlaceBlockNearbyTask extends Task {

    private final Block[] _toPlace;

    private final MovementProgressChecker _progressChecker = new MovementProgressChecker();
    private final TimeoutWanderTask _wander = new TimeoutWanderTask(5);

    private final TimerGame _randomlookTimer = new TimerGame(0.25);
    private final Predicate<BlockPos> _canPlaceHere;
    private BlockPos _justPlaced; // Where we JUST placed a block.
    private BlockPos _tryPlace;   // Where we should TRY placing a block.
    // Oof, necesarry for the onBlockPlaced action.
    private AltoClef _mod;
    private Subscription<BlockPlaceEvent> _onBlockPlaced;

    public PlaceBlockNearbyTask(Predicate<BlockPos> canPlaceHere, Block... toPlace) {
        _toPlace = toPlace;
        _canPlaceHere = canPlaceHere;
    }

    public PlaceBlockNearbyTask(Block... toPlace) {
        this(blockPos -> true, toPlace);
    }

    @Override
    protected void onStart(AltoClef mod) {
        _progressChecker.reset();
        _mod = mod;
        mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, false);

        // Check for blocks being placed
        _onBlockPlaced = EventBus.subscribe(BlockPlaceEvent.class, evt -> {
            if (ArrayUtils.contains(_toPlace, evt.blockState.getBlock())) {
                stopPlacing(_mod);
            }
        });
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
            _progressChecker.reset();
        }
        // Method:
        // - If looking at placable block
        //      Place immediately
        // Find a spot to place
        // - Prefer flat areas (open space, block below) closest to player
        // -

        // Close screen first
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
        if (!cursorStack.isEmpty()) {
            Optional<Slot> moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
            if (moveTo.isPresent()) {
                mod.getSlotHandler().clickSlot(moveTo.get(), 0, SlotActionType.PICKUP);
                return null;
            }
            if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                return null;
            }
            Optional<Slot> garbage = StorageHelper.getGarbageSlot(mod);
            // Try throwing away cursor slot if it's garbage
            if (garbage.isPresent()) {
                mod.getSlotHandler().clickSlot(garbage.get(), 0, SlotActionType.PICKUP);
                return null;
            }
            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
        } else {
            StorageHelper.closeScreen();
        }

        // Try placing where we're looking right now.
        BlockPos current = getCurrentlyLookingBlockPlace(mod);
        if (current != null && _canPlaceHere.test(current)) {
            setDebugState("Placing since we can...");
            if (mod.getSlotHandler().forceEquipItem(ItemHelper.blocksToItems(_toPlace))) {
                if (place(mod, current)) {
                    return null;
                }
            }
        }

        // Wander while we can.
        if (_wander.isActive() && !_wander.isFinished(mod)) {
            setDebugState("Wandering, will try to place again later.");
            _progressChecker.reset();
            return _wander;
        }
        // Fail check
        if (!_progressChecker.check(mod)) {
            Debug.logMessage("Failed placing, wandering and trying again.");
            LookHelper.randomOrientation(mod);
            if (_tryPlace != null) {
                mod.getBlockTracker().requestBlockUnreachable(_tryPlace);
                _tryPlace = null;
            }
            return _wander;
        }

        // Try to place at a particular spot.
        if (_tryPlace == null || !WorldHelper.canReach(mod, _tryPlace)) {
            _tryPlace = locateClosePlacePos(mod);
        }
        if (_tryPlace != null) {
            setDebugState("Trying to place at " + _tryPlace);
            _justPlaced = _tryPlace;
            return new PlaceBlockTask(_tryPlace, _toPlace, false, true);
        }

        // Look in random places to maybe get a random hit
        if (_randomlookTimer.elapsed()) {
            _randomlookTimer.reset();
            LookHelper.randomOrientation(mod);
        }

        setDebugState("Wandering until we randomly place or find a good place spot.");
        return new TimeoutWanderTask();
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        stopPlacing(mod);
        EventBus.unsubscribe(_onBlockPlaced);
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof PlaceBlockNearbyTask task) {
            return Arrays.equals(task._toPlace, _toPlace);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Place " + Arrays.toString(_toPlace) + " nearby";
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return _justPlaced != null && ArrayUtils.contains(_toPlace, mod.getWorld().getBlockState(_justPlaced).getBlock());
    }

    public BlockPos getPlaced() {
        return _justPlaced;
    }

    private BlockPos getCurrentlyLookingBlockPlace(AltoClef mod) {
        HitResult hit = MinecraftClient.getInstance().crosshairTarget;
        if (hit instanceof BlockHitResult bhit) {
            BlockPos bpos = bhit.getBlockPos();//.subtract(bhit.getSide().getVector());
            //Debug.logMessage("TEMP: A: " + bpos);
            IPlayerContext ctx = mod.getClientBaritone().getPlayerContext();
            if (MovementHelper.canPlaceAgainst(ctx, bpos)) {
                BlockPos placePos = bhit.getBlockPos().add(bhit.getSide().getVector());
                // Don't place inside the player.
                if (WorldHelper.isInsidePlayer(mod, placePos)) {
                    return null;
                }
                //Debug.logMessage("TEMP: B (actual): " + placePos);
                if (WorldHelper.canPlace(mod, placePos)) {
                    return placePos;
                }
            }
        }
        return null;
    }

    private boolean blockEquipped(AltoClef mod) {
        return StorageHelper.isEquipped(mod, ItemHelper.blocksToItems(_toPlace));
    }

    private boolean place(AltoClef mod, BlockPos targetPlace) {
        if (!mod.getExtraBaritoneSettings().isInteractionPaused() && blockEquipped(mod)) {
            // Shift click just for 100% container security.
            mod.getInputControls().hold(Input.SNEAK);

            //mod.getInputControls().tryPress(Input.CLICK_RIGHT);
            // This appears to work on servers...
            // TODO: Helper lol
            HitResult mouseOver = MinecraftClient.getInstance().crosshairTarget;
            if (mouseOver == null || mouseOver.getType() != HitResult.Type.BLOCK) {
                return false;
            }
            Hand hand = Hand.MAIN_HAND;
            assert MinecraftClient.getInstance().interactionManager != null;
            if (MinecraftClient.getInstance().interactionManager.interactBlock(mod.getPlayer(), hand, (BlockHitResult) mouseOver) == ActionResult.SUCCESS &&
                    mod.getPlayer().isSneaking()) {
                mod.getPlayer().swingHand(hand);
                _justPlaced = targetPlace;
                Debug.logMessage("PRESSED");
                return true;
            }

            //mod.getControllerExtras().mouseClickOverride(1, true);
            //mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
            return true;
        }
        return false;
    }

    private void stopPlacing(AltoClef mod) {
        mod.getInputControls().release(Input.SNEAK);
        //mod.getControllerExtras().mouseClickOverride(1, false);
        // Oof, these sometimes cause issues so this is a bit of a duct tape fix.
        mod.getClientBaritone().getBuilderProcess().onLostControl();
    }

    private BlockPos locateClosePlacePos(AltoClef mod) {
        int range = 7;
        BlockPos best = null;
        double smallestScore = Double.POSITIVE_INFINITY;
        BlockPos start = mod.getPlayer().getBlockPos().add(-range, -range, -range);
        BlockPos end = mod.getPlayer().getBlockPos().add(range, range, range);
        for (BlockPos blockPos : WorldHelper.scanRegion(mod, start, end)) {
            boolean solid = WorldHelper.isSolid(mod, blockPos);
            boolean inside = WorldHelper.isInsidePlayer(mod, blockPos);
            // We can't break this block.
            if (solid && !WorldHelper.canBreak(mod, blockPos)) {
                continue;
            }
            // We can't place here as defined by user.
            if (!_canPlaceHere.test(blockPos)) {
                continue;
            }
            // We can't place here.
            if (!WorldHelper.canReach(mod, blockPos) || !WorldHelper.canPlace(mod, blockPos)) {
                continue;
            }
            boolean hasBelow = WorldHelper.isSolid(mod, blockPos.down());
            double distSq = blockPos.getSquaredDistance(mod.getPlayer().getPos());

            double score = distSq + (solid ? 4 : 0) + (hasBelow ? 0 : 10) + (inside ? 3 : 0);

            if (score < smallestScore) {
                best = blockPos;
                smallestScore = score;
            }
        }

        return best;
    }
}
