package baritone.api.schematic;

import baritone.api.IBaritone;
import baritone.api.utils.BlockOptionalMetaLookup;
import baritone.api.utils.ISchematic;
import net.minecraft.block.state.IBlockState;

public class ReplaceSchematic extends MaskSchematic {
    private final BlockOptionalMetaLookup filter;
    private final boolean[][][] cache;

    public ReplaceSchematic(IBaritone baritone, ISchematic schematic, BlockOptionalMetaLookup filter) {
        super(baritone, schematic);
        this.filter = filter;
        this.cache = new boolean[lengthZ()][heightY()][widthX()];
    }

    protected boolean partOfMask(int x, int y, int z, IBlockState currentState) {
        return cache[x][y][z] || (cache[x][y][z] = filter.has(currentState));
    }
}
