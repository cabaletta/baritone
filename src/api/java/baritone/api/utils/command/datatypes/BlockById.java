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
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;

import java.util.stream.Stream;

public class BlockById implements IDatatypeFor<Block> {
    public final Block block;

    public BlockById() {
        block = null;
    }

    public BlockById(ArgConsumer consumer) {
        ResourceLocation id = new ResourceLocation(consumer.getString());

        if ((block = Block.REGISTRY.getObject(id)) == Blocks.AIR) {
            throw new IllegalArgumentException("no block found by that id");
        }
    }

    @Override
    public Block get() {
        return block;
    }

    @Override
    public Stream<String> tabComplete(ArgConsumer consumer) {
        return new TabCompleteHelper()
            .append(
                Block.REGISTRY.getKeys()
                    .stream()
                    .map(Object::toString)
            )
            .filterPrefixNamespaced(consumer.getString())
            .sortAlphabetically()
            .stream();
    }
}
