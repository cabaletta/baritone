package baritone.api.utils.command.datatypes;

import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import baritone.api.utils.command.helpers.tabcomplete.TabCompleteHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.util.ResourceLocation;

import java.util.stream.Stream;

import static java.util.Objects.isNull;

public class EntityClassById implements IDatatypeFor<Class<? extends Entity>> {
    public final Class<? extends Entity> entity;

    public EntityClassById() {
        entity = null;
    }

    public EntityClassById(ArgConsumer consumer) {
        ResourceLocation id = new ResourceLocation(consumer.getString());

        if (isNull(entity = EntityList.REGISTRY.getObject(id))) {
            throw new RuntimeException("no entity found by that id");
        }
    }

    @Override
    public Class<? extends Entity> get() {
        return entity;
    }

    @Override
    public Stream<String> tabComplete(ArgConsumer consumer) {
        return new TabCompleteHelper()
            .append(
                EntityList.getEntityNameList()
                    .stream()
                    .map(Object::toString)
            )
            .filterPrefixNamespaced(consumer.getString())
            .sortAlphabetically()
            .stream();
    }
}
