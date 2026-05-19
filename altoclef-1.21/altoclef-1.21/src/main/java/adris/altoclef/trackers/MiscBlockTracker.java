package adris.altoclef.trackers;

import adris.altoclef.AltoClef;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Sometimes we want to track specific block related things, like the last nether portal we used.
 * I don't want to pollute other trackers to add these.
 */
public class MiscBlockTracker {

    private final AltoClef _mod;

    private final Map<Dimension, BlockPos> _lastNetherPortalsUsed = new HashMap<>();

    // Make sure we only care about the nether portal we ENTERED through
    private Dimension _lastDimension;
    private boolean _newDimensionTriggered;

    public MiscBlockTracker(AltoClef mod) {
        _mod = mod;
    }

    public void tick() {
        if (WorldHelper.getCurrentDimension() != _lastDimension) {
            _lastDimension = WorldHelper.getCurrentDimension();
            _newDimensionTriggered = true;
        }

        if (AltoClef.inGame() && _newDimensionTriggered) {
            for (BlockPos check : WorldHelper.scanRegion(_mod, _mod.getPlayer().getBlockPos().add(-1, -1, -1), _mod.getPlayer().getBlockPos().add(1, 1, 1))) {
                Block currentBlock = _mod.getWorld().getBlockState(check).getBlock();
                if (currentBlock == Blocks.NETHER_PORTAL) {
                    // Make sure we get the lowest nether portal, as we can only really enter from the bottom.
                    while (check.getY() > 0) {
                        if (_mod.getWorld().getBlockState(check.down()).getBlock() == Blocks.NETHER_PORTAL) {
                            check = check.down();
                        } else {
                            break;
                        }
                    }
                    BlockPos below = check.down();
                    if (WorldHelper.isSolid(_mod, below)) {
                        _lastNetherPortalsUsed.put(WorldHelper.getCurrentDimension(), check);
                        _newDimensionTriggered = false;
                    }
                    break;
                }
            }
        }
    }

    public void reset() {
        _lastNetherPortalsUsed.clear();
    }

    public Optional<BlockPos> getLastUsedNetherPortal(Dimension dimension) {
        if (_lastNetherPortalsUsed.containsKey(dimension)) {
            BlockPos portalPos = _lastNetherPortalsUsed.get(dimension);
            // Check whether our nether portal pos is invalid.
            if (_mod.getChunkTracker().isChunkLoaded(portalPos)) {
                if (!_mod.getBlockTracker().blockIsValid(portalPos, Blocks.NETHER_PORTAL)) {
                    _lastNetherPortalsUsed.remove(dimension);
                    return Optional.empty();
                }
            }
            return Optional.ofNullable(portalPos);
        }
        return Optional.empty();
    }
}
