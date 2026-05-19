package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.CraftInInventoryTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.*;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.Arrays;

public class CollectPlanksTask extends ResourceTask {

    private final Item[] _planks;
    private final Item[] _logs;
    private final int _targetCount;
    private boolean _logsInNether;

    public CollectPlanksTask(Item[] planks, Item[] logs, int count, boolean logsInNether) {
        super(new ItemTarget(planks, count));
        _planks = planks;
        _logs = logs;
        _targetCount = count;
        _logsInNether = logsInNether;
    }

    public CollectPlanksTask(int count) {
        this(ItemHelper.PLANKS, ItemHelper.LOG, count, false);
    }

    public CollectPlanksTask(Item plank, Item log, int count) {
        this(new Item[]{plank}, new Item[]{log}, count, false);
    }

    public CollectPlanksTask(Item plank, int count) {
        this(plank, ItemHelper.planksToLog(plank), count);
    }

    private static CraftingRecipe generatePlankRecipe(Item[] logs) {
        return CraftingRecipe.newShapedRecipe(
                "planks",
                new Item[][]{
                        logs, null,
                        null, null
                },
                4
        );
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {

    }

    @Override
    protected Task onResourceTick(AltoClef mod) {

        // Craft when we can
        int totalInventoryPlankCount = mod.getItemStorage().getItemCount(_planks);
        int potentialPlanks = totalInventoryPlankCount + mod.getItemStorage().getItemCount(_logs) * 4;
        if (potentialPlanks >= _targetCount) {
            for (Item logCheck : _logs) {
                int count = mod.getItemStorage().getItemCount(logCheck);
                if (count > 0) {
                    Item plankCheck = ItemHelper.logToPlanks(logCheck);
                    if (plankCheck == null) {
                        Debug.logError("Invalid/Un-convertable log: " + logCheck + " (failed to find corresponding plank)");
                    }
                    int plankCount = mod.getItemStorage().getItemCount(plankCheck);
                    int otherPlankCount = totalInventoryPlankCount - plankCount;
                    int targetTotalPlanks = Math.min(count * 4 + plankCount, _targetCount - otherPlankCount);
                    setDebugState("We have " + logCheck + ", crafting " + targetTotalPlanks + " planks.");
                    return new CraftInInventoryTask(new RecipeTarget(plankCheck, targetTotalPlanks, generatePlankRecipe(_logs)));
                }
            }
        }

        // Collect planks and logs
        ArrayList<ItemTarget> blocksTomine = new ArrayList<>(2);
        blocksTomine.add(new ItemTarget(_logs));
        // Ignore planks if we're told to.
        if (!mod.getBehaviour().exclusivelyMineLogs()) {
            // TODO: Add planks back in, but with a heuristic check (so we don't go for abandoned mineshafts)
            //blocksTomine.add(new ItemTarget(ItemUtil.PLANKS));
        }

        ResourceTask mineTask = new MineAndCollectTask(blocksTomine.toArray(ItemTarget[]::new), MiningRequirement.HAND);
        // Kinda jank
        if (_logsInNether) {
            mineTask.forceDimension(Dimension.NETHER);
        }
        return mineTask;
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        return other instanceof CollectPlanksTask;
    }

    @Override
    protected String toDebugStringName() {
        return "Crafting " + _targetCount + " planks " + Arrays.toString(_planks);
    }

    public CollectPlanksTask logsInNether() {
        _logsInNether = true;
        return this;
    }
}
