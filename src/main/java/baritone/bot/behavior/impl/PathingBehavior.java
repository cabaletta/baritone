/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.bot.behavior.impl;

import baritone.bot.Baritone;
import baritone.bot.behavior.Behavior;
import baritone.bot.event.events.ChatEvent;
import baritone.bot.event.events.RenderEvent;
import baritone.bot.event.events.TickEvent;
import baritone.bot.pathing.calc.AStarPathFinder;
import baritone.bot.pathing.calc.AbstractNodeCostSearch;
import baritone.bot.pathing.calc.IPathFinder;
import baritone.bot.pathing.goals.Goal;
import baritone.bot.pathing.goals.GoalBlock;
import baritone.bot.pathing.goals.GoalXZ;
import baritone.bot.pathing.goals.GoalYLevel;
import baritone.bot.pathing.path.IPath;
import baritone.bot.pathing.path.PathExecutor;
import baritone.bot.utils.PathRenderer;
import net.minecraft.util.math.BlockPos;

import java.awt.*;
import java.util.Arrays;
import java.util.Optional;

public class PathingBehavior extends Behavior {

    public static final PathingBehavior INSTANCE = new PathingBehavior();

    private PathingBehavior() {}

    private PathExecutor current;
    private PathExecutor next;

    private Goal goal;

    private volatile boolean isPathCalcInProgress;
    private final Object pathCalcLock = new Object();

