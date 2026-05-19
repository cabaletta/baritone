package adris.altoclef.trackers;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.BlockPlaceEvent;
import adris.altoclef.trackers.blacklisting.WorldLocateBlacklist;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.BaritoneHelper;
import adris.altoclef.util.helpers.ConfigHelper;
import adris.altoclef.util.helpers.StlHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.time.TimerGame;
import baritone.Baritone;
import baritone.api.utils.BlockOptionalMetaLookup;
import baritone.pathing.movement.CalculationContext;
import baritone.process.MineProcess;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Tracks blocks the way we want it, when we want it.
 * <p>
 * Gives you a "Check and don't care" interface where you can check for blocks and their locations over and over again
 * without scanning the world over and over again.
 * <p>
 * Also keeps track of blacklists for unreachable blocks
 */
public class BlockTracker extends Tracker {

    // This should be moved to an instance variable
    // but if set to true, block scanning will happen
    // asynchronously to spread out the expensive cost of scanning.
    private static final boolean ASYNC_SCANNING = true;
    private static BlockTrackerConfig _config = new BlockTrackerConfig();

    static {
        ConfigHelper.loadConfig("configs/block_tracker.json", BlockTrackerConfig::new, BlockTrackerConfig.class, newConfig -> _config = newConfig);
    }

    private final HashMap<Dimension, PosCache> _caches = new HashMap<>();

    //private final PosCache _cache = new PosCache(100, 64*1.5);

    private final TimerGame _timer = new TimerGame(_config.scanInterval);

    private final TimerGame _forceElapseTimer = new TimerGame(_config.scanIntervalWhenNewBlocksFound);

    // A scan can last no more than 15 seconds
    private final TimerGame _asyncForceResetScanFlag = new TimerGame(15);

    private final Map<Block, Integer> _trackingBlocks = new HashMap<>();

    private final Object _scanMutex = new Object();
    // Only perform scans at the END of our frame
    private final Semaphore _endOfFrameMutex = new Semaphore(1);
    //private Block _currentlyTracking = null;
    private final AltoClef _mod;
    private boolean _scanning = false;

    public BlockTracker(AltoClef mod, TrackerManager manager) {
        super(manager);
        _mod = mod;
        // First time, track immediately
        _timer.forceElapse();
        _forceElapseTimer.forceElapse();

        // Listen for block placement
        EventBus.subscribe(BlockPlaceEvent.class, evt -> addBlock(evt.blockState.getBlock(), evt.blockPos));
    }

    @Override
    protected void updateState() {
        if (shouldUpdate()) {
            update();
        }
    }

    // We want our `_trackingBlocks` value to be read AFTER tasks have finished ticking
    public void preTickTask() {
        try {
            _endOfFrameMutex.acquire();
        } catch (InterruptedException e) {
            Debug.logWarning("Pre-tick failed to acquire block track mutex! (send logs)");
            e.printStackTrace();
        }
    }

    public void postTickTask() {
        _endOfFrameMutex.release();
    }

    @Override
    protected void reset() {
        // Tasks will handle de-tracking blocks.
        if (!_caches.values().isEmpty()) {
            for (PosCache cache : _caches.values()) {
                cache.clear();
            }
        }
    }

    public boolean isTracking(Block block) {
        synchronized (_trackingBlocks) {
            return _trackingBlocks.containsKey(block) && _trackingBlocks.get(block) > 0;
        }
    }

