package adris.altoclef.util.serialization;

import adris.altoclef.util.helpers.ItemHelper;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import net.minecraft.item.Item;

import java.io.IOException;
import java.util.List;

public class ItemSerializer extends StdSerializer<Object> {
    public ItemSerializer() {
        this(null);
    }

    public ItemSerializer(Class<Object> vc) {
        super(vc);
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        List<Item> items = (List<Item>) value;
        gen.writeStartArray();
        for (Item item : items) {
            String key = ItemHelper.trimItemName(item.getTranslationKey());
            gen.writeString(key);
        }
        gen.writeEndArray();
    }
}
