/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

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
