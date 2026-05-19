package adris.altoclef.tasks.resources;

import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.helpers.ItemHelper;

public class CollectSaplingsTask extends MineAndCollectTask {
    public CollectSaplingsTask(int count) {
        super(new ItemTarget(ItemHelper.SAPLINGS, count), ItemHelper.SAPLING_SOURCES,
                MiningRequirement.HAND);
    }
}
