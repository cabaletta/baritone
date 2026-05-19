package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.movement.DefaultGoToDimensionTask;
import adris.altoclef.tasks.movement.GetCloseToBlockTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import adris.altoclef.util.time.TimerGame;
import baritone.api.utils.input.Input;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext;

import java.util.HashSet;
import java.util.Optional;
import java.util.function.Predicate;

public class CollectBucketLiquidTask extends ResourceTask {

    private final HashSet<BlockPos> _blacklist = new HashSet<>();
    private final TimerGame _tryImmediatePickupTimer = new TimerGame(3);
    private final TimerGame _pickedUpTimer = new TimerGame(0.5);
    private final int _count;

    //private IProgressChecker<Double> _checker = new LinearProgressChecker(5, 0.1);
    private final Item _target;
    private final Block _toCollect;
    private final String _liquidName;
    private final MovementProgressChecker _progressChecker = new MovementProgressChecker();

    private boolean wasWandering = false;

    public CollectBucketLiquidTask(String liquidName, Item filledBucket, int targetCount, Block toCollect) {
        super(filledBucket, targetCount);
        _liquidName = liquidName;
        _target = filledBucket;
        _count = targetCount;
        _toCollect = toCollect;
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onResourceStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(_toCollect);
        // Track fluids
        mod.getBehaviour().push();
        mod.getBehaviour().setRayTracingFluidHandling(RaycastContext.FluidHandling.SOURCE_ONLY);

        // Avoid breaking / placing blocks at our liquid
        mod.getBehaviour().avoidBlockBreaking((pos) -> MinecraftClient.getInstance().world.getBlockState(pos).getBlock() == _toCollect);
        mod.getBehaviour().avoidBlockPlacing((pos) -> MinecraftClient.getInstance().world.getBlockState(pos).getBlock() == _toCollect);

        //_blacklist.clear();

        _progressChecker.reset();
    }


    @Override
    protected Task onTick(AltoClef mod) {
        Task result = super.onTick(mod);
        // Reset our "first time" timeout/wander flag.
        if (!thisOrChildAreTimedOut()) {
            wasWandering = false;
        }
        return result;
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
            _progressChecker.reset();
        }
        // If we're standing inside a liquid, go pick it up.
        if (_tryImmediatePickupTimer.elapsed() && !mod.getItemStorage().hasItem(Items.WATER_BUCKET)) {
            Block standingInside = mod.getWorld().getBlockState(mod.getPlayer().getBlockPos()).getBlock();
            if (standingInside == _toCollect) {
                setDebugState("Trying to collect (we are in it)");
                mod.getInputControls().forceLook(0, 90);
                //mod.getClientBaritone().getLookBehavior().updateTarget(new Rotation(0, 90), true);
                //Debug.logMessage("Looking at " + _toCollect + ", picking up right away.");
                _tryImmediatePickupTimer.reset();
                if (mod.getSlotHandler().forceEquipItem(Items.BUCKET)) {
                    mod.getInputControls().tryPress(Input.CLICK_RIGHT);
                    mod.getExtraBaritoneSettings().setInteractionPaused(true);
                    _pickedUpTimer.reset();
                    _progressChecker.reset();
                }
                return null;
            }
        }

        if (!_pickedUpTimer.elapsed()) {
            mod.getExtraBaritoneSettings().setInteractionPaused(false);
            _progressChecker.reset();
            // Wait for force pickup
            return null;
        }

        // Get buckets if we need em
        int bucketsNeeded = _count - mod.getItemStorage().getItemCount(Items.BUCKET) - mod.getItemStorage().getItemCount(_target);
        if (bucketsNeeded > 0) {
            setDebugState("Getting bucket...");
            return TaskCatalogue.getItemTask(Items.BUCKET, bucketsNeeded);
        }

        Predicate<BlockPos> isSourceLiquid = blockPos -> {
            if (_blacklist.contains(blockPos)) return false;
            if (!WorldHelper.canReach(mod, blockPos)) return false;
            if (!WorldHelper.canReach(mod, blockPos.up())) return false; // We may try reaching the block above.
            assert MinecraftClient.getInstance().world != null;
            // We break the block above. If it's bedrock, ignore.
            if (mod.getWorld().getBlockState(blockPos.up()).getBlock() == Blocks.BEDROCK) {
                return false;
            }
            return WorldHelper.isSourceBlock(mod, blockPos, false);
        };

        // Find nearest water and right click it
        if (mod.getBlockTracker().isTracking(_toCollect)) {
            Optional<BlockPos> nearestSource = mod.getBlockTracker().getNearestTracking(isSourceLiquid, _toCollect);
            if (nearestSource.isPresent()) {
                Block nearestSourceBlock = mod.getWorld().getBlockState(nearestSource.get()).getBlock();
                // We want to MINIMIZE this distance to liquid.
                setDebugState("Trying to collect...");
                //Debug.logMessage("TEST: " + RayTraceUtils.fluidHandling);
                return new DoToClosestBlockTask(blockPos -> {
                    // Clear above if lava because we can't enter.
                    // but NOT if we're standing right above.
                    if (WorldHelper.isSolid(mod, blockPos.up())) {
                        if (!_progressChecker.check(mod)) {
                            mod.getClientBaritone().getPathingBehavior().cancelEverything();
                            mod.getClientBaritone().getPathingBehavior().forceCancel();
                            mod.getClientBaritone().getExploreProcess().onLostControl();
                            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
                            Debug.logMessage("Failed to break, blacklisting.");
                            mod.getBlockTracker().requestBlockUnreachable(blockPos);
                            _blacklist.add(blockPos);
                        }
                        return new DestroyBlockTask(blockPos.up());
                    }
                    // We can reach the block.
                    if (LookHelper.getReach(blockPos).isPresent() &&
                            mod.getClientBaritone().getPathingBehavior().isSafeToCancel()) {
                        return new InteractWithBlockTask(new ItemTarget(Items.BUCKET, 1), blockPos, _toCollect != Blocks.LAVA);
                    }
                    // Get close enough.
                    // up because if we go below we'll try to move next to the liquid (for lava, not a good move)
                    if (this.thisOrChildAreTimedOut() && !wasWandering) {
                        mod.getBlockTracker().requestBlockUnreachable(blockPos.up());
                        wasWandering = true;
                    }
                    return new GetCloseToBlockTask(blockPos.up());
                }, isSourceLiquid, nearestSourceBlock);
            }
        }

        // Dimension
        if (_toCollect == Blocks.WATER && WorldHelper.getCurrentDimension() == Dimension.NETHER) {
            return new DefaultGoToDimensionTask(Dimension.OVERWORLD);
        }

        // Oof, no liquid found.
        setDebugState("Searching for liquid by wandering around aimlessly");

        return new TimeoutWanderTask();
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(_toCollect);
        mod.getBehaviour().pop();
        //mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, false);
        mod.getExtraBaritoneSettings().setInteractionPaused(false);
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        if (other instanceof CollectBucketLiquidTask task) {
            if (task._count != _count) return false;
            return task._toCollect == _toCollect;
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return "Collect " + _count + " " + _liquidName + " buckets";
    }

    public static class CollectWaterBucketTask extends CollectBucketLiquidTask {
        public CollectWaterBucketTask(int targetCount) {
            super("water", Items.WATER_BUCKET, targetCount, Blocks.WATER);
        }
    }

    public static class CollectLavaBucketTask extends CollectBucketLiquidTask {
        public CollectLavaBucketTask(int targetCount) {
            super("lava", Items.LAVA_BUCKET, targetCount, Blocks.LAVA);
        }
    }

}
