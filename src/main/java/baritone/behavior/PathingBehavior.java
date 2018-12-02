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

package baritone.behavior;

import baritone.Baritone;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.event.events.PathEvent;
import baritone.api.event.events.PlayerUpdateEvent;
import baritone.api.event.events.RenderEvent;
import baritone.api.event.events.TickEvent;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.PathCalculationResult;
import baritone.api.utils.interfaces.IGoalRenderPos;
import baritone.pathing.calc.AStarPathFinder;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.path.CutoffPath;
import baritone.pathing.path.PathExecutor;
import baritone.utils.Helper;
import baritone.utils.PathRenderer;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.EmptyChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;

public final class PathingBehavior extends Behavior implements IPathingBehavior, Helper {

    private PathExecutor current;
    private PathExecutor next;

    private Goal goal;

    private boolean safeToCancel;
    private boolean pauseRequestedLastTick;
    private boolean cancelRequested;
    private boolean calcFailedLastTick;

    private volatile AbstractNodeCostSearch inProgress;
    private final Object pathCalcLock = new Object();

    private final Object pathPlanLock = new Object();

    private boolean lastAutoJump;

    private final LinkedBlockingQueue<PathEvent> toDispatch = new LinkedBlockingQueue<>();

    public PathingBehavior(Baritone baritone) {
        super(baritone);
    }

    private void queuePathEvent(PathEvent event) {
        toDispatch.add(event);
    }

    private void dispatchEvents() {
        ArrayList<PathEvent> curr = new ArrayList<>();
        toDispatch.drainTo(curr);
        calcFailedLastTick = curr.contains(PathEvent.CALC_FAILED);
        for (PathEvent event : curr) {
            baritone.getGameEventHandler().onPathEvent(event);
        }
    }

    @Override
    public void onTick(TickEvent event) {
        dispatchEvents();
        if (event.getType() == TickEvent.Type.OUT) {
            secretInternalSegmentCancel();
            baritone.getPathingControlManager().cancelEverything();
            return;
        }
        baritone.getPathingControlManager().preTick();
        tickPath();
        dispatchEvents();
    }

    private void tickPath() {
        if (pauseRequestedLastTick && safeToCancel) {
            pauseRequestedLastTick = false;
            baritone.getInputOverrideHandler().clearAllKeys();
            baritone.getInputOverrideHandler().getBlockBreakHelper().stopBreakingBlock();
            return;
        }
        if (cancelRequested) {
            cancelRequested = false;
            baritone.getInputOverrideHandler().clearAllKeys();
        }
        if (current == null) {
            return;
        }
        safeToCancel = current.onTick();
        synchronized (pathPlanLock) {
            if (current.failed() || current.finished()) {
                current = null;
                if (goal == null || goal.isInGoal(ctx.playerFeet())) {
                    logDebug("All done. At " + goal);
                    queuePathEvent(PathEvent.AT_GOAL);
                    next = null;
                    return;
                }
                if (next != null && !next.getPath().positions().contains(ctx.playerFeet())) {
                    // if the current path failed, we may not actually be on the next one, so make sure
                    logDebug("Discarding next path as it does not contain current position");
                    // for example if we had a nicely planned ahead path that starts where current ends
                    // that's all fine and good
                    // but if we fail in the middle of current
                    // we're nowhere close to our planned ahead path
                    // so need to discard it sadly.
                    queuePathEvent(PathEvent.DISCARD_NEXT);
                    next = null;
                }
                if (next != null) {
                    logDebug("Continuing on to planned next path");
                    queuePathEvent(PathEvent.CONTINUING_ONTO_PLANNED_NEXT);
                    current = next;
                    next = null;
                    current.onTick();
                    return;
                }
                // at this point, current just ended, but we aren't in the goal and have no plan for the future
                synchronized (pathCalcLock) {
                    if (inProgress != null) {
                        queuePathEvent(PathEvent.PATH_FINISHED_NEXT_STILL_CALCULATING);
                        // if we aren't calculating right now
                        return;
                    }
                    queuePathEvent(PathEvent.CALC_STARTED);
                    findPathInNewThread(pathStart(), true);
                }
                return;
            }
            // at this point, we know current is in progress
            if (safeToCancel && next != null && next.snipsnapifpossible()) {
                // a movement just ended; jump directly onto the next path
                logDebug("Splicing into planned next path early...");
                queuePathEvent(PathEvent.SPLICING_ONTO_NEXT_EARLY);
                current = next;
                next = null;
                current.onTick();
                return;
            }
            current = current.trySplice(next);
            if (next != null && current.getPath().getDest().equals(next.getPath().getDest())) {
                next = null;
            }
            synchronized (pathCalcLock) {
                if (inProgress != null) {
                    // if we aren't calculating right now
                    return;
                }
                if (next != null) {
                    // and we have no plan for what to do next
                    return;
                }
                if (goal == null || goal.isInGoal(current.getPath().getDest())) {
                    // and this path dosen't get us all the way there
                    return;
                }
                if (ticksRemainingInSegment().get() < Baritone.settings().planningTickLookAhead.get()) {
                    // and this path has 5 seconds or less left
                    logDebug("Path almost over. Planning ahead...");
                    queuePathEvent(PathEvent.NEXT_SEGMENT_CALC_STARTED);
                    findPathInNewThread(current.getPath().getDest(), false);
                }
            }
        }
    }

