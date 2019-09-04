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

import baritone.api.utils.BlockOptionalMeta;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;

import java.util.stream.Stream;

public class ForBlockOptionalMeta implements IDatatypeFor<BlockOptionalMeta> {
    public final BlockOptionalMeta selector;

    public ForBlockOptionalMeta() {
        selector = null;
    }

    public ForBlockOptionalMeta(ArgConsumer consumer) {
        selector = new BlockOptionalMeta(consumer.getString());
    }

    @Override
    public BlockOptionalMeta get() {
        return selector;
    }

    @Override
    public Stream<String> tabComplete(ArgConsumer consumer) {
        return consumer.tabCompleteDatatype(BlockById.class);
    }
}
