package baritone.selection;

import baritone.api.selection.ISelection;
import baritone.api.utils.BetterBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.AABB;

public class Selection implements ISelection {

    private final BetterBlockPos pos1;
    private final BetterBlockPos pos2;
    private final BetterBlockPos min;
    private final BetterBlockPos max;
    private final Vec3i size;
    private final AABB aabb;

    public Selection(BetterBlockPos pos1, BetterBlockPos pos2) {
        this.pos1 = pos1;
        this.pos2 = pos2;

        this.min = new BetterBlockPos(
                Math.min(pos1.x, pos2.x),
                Math.min(pos1.y, pos2.y),
                Math.min(pos1.z, pos2.z)
        );

        this.max = new BetterBlockPos(
                Math.max(pos1.x, pos2.x),
                Math.max(pos1.y, pos2.y),
                Math.max(pos1.z, pos2.z)
        );

        this.size = new Vec3i(
                max.x - min.x + 1,
                max.y - min.y + 1,
                max.z - min.z + 1
        );

        this.aabb = new AABB(this.min);
    }

    @Override
    public BetterBlockPos pos1() {
        return pos1;
    }

    @Override
    public BetterBlockPos pos2() {
        return pos2;
    }

    @Override
    public BetterBlockPos min() {
        return min;
    }

    @Override
    public BetterBlockPos max() {
        return max;
    }

    @Override
    public Vec3i size() {
        return size;
    }

    @Override
    public AABB aabb() {
        return aabb;
    }

    @Override
    public int hashCode() {
        return pos1.hashCode() ^ pos2.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Selection{pos1=%s,pos2=%s}", pos1, pos2);
    }

    /**
     * Since it might not be immediately obvious what this does, let me explain.
     * <p>
     * Let's say you specify Direction.UP, this functions returns if pos2 is the highest BlockPos.
     * If you specify Direction.DOWN, it returns if pos2 is the lowest BlockPos.
     *
     * @param facing The direction to check.
     * @return {@code true} if pos2 is further in that direction than pos1, {@code false} if it isn't, and something
     * else if they're both at the same position on that axis (it really doesn't matter)
     */
    private boolean isPos2(Direction facing) {
        boolean negative = facing.getAxisDirection().getStep() < 0;

        switch (facing.getAxis()) {
            case X:
                return (pos2.x > pos1.x) ^ negative;
            case Y:
                return (pos2.y > pos1.y) ^ negative;
            case Z:
                return (pos2.z > pos1.z) ^ negative;
            default:
                throw new IllegalStateException("Bad Direction.Axis");
        }
    }

    @Override
    public ISelection expand(Direction direction, int blocks) {
        if (isPos2(direction)) {
            return new Selection(pos1, pos2.relative(direction, blocks));
        } else {
            return new Selection(pos1.relative(direction, blocks), pos2);
        }
    }

    @Override
    public ISelection contract(Direction direction, int blocks) {
        if (isPos2(direction)) {
            return new Selection(pos1.relative(direction, blocks), pos2);
        } else {
            return new Selection(pos1, pos2.relative(direction, blocks));
        }
    }

    @Override
    public ISelection shift(Direction direction, int blocks) {
        return new Selection(pos1.relative(direction, blocks), pos2.relative(direction, blocks));
    }
}
