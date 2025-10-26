package baritone.process;

import baritone.Baritone;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalComposite;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.process.IBlockScanProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.BetterBlockPos;
import baritone.cache.CachedChunk;
import baritone.utils.BaritoneProcessHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Process for scanning an area to find specific blocks
 */
public class BlockScanProcess extends BaritoneProcessHelper implements IBlockScanProcess {

    private boolean isActive = false;
    private List<Block> targetBlocks = new ArrayList<>();
    private Map<Block, List<BetterBlockPos>> foundBlocks = new HashMap<>();
    
    // Scan mode: true = scan entire area, false = stop at first find
    private boolean scanEntireArea = true;
    
    // Search area parameters
    private BetterBlockPos center;
    private int radius = -1;
    private BetterBlockPos corner1;
    private BetterBlockPos corner2;
    
    // Scanning progress
    private Set<ChunkPos> scannedChunks = new HashSet<>();
    private List<ChunkPos> chunksToScan = new ArrayList<>();
    private int currentChunkIndex = 0;
    private boolean scanComplete = false;
    private int tickCounter = 0;
    private int lastProgressReport = 0;
    
    // ETA calculation
    private long scanStartTime = 0;
    private int chunksScannedAtLastCheck = 0;
    private long lastCheckTime = 0;
    private double avgChunksPerSecond = 0;

    public BlockScanProcess(Baritone baritone) {
        super(baritone);
    }

    /**
     * Start scanning with a radius around the player
     */
    public void startScanRadius(List<Block> blocks, int radius, boolean scanAll) {
        this.targetBlocks = new ArrayList<>(blocks);
        this.radius = radius;
        this.center = ctx.playerFeet();
        this.corner1 = null;
        this.corner2 = null;
        this.scanEntireArea = scanAll;
        initializeScan();
    }

    /**
     * Start scanning a rectangular region
     */
    public void startScanRegion(List<Block> blocks, BlockPos pos1, BlockPos pos2, boolean scanAll) {
        this.targetBlocks = new ArrayList<>(blocks);
        this.radius = -1;
        this.corner1 = new BetterBlockPos(pos1);
        this.corner2 = new BetterBlockPos(pos2);
        this.center = new BetterBlockPos(
            (corner1.x + corner2.x) / 2,
            (corner1.y + corner2.y) / 2,
            (corner1.z + corner2.z) / 2
        );
        this.scanEntireArea = scanAll;
        initializeScan();
    }

    private void initializeScan() {
        this.isActive = true;
        this.foundBlocks.clear();
        this.scannedChunks.clear();
        this.chunksToScan.clear();
        this.currentChunkIndex = 0;
        this.scanComplete = false;
        this.tickCounter = 0;
        this.lastProgressReport = 0;
        
        // Initialize ETA tracking
        this.scanStartTime = System.currentTimeMillis();
        this.lastCheckTime = this.scanStartTime;
        this.chunksScannedAtLastCheck = 0;
        this.avgChunksPerSecond = 0;

        // Initialize found blocks map
        for (Block block : targetBlocks) {
            foundBlocks.put(block, new ArrayList<>());
        }

        // Calculate chunks to scan
        calculateChunksToScan();

        logDirect("Starting block scan for: " + 
            targetBlocks.stream().map(Block::getTranslationKey).collect(Collectors.joining(", ")));
        logDirect("Scan mode: " + (scanEntireArea ? "Complete area" : "Stop at first find"));
        logDirect("Total chunks to scan: " + chunksToScan.size());
    }

