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
import baritone.api.pathing.goals.GoalYLevel;
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

public final class ExploreProcess extends BaritoneProcessHelper implements IExploreProcess {

    private BlockPos explorationOrigin;

    private IChunkFilter filter;

    private int distanceCompleted;

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
        distanceCompleted = 0;
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
            if (Baritone.settings().notificationOnExploreFinished.value) {
                logNotification("Exploration failed", true);
            }
            onLostControl();
            return null;
        }
        IChunkFilter filter = calcFilter();
        if (!Baritone.settings().disableCompletionCheck.value && filter.countRemain() == 0) {
            logDirect("Explored all chunks");
            if (Baritone.settings().notificationOnExploreFinished.value) {
                logNotification("Explored all chunks", false);
            }
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
        int count = Math.min(filter.countRemain(), Baritone.settings().exploreChunkSetMinimumSize.value);
        List<BlockPos> centers = new ArrayList<>();
        int renderDistance = Baritone.settings().worldExploringChunkOffset.value;
        for (int dist = distanceCompleted; ; dist++) {
            for (int dx = -dist; dx <= dist; dx++) {
                int zval = dist - Math.abs(dx);
                for (int mult = 0; mult < 2; mult++) {
                    int dz = (mult * 2 - 1) * zval; // dz can be either -zval or zval
                    int trueDist = Math.abs(dx) + Math.abs(dz);
                    if (trueDist != dist) {
                        throw new IllegalStateException();
                    }
                    switch (filter.isAlreadyExplored(chunkX + dx, chunkZ + dz)) {
                        case UNKNOWN:
                            return null; // awaiting load
                        case NOT_EXPLORED:
                            break; // note: this breaks the switch not the for
                        case EXPLORED:
                            continue; // note: this continues the for
                        default:
                    }
                    int centerX = ((chunkX + dx) << 4) + 8;
                    int centerZ = ((chunkZ + dz) << 4) + 8;
                    int offset = renderDistance << 4;
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
            if (dist % 10 == 0) {
                count = Math.min(filter.countRemain(), Baritone.settings().exploreChunkSetMinimumSize.value);
            }
            if (centers.size() >= count) {
                return centers.stream().map(pos -> createGoal(pos.getX(), pos.getZ())).toArray(Goal[]::new);
            }
            if (centers.isEmpty()) {
                // we have explored everything from 0 to dist inclusive
                // next time we should start our check at dist+1
                distanceCompleted = dist + 1;
            }
        }
    }

    private static Goal createGoal(int x, int z) {
        if (Baritone.settings().exploreMaintainY.value == -1) {
            return new GoalXZ(x, z);
        }
        // don't use a goalblock because we still want isInGoal to return true if X and Z are correct
        // we just want to try and maintain Y on the way there, not necessarily end at that specific Y
        return new GoalXZ(x, z) {
            @Override
            public double heuristic(int x, int y, int z) {
                return super.heuristic(x, y, z) + GoalYLevel.calculate(Baritone.settings().exploreMaintainY.value, y);
            }
        };
    }

    private enum Status {
        EXPLORED, NOT_EXPLORED, UNKNOWN;
    }

    private interface IChunkFilter {

        Status isAlreadyExplored(int chunkX, int chunkZ);

        int countRemain();
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
        public int countRemain() {
            return Integer.MAX_VALUE;
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
        public int countRemain() {
            if (!invert) {
                // if invert is false, anything not on the list is uncached
                return Integer.MAX_VALUE;
            }
            // but if invert is true, anything not on the list IS assumed cached
            // so we are done if everything on our list is cached!
            int countRemain = 0;
            BaritoneChunkCache bcc = new BaritoneChunkCache();
            for (MyChunkPos pos : positions) {
                if (bcc.isAlreadyExplored(pos.x, pos.z) != Status.EXPLORED) {
                    // either waiting for it or dont have it at all
                    countRemain++;
                    if (countRemain >= Baritone.settings().exploreChunkSetMinimumSize.value) {
                        return countRemain;
                    }
                }
            }
            return countRemain;
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
        public int countRemain() {
            return Math.min(a.countRemain(), b.countRemain());
        }
    }

    @Override
    public void onLostControl() {
        explorationOrigin = null;
    }

    @Override
    public String displayName0() {
        return "Exploring around " + explorationOrigin + ", distance completed " + distanceCompleted + ", currently going to " + new GoalComposite(closestUncachedChunks(explorationOrigin, calcFilter()));
    }
}