    @Override
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (current != null) {
            switch (event.getState()) {
                case PRE:
                    lastAutoJump = mc.gameSettings.autoJump;
                    mc.gameSettings.autoJump = false;
                    break;
                case POST:
                    mc.gameSettings.autoJump = lastAutoJump;
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public Optional<Double> ticksRemainingInSegment() {
        if (current == null) {
            return Optional.empty();
        }
        return Optional.of(current.getPath().ticksRemainingFrom(current.getPosition()));
    }

    public void secretInternalSetGoal(Goal goal) {
        this.goal = goal;
    }

    public boolean secretInternalSetGoalAndPath(Goal goal) {
        secretInternalSetGoal(goal);
        return secretInternalPath();
    }

    @Override
    public Goal getGoal() {
        return goal;
    }

    @Override
    public PathExecutor getCurrent() {
        return current;
    }

    @Override
    public PathExecutor getNext() {
        return next;
    }

    @Override
    public Optional<AbstractNodeCostSearch> getInProgress() {
        return Optional.ofNullable(inProgress);
    }

    @Override
    public boolean isPathing() {
        return this.current != null;
    }

    public boolean isSafeToCancel() {
        return current == null || safeToCancel;
    }

    public void requestPause() {
        pauseRequestedLastTick = true;
    }

    public boolean cancelSegmentIfSafe() {
        if (isSafeToCancel()) {
            secretInternalSegmentCancel();
            return true;
        }
        return false;
    }

    @Override
    public boolean cancelEverything() {
        boolean doIt = isSafeToCancel();
        if (doIt) {
            secretInternalSegmentCancel();
        }
        baritone.getPathingControlManager().cancelEverything();
        return doIt;
    }

    public boolean calcFailedLastTick() { // NOT exposed on public api
        return calcFailedLastTick;
    }

    public void softCancelIfSafe() {
        synchronized (pathPlanLock) {
            if (!isSafeToCancel()) {
                return;
            }
            current = null;
            next = null;
        }
        cancelRequested = true;
        getInProgress().ifPresent(AbstractNodeCostSearch::cancel); // only cancel ours
        // do everything BUT clear keys
    }

    // just cancel the current path
    public void secretInternalSegmentCancel() {
        queuePathEvent(PathEvent.CANCELED);
        synchronized (pathPlanLock) {
            current = null;
            next = null;
        }
        baritone.getInputOverrideHandler().clearAllKeys();
        getInProgress().ifPresent(AbstractNodeCostSearch::cancel);
        baritone.getInputOverrideHandler().getBlockBreakHelper().stopBreakingBlock();
    }

    public void forceCancel() { // NOT exposed on public api
        cancelEverything();
        secretInternalSegmentCancel();
        inProgress = null;
    }

    /**
     * Start calculating a path if we aren't already
     *
     * @return true if this call started path calculation, false if it was already calculating or executing a path
     */
    public boolean secretInternalPath() {
        if (goal == null) {
            return false;
        }
        if (goal.isInGoal(ctx.playerFeet())) {
            return false;
        }
        synchronized (pathPlanLock) {
            if (current != null) {
                return false;
            }
            synchronized (pathCalcLock) {
                if (inProgress != null) {
                    return false;
                }
                queuePathEvent(PathEvent.CALC_STARTED);
                findPathInNewThread(pathStart(), true);
                return true;
            }
        }
    }

    public void secretCursedFunctionDoNotCall(IPath path) {
        synchronized (pathPlanLock) {
            current = new PathExecutor(this, path);
        }
    }

    /**
     * See issue #209
     *
     * @return The starting {@link BlockPos} for a new path
     */
    public BetterBlockPos pathStart() { // TODO move to a helper or util class
        BetterBlockPos feet = ctx.playerFeet();
        if (!MovementHelper.canWalkOn(ctx, feet.down())) {
            if (ctx.player().onGround) {
                double playerX = ctx.player().posX;
                double playerZ = ctx.player().posZ;
                ArrayList<BetterBlockPos> closest = new ArrayList<>();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        closest.add(new BetterBlockPos(feet.x + dx, feet.y, feet.z + dz));
                    }
                }
                closest.sort(Comparator.comparingDouble(pos -> ((pos.x + 0.5D) - playerX) * ((pos.x + 0.5D) - playerX) + ((pos.z + 0.5D) - playerZ) * ((pos.z + 0.5D) - playerZ)));
                for (int i = 0; i < 4; i++) {
                    BetterBlockPos possibleSupport = closest.get(i);
                    double xDist = Math.abs((possibleSupport.x + 0.5D) - playerX);
                    double zDist = Math.abs((possibleSupport.z + 0.5D) - playerZ);
                    if (xDist > 0.8 && zDist > 0.8) {
                        // can't possibly be sneaking off of this one, we're too far away
                        continue;
                    }
                    if (MovementHelper.canWalkOn(ctx, possibleSupport.down()) && MovementHelper.canWalkThrough(ctx, possibleSupport) && MovementHelper.canWalkThrough(ctx, possibleSupport.up())) {
                        // this is plausible
                        //logDebug("Faking path start assuming player is standing off the edge of a block");
                        return possibleSupport;
                    }
                }

            } else {
                // !onGround
                // we're in the middle of a jump
                if (MovementHelper.canWalkOn(ctx, feet.down().down())) {
                    //logDebug("Faking path start assuming player is midair and falling");
                    return feet.down();
                }
            }
        }
        return feet;
    }

