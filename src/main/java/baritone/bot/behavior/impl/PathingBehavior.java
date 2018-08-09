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

    private Goal goal;

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() == TickEvent.Type.OUT || current == null) {
            return;
        }
        current.onTick(event);
        if (current.failed() || current.finished()) {
            current = null;
            if (!goal.isInGoal(playerFeet()))
                findPathInNewThread(playerFeet(), true);
        }
    }

    @Override
    public void onSendChatMessage(ChatEvent event) {
        String msg = event.getMessage();
        if (msg.equals("goal")) {
            goal = new GoalBlock(playerFeet());
            displayChatMessageRaw("Goal: " + goal);
            event.cancel();
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
        new Thread(() -> {
            if (talkAboutIt) {
                displayChatMessageRaw("Starting to search for path from " + start + " to " + goal);
            }

            findPath(start).map(PathExecutor::new).ifPresent(path -> current = path);
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
                displayChatMessageRaw("Finished finding a path from " + start + " to " + goal + ". " + current.getPath().getNumNodesConsidered() + " nodes considered");
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
