package adris.altoclef.util.serialization;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.*;

public abstract class AbstractVectorDeserializer<T, UnitType> extends StdDeserializer<T> {
    public AbstractVectorDeserializer() {
        this(null);
    }

    public AbstractVectorDeserializer(Class<T> vc) {
        super(vc);
    }

    protected abstract String getTypeName();

    protected abstract String[] getComponents();

    protected abstract UnitType parseUnit(String unit) throws Exception;

    protected abstract T deserializeFromUnits(List<UnitType> units);

    protected abstract boolean isUnitTokenValid(JsonToken unitToken);


    UnitType trySet(JsonParser p, Map<String, UnitType> map, String key) throws JsonParseException {
        if (map.containsKey(key)) {
            return map.get(key);
        }
        throw new JsonParseException(p, getTypeName() + " should have key for " + key + " key, but one was not found.");
    }

    UnitType tryParse(JsonParser p, String whole, String part) throws JsonParseException {
        try {
            return parseUnit(part.trim());
        } catch (Exception e) {
            throw new JsonParseException(p, "Failed to parse " + getTypeName() + " string \""
                    + whole + "\", specificaly part \"" + part + "\".");
        }
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String[] neededComponents = getComponents();
        if (p.getCurrentToken() == JsonToken.VALUE_STRING) {
            String bposString = p.getValueAsString();
            String[] parts = bposString.split(",");
            if (parts.length != neededComponents.length) {
                throw new JsonParseException(p, "Invalid " + getTypeName() + " string: \"" + bposString + "\", must be in form \"" + String.join(",", neededComponents) + "\".");
            }
            ArrayList<UnitType> resultingUnits = new ArrayList<UnitType>();
            for (String part : parts) {
                resultingUnits.add(tryParse(p, bposString, part));
            }
            return deserializeFromUnits(resultingUnits);
        } else if (p.getCurrentToken() == JsonToken.START_OBJECT) {
            Map<String, UnitType> parts = new HashMap<>();
            p.nextToken();
            while (p.getCurrentToken() != JsonToken.END_OBJECT) {
                if (p.getCurrentToken() == JsonToken.FIELD_NAME) {
                    String fName = p.getCurrentName();
                    p.nextToken();
                    if (!isUnitTokenValid(p.currentToken())) {
                        throw new JsonParseException(p, "Invalid token for " + getTypeName() + ". Got: " + p.getCurrentToken());
                    }
                    try {
                        parts.put(p.getCurrentName(), parseUnit(p.getValueAsString()));
                    } catch (Exception e) {
                        throw new JsonParseException(p, "Failed to parse unit " + p.getCurrentName());
                    }
                    p.nextToken();
                } else {
                    throw new JsonParseException(p, "Invalid structure, expected field name (like " + String.join(",", neededComponents) + ")");
                }
            }
            if (parts.size() != neededComponents.length) {
                throw new JsonParseException(p, "Expected [" + String.join(",", neededComponents) + "] keys to be part of a blockpos object. Got " + Arrays.toString(parts.keySet().toArray(String[]::new)));
            }
            ArrayList<UnitType> resultingUnits = new ArrayList<UnitType>();
            for (String componentName : neededComponents) {
                resultingUnits.add(trySet(p, parts, componentName));
            }
            return deserializeFromUnits(resultingUnits);
        }
        throw new JsonParseException(p, "Invalid token: " + p.getCurrentToken());
    }
}
