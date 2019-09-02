package baritone.api.schematic;

import baritone.api.utils.ISchematic;

public class CompositeSchematicEntry {
    public final ISchematic schematic;
    public final int x;
    public final int y;
    public final int z;

    public CompositeSchematicEntry(ISchematic schematic, int x, int y, int z) {
        this.schematic = schematic;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
