package baritone.api.schematic;

import baritone.api.IBaritone;
import baritone.api.utils.BlockOptionalMeta;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

public class FillBomSchematic extends AbstractSchematic {
    private final BlockOptionalMeta bom;

    public FillBomSchematic(IBaritone baritone, int x, int y, int z, BlockOptionalMeta bom) {
        super(baritone, x, y, z);
        this.bom = bom;
    }

    public BlockOptionalMeta getBom() {
        return bom;
    }

    @Override
    public IBlockState desiredState(int x, int y, int z, IBlockState current) {
        if (bom.matches(current)) {
            return current;
        } else if (current.getBlock() != Blocks.AIR) {
            return Blocks.AIR.getDefaultState();
        }

        for (IBlockState placeable : approxPlaceable()) {
            if (bom.matches(placeable)) {
                return placeable;
            }
        }

        return bom.getAnyBlockState();
    }
}
