package adris.altoclef.util.progresscheck;

import adris.altoclef.AltoClef;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class MovementProgressChecker {

    private final IProgressChecker<Vec3d> _distanceChecker;
    private final IProgressChecker<Double> _mineChecker;

    private BlockPos _lastBreakingBlock = null;

    public MovementProgressChecker(double distanceTimeout, double minDistance, double mineTimeout, double minMineProgress, int attempts) {
        _distanceChecker = new ProgressCheckerRetry<>(new DistanceProgressChecker(distanceTimeout, minDistance), attempts);
        _mineChecker = new LinearProgressChecker(mineTimeout, minMineProgress);
    }

    public MovementProgressChecker(double distanceTimeout, double minDistance, double mineTimeout, double minMineProgress) {
        this(distanceTimeout, minDistance, mineTimeout, minMineProgress, 1);
    }

    public MovementProgressChecker(int attempts) {
        this(6, 0.1, 0.5, 0.001, attempts);
    }

    public MovementProgressChecker() {
        this(1);
    }

    public boolean check(AltoClef mod) {

        // Allow pause on eat
        if (mod.getFoodChain().needsToEat()) {
            _distanceChecker.reset();
            _mineChecker.reset();
        }

        if (mod.getControllerExtras().isBreakingBlock()) {
            BlockPos breakBlock = mod.getControllerExtras().getBreakingBlockPos();
            // If we broke a block, we made progress.
            // We must also delay reseting the distance checker UNTIL we break a block.
            // Because otherwise we risk not failing if we keep retrtying to mine and don't succeed.
            if (_lastBreakingBlock != null && WorldHelper.isAir(mod, _lastBreakingBlock)) {
                _distanceChecker.reset();
                _mineChecker.reset();
            }
            _lastBreakingBlock = breakBlock;
            _mineChecker.setProgress(mod.getControllerExtras().getBreakingBlockProgress());
            return !_mineChecker.failed();
        } else {
            _mineChecker.reset();
            _distanceChecker.setProgress(mod.getPlayer().getPos());
            return !_distanceChecker.failed();
        }
    }

    public void reset() {
        _distanceChecker.reset();
        _mineChecker.reset();
    }

}
