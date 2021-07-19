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
import java.util.stream.Stream;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public enum BlockById implements IDatatypeFor<Block> {
    INSTANCE;

    @Override
    public Block get(IDatatypeContext ctx) throws CommandException {
        ResourceLocation id = new ResourceLocation(ctx.getConsumer().getString());
        Block block;
        if ((block = Registry.BLOCK.getOptional(id).orElse(null)) == null) {
            throw new IllegalArgumentException("no block found by that id");
        }
        return block;
    }

    @Override
    public Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException {
        return new TabCompleteHelper()
                .append(
                        Registry.BLOCK.keySet()
                                .stream()
                                .map(Object::toString)
                )
                .filterPrefixNamespaced(ctx.getConsumer().getString())
                .sortAlphabetically()
                .stream();
    }
}
