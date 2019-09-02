package baritone.api.schematic;

import baritone.api.utils.ISchematic;
import net.minecraft.block.state.IBlockState;

public abstract class MaskSchematic extends AbstractSchematic {
    private final ISchematic schematic;

    public MaskSchematic(ISchematic schematic) {
        super(schematic.widthX(), schematic.heightY(), schematic.lengthZ());
        this.schematic = schematic;
    }

    protected abstract boolean partOfMask(int x, int y, int z, IBlockState currentState);

    @Override
    public boolean inSchematic(int x, int y, int z, IBlockState currentState) {
        return schematic.inSchematic(x, y, z, currentState) && partOfMask(x, y, z, currentState);
    }

    @Override
    public IBlockState desiredState(int x, int y, int z, IBlockState current) {
        return schematic.desiredState(x, y, z, current);
    }
}
