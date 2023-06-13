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

package baritone.api.command.datatypes;

import baritone.api.command.exception.CommandException;
import baritone.api.command.helpers.TabCompleteHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.util.ResourceLocation;

import java.util.stream.Stream;

public enum EntityClassById implements IDatatypeFor<Class<? extends Entity>> {
    INSTANCE;

    @Override
    public Class<? extends Entity> get(IDatatypeContext ctx) throws CommandException {
        ResourceLocation id = new ResourceLocation(ctx.getConsumer().getString());
        Class<? extends Entity> entity;
        try {
            entity = EntityList.REGISTRY.getObject(id);
        } catch (NoSuchFieldError e) {
            // Forge removes EntityList.REGISTRY field and provides the getClass method as a replacement
            // See https://github.com/MinecraftForge/MinecraftForge/blob/1.12.x/patches/minecraft/net/minecraft/entity/EntityList.java.patch
            try {
                entity = (Class<? extends Entity>) EntityList.class.getMethod("getClass", ResourceLocation.class).invoke(null, id);
            } catch (Exception ex) {
                throw new RuntimeException("EntityList.REGISTRY does not exist and failed to call the Forge-replacement method", ex);
            }
        }

        if (entity == null) {
            throw new IllegalArgumentException("no entity found by that id");
        }
        return entity;
    }

    @Override
    public Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException {
        return new TabCompleteHelper()
                .append(EntityList.getEntityNameList().stream().map(Object::toString))
                .filterPrefixNamespaced(ctx.getConsumer().getString())
                .sortAlphabetically()
                .stream();
    }
}
