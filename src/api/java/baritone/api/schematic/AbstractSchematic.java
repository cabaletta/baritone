package baritone.api.schematic;

import baritone.api.utils.ISchematic;

public abstract class AbstractSchematic implements ISchematic {
    protected int x;
    protected int y;
    protected int z;

    public AbstractSchematic(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public int widthX() {
        return x;
    }

    @Override
    public int heightY() {
        return y;
    }

    @Override
    public int lengthZ() {
        return z;
    }
}
