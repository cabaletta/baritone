package baritone.api.utils;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlockListFilter implements IBlockFilter {
    private final List<Block> blocks = new ArrayList<>();

    public BlockListFilter(List<Block> blocks) {
        this.blocks.addAll(blocks);
    }

    public BlockListFilter(Block... blocks) {
        this.blocks.addAll(Arrays.asList(blocks));
    }

    @Override
    public boolean selected(@Nonnull IBlockState blockstate) {
        return blocks.contains(blockstate.getBlock());
    }

    @Override
    public List<Block> blocks() {
        return blocks;
    }

    @Override
    public String toString() {
        return String.format(
            "BlockListFilter{%s}",
            String.join(
                ",",
                blocks.stream()
                    .map(Block.REGISTRY::getNameForObject)
                    .map(ResourceLocation::toString)
                    .toArray(String[]::new)
            )
        );
    }
}
