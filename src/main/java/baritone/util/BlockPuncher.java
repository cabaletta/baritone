/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.util;

import java.util.ArrayList;
import java.util.Arrays;
import baritone.Baritone;
import baritone.pathfinding.goals.Goal;
import baritone.pathfinding.goals.GoalComposite;
import baritone.pathfinding.goals.GoalTwoBlocks;
import baritone.util.Out;
import net.minecraft.util.math.BlockPos;

/**
 * yeah so just like go and punch this type of block
 *
 * @author avecowa
 * @author leijurv
 *
 * =P
 */
public class BlockPuncher {
    public static boolean setGoalTo(String... block) {
        ArrayList<BlockPos> closest = Memory.closest(10, block);
        if (closest == null || closest.isEmpty()) {
            Out.gui("NO " + Arrays.asList(block) + " NEARBY. OH MAN", Out.Mode.Standard);
            return false;
        }
        Goal[] goals = new Goal[closest.size()];
        for (int i = 0; i < goals.length; i++) {
            goals[i] = new GoalTwoBlocks(closest.get(i));
        }
        Baritone.goal = new GoalComposite(goals);
        return true;
    }
    public static boolean tick(String... block) {
        if (!setGoalTo(block)) {
            return false;
        }
        if (Baritone.currentPath == null && !Baritone.isThereAnythingInProgress) {
            Baritone.findPathInNewThread(false);
        }
        return true;
    }
}