    private void calculateChunksToScan() {
        Set<ChunkPos> chunks = new HashSet<>();

        if (radius > 0) {
            // Radius-based scanning
            int chunkRadius = (radius / 16) + 1;
            int centerChunkX = center.x >> 4;
            int centerChunkZ = center.z >> 4;

            for (int x = centerChunkX - chunkRadius; x <= centerChunkX + chunkRadius; x++) {
                for (int z = centerChunkZ - chunkRadius; z <= centerChunkZ + chunkRadius; z++) {
                    chunks.add(new ChunkPos(x, z));
                }
            }
        } else if (corner1 != null && corner2 != null) {
            // Region-based scanning
            int minX = Math.min(corner1.x, corner2.x) >> 4;
            int maxX = Math.max(corner1.x, corner2.x) >> 4;
            int minZ = Math.min(corner1.z, corner2.z) >> 4;
            int maxZ = Math.max(corner1.z, corner2.z) >> 4;

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    chunks.add(new ChunkPos(x, z));
                }
            }
        }

        // Sort chunks by distance from center for efficient scanning
        chunksToScan = chunks.stream()
            .sorted(Comparator.comparingDouble(cp -> {
                int dx = (cp.x << 4) - center.x;
                int dz = (cp.z << 4) - center.z;
                return dx * dx + dz * dz;
            }))
            .collect(Collectors.toList());
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        if (!isActive || scanComplete) {
            return new PathingCommand(null, PathingCommandType.DEFER);
        }

        tickCounter++;

        // Report progress every 5 seconds (100 ticks)
        if (tickCounter - lastProgressReport >= 100) {
            reportProgress();
            lastProgressReport = tickCounter;
        }

        // OPTIMIZED: Only scan currently loaded chunks (no pathfinding needed!)
        scanCurrentlyLoadedChunks();

        // Check if we should stop (found something in stop-at-first mode)
        if (!scanEntireArea && hasFoundAnyBlock()) {
            completeScan();
            return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
        }

        // Check if all chunks in range are scanned
        if (scannedChunks.size() >= chunksToScan.size()) {
            completeScan();
            return new PathingCommand(null, PathingCommandType.DEFER);
        }

        // For unscanned chunks beyond render distance, move player closer
        ChunkPos nextUnscannedChunk = findNextUnscannedChunk();
        if (nextUnscannedChunk != null) {
            // Move toward the chunk center
            int targetX = (nextUnscannedChunk.x << 4) + 8;
            int targetZ = (nextUnscannedChunk.z << 4) + 8;
            Goal goal = new GoalXZ(targetX, targetZ);
            return new PathingCommand(goal, PathingCommandType.SET_GOAL_AND_PATH);
        }

        return new PathingCommand(null, PathingCommandType.DEFER);
    }

    /**
     * OPTIMIZED: Scan only currently loaded chunks without pathfinding
     * This is much faster and works well with limited render distance
     */
    private void scanCurrentlyLoadedChunks() {
        for (ChunkPos chunkPos : chunksToScan) {
            if (scannedChunks.contains(chunkPos)) {
                continue;
            }

            // Check if chunk is actually loaded in the world
            if (!isChunkLoaded(chunkPos)) {
                continue;
            }

            scanChunk(chunkPos);
            scannedChunks.add(chunkPos);

            // If stop-at-first mode and we found something, break
            if (!scanEntireArea && hasFoundAnyBlock()) {
                break;
            }
        }
    }

    /**
     * Find the next unscanned chunk closest to the player
     */
    private ChunkPos findNextUnscannedChunk() {
        BlockPos playerPos = ctx.playerFeet();
        ChunkPos playerChunk = new ChunkPos(playerPos);
        
        return chunksToScan.stream()
            .filter(cp -> !scannedChunks.contains(cp))
            .filter(cp -> !isChunkLoaded(cp)) // Only unloaded chunks need pathfinding
            .min(Comparator.comparingDouble(cp -> {
                int dx = cp.x - playerChunk.x;
                int dz = cp.z - playerChunk.z;
                return dx * dx + dz * dz;
            }))
            .orElse(null);
    }

    /**
     * Check if a chunk is loaded (not null and not empty)
     */
    private boolean isChunkLoaded(ChunkPos chunkPos) {
        try {
            Chunk chunk = ctx.world().getChunk(chunkPos.x, chunkPos.z);
            return chunk != null && !(chunk instanceof EmptyChunk);
        } catch (Exception e) {
            return false;
        }
    }

    private void scanChunk(ChunkPos chunkPos) {
        Chunk chunk = ctx.world().getChunk(chunkPos.x, chunkPos.z);
        
        // Skip empty chunks
        if (chunk instanceof EmptyChunk) {
            return;
        }
        
        int minY, maxY;
        if (corner1 != null && corner2 != null) {
            minY = Math.min(corner1.y, corner2.y);
            maxY = Math.max(corner1.y, corner2.y);
        } else {
            minY = 0;
            maxY = 256;
        }

        int baseX = chunkPos.x << 4;
        int baseZ = chunkPos.z << 4;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    BlockPos pos = new BlockPos(baseX + x, y, baseZ + z);
                    
                    // Check if position is within search area
                    if (!isInSearchArea(pos)) {
                        continue;
                    }

                    Block block = ctx.world().getBlockState(pos).getBlock();
                    
                    if (targetBlocks.contains(block)) {
                        BetterBlockPos foundPos = new BetterBlockPos(pos);
                        List<BetterBlockPos> positions = foundBlocks.get(block);
                        if (!positions.contains(foundPos)) {
                            positions.add(foundPos);
                            logDirect("Found " + block.getTranslationKey() + " at " + foundPos);
                        }
                    }
                }
            }
        }
    }

    private boolean isInSearchArea(BlockPos pos) {
        if (radius > 0) {
            // Check radius from center
            int dx = pos.getX() - center.x;
            int dz = pos.getZ() - center.z;
            return (dx * dx + dz * dz) <= (radius * radius);
        } else if (corner1 != null && corner2 != null) {
            // Check if within rectangular bounds
            int minX = Math.min(corner1.x, corner2.x);
            int maxX = Math.max(corner1.x, corner2.x);
            int minY = Math.min(corner1.y, corner2.y);
            int maxY = Math.max(corner1.y, corner2.y);
            int minZ = Math.min(corner1.z, corner2.z);
            int maxZ = Math.max(corner1.z, corner2.z);

            return pos.getX() >= minX && pos.getX() <= maxX &&
                   pos.getY() >= minY && pos.getY() <= maxY &&
                   pos.getZ() >= minZ && pos.getZ() <= maxZ;
        }
        return false;
    }

    private boolean hasFoundAnyBlock() {
        return foundBlocks.values().stream().anyMatch(list -> !list.isEmpty());
    }

    private void reportProgress() {
        long currentTime = System.currentTimeMillis();
        int chunksScanned = scannedChunks.size();
        int totalChunks = chunksToScan.size();
        int progress = (chunksScanned * 100) / totalChunks;
        int blocksFound = foundBlocks.values().stream().mapToInt(List::size).sum();
        
        // Calculate ETA
        String etaString = calculateETA(currentTime, chunksScanned, totalChunks);
        
        logDirect(String.format("[Scan Progress] %d%% (%d/%d chunks) | Blocks found: %d | ETA: %s", 
            progress, chunksScanned, totalChunks, blocksFound, etaString));
    }
    
    private String calculateETA(long currentTime, int chunksScanned, int totalChunks) {
        // Need at least 2 seconds of data for accurate ETA
        long timeElapsed = currentTime - scanStartTime;
        if (timeElapsed < 2000 || chunksScanned == 0) {
            return "Calculating...";
        }
        
        // Calculate average chunks per second
        double currentRate = (double)(chunksScanned - chunksScannedAtLastCheck) / 
                            ((currentTime - lastCheckTime) / 1000.0);
        
        // Smooth the rate with exponential moving average
        if (avgChunksPerSecond == 0) {
            avgChunksPerSecond = currentRate;
        } else {
            avgChunksPerSecond = (avgChunksPerSecond * 0.7) + (currentRate * 0.3);
        }
        
        // Update tracking
        chunksScannedAtLastCheck = chunksScanned;
        lastCheckTime = currentTime;
        
        // Calculate remaining time
        int remainingChunks = totalChunks - chunksScanned;
        if (avgChunksPerSecond <= 0) {
            return "Unknown";
        }
        
        long remainingSeconds = (long)(remainingChunks / avgChunksPerSecond);
        
        // Format time
        if (remainingSeconds < 60) {
            return remainingSeconds + "s";
        } else if (remainingSeconds < 3600) {
            long minutes = remainingSeconds / 60;
            long seconds = remainingSeconds % 60;
            return String.format("%dm %ds", minutes, seconds);
        } else {
            long hours = remainingSeconds / 3600;
            long minutes = (remainingSeconds % 3600) / 60;
            return String.format("%dh %dm", hours, minutes);
        }
    }

    private void completeScan() {
        scanComplete = true;
        isActive = false;

        logDirect("=== Scan Complete ===");
        logDirect("Scanned " + scannedChunks.size() + " / " + chunksToScan.size() + " chunks");

        int totalFound = 0;
        for (Map.Entry<Block, List<BetterBlockPos>> entry : foundBlocks.entrySet()) {
            List<BetterBlockPos> positions = entry.getValue();
            if (!positions.isEmpty()) {
                logDirect(entry.getKey().getTranslationKey() + ": " + positions.size() + " found");
                totalFound += positions.size();
            }
        }

        if (totalFound == 0) {
            logDirect("No target blocks found in search area");
        } else {
            logDirect("Total blocks found: " + totalFound);
        }
    }

    public void stop() {
        if (isActive) {
            logDirect("Block scan stopped");
            reportProgress();
            isActive = false;
            scanComplete = true;
        }
    }

    public void clearResults() {
        foundBlocks.clear();
        for (Block block : targetBlocks) {
            foundBlocks.put(block, new ArrayList<>());
        }
        logDirect("Scan results cleared");
    }

    public Map<Block, List<BetterBlockPos>> getFoundBlocks() {
        return new HashMap<>(foundBlocks);
    }
    
    public int getProgress() {
        if (chunksToScan.isEmpty()) return 0;
        return (scannedChunks.size() * 100) / chunksToScan.size();
    }
    
    public String getProgressString() {
        int blocksFound = foundBlocks.values().stream().mapToInt(List::size).sum();
        return String.format("Progress: %d%% (%d/%d chunks) | Found: %d blocks", 
            getProgress(), scannedChunks.size(), chunksToScan.size(), blocksFound);
    }

    @Override
    public void onLostControl() {
        // If we lost control and scan is not complete, it means user cancelled
        // or another process took over
        if (isActive && !scanComplete) {
            logDirect("[Scan] Paused - will resume when possible");
        }
    }
    
    /**
     * Called when #cancel is used - properly stop the scan
     */
    public void forceStop() {
        if (isActive) {
            logDirect("[Scan] Cancelled by user");
            reportProgress();
            isActive = false;
            scanComplete = true;
        }
    }

    @Override
    public String displayName0() {
        return "Block Scanner";
    }

    @Override
    public double priority() {
        return 4; // Lower priority than most processes so #cancel works
    }

    @Override
    public boolean isTemporary() {
        return false;
    }
}