    /**
     * In a new thread, pathfind to target blockpos
     *
     * @param start
     * @param talkAboutIt
     */
    private void findPathInNewThread(final BlockPos start, final boolean talkAboutIt) {
        // this must be called with synchronization on pathCalcLock!
        // actually, we can check this, muahaha
        if (!Thread.holdsLock(pathCalcLock)) {
            throw new IllegalStateException("Must be called with synchronization on pathCalcLock");
            // why do it this way? it's already indented so much that putting the whole thing in a synchronized(pathCalcLock) was just too much lol
        }
        if (inProgress != null) {
            throw new IllegalStateException("Already doing it"); // should have been checked by caller
        }
        Goal goal = this.goal;
        if (goal == null) {
            logDebug("no goal"); // TODO should this be an exception too? definitely should be checked by caller
            return;
        }
        long primaryTimeout;
        long failureTimeout;
        if (current == null) {
            primaryTimeout = Baritone.settings().primaryTimeoutMS.get();
            failureTimeout = Baritone.settings().failureTimeoutMS.get();
        } else {
            primaryTimeout = Baritone.settings().planAheadPrimaryTimeoutMS.get();
            failureTimeout = Baritone.settings().planAheadFailureTimeoutMS.get();
        }
        CalculationContext context = new CalculationContext(baritone, true); // not safe to create on the other thread, it looks up a lot of stuff in minecraft
        AbstractNodeCostSearch pathfinder = createPathfinder(start, goal, current == null ? null : current.getPath(), context, true);
        if (!Objects.equals(pathfinder.getGoal(), goal)) {
            logDebug("Simplifying " + goal.getClass() + " to GoalXZ due to distance");
        }
        inProgress = pathfinder;
        Baritone.getExecutor().execute(() -> {
            if (talkAboutIt) {
                logDebug("Starting to search for path from " + start + " to " + goal);
            }

            PathCalculationResult calcResult = pathfinder.calculate(primaryTimeout, failureTimeout);
            Optional<IPath> path = calcResult.getPath();
            if (Baritone.settings().cutoffAtLoadBoundary.get()) {
                path = path.map(p -> {
                    IPath result = p.cutoffAtLoadedChunks(context.world());

                    if (result instanceof CutoffPath) {
                        logDebug("Cutting off path at edge of loaded chunks");
                        logDebug("Length decreased by " + (p.length() - result.length()));
                    } else {
                        logDebug("Path ends within loaded chunks");
                    }

                    return result;
                });
            }

            Optional<PathExecutor> executor = path.map(p -> {
                IPath result = p.staticCutoff(goal);

                if (result instanceof CutoffPath) {
                    logDebug("Static cutoff " + p.length() + " to " + result.length());
                }

                return result;
            }).map(p -> new PathExecutor(this, p));

            synchronized (pathPlanLock) {
                if (current == null) {
                    if (executor.isPresent()) {
                        queuePathEvent(PathEvent.CALC_FINISHED_NOW_EXECUTING);
                        current = executor.get();
                    } else {
                        if (calcResult.getType() != PathCalculationResult.Type.CANCELLATION && calcResult.getType() != PathCalculationResult.Type.EXCEPTION) {
                            // don't dispatch CALC_FAILED on cancellation
                            queuePathEvent(PathEvent.CALC_FAILED);
                        }
                    }
                } else {
                    if (next == null) {
                        if (executor.isPresent()) {
                            queuePathEvent(PathEvent.NEXT_SEGMENT_CALC_FINISHED);
                            next = executor.get();
                        } else {
                            queuePathEvent(PathEvent.NEXT_CALC_FAILED);
                        }
                    } else {
                        //throw new IllegalStateException("I have no idea what to do with this path");
                        // no point in throwing an exception here, and it gets it stuck with inProgress being not null
                        logDirect("Warning: PathingBehaivor illegal state! Discarding invalid path!");
                    }
                }
                if (talkAboutIt && current != null && current.getPath() != null) {
                    if (goal == null || goal.isInGoal(current.getPath().getDest())) {
                        logDebug("Finished finding a path from " + start + " to " + goal + ". " + current.getPath().getNumNodesConsidered() + " nodes considered");
                    } else {
                        logDebug("Found path segment from " + start + " towards " + goal + ". " + current.getPath().getNumNodesConsidered() + " nodes considered");
                    }
                }
                synchronized (pathCalcLock) {
                    inProgress = null;
                }
            }
        });
    }

    public static AbstractNodeCostSearch createPathfinder(BlockPos start, Goal goal, IPath previous, CalculationContext context, boolean allowSimplifyUnloaded) {
        Goal transformed = goal;
        if (Baritone.settings().simplifyUnloadedYCoord.get() && goal instanceof IGoalRenderPos && allowSimplifyUnloaded) {
            BlockPos pos = ((IGoalRenderPos) goal).getGoalPos();
            if (context.world().getChunk(pos) instanceof EmptyChunk) {
                transformed = new GoalXZ(pos.getX(), pos.getZ());
            }
        }
        LongOpenHashSet favoredPositions = null;
        if (Baritone.settings().backtrackCostFavoringCoefficient.get() != 1D && previous != null) {
            LongOpenHashSet tmp = new LongOpenHashSet();
            previous.positions().forEach(pos -> tmp.add(BetterBlockPos.longHash(pos)));
            favoredPositions = tmp;
        }
        return new AStarPathFinder(start.getX(), start.getY(), start.getZ(), transformed, favoredPositions, context);
    }

    @Override
    public void onRenderPass(RenderEvent event) {
        PathRenderer.render(event, this);
    }
}
