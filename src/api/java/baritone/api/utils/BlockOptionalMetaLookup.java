package baritone.api.utils;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import org.apache.commons.lang3.ArrayUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BlockOptionalMetaLookup {
    private final Map<Block, int[]> lookup = new HashMap<>();

    public BlockOptionalMetaLookup() {
    }

    public BlockOptionalMetaLookup(BlockOptionalMeta... boms) {
        put(boms);
    }

    public BlockOptionalMetaLookup(Block... blocks) {
        put(blocks);
    }

    public BlockOptionalMetaLookup(List<Block> blocks) {
        put(blocks);
    }

    public void put(BlockOptionalMeta bom) {
        final int[] metaArr = new int[] {bom.getMeta()};
        lookup.compute(bom.getBlock(), (__, arr) -> arr == null ? metaArr : ArrayUtils.addAll(arr, metaArr));
    }

    public void put(BlockOptionalMeta... boms) {
        for (BlockOptionalMeta bom : boms) {
            put(bom);
        }
    }

    public void put(Block... blocks) {
        for (Block block : blocks) {
            put(new BlockOptionalMeta(block));
        }
    }

    public void put(List<Block> blocks) {
        for (Block block : blocks) {
            put(new BlockOptionalMeta(block));
        }
    }

    public boolean has(Block block) {
        return lookup.containsKey(block);
    }

    public boolean has(IBlockState state) {
        Block block = state.getBlock();
        int[] arr = lookup.get(block);

        if (arr == null) {
            return false;
        }

        int meta = block.damageDropped(state);
        for (int value : arr) {
            if (value == meta) {
                return true;
            }
        }

        return false;
    }

    public Set<Block> blocks() {
        return lookup.keySet();
    }
}
