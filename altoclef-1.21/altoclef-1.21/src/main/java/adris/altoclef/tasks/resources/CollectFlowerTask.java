package adris.altoclef.tasks.resources;

import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.helpers.ItemHelper;

public class CollectFlowerTask extends MineAndCollectTask {
    public CollectFlowerTask(int count) {
        super(new ItemTarget(ItemHelper.FLOWER, count), ItemHelper.itemsToBlocks(ItemHelper.FLOWER), MiningRequirement.HAND);
    }
}