    /**
     * Starts tracking/pay attention to some blocks.
     * <b>IMPORTANT:</b> ALWAYS pair with {@link #stopTracking(Block...) stopTracking}! Otherwise this block type will be
     * tracked forever (not the end of the world, but other block types will be lost.
     */
    public void trackBlock(Block... blocks) {
        synchronized (_trackingBlocks) {
            for (Block block : blocks) {
                if (!_trackingBlocks.containsKey(block)) {
                    // We're tracking a new block, so we're not updated.
                    setDirty();
                    _trackingBlocks.put(block, 0);
                    // Force a rescan if these are new blocks and we aren't doing this like every frame.
                    if (_forceElapseTimer.elapsed()) {
                        _timer.forceElapse();
                        _forceElapseTimer.reset();
                        _forceElapseTimer.setInterval(_config.scanIntervalWhenNewBlocksFound);
                    }
                }
                _trackingBlocks.put(block, _trackingBlocks.get(block) + 1);
            }
        }
    }

    /**
     * Stops tracking some blocks, after calling {@link #trackBlock(Block...) trackBlock}.
     * <p>
     * Only call this once for every {@link #trackBlock(Block...) trackBlock}.
     */
    public void stopTracking(Block... blocks) {
        synchronized (_trackingBlocks) {
            for (Block block : blocks) {
                if (_trackingBlocks.containsKey(block)) {
                    int current = _trackingBlocks.get(block);
                    if (current == 0) {
                        Debug.logWarning("Untracked block " + block + " more times than necessary. BlockTracker stack is unreliable from this point on.");
                    } else {
                        _trackingBlocks.put(block, current - 1);
                        if (_trackingBlocks.get(block) <= 0) {
                            _trackingBlocks.remove(block);
                        }
                    }
                }
            }
        }
    }

    /**
     * Manually add a block at a position.
     */
    public void addBlock(Block block, BlockPos pos) {
        if (blockIsValid(pos, block)) {
            synchronized (_scanMutex) {
                currentCache().addBlock(block, pos);
            }
        } else {
            Debug.logInternal("INVALID SET: " + block + " " + pos);
        }
    }

    public boolean anyFound(Block... blocks) {
        updateState();
        synchronized (_scanMutex) {
            return currentCache().anyFound(blocks);
        }
    }

    /**
     * Checks whether any blocks of a type have been found.
     *
     * @param isValidTest A filter predicate, returns true if a block at a position should be included.
     * @param blocks      The blocks to check for
     */
    public boolean anyFound(Predicate<BlockPos> isValidTest, Block... blocks) {
        updateState();
        synchronized (_scanMutex) {
            return currentCache().anyFound(isValidTest, blocks);
        }
    }

    public Optional<BlockPos> getNearestTracking(Block... blocks) {
        // Add juuust a little, to prevent digging down all the time/bias towards blocks BELOW the player
        return getNearestTracking(_mod.getPlayer().getPos().add(0, 0.6f, 0), blocks);
    }

    public Optional<BlockPos> getNearestTracking(Vec3d pos, Block... blocks) {
        return getNearestTracking(pos, p -> true, blocks);
    }

    public Optional<BlockPos> getNearestTracking(Predicate<BlockPos> isValidTest, Block... blocks) {
        return getNearestTracking(_mod.getPlayer().getPos().add(0, 0.6f, 0), isValidTest, blocks);
    }

    /**
     * Gets the nearest tracked block.
     *
     * @param pos         From what position? (defaults to the player's position)
     * @param isValidTest Filter predicate
     * @param blocks      The blocks to check for
     * @return Optional.of(block position) if found, otherwise Optional.empty
     */
    public Optional<BlockPos> getNearestTracking(Vec3d pos, Predicate<BlockPos> isValidTest, Block... blocks) {
        synchronized (_trackingBlocks) {
            for (Block block : blocks) {
                if (!_trackingBlocks.containsKey(block)) {
                    Debug.logWarning("BlockTracker: Not tracking block " + block + " right now.");
                    return Optional.empty();
                }
            }
        }
        // Make sure we've scanned the first time if we need to.
        updateState();
        synchronized (_scanMutex) {
            return currentCache().getNearest(_mod, pos, isValidTest, blocks);
        }
    }

    /**
     * Returns the locations of all tracked blocks of a given type
     */
    public List<BlockPos> getKnownLocations(Block... blocks) {
        updateState();
        synchronized (_scanMutex) {
            return currentCache().getKnownLocations(blocks);
        }
    }

