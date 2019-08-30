package baritone.api.utils;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class CompositeBlockFilter implements IBlockFilter {
    List<IBlockFilter> filters = new ArrayList<>();

    public CompositeBlockFilter() {
    }

    public CompositeBlockFilter(List<? extends IBlockFilter> filters) {
        this.filters.addAll(filters);
    }

    public CompositeBlockFilter(IBlockFilter... filters) {
        this.filters.addAll(asList(filters));
    }

    @Override
    public boolean selected(@Nonnull IBlockState blockstate) {
        return filters.stream()
            .map(f -> f.selected(blockstate))
            .filter(Boolean::valueOf).findFirst()
            .orElse(false);
    }

    @Override
    public List<Block> blocks() {
        return filters.stream()
            .map(IBlockFilter::blocks)
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public String toString() {
        return String.format(
            "CompositeBlockFilter{%s}",
            String.join(",", filters.stream().map(Object::toString).toArray(String[]::new))
        );
    }
}
