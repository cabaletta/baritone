package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;

/**
 * Explores/Loads all chunks of a biome.
 */
public class SearchWithinBiomeTask extends SearchChunksExploreTask {

    private final RegistryKey<Biome> _toSearch;

    public SearchWithinBiomeTask(RegistryKey<Biome> toSearch) {
        _toSearch = toSearch;
    }

    @Override
    protected boolean isChunkWithinSearchSpace(AltoClef mod, ChunkPos pos) {
        RegistryEntry<Biome> b = mod.getWorld().getBiome(pos.getStartPos().add(1, 1, 1));
        return b.matchesKey(_toSearch);
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof SearchWithinBiomeTask task) {
            return task._toSearch == _toSearch;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Searching for+within biome: " + _toSearch;
    }
}
