package baritone.bot.pathing.calc;

import baritone.bot.pathing.action.Action;
import baritone.bot.pathing.goals.Goal;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * A node based implementation of IPath
 *
 * @author leijurv
 */
class Path implements IPath {
    public final BlockPos start;
    public final BlockPos end;
    public final Goal goal;
    /**
     * The blocks on the path. Guaranteed that path.get(0) equals start and
     * path.get(path.size()-1) equals end
     */
    public final ArrayList<BlockPos> path;
    final ArrayList<Action> actions;

    Path(PathNode start, PathNode end, Goal goal) {
        this.start = start.pos;
        this.end = end.pos;
        this.goal = goal;
        this.path = new ArrayList<>();
        this.actions = new ArrayList<>();
        assemblePath(start, end);
    }

    private final void assemblePath(PathNode start, PathNode end) {
        PathNode current = end;
        LinkedList<BlockPos> tempPath = new LinkedList<>();//repeatedly inserting to the beginning of an arraylist is O(n^2)
        LinkedList<Action> tempActions = new LinkedList<>();//instead, do it into a linked list, then convert at the end
        while (!current.equals(start)) {
            tempPath.addFirst(current.pos);
            tempActions.addFirst(current.previousAction);
            current = current.previous;
        }
        tempPath.addFirst(start.pos);
        //can't directly convert from the PathNode pseudo linked list to an array because we don't know how long it is
        //inserting into a LinkedList<E> keeps track of length, then when we addall (which calls .toArray) it's able
        //to performantly do that conversion since it knows the length.
        path.addAll(tempPath);
        actions.addAll(tempActions);
    }

    protected void sanityCheck() {

    }

    @Override
    public List<Action> actions() {
        return actions;
    }

    @Override
    public List<BlockPos> positions() {
        return path;
    }

    @Override
    public Collection<BlockPos> getBlocksToMine() {
        return null;
    }

    @Override
    public Collection<BlockPos> getBlocksToPlace() {
        return null;
    }
}