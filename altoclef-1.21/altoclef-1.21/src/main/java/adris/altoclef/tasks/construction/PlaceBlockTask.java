package adris.altoclef.tasks.construction;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.api.schematic.AbstractSchematic;
import baritone.api.schematic.ISchematic;
import baritone.api.utils.BlockOptionalMeta;
import baritone.api.utils.input.Input;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Place a block type at a position
 */
public class PlaceBlockTask extends Task implements ITaskRequiresGrounded {

    private static final int MIN_MATERIALS = 1;
    private static final int PREFERRED_MATERIALS = 32;
    private final BlockPos _target;
    private final Block[] _toPlace;
    private final boolean _useThrowaways;
    private final boolean _autoCollectStructureBlocks;
    private final MovementProgressChecker _progressChecker = new MovementProgressChecker();
    private final TimeoutWanderTask _wanderTask = new TimeoutWanderTask(5); // This can get stuck forever, so we increase the range.
    private Task _materialTask;
    private int _failCount = 0;

    public PlaceBlockTask(BlockPos target, Block[] toPlace, boolean useThrowaways, boolean autoCollectStructureBlocks) {
        _target = target;
        _toPlace = toPlace;
        _useThrowaways = useThrowaways;
        _autoCollectStructureBlocks = autoCollectStructureBlocks;
    }

    public PlaceBlockTask(BlockPos target, Block... toPlace) {
        this(target, toPlace, false, false);
    }

    public static int getMaterialCount(AltoClef mod) {
        return mod.getItemStorage().getItemCount(Items.DIRT, Items.COBBLESTONE, Items.NETHERRACK, Items.COBBLED_DEEPSLATE);
    }

    public static Task getMaterialTask(int count) {
        return TaskCatalogue.getSquashedItemTask(new ItemTarget(Items.DIRT, count), new ItemTarget(Items.COBBLESTONE,
                count), new ItemTarget(Items.NETHERRACK, count), new ItemTarget(Items.COBBLED_DEEPSLATE, count));
    }

    @Override
    protected void onStart(AltoClef mod) {
        _progressChecker.reset();
        mod.getBehaviour().push();
        mod.getBehaviour().addProtectedItems(ItemHelper.blocksToItems(_toPlace));
        // If we get interrupted by another task, this might cause problems...
        //_wanderTask.resetWander();
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (WorldHelper.isInNetherPortal(mod)) {
            if (!mod.getClientBaritone().getPathingBehavior().isPathing()) {
                setDebugState("Getting out from nether portal");
                mod.getInputControls().hold(Input.SNEAK);
                mod.getInputControls().hold(Input.MOVE_FORWARD);
                return null;
            } else {
                mod.getInputControls().release(Input.SNEAK);
                mod.getInputControls().release(Input.MOVE_BACK);
                mod.getInputControls().release(Input.MOVE_FORWARD);
            }
        } else {
            if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
                mod.getInputControls().release(Input.SNEAK);
                mod.getInputControls().release(Input.MOVE_BACK);
                mod.getInputControls().release(Input.MOVE_FORWARD);
            }
        }
        // Perform timeout wander
        if (_wanderTask.isActive() && !_wanderTask.isFinished(mod)) {
            setDebugState("Wandering.");
            _progressChecker.reset();
            return _wanderTask;
        }
        if (_autoCollectStructureBlocks) {
            if (_materialTask != null && _materialTask.isActive() && !_materialTask.isFinished(mod)) {
                setDebugState("No structure items, collecting cobblestone + dirt as default.");
                if (getMaterialCount(mod) < PREFERRED_MATERIALS) {
                    return _materialTask;
                } else {
                    _materialTask = null;
                }
            }

            //Item[] items = Util.toArray(Item.class, mod.getClientBaritoneSettings().acceptableThrowawayItems.value);
            if (getMaterialCount(mod) < MIN_MATERIALS) {
                // TODO: Mine items, extract their resource key somehow.
                _materialTask = getMaterialTask(PREFERRED_MATERIALS);
                _progressChecker.reset();
                return _materialTask;
            }
        }


        // Check if we're approaching our point. If we fail, wander for a bit.
        if (!_progressChecker.check(mod)) {
            _failCount++;
            if (!tryingAlternativeWay()) {
                Debug.logMessage("Failed to place, wandering timeout.");
                return _wanderTask;
            } else {
                Debug.logMessage("Trying alternative way of placing block...");
            }
        }


        // Place block
        if (tryingAlternativeWay()) {
            setDebugState("Alternative way: Trying to go above block to place block.");
            return new GetToBlockTask(_target.up(), false);
        } else {
            setDebugState("Letting baritone place a block.");
            // Perform baritone placement
            if (!mod.getClientBaritone().getBuilderProcess().isActive()) {
                Debug.logInternal("Run Structure Build");
                ISchematic schematic = new PlaceStructureSchematic(mod);
                mod.getClientBaritone().getBuilderProcess().build("structure", schematic, _target);
            }
        }
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBehaviour().pop();
        mod.getClientBaritone().getBuilderProcess().onLostControl();
    }

    //TODO: Place structure where a leaf block was???? Might need to delete the block first if it's not empty/air/water.

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof PlaceBlockTask task) {
            return task._target.equals(_target) && task._useThrowaways == _useThrowaways && Arrays.equals(task._toPlace, _toPlace);
        }
        return false;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        assert MinecraftClient.getInstance().world != null;
        if (_useThrowaways) {
            return WorldHelper.isSolid(mod, _target);
        }
        BlockState state = mod.getWorld().getBlockState(_target);
        return ArrayUtils.contains(_toPlace, state.getBlock());
    }

    @Override
    protected String toDebugString() {
        return "Place structure" + ArrayUtils.toString(_toPlace) + " at " + _target.toShortString();
    }

    private boolean tryingAlternativeWay() {
        return _failCount % 4 == 3;
    }

    private class PlaceStructureSchematic extends AbstractSchematic {

        private final AltoClef _mod;

        public PlaceStructureSchematic(AltoClef mod) {
            super(1, 1, 1);
            _mod = mod;
        }

        @Override
        public BlockState desiredState(int x, int y, int z, BlockState blockState, List<BlockState> available) {
            if (x == 0 && y == 0 && z == 0) {
                // Place!!
                if (!available.isEmpty()) {
                    for (BlockState possible : available) {
                        if (possible == null) continue;
                        if (_useThrowaways && _mod.getClientBaritoneSettings().acceptableThrowawayItems.value.contains(possible.getBlock().asItem())) {
                            return possible;
                        }
                        if (Arrays.asList(_toPlace).contains(possible.getBlock())) {
                            return possible;
                        }
                    }
                }
                Debug.logInternal("Failed to find throwaway block");
                // No throwaways available!!
                return new BlockOptionalMeta(Blocks.DIRT).getAnyBlockState();
            }
            // Don't care.
            return blockState;
        }
    }
}
