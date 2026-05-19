package adris.altoclef.util.serialization;

import net.minecraft.util.math.ChunkPos;

import java.util.Arrays;
import java.util.Collection;

public class ChunkPosSerializer extends AbstractVectorSerializer<ChunkPos> {
    @Override
    protected Collection<String> getParts(ChunkPos value) {
        return Arrays.asList("" + value.x, "" + value.z);
    }
}
