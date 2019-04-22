/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.process;

import baritone.Baritone;
import baritone.api.cache.ICachedWorld;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalComposite;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.process.IExploreProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.MyChunkPos;
import baritone.cache.CachedWorld;
import baritone.utils.BaritoneProcessHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ExploreProcess extends BaritoneProcessHelper implements IExploreProcess {

    private BlockPos explorationOrigin;

    private IChunkFilter filter;

    public ExploreProcess(Baritone baritone) {
        super(baritone);
    }

    @Override
    public boolean isActive() {
        return explorationOrigin != null;
    }

    @Override
    public void explore(int centerX, int centerZ) {
        explorationOrigin = new BlockPos(centerX, 0, centerZ);
    }

    @Override
    public void applyJsonFilter(Path path, boolean invert) throws Exception {
        filter = new JsonChunkFilter(path, invert);
    }

    public IChunkFilter calcFilter() {
        IChunkFilter filter;
        if (this.filter != null) {
            filter = new EitherChunk(this.filter, new BaritoneChunkCache());
        } else {
            filter = new BaritoneChunkCache();
        }
        return filter;
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        if (calcFailed) {
            logDirect("Failed");
            onLostControl();
            return null;
        }
        IChunkFilter filter = calcFilter();
        if (filter.finished()) {
            logDirect("Explored all chunks");
            onLostControl();
            return null;
        }
        Goal[] closestUncached = closestUncachedChunks(explorationOrigin, filter);
        if (closestUncached == null) {
            logDebug("awaiting region load from disk");
            return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
        }
        return new PathingCommand(new GoalComposite(closestUncached), PathingCommandType.FORCE_REVALIDATE_GOAL_AND_PATH);
    }

    private Goal[] closestUncachedChunks(BlockPos center, IChunkFilter filter) {
        int chunkX = center.getX() >> 4;
        int chunkZ = center.getZ() >> 4;
        for (int dist = 0; ; dist++) {
            List<BlockPos> centers = new ArrayList<>();
            for (int dx = -dist; dx <= dist; dx++) {
                for (int dz = -dist; dz <= dist; dz++) {
                    int trueDist = Baritone.settings().exploreUsePythagorean.value ? dx * dx + dz * dz : Math.abs(dx) + Math.abs(dz);
                    if (trueDist != dist) {
                        continue; // not considering this one just yet in our expanding search
                    }
                    switch (filter.isAlreadyExplored(chunkX + dx, chunkZ + dz)) {
                        case UNKNOWN:
                            return null; // awaiting load
                        case NOT_EXPLORED:
                            break; // note: this breaks the switch not the for
                        case EXPLORED:
                            continue; // note: this continues the for
                    }
                    int centerX = (chunkX + dx) * 16 + 8;
                    int centerZ = (chunkZ + dz) * 18 + 8;
                    int offset = 16 * Baritone.settings().worldExploringChunkOffset.value;
                    if (dx < 0) {
                        centerX -= offset;
                    } else {
                        centerX += offset;
                    }
                    if (dz < 0) {
                        centerZ -= offset;
                    } else {
                        centerZ += offset;
                    }
                    centers.add(new BlockPos(centerX, 0, centerZ));
                }
            }
            if (centers.size() > Baritone.settings().exploreChunkSetMinimumSize.value) {
                return centers.stream().map(pos -> new GoalXZ(pos.getX(), pos.getZ())).toArray(Goal[]::new);
            }
        }
    }

    private enum Status {
        EXPLORED, NOT_EXPLORED, UNKNOWN;
    }

    private interface IChunkFilter {
        Status isAlreadyExplored(int chunkX, int chunkZ);

        boolean finished();
    }

    private class BaritoneChunkCache implements IChunkFilter {

        private final ICachedWorld cache = baritone.getWorldProvider().getCurrentWorld().getCachedWorld();

        @Override
        public Status isAlreadyExplored(int chunkX, int chunkZ) {
            int centerX = chunkX << 4;
            int centerZ = chunkZ << 4;
            if (cache.isCached(centerX, centerZ)) {
                return Status.EXPLORED;
            }
            if (!((CachedWorld) cache).regionLoaded(centerX, centerZ)) {
                Baritone.getExecutor().execute(() -> {
                    ((CachedWorld) cache).tryLoadFromDisk(centerX >> 9, centerZ >> 9);
                });
                return Status.UNKNOWN; // we still need to load regions from disk in order to decide properly
            }
            return Status.NOT_EXPLORED;
        }

        @Override
        public boolean finished() {
            return false;
        }
    }

    private class JsonChunkFilter implements IChunkFilter {
        private final boolean invert; // if true, the list is interpreted as a list of chunks that are NOT explored, if false, the list is interpreted as a list of chunks that ARE explored
        private final LongOpenHashSet inFilter;
        private final MyChunkPos[] positions;

        private JsonChunkFilter(Path path, boolean invert) throws Exception { // ioexception, json exception, etc
            this.invert = invert;
            Gson gson = new GsonBuilder().create();
            positions = gson.fromJson(new InputStreamReader(Files.newInputStream(path)), MyChunkPos[].class);
            logDirect("Loaded " + positions.length + " positions");
            inFilter = new LongOpenHashSet();
            for (MyChunkPos mcp : positions) {
                inFilter.add(ChunkPos.asLong(mcp.x, mcp.z));
            }
        }

        @Override
        public Status isAlreadyExplored(int chunkX, int chunkZ) {
            if (inFilter.contains(ChunkPos.asLong(chunkX, chunkZ)) ^ invert) {
                // either it's on the list of explored chunks, or it's not on the list of unexplored chunks
                // either way, we have it
                return Status.EXPLORED;
            } else {
                // either it's not on the list of explored chunks, or it's on the list of unexplored chunks
                // either way, it depends on if baritone has cached it so defer to that
                return Status.UNKNOWN;
            }
        }

        @Override
        public boolean finished() {
            if (!invert) {
                // if invert is false, anything not on the list is uncached
                return false;
            }
            // but if invert is true, anything not on the list IS assumed cached
            // so we are done if everything on our list is cached!
            BaritoneChunkCache bcc = new BaritoneChunkCache();
            for (MyChunkPos pos : positions) {
                if (bcc.isAlreadyExplored(pos.x, pos.z) != Status.EXPLORED) {
                    // either waiting for it or dont have it at all
                    return false;
                }
            }
            return true; // we have everything cached
        }
    }

    private class EitherChunk implements IChunkFilter {
        private final IChunkFilter a;
        private final IChunkFilter b;

        private EitherChunk(IChunkFilter a, IChunkFilter b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public Status isAlreadyExplored(int chunkX, int chunkZ) {
            if (a.isAlreadyExplored(chunkX, chunkZ) == Status.EXPLORED) {
                return Status.EXPLORED;
            }
            return b.isAlreadyExplored(chunkX, chunkZ);
        }

        @Override
        public boolean finished() {
            return a.finished() || b.finished();
        }
    }

    @Override
    public void onLostControl() {
        explorationOrigin = null;
    }

    @Override
    public String displayName0() {
        return "Exploring around " + explorationOrigin + ", currently going to " + new GoalComposite(closestUncachedChunks(explorationOrigin, calcFilter()));
    }
}
