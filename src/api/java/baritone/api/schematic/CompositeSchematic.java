package baritone.api.schematic;

import baritone.api.utils.ISchematic;
import net.minecraft.block.state.IBlockState;

import java.util.ArrayList;
import java.util.List;

public class CompositeSchematic extends AbstractSchematic {
    private final List<CompositeSchematicEntry> schematics;
    private CompositeSchematicEntry[] schematicArr;

    private void recalcArr() {
        schematicArr = schematics.toArray(new CompositeSchematicEntry[0]);

        for (CompositeSchematicEntry entry : schematicArr) {
            this.x = Math.max(x, entry.x + entry.schematic.widthX());
            this.y = Math.max(y, entry.y + entry.schematic.heightY());
            this.z = Math.max(z, entry.z + entry.schematic.lengthZ());
        }
    }

    public CompositeSchematic(int x, int y, int z) {
        super(x, y, z);
        schematics = new ArrayList<>();
        recalcArr();
    }

    public void put(ISchematic extra, int x, int y, int z) {
        schematics.add(new CompositeSchematicEntry(extra, x, y, z));
        recalcArr();
    }

    private CompositeSchematicEntry getSchematic(int x, int y, int z, IBlockState currentState) {
        for (CompositeSchematicEntry entry : schematicArr) {
            if (x >= entry.x && y >= entry.y && z >= entry.z &&
                entry.schematic.inSchematic(x - entry.x, y - entry.y, z - entry.z, currentState)) {
                return entry;
            }
        }

        return null;
    }

    @Override
    public boolean inSchematic(int x, int y, int z, IBlockState currentState) {
        CompositeSchematicEntry entry = getSchematic(x, y, z, currentState);
        return entry != null && entry.schematic.inSchematic(x - entry.x, y - entry.y, z - entry.z, currentState);
    }

    @Override
    public IBlockState desiredState(int x, int y, int z, IBlockState current) {
        CompositeSchematicEntry entry = getSchematic(x, y, z, current);

        if (entry == null) {
            throw new IllegalStateException("couldn't find schematic for this position");
        }

        return entry.schematic.desiredState(x - entry.x, y - entry.y, z - entry.z, current);
    }
}