    private final Object pathPlanLock = new Object();

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() == TickEvent.Type.OUT || current == null) {
            return;
        }
        boolean safe = current.onTick(event);
        synchronized (pathPlanLock) {
            if (current.failed() || current.finished()) {
                current = null;
                if (next != null && !next.getPath().positions().contains(playerFeet())) {
                    // if the current path failed, we may not actually be on the next one, so make sure
                    displayChatMessageRaw("Discarding next path as it does not contain current position");
                    // for example if we had a nicely planned ahead path that starts where current ends
                    // that's all fine and good
                    // but if we fail in the middle of current
                    // we're nowhere close to our planned ahead path
                    // so need to discard it sadly.
                    next = null;
                }
                if (next != null) {
                    current = next;
                    next = null;
                    return;
                }
                if (goal.isInGoal(playerFeet())) {
                    return;
                }
                // at this point, current just ended, but we aren't in the goal and have no plan for the future
                synchronized (pathCalcLock) {
                    if (isPathCalcInProgress) {
                        // if we aren't calculating right now
                        return;
                    }
                    findPathInNewThread(playerFeet(), true);
                }
                return;
            }
            // at this point, we know current is in progress
            if (safe) {
                // a movement just ended
                if (next != null) {
                    if (next.getPath().positions().contains(playerFeet())) {
                        // jump directly onto the next path
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
                if (goal.isInGoal(current.getPath().getDest())) {
                    // and this path dosen't get us all the way there
                    return;
                }
                if (current.getPath().ticksRemainingFrom(current.getPosition()) < 200) {
                    // and this path has 5 seconds or less left
                    displayChatMessageRaw("Path almost over; planning ahead");
                    findPathInNewThread(current.getPath().getDest(), false);
                }
            }
        }
    }

    @Override
    public void onSendChatMessage(ChatEvent event) {
        String msg = event.getMessage();
        if (msg.toLowerCase().startsWith("goal")) {
            event.cancel();
            String[] params = msg.toLowerCase().substring(4).trim().split(" ");
            if (params[0].equals("")) {
                params = new String[]{};
            }
            try {
                switch (params.length) {
                    case 0:
                        goal = new GoalBlock(playerFeet());
                        break;
                    case 1:
                        goal = new GoalYLevel(Integer.parseInt(params[0]));
                        break;
                    case 2:
                        goal = new GoalXZ(Integer.parseInt(params[0]), Integer.parseInt(params[1]));
                        break;
                    case 3:
                        goal = new GoalBlock(new BlockPos(Integer.parseInt(params[0]), Integer.parseInt(params[1]), Integer.parseInt(params[2])));
                        break;
                    default:
                        displayChatMessageRaw("unable to understand lol");
                        return;
                }
            } catch (NumberFormatException ex) {
                displayChatMessageRaw("unable to parse integer " + ex);
                return;
            }
            displayChatMessageRaw("Goal: " + goal);
            return;
        }
        if (msg.equals("path")) {
            findPathInNewThread(playerFeet(), true);
            event.cancel();
            return;
        }
        if (msg.toLowerCase().equals("slowpath")) {
            AStarPathFinder.slowPath ^= true;
            event.cancel();
            return;
        }
        if (msg.toLowerCase().equals("cancel")) {
            current = null;
            Baritone.INSTANCE.getInputOverrideHandler().clearAllKeys();
            event.cancel();
            displayChatMessageRaw("ok canceled");
            return;
        }
        if (msg.toLowerCase().startsWith("thisway")) {
            goal = GoalXZ.fromDirection(playerFeetAsVec(), player().rotationYaw, Double.parseDouble(msg.substring(7).trim()));
            displayChatMessageRaw("Goal: " + goal);
            event.cancel();
            return;
        }
    }

    public PathExecutor getExecutor() {
        return current;
    }

    public Optional<IPath> getPath() {
        return Optional.ofNullable(current).map(PathExecutor::getPath);
    }

    /**
     * In a new thread, pathfind to target blockpos
     *
     * @param start
     * @param talkAboutIt
     */
    public void findPathInNewThread(final BlockPos start, final boolean talkAboutIt) {
        synchronized (pathCalcLock) {
            if (isPathCalcInProgress) {
                throw new IllegalStateException("Already doing it");
            }
            isPathCalcInProgress = true;
        }
        new Thread(() -> {
            if (talkAboutIt) {
                displayChatMessageRaw("Starting to search for path from " + start + " to " + goal);
            }

            findPath(start).map(PathExecutor::new).ifPresent(path -> {
                synchronized (pathPlanLock) {
                    if (current == null) {
                        current = path;
                    } else {
                        if (next == null) {
                            next = path;
                        } else {
                            throw new IllegalStateException("I have no idea what to do with this path");
                        }
                    }
                }
            });
            /*
            isThereAnythingInProgress = false;
            if (!currentPath.goal.isInGoal(currentPath.end)) {
                if (talkAboutIt) {
                    Out.gui("I couldn't get all the way to " + goal + ", but I'm going to get as close as I can. " + currentPath.numNodes + " nodes considered", Out.Mode.Standard);
                }
                planAhead();
            } else if (talkAboutIt) {
                Out.gui(, Out.Mode.Debug);
            }
            */
            if (talkAboutIt && current != null && current.getPath() != null) {
                displayChatMessageRaw("Finished finding a path from " + start + " towards " + goal + ". " + current.getPath().getNumNodesConsidered() + " nodes considered");
            }
            synchronized (pathCalcLock) {
                isPathCalcInProgress = false;
            }
        }).start();
    }

    /**
     * Actually do the pathing
     *
     * @param start
     * @return
     */
    private Optional<IPath> findPath(BlockPos start) {
        if (goal == null) {
            displayChatMessageRaw("no goal");
            return Optional.empty();
        }
        try {
            IPathFinder pf = new AStarPathFinder(start, goal);
            return pf.calculate();
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public void onRenderPass(RenderEvent event) {
        // System.out.println("Render passing");
        // System.out.println(event.getPartialTicks());
        float partialTicks = event.getPartialTicks();
        long start = System.nanoTime();

        // Render the current path, if there is one
        if (current != null && current.getPath() != null) {
            int renderBegin = Math.max(current.getPosition() - 3, 0);
            PathRenderer.drawPath(current.getPath(), renderBegin, player(), partialTicks, Color.RED);
        }

        long split = System.nanoTime();
        if (current != null) {
            PathRenderer.drawManySelectionBoxes(player(), current.toBreak(), partialTicks, Color.RED);
            PathRenderer.drawManySelectionBoxes(player(), current.toPlace(), partialTicks, Color.GREEN);
            PathRenderer.drawManySelectionBoxes(player(), current.toWalkInto(), partialTicks, Color.MAGENTA);
        }

        // If there is a path calculation currently running, render the path calculation process
        AbstractNodeCostSearch.getCurrentlyRunning().ifPresent(currentlyRunning -> {
            currentlyRunning.bestPathSoFar().ifPresent(p -> {
                PathRenderer.drawPath(p, 0, player(), partialTicks, Color.BLUE);
                currentlyRunning.pathToMostRecentNodeConsidered().ifPresent(mr -> {

                    PathRenderer.drawPath(mr, 0, player(), partialTicks, Color.CYAN);
                    PathRenderer.drawManySelectionBoxes(player(), Arrays.asList(mr.getDest()), partialTicks, Color.CYAN);
                });
            });
        });
        long end = System.nanoTime();
        //System.out.println((end - split) + " " + (split - start));
        // if (end - start > 0)
        //   System.out.println("Frame took " + (split - start) + " " + (end - split));

    }
}