    public Optional<BlockPos> getNearestWithinRange(BlockPos pos, double range, Block... blocks) {
        return getNearestWithinRange(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), range, blocks);
    }

    /**
     * Scans a radius for the closest block of a given type .
     *
     * @param pos    The center of this radius
     * @param range  Radius to scan for
     * @param blocks What blocks to check for
     */
    public Optional<BlockPos> getNearestWithinRange(Vec3d pos, double range, Block... blocks) {
        int minX = (int) Math.floor(pos.x - range),
                maxX = (int) Math.floor(pos.x + range),
                minY = (int) Math.floor(pos.y - range),
                maxY = (int) Math.floor(pos.y + range),
                minZ = (int) Math.floor(pos.z - range),
                maxZ = (int) Math.floor(pos.z + range);
        double closestDistance = Float.POSITIVE_INFINITY;
        BlockPos nearest = null;
        for (int x = minX; x <= maxX; ++x) {
            for (int y = minY; y <= maxY; ++y) {
                for (int z = minZ; z <= maxZ; ++z) {
                    BlockPos check = new BlockPos(x, y, z);
                    synchronized (_scanMutex) {
                        if (currentCache().blockUnreachable(check)) continue;
                    }

                    assert MinecraftClient.getInstance().world != null;
                    Block b = MinecraftClient.getInstance().world.getBlockState(check).getBlock();
                    boolean valid = false;
                    for (Block type : blocks) {
                        if (type == b) {
                            valid = true;
                            break;
                        }
                    }
                    if (!valid) continue;
                    if (check.isWithinDistance(pos, range)) {
                        double sq = check.getSquaredDistance(pos);
                        if (sq < closestDistance) {
                            closestDistance = sq;
                            nearest = check;
                        }
                    }
                }
            }
        }
        return Optional.ofNullable(nearest);
    }

    private boolean shouldUpdate() {
        return _timer.elapsed();
    }

    private void update() {
        // Perform a baritone scan
        _timer.reset();
        _timer.setInterval(_config.scanInterval);
        CalculationContext ctx = new CalculationContext(_mod.getClientBaritone(), _config.scanAsynchronously);
        if (_config.scanAsynchronously) {
            if (_scanning && _asyncForceResetScanFlag.elapsed()) {
                Debug.logMessage("SCANNING TOOK TOO LONG! Will assume it ended mid way. Hopefully this won't break anything...");
                _scanning = false;
            }
            if (!_scanning) {
                Baritone.getExecutor().execute(() -> {
                    _scanning = true;
                    _asyncForceResetScanFlag.reset();
                    rescanWorld(ctx, true);
                    _scanning = false;
                });
            }
        } else {
            // Synchronous scanning.
            rescanWorld(ctx, false);
        }
    }

    private void rescanWorld(CalculationContext ctx, boolean async) {
        Block[] blocksToScan;
        if (async) {
            // Wait for end of frame
            try {
                _endOfFrameMutex.acquire();
                _endOfFrameMutex.release();
            } catch (InterruptedException e) {
                Debug.logWarning("RESCAN INTERRUPTED! Will SKIP the scan (see logs)");
                _endOfFrameMutex.release();
                e.printStackTrace();
                return;
            }
        }
        synchronized (_trackingBlocks) {
            Debug.logInternal("Rescanning world for " + _trackingBlocks.size() + " blocks... Hopefully not dummy slow.");
            blocksToScan = new Block[_trackingBlocks.size()];
            _trackingBlocks.keySet().toArray(blocksToScan);
        }

        List<BlockPos> knownBlocks;
        synchronized (_scanMutex) {
            knownBlocks = currentCache().getKnownLocations(blocksToScan);
        }

        // Clear invalid block pos before rescan
        if (!knownBlocks.isEmpty()) {
            for (BlockPos check : knownBlocks) {
                if (!blockIsValid(check, blocksToScan)) {
                    //Debug.logInternal("Removed at " + check);
                    synchronized (_scanMutex) {
                        currentCache().removeBlock(check, blocksToScan);
                    }
                }
            }
        }

        // The scanning may run asynchronously.
        BlockOptionalMetaLookup boml = new BlockOptionalMetaLookup(blocksToScan);
        List<BlockPos> found = MineProcess.searchWorld(ctx, boml, _config.maxCacheSizePerBlockType, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        synchronized (_scanMutex) {
            if (MinecraftClient.getInstance().world != null) {
                if (!found.isEmpty()) {
                    for (BlockPos pos : found) {
                        Block block = MinecraftClient.getInstance().world.getBlockState(pos).getBlock();
                        synchronized (_trackingBlocks) {
                            if (_trackingBlocks.containsKey(block)) {
                                //Debug.logInternal("Good: " + block + " at " + pos);
                                currentCache().addBlock(block, pos);
                            }
                        }
                    }
                }

                // Purge if we have too many blocks tracked at once.
                currentCache().smartPurge(_mod, _mod.getPlayer().getPos());
            }
        }
    }

    // Checks whether it would be WRONG to say "at pos the block is block"
    // Returns true if wrong, false if correct OR undetermined/unsure.
    public boolean blockIsValid(BlockPos pos, Block... blocks) {
        synchronized (_scanMutex) {
            // We can't reach it, don't even try.
            if (currentCache().blockUnreachable(pos)) {
                return false;
            }
        }
        // It might be OK to remove this. Will have to test.
        if (!_mod.getChunkTracker().isChunkLoaded(pos)) {
            //Debug.logInternal("(failed chunkcheck: " + new ChunkPos(pos) + ")");
            //Debug.logStack();
            return true;
        }
        // I'm bored
        ClientWorld zaWarudo = MinecraftClient.getInstance().world;
        // No world, therefore we don't assume block is invalid.
        if (zaWarudo == null) {
            return true;
        }
        try {
            for (Block block : blocks) {
                if (zaWarudo.isAir(pos) && WorldHelper.isAir(block)) {
                    return true;
                }
                BlockState state = zaWarudo.getBlockState(pos);
                if (state.getBlock() == block) {
                    return true;
                }
            }
            return false;
        } catch (NullPointerException e) {
            // Probably out of chunk. This means we can't judge its state.
            return true;
        }
    }

    /**
     * @param pos BlockPos to check for
     * @return Whether that block is considered unreachable
     */
    public boolean unreachable(BlockPos pos) {
        synchronized (_scanMutex) {
            return currentCache().blockUnreachable(pos);
        }
    }

    /**
     * Inform the block tracker that the bot was NOT able to reach a block.
     *
     * @param pos             block that we were unable to reach
     * @param allowedFailures how many times we can try reaching before we finally declare this block "unreachable"
     */
    public void requestBlockUnreachable(BlockPos pos, int allowedFailures) {
        synchronized (_scanMutex) {
            currentCache().blacklistBlockUnreachable(_mod, pos, allowedFailures);
        }
    }

    public void requestBlockUnreachable(BlockPos pos) {
        requestBlockUnreachable(pos, _config.defaultUnreachableAttemptsAllowed);
    }

    private PosCache currentCache() {
        Dimension dimension = WorldHelper.getCurrentDimension();
        if (!_caches.containsKey(dimension)) {
            _caches.put(dimension, new PosCache());
        }
        return _caches.get(dimension);
    }


    static class PosCache {
        private final HashMap<Block, List<BlockPos>> _cachedBlocks = new HashMap<>();

        private final HashMap<BlockPos, Block> _cachedByPosition = new HashMap<>();

        private final WorldLocateBlacklist _blacklist = new WorldLocateBlacklist();

        public boolean anyFound(Block... blocks) {
            for (Block block : blocks) {
                if (_cachedBlocks.containsKey(block)) return true;
            }
            return false;
        }

        public boolean anyFound(Predicate<BlockPos> isValidTest, Block... blocks) {
            for (Block block : blocks) {
                if (_cachedBlocks.containsKey(block)) {
                    for (BlockPos pos : _cachedBlocks.get(block)) {
                        if (isValidTest.test(pos)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        public List<BlockPos> getKnownLocations(Block... blocks) {
            List<BlockPos> result = new ArrayList<>();
            for (Block block : blocks) {
                List<BlockPos> found = _cachedBlocks.get(block);
                if (found != null) {
                    result.addAll(found);
                }
            }
            return result;
        }

        public void removeBlock(BlockPos pos, Block... blocks) {
            for (Block block : blocks) {
                if (_cachedBlocks.containsKey(block)) {
                    _cachedBlocks.get(block).remove(pos);
                    _cachedByPosition.remove(pos);
                    if (_cachedBlocks.get(block).size() == 0) {
                        _cachedBlocks.remove(block);
                    }
                }
            }
        }

        public void addBlock(Block block, BlockPos pos) {
            if (blockUnreachable(pos)) return;
            if (_cachedByPosition.containsKey(pos)) {
                if (_cachedByPosition.get(pos) == block) {
                    // We're already tracked
                    return;
                } else {
                    // We're tracked incorrectly, fix
                    removeBlock(pos, block);
                }
            }
            if (!anyFound(block)) {
                _cachedBlocks.put(block, new ArrayList<>());
            }
            _cachedBlocks.get(block).add(pos);
            _cachedByPosition.put(pos, block);
        }


        public void clear() {
            Debug.logInternal("CLEARED BLOCK CACHE");
            _cachedBlocks.clear();
            _cachedByPosition.clear();
            _blacklist.clear();
        }

        public int getBlockTrackCount() {
            int count = 0;
            if (!_cachedBlocks.values().isEmpty()) {
                for (List<BlockPos> list : _cachedBlocks.values()) {
                    count += list.size();
                }
            }
            return count;
        }

        public void blacklistBlockUnreachable(AltoClef mod, BlockPos pos, int allowedFailures) {
            _blacklist.blackListItem(mod, pos, allowedFailures);
        }

        public boolean blockUnreachable(BlockPos pos) {
            return _blacklist.unreachable(pos);
        }

        // Gets nearest block. For now does linear search. In the future might optimize this a bit
        public Optional<BlockPos> getNearest(AltoClef mod, Vec3d position, Predicate<BlockPos> isValid, Block... blocks) {
            if (!anyFound(blocks)) {
                //Debug.logInternal("(failed cataloguecheck for " + block.getTranslationKey() + ")");
                return Optional.empty();
            }

            BlockPos closest = null;
            double minScore = Double.POSITIVE_INFINITY;

            List<BlockPos> blockList = getKnownLocations(blocks);

            int toPurge = blockList.size() - _config.maxCacheSizePerBlockType;

            boolean closestPurged = false;
            if (!blockList.isEmpty()) {
                for (BlockPos pos : blockList) {
                    // If our current block isn't valid, fix it up. This cleans while we're iterating.
                    if (!mod.getBlockTracker().blockIsValid(pos, blocks)) {
                        removeBlock(pos, blocks);
                        continue;
                    }
                    if (!isValid.test(pos)) continue;

                    double score = BaritoneHelper.calculateGenericHeuristic(position, WorldHelper.toVec3d(pos));

                    boolean currentlyClosest = false;
                    boolean purged = false;

                    if (score < minScore) {
                        minScore = score;
                        closest = pos;
                        currentlyClosest = true;
                    }

                    if (toPurge > 0) {
                        double sqDist = position.squaredDistanceTo(WorldHelper.toVec3d(pos));
                        if (sqDist > _config.cutoffDistance * _config.cutoffDistance) {
                            // cut this one off.
                            for (Block block : blocks) {
                                if (_cachedBlocks.containsKey(block)) {
                                    removeBlock(pos, block);
                                }
                            }
                            toPurge--;
                            purged = true;
                        }
                    }

                    if (currentlyClosest) {
                        closestPurged = purged;
                    }
                }
            }

            while (toPurge > 0) {
                if (blockList.size() == 0) {
                    //noinspection UnusedAssignment
                    toPurge = 0;
                    break;
                }
                blockList.remove(blockList.size() - 1);
                toPurge--;
            }

            // Special case: Our closest was purged. Add us back.
            if (closestPurged) {
                Debug.logInternal("Rare edge case: Closest block was purged cause it was real far away, it will now be added back.");
                blockList.add(closest);
            }

            return Optional.ofNullable(closest);
        }

        /**
         * Purge enough blocks so our size is small enough
         */
        public void smartPurge(AltoClef mod, Vec3d playerPos) {

            // Clear cached by position blocks, as they can be a handful.
            try {
                int MAX_CACHE_SIZE = _config.maxTotalCacheSize;
                if (_cachedByPosition.size() > MAX_CACHE_SIZE) {
                    List<BlockPos> toRemoveList = new ArrayList<>(_cachedByPosition.size() - MAX_CACHE_SIZE);
                    // Just purge randomly.
                    if (!_cachedByPosition.keySet().isEmpty()) {
                        for (BlockPos pos : _cachedByPosition.keySet()) {
                            if (_cachedByPosition.size() - toRemoveList.size() < MAX_CACHE_SIZE) {
                                break;
                            }
                            toRemoveList.add(pos);
                        }
                    }
                    if (!toRemoveList.isEmpty()) {
                        for (BlockPos toDelete : toRemoveList) {
                            _cachedByPosition.remove(toDelete);
                        }
                    }
                }
            } catch (Exception e) {
                Debug.logWarning("Failed to purge/reduce _cachedByPosition cache.: Its size remains at " + _cachedByPosition.size());
            }

            // ^^^ TODO: Something about that feels fishy, particularly how it's disconnected from the _cachedBlocks purging.
            // I smell a dangerous edge case bug.
            if (!_cachedBlocks.keySet().isEmpty()) {
                for (Block block : _cachedBlocks.keySet()) {
                    List<BlockPos> tracking = _cachedBlocks.get(block);

                    // Clear blacklisted blocks
                    try {
                        // Untrack the blocks further away
                        tracking = tracking.stream()
                                .filter(pos -> !_blacklist.unreachable(pos))
                                // This is invalid, because some blocks we may want to GO TO not BREAK.
                                //.filter(pos -> !mod.getExtraBaritoneSettings().shouldAvoidBreaking(pos))
                                .distinct()
                                .sorted(StlHelper.compareValues((BlockPos blockpos) -> blockpos.getSquaredDistance(playerPos)))
                                .collect(Collectors.toList());
                        tracking = tracking.stream()
                                .limit(_config.maxCacheSizePerBlockType)
                                .collect(Collectors.toList());
                        // This won't update otherwise.
                        _cachedBlocks.put(block, tracking);
                    } catch (IllegalArgumentException e) {
                        // Comparison method violates its general contract: Sometimes transitivity breaks.
                        // In which case, ignore it.
                        Debug.logWarning("Failed to purge/reduce block search count for " + block + ": It remains at " + tracking.size());
                    }
                }
            }
        }
    }

    static class BlockTrackerConfig {
        public double scanInterval = 7;
        public double scanIntervalWhenNewBlocksFound = 2;
        public boolean scanAsynchronously = true;
        public int maxTotalCacheSize = 2500;
        public int maxCacheSizePerBlockType = 25;
        public double cutoffDistance = 128;
        public int defaultUnreachableAttemptsAllowed = 4;
    }
}
