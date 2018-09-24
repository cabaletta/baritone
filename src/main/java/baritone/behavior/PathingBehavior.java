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
import baritone.api.pathing.goals.Goal;
import baritone.pathing.calc.AStarPathFinder;
import baritone.pathing.calc.AbstractNodeCostSearch;
import baritone.pathing.calc.IPathFinder;
import baritone.pathing.goals.GoalXZ;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.path.IPath;
import baritone.pathing.path.PathExecutor;
import baritone.utils.BlockStateInterface;
import baritone.utils.Helper;
import baritone.utils.PathRenderer;
import baritone.utils.interfaces.IGoalRenderPos;
import baritone.utils.pathing.BetterBlockPos;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.EmptyChunk;

import java.awt.*;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;

public final class PathingBehavior extends Behavior implements IPathingBehavior, Helper {

    public static final PathingBehavior INSTANCE = new PathingBehavior();

    private PathExecutor current;
    private PathExecutor next;

    private Goal goal;

    private volatile boolean isPathCalcInProgress;
    private final Object pathCalcLock = new Object();

    private final Object pathPlanLock = new Object();

    private boolean lastAutoJump;

    private PathingBehavior() {}

    private void dispatchPathEvent(PathEvent event) {
        Baritone.INSTANCE.getExecutor().execute(() -> Baritone.INSTANCE.getGameEventHandler().onPathEvent(event));
    }

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() == TickEvent.Type.OUT) {
            this.cancel();
            return;
        }
        mc.playerController.setPlayerCapabilities(mc.player);
        if (current == null) {
            return;
        }
        boolean safe = current.onTick(event);
        synchronized (pathPlanLock) {
            if (current.failed() || current.finished()) {
                current = null;
                if (goal == null || goal.isInGoal(playerFeet())) {
                    logDebug("All done. At " + goal);
                    dispatchPathEvent(PathEvent.AT_GOAL);
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
                    dispatchPathEvent(PathEvent.DISCARD_NEXT);
                    next = null;
                }
                if (next != null) {
                    logDebug("Continuing on to planned next path");
                    dispatchPathEvent(PathEvent.CONTINUING_ONTO_PLANNED_NEXT);
                    current = next;
                    next = null;
                    return;
                }
                // at this point, current just ended, but we aren't in the goal and have no plan for the future
                synchronized (pathCalcLock) {
                    if (isPathCalcInProgress) {
                        dispatchPathEvent(PathEvent.PATH_FINISHED_NEXT_STILL_CALCULATING);
                        // if we aren't calculating right now
                        return;
                    }
                    dispatchPathEvent(PathEvent.CALC_STARTED);
                    findPathInNewThread(pathStart(), true, Optional.empty());
                }
                return;
            }
            // at this point, we know current is in progress
            if (safe) {
                // a movement just ended
                if (next != null) {
                    if (next.getPath().positions().contains(playerFeet())) {
                        // jump directly onto the next path
                        logDebug("Splicing into planned next path early...");
                        dispatchPathEvent(PathEvent.SPLICING_ONTO_NEXT_EARLY);
                        current = next;
                        next = null;
                        return;
                    }
                }
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
                    dispatchPathEvent(PathEvent.NEXT_SEGMENT_CALC_STARTED);
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

    @Override
    public Goal getGoal() {
        return goal;
    }

    public PathExecutor getCurrent() {
        return current;
    }

    public PathExecutor getNext() {
        return next;
    }

    // TODO: Expose this method in the API?
    // In order to do so, we'd need to move over IPath which has a whole lot of references to other
    // things that may not need to be exposed necessarily, so we'll need to figure that out.
    public Optional<IPath> getPath() {
        return Optional.ofNullable(current).map(PathExecutor::getPath);
    }

    @Override
    public boolean isPathing() {
        return this.current != null;
    }

    @Override
    public void cancel() {
        dispatchPathEvent(PathEvent.CANCELED);
        current = null;
        next = null;
        Baritone.INSTANCE.getInputOverrideHandler().clearAllKeys();
        AbstractNodeCostSearch.getCurrentlyRunning().ifPresent(AbstractNodeCostSearch::cancel);
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
                dispatchPathEvent(PathEvent.CALC_STARTED);
                findPathInNewThread(pathStart(), true, Optional.empty());
                return true;
            }
        }
    }

    /**
     * @return The starting {@link BlockPos} for a new path
     */
    private BlockPos pathStart() {
        BetterBlockPos feet = playerFeet();
        if (BlockStateInterface.get(feet.down()).getBlock().equals(Blocks.AIR) && MovementHelper.canWalkOn(feet.down().down())) {
            return feet.down();
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
        Baritone.INSTANCE.getExecutor().execute(() -> {
            if (talkAboutIt) {
                logDebug("Starting to search for path from " + start + " to " + goal);
            }

            Optional<IPath> path = findPath(start, previous);
            if (Baritone.settings().cutoffAtLoadBoundary.get()) {
                path = path.map(IPath::cutoffAtLoadedChunks);
            }
            Optional<PathExecutor> executor = path.map(p -> p.staticCutoff(goal)).map(PathExecutor::new);
            synchronized (pathPlanLock) {
                if (current == null) {
                    if (executor.isPresent()) {
                        dispatchPathEvent(PathEvent.CALC_FINISHED_NOW_EXECUTING);
                        current = executor.get();
                    } else {
                        dispatchPathEvent(PathEvent.CALC_FAILED);
                    }
                } else {
                    if (next == null) {
                        if (executor.isPresent()) {
                            dispatchPathEvent(PathEvent.NEXT_SEGMENT_CALC_FINISHED);
                            next = executor.get();
                        } else {
                            dispatchPathEvent(PathEvent.NEXT_CALC_FAILED);
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
    private Optional<IPath> findPath(BlockPos start, Optional<IPath> previous) {
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

            // TODO simplify each individual goal in a GoalComposite
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
        Optional<HashSet<Long>> favoredPositions = previous.map(IPath::positions).map(Collection::stream).map(x -> x.map(y -> y.hashCode)).map(x -> x.collect(Collectors.toList())).map(HashSet::new); // <-- okay this is EPIC
        try {
            IPathFinder pf = new AStarPathFinder(start, goal, favoredPositions);
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

    @Override
    public void onRenderPass(RenderEvent event) {
        // System.out.println("Render passing");
        // System.out.println(event.getPartialTicks());
        float partialTicks = event.getPartialTicks();
        if (goal != null && Baritone.settings().renderGoal.value) {
            PathRenderer.drawLitDankGoalBox(player(), goal, partialTicks, Baritone.settings().colorGoalBox.get());
        }
        if (!Baritone.settings().renderPath.get()) {
            return;
        }

        //long start = System.nanoTime();


        PathExecutor current = this.current; // this should prevent most race conditions?
        PathExecutor next = this.next; // like, now it's not possible for current!=null to be true, then suddenly false because of another thread
        // TODO is this enough, or do we need to acquire a lock here?
        // TODO benchmark synchronized in render loop

        // Render the current path, if there is one
        if (current != null && current.getPath() != null) {
            int renderBegin = Math.max(current.getPosition() - 3, 0);
            PathRenderer.drawPath(current.getPath(), renderBegin, player(), partialTicks, Baritone.settings().colorCurrentPath.get(), Baritone.settings().fadePath.get(), 10, 20);
        }
        if (next != null && next.getPath() != null) {
            PathRenderer.drawPath(next.getPath(), 0, player(), partialTicks, Baritone.settings().colorNextPath.get(), Baritone.settings().fadePath.get(), 10, 20);
        }

        //long split = System.nanoTime();
        if (current != null) {
            PathRenderer.drawManySelectionBoxes(player(), current.toBreak(), partialTicks, Baritone.settings().colorBlocksToBreak.get());
            PathRenderer.drawManySelectionBoxes(player(), current.toPlace(), partialTicks, Baritone.settings().colorBlocksToPlace.get());
            PathRenderer.drawManySelectionBoxes(player(), current.toWalkInto(), partialTicks, Baritone.settings().colorBlocksToWalkInto.get());
        }

        // If there is a path calculation currently running, render the path calculation process
        AbstractNodeCostSearch.getCurrentlyRunning().ifPresent(currentlyRunning -> {
            currentlyRunning.bestPathSoFar().ifPresent(p -> {
                PathRenderer.drawPath(p, 0, player(), partialTicks, Baritone.settings().colorBestPathSoFar.get(), Baritone.settings().fadePath.get(), 10, 20);
                currentlyRunning.pathToMostRecentNodeConsidered().ifPresent(mr -> {

                    PathRenderer.drawPath(mr, 0, player(), partialTicks, Baritone.settings().colorMostRecentConsidered.get(), Baritone.settings().fadePath.get(), 10, 20);
                    PathRenderer.drawManySelectionBoxes(player(), Collections.singletonList(mr.getDest()), partialTicks, Baritone.settings().colorMostRecentConsidered.get());
                });
            });
        });
        //long end = System.nanoTime();
        //System.out.println((end - split) + " " + (split - start));
        // if (end - start > 0) {
        //   System.out.println("Frame took " + (split - start) + " " + (end - split));
        //}
    }

    @Override
    public void onDisable() {
        Baritone.INSTANCE.getInputOverrideHandler().clearAllKeys();
    }
}
