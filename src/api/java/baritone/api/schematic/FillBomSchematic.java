package baritone.api.schematic;

import baritone.api.utils.BlockOptionalMeta;
import net.minecraft.block.state.IBlockState;

public class FillBomSchematic extends AbstractSchematic {
    private final BlockOptionalMeta bom;

    public FillBomSchematic(int x, int y, int z, BlockOptionalMeta bom) {
        super(x, y, z);
        this.bom = bom;
    }

    public BlockOptionalMeta getBom() {
        return bom;
    }

    @Override
    public IBlockState desiredState(int x, int y, int z, IBlockState current) {
        if (bom.matches(current)) {
            return current;
        }

        return bom.getAnyBlockState();
    }
}
