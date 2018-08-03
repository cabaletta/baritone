package baritone.bot.pathing.calc;

import baritone.bot.pathing.goals.Goal;
import baritone.bot.pathing.movement.Movement;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.stream.Collectors;

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
    public final List<BlockPos> path;

    final List<Movement> movements;

    Path(PathNode start, PathNode end, Goal goal) {
        this.start = start.pos;
        this.end = end.pos;
        this.goal = goal;
        this.path = new ArrayList<>();
        this.movements = new ArrayList<>();
        assemblePath(start, end);
        sanityCheck();
    }

    private void assemblePath(PathNode start, PathNode end) {
        if (!path.isEmpty() || !movements.isEmpty()) {
            throw new IllegalStateException();
        }
        PathNode current = end;
        LinkedList<BlockPos> tempPath = new LinkedList<>(); // Repeatedly inserting to the beginning of an arraylist is O(n^2)
        LinkedList<Movement> tempMovements = new LinkedList<>(); // Instead, do it into a linked list, then convert at the end
        while (!current.equals(start)) {
            tempPath.addFirst(current.pos);
            tempMovements.addFirst(current.previousMovement);
            current = current.previous;
        }
        tempPath.addFirst(start.pos);
        // Can't directly convert from the PathNode pseudo linked list to an array because we don't know how long it is
        // inserting into a LinkedList<E> keeps track of length, then when we addall (which calls .toArray) it's able
        // to performantly do that conversion since it knows the length.
        path.addAll(tempPath);
        movements.addAll(tempMovements);
    }

    /**
     * Performs a series of checks to ensure that the assembly of the path went as expected.
     */
    private void sanityCheck() {
        if (!start.equals(path.get(0))) {
            throw new IllegalStateException("Start node does not equal first path element");
        }
        if (!end.equals(path.get(path.size() - 1))) {
            throw new IllegalStateException("End node does not equal last path element");
        }
        if (path.size() != movements.size() + 1) {
            throw new IllegalStateException("Size of path array is unexpected");
        }
        for (int i = 0; i < path.size(); i++) {
            BlockPos src = path.get(i);
            BlockPos dest = path.get(i + 1);
            Movement movement = movements.get(i);
            if (!src.equals(movement.getSrc())) {
                throw new IllegalStateException("Path source is not equal to the movement source");
            }
            if (!dest.equals(movement.getDest())) {
                throw new IllegalStateException("Path destination is not equal to the movement destination");
            }
        }
    }

    @Override
    public List<Movement> movements() {
        return Collections.unmodifiableList(movements);
    }

    @Override
    public List<BlockPos> positions() {
        return Collections.unmodifiableList(path);
    }

    @Override
    public Collection<BlockPos> getBlocksToMine() {
        return movements.stream().map(move -> move.positionsToBreak).flatMap(Arrays::stream).collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public Collection<BlockPos> getBlocksToPlace() {
        return movements.stream().map(move -> move.positionsToPlace).flatMap(Arrays::stream).collect(Collectors.toCollection(HashSet::new));
    }
}
