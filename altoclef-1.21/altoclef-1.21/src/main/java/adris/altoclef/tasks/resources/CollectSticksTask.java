package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.CraftInInventoryTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.CraftingRecipe;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.RecipeTarget;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public class CollectSticksTask extends ResourceTask {

    private final int _targetCount;

    public CollectSticksTask(int targetCount) {
        super(Items.STICK, targetCount);
        _targetCount = targetCount;
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        mod.getBehaviour().push();
        mod.getBlockTracker().trackBlock(Blocks.DEAD_BUSH);
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        Optional<BlockPos> nearestBush = mod.getBlockTracker().getNearestTracking(Blocks.DEAD_BUSH);
        // If there's a dead bush within range, go get it
        if (nearestBush.isPresent() && nearestBush.get().isWithinDistance(mod.getPlayer().getPos(), 20)) {
            return new MineAndCollectTask(Items.DEAD_BUSH, 999999, new Block[]{Blocks.DEAD_BUSH}, MiningRequirement.HAND);
        }
        // else craft from wood
        return new CraftInInventoryTask(new RecipeTarget(Items.STICK, _targetCount, CraftingRecipe.newShapedRecipe("sticks", new ItemTarget[]{new ItemTarget("planks"), null, new ItemTarget("planks"), null}, 4)));
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.DEAD_BUSH);
        mod.getBehaviour().pop();
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        return other instanceof CollectSticksTask;
    }

    @Override
    protected String toDebugStringName() {
        return "Crafting " + _targetCount + " sticks";
    }
}
