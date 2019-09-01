package baritone.api.utils;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;

public class BlockOptionalMetaLookup {
    private final BlockOptionalMeta[] boms;

    public BlockOptionalMetaLookup(BlockOptionalMeta... boms) {
        this.boms = boms;
    }

    public BlockOptionalMetaLookup(Block... blocks) {
        this.boms = Arrays.stream(blocks)
            .map(BlockOptionalMeta::new)
            .toArray(BlockOptionalMeta[]::new);
    }

    public BlockOptionalMetaLookup(List<Block> blocks) {
        this.boms = blocks.stream()
            .map(BlockOptionalMeta::new)
            .toArray(BlockOptionalMeta[]::new);
    }

    public boolean has(Block block) {
        for (BlockOptionalMeta bom : boms) {
            if (bom.getBlock() == block) {
                return true;
            }
        }

        return false;
    }

    public boolean has(IBlockState state) {
        for (BlockOptionalMeta bom : boms) {
            if (bom.matches(state)) {
                return true;
            }
        }

        return false;
    }

    public boolean has(ItemStack stack) {
        for (BlockOptionalMeta bom : boms) {
            if (bom.matches(stack)) {
                return true;
            }
        }

        return false;
    }

    public List<BlockOptionalMeta> blocks() {
        return asList(boms);
    }

    @Override
    public String toString() {
        return String.format(
            "BlockOptionalMetaLookup{%s}",
            Arrays.toString(boms)
        );
    }
}
