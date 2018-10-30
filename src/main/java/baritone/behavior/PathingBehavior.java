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
import baritone.api.pathing.calc.IPathFinder;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.interfaces.IGoalRenderPos;
import baritone.pathing.calc.AStarPathFinder;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.calc.CutoffPath;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.path.PathExecutor;
import baritone.utils.BlockBreakHelper;
import baritone.utils.Helper;
import baritone.utils.PathRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.EmptyChunk;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public final class PathingBehavior extends Behavior implements IPathingBehavior, Helper {

    private PathExecutor current;
    private PathExecutor next;

    private Goal goal;

    private volatile boolean isPathCalcInProgress;
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
        for (PathEvent event : curr) {
            Baritone.INSTANCE.getGameEventHandler().onPathEvent(event);
        }
    }

    @Override
    public void onTick(TickEvent event) {
        dispatchEvents();
        if (event.getType() == TickEvent.Type.OUT) {
            cancel();
            return;
        }
        tickPath();
        dispatchEvents();
    }

    private void tickPath() {
        if (current == null) {
            return;
        }
        boolean safe = current.onTick();
        synchronized (pathPlanLock) {
            if (current.failed() || current.finished()) {
                current = null;
                if (goal == null || goal.isInGoal(playerFeet())) {
                    logDebug("All done. At " + goal);
                    queuePathEvent(PathEvent.AT_GOAL);
                    next = null;
                    return;
                }
                if (next != null && !next.getPath().positions().contains(playerFeet())) {
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
                    if (isPathCalcInProgress) {
                        queuePathEvent(PathEvent.PATH_FINISHED_NEXT_STILL_CALCULATING);
                        // if we aren't calculating right now
                        return;
                    }
                    queuePathEvent(PathEvent.CALC_STARTED);
                    findPathInNewThread(pathStart(), true, Optional.empty());
                }
                return;
            }
            // at this point, we know current is in progress
            if (safe && next != null && next.snipsnapifpossible()) {
                // a movement just ended; jump directly onto the next path
                logDebug("Splicing into planned next path early...");
                queuePathEvent(PathEvent.SPLICING_ONTO_NEXT_EARLY);
                current = next;
                next = null;
                current.onTick();
                return;
            }
            synchronized (pathCalcLock) {
                if (isPathCalcInProgress) {
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
                    findPathInNewThread(current.getPath().getDest(), false, Optional.of(current.getPath()));
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

    @Override
    public void setGoal(Goal goal) {
        this.goal = goal;
    }

    public boolean setGoalAndPath(Goal goal) {
        setGoal(goal);
        return path();
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
    public Optional<IPathFinder> getPathFinder() {
        return Optional.ofNullable(AbstractNodeCostSearch.currentlyRunning());
    }

    @Override
    public boolean isPathing() {
        return this.current != null;
    }

    @Override
    public void cancel() {
        queuePathEvent(PathEvent.CANCELED);
        current = null;
        next = null;
        Baritone.INSTANCE.getInputOverrideHandler().clearAllKeys();
        AbstractNodeCostSearch.getCurrentlyRunning().ifPresent(AbstractNodeCostSearch::cancel);
        BlockBreakHelper.stopBreakingBlock();
    }

    public void forceCancel() { // NOT exposed on public api
        isPathCalcInProgress = false;
    }

    /**
     * Start calculating a path if we aren't already
     *
     * @return true if this call started path calculation, false if it was already calculating or executing a path
     */
    @Override
    public boolean path() {
        if (goal == null) {
            return false;
        }
        if (goal.isInGoal(playerFeet())) {
            return false;
        }
        synchronized (pathPlanLock) {
            if (current != null) {
                return false;
            }
            synchronized (pathCalcLock) {
                if (isPathCalcInProgress) {
                    return false;
                }
                queuePathEvent(PathEvent.CALC_STARTED);
                findPathInNewThread(pathStart(), true, Optional.empty());
                return true;
            }
        }
    }

    /**
     * See issue #209
     *
     * @return The starting {@link BlockPos} for a new path
     */
    public BlockPos pathStart() {
        BetterBlockPos feet = playerFeet();
        if (!MovementHelper.canWalkOn(feet.down())) {
            if (player().onGround) {
                double playerX = player().posX;
                double playerZ = player().posZ;
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
                    if (MovementHelper.canWalkOn(possibleSupport.down()) && MovementHelper.canWalkThrough(possibleSupport) && MovementHelper.canWalkThrough(possibleSupport.up())) {
                        // this is plausible
                        logDebug("Faking path start assuming player is standing off the edge of a block");
                        return possibleSupport;
                    }
                }

            } else {
                // !onGround
                // we're in the middle of a jump
                if (MovementHelper.canWalkOn(feet.down().down())) {
                    logDebug("Faking path start assuming player is midair and falling");
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
    private void findPathInNewThread(final BlockPos start, final boolean talkAboutIt, final Optional<IPath> previous) {
        synchronized (pathCalcLock) {
            if (isPathCalcInProgress) {
                throw new IllegalStateException("Already doing it");
            }
            isPathCalcInProgress = true;
        }
        CalculationContext context = new CalculationContext(); // not safe to create on the other thread, it looks up a lot of stuff in minecraft
        Baritone.INSTANCE.getExecutor().execute(() -> {
            if (talkAboutIt) {
                logDebug("Starting to search for path from " + start + " to " + goal);
            }

            Optional<IPath> path = findPath(start, previous, context);
            if (Baritone.settings().cutoffAtLoadBoundary.get()) {
                path = path.map(p -> {
                    IPath result = p.cutoffAtLoadedChunks();

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
            }).map(PathExecutor::new);

            synchronized (pathPlanLock) {
                if (current == null) {
                    if (executor.isPresent()) {
                        queuePathEvent(PathEvent.CALC_FINISHED_NOW_EXECUTING);
                        current = executor.get();
                    } else {
                        queuePathEvent(PathEvent.CALC_FAILED);
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
                        throw new IllegalStateException("I have no idea what to do with this path");
                    }
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
                isPathCalcInProgress = false;
            }
        });
    }

    /**
     * Actually do the pathing
     *
     * @param start
     * @return
     */
    private Optional<IPath> findPath(BlockPos start, Optional<IPath> previous, CalculationContext context) {
        Goal goal = this.goal;
        if (goal == null) {
            logDebug("no goal");
            return Optional.empty();
        }
        if (Baritone.settings().simplifyUnloadedYCoord.get()) {
            BlockPos pos = null;
            if (goal instanceof IGoalRenderPos) {
                pos = ((IGoalRenderPos) goal).getGoalPos();
            }

            if (pos != null && world().getChunk(pos) instanceof EmptyChunk) {
                logDebug("Simplifying " + goal.getClass() + " to GoalXZ due to distance");
                goal = new GoalXZ(pos.getX(), pos.getZ());
            }
        }
        long timeout;
        if (current == null) {
            timeout = Baritone.settings().pathTimeoutMS.<Long>get();
        } else {
            timeout = Baritone.settings().planAheadTimeoutMS.<Long>get();
        }
        Optional<HashSet<Long>> favoredPositions;
        if (Baritone.settings().backtrackCostFavoringCoefficient.get() == 1D) {
            favoredPositions = Optional.empty();
        } else {
            favoredPositions = previous.map(IPath::positions).map(Collection::stream).map(x -> x.map(BetterBlockPos::longHash)).map(x -> x.collect(Collectors.toList())).map(HashSet::new); // <-- okay this is EPIC
        }
        try {
            IPathFinder pf = new AStarPathFinder(start.getX(), start.getY(), start.getZ(), goal, favoredPositions, context);
            return pf.calculate(timeout);
        } catch (Exception e) {
            logDebug("Pathing exception: " + e);
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public void revalidateGoal() {
        if (!Baritone.settings().cancelOnGoalInvalidation.get()) {
            return;
        }
        synchronized (pathPlanLock) {
            if (current == null || goal == null) {
                return;
            }
            Goal intended = current.getPath().getGoal();
            BlockPos end = current.getPath().getDest();
            if (intended.isInGoal(end) && !goal.isInGoal(end)) {
                // this path used to end in the goal
                // but the goal has changed, so there's no reason to continue...
                cancel();
            }
        }
    }

    @Override
    public void onRenderPass(RenderEvent event) {
        PathRenderer.render(event, this);
    }

    @Override
    public void onDisable() {
        Baritone.INSTANCE.getInputOverrideHandler().clearAllKeys();
    }
}
