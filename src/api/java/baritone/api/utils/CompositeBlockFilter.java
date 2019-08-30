package baritone.api.utils;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CompositeBlockFilter implements IBlockFilter {
    private IBlockFilter[] filters;

    public CompositeBlockFilter(List<? extends IBlockFilter> filters) {
        this.filters = filters.toArray(new IBlockFilter[0]);
    }

    public CompositeBlockFilter(IBlockFilter... filters) {
        this.filters = filters;
    }

    @Override
    public boolean selected(@Nonnull IBlockState blockstate) {
        for (IBlockFilter filter : filters) {
            if (filter.selected(blockstate)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<Block> blocks() {
        return Arrays.stream(filters)
            .map(IBlockFilter::blocks)
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public String toString() {
        return String.format(
            "CompositeBlockFilter{%s}",
            String.join(",", Arrays.stream(filters).map(Object::toString).toArray(String[]::new))
        );
    }
}
