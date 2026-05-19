package adris.altoclef.util;

import adris.altoclef.util.helpers.WorldHelper;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

public class BlockRange {
    public BlockPos start;
    public BlockPos end;
    public Dimension dimension = Dimension.OVERWORLD;

    // For deserialization
    private BlockRange() {
    }

    public BlockRange(BlockPos start, BlockPos end, Dimension dimension) {
        this.start = start;
        this.end = end;
        this.dimension = dimension;
    }

    public boolean contains(BlockPos pos) {
        return contains(pos, WorldHelper.getCurrentDimension());
    }

    public boolean contains(BlockPos pos, Dimension dimension) {
        if (this.dimension != dimension)
            return false;
        return (start.getX() <= pos.getX() && pos.getX() <= end.getX() &&
                start.getZ() <= pos.getZ() && pos.getZ() <= end.getZ() &&
                start.getY() <= pos.getY() && pos.getY() <= end.getY());
    }

    @JsonIgnore
    public BlockPos getCenter() {
        BlockPos sum = start.add(end);
        return new BlockPos(sum.getX() / 2, sum.getY() / 2, sum.getZ() / 2);
    }

    public String toString() {
        return "[" + start.toShortString() + " -> " + end.toShortString() + ", (" + dimension + ")]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockRange that = (BlockRange) o;
        return Objects.equals(start, that.start) && Objects.equals(end, that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }
}
