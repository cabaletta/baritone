package adris.altoclef.util.serialization;

import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.Collection;

public class Vec3dSerializer extends AbstractVectorSerializer<Vec3d> {
    @Override
    protected Collection<String> getParts(Vec3d value) {
        return Arrays.asList("" + value.getX(), "" + value.getY(), "" + value.getZ());
    }
}
