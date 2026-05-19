package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import net.minecraft.block.Block;
import net.minecraft.util.math.ChunkPos;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.HashSet;

public class SearchChunkForBlockTask extends SearchChunksExploreTask {

    private final HashSet<Block> _toSearchFor = new HashSet<>();

    public SearchChunkForBlockTask(Block... blocks) {
        _toSearchFor.addAll(Arrays.asList(blocks));
    }

    @Override
    protected boolean isChunkWithinSearchSpace(AltoClef mod, ChunkPos pos) {
        return mod.getChunkTracker().scanChunk(pos, block -> {
                    return _toSearchFor.contains(mod.getWorld().getBlockState(block).getBlock());
                }
        );
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof SearchChunkForBlockTask blockTask) {
            return Arrays.equals(blockTask._toSearchFor.toArray(Block[]::new), _toSearchFor.toArray(Block[]::new));
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Searching chunk for blocks " + ArrayUtils.toString(_toSearchFor.toArray(Block[]::new));
    }
}
