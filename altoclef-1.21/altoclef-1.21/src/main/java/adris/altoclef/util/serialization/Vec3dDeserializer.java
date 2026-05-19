package adris.altoclef.util.serialization;

import com.fasterxml.jackson.core.JsonToken;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class Vec3dDeserializer extends AbstractVectorDeserializer<Vec3d, Double> {
    @Override
    protected String getTypeName() {
        return "Vec3d";
    }

    @Override
    protected String[] getComponents() {
        return new String[]{"x", "y"};
    }

    @Override
    protected Double parseUnit(String unit) throws Exception {
        return Double.parseDouble(unit);
    }

    @Override
    protected Vec3d deserializeFromUnits(List<Double> units) {
        return new Vec3d(units.get(0), units.get(1), units.get(2));
    }

    @Override
    protected boolean isUnitTokenValid(JsonToken token) {
        return token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT;
    }

}
