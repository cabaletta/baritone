package adris.altoclef.util.serialization;

import com.fasterxml.jackson.core.JsonToken;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class BlockPosDeserializer extends AbstractVectorDeserializer<BlockPos, Integer> {
    @Override
    protected String getTypeName() {
        return "BlockPos";
    }

    @Override
    protected String[] getComponents() {
        return new String[]{"x", "y", "z"};
    }

    @Override
    protected Integer parseUnit(String unit) throws Exception {
        return Integer.parseInt(unit);
    }

    @Override
    protected BlockPos deserializeFromUnits(List<Integer> units) {
        return new BlockPos(units.get(0), units.get(1), units.get(2));
    }

    @Override
    protected boolean isUnitTokenValid(JsonToken token) {
        return token == JsonToken.VALUE_NUMBER_INT;
    }
}
