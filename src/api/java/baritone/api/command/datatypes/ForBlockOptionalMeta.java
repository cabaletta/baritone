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
import baritone.api.utils.BlockOptionalMeta;
import net.minecraft.block.Block;
import net.minecraft.state.Property;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ForBlockOptionalMeta implements IDatatypeFor<BlockOptionalMeta> {
    INSTANCE;

    /**
     * Matches (domain:)?name([(property=value)*])? but the input can be truncated at any position.
     * domain and name are [a-z0-9_.-]+ and [a-z0-9/_.-]+ because that's what mc 1.13+ accepts.
     * property and value use the same format as domain.
     */
    // Good luck reading this.
    private static Pattern PATTERN = Pattern.compile("(?:[a-z0-9_.-]+:)?(?:[a-z0-9/_.-]+(?:\\[(?:(?:[a-z0-9_.-]+=[a-z0-9_.-]+,)*(?:[a-z0-9_.-]+(?:=(?:[a-z0-9_.-]+(?:\\])?)?)?)?|\\])?)?)?");

    @Override
    public BlockOptionalMeta get(IDatatypeContext ctx) throws CommandException {
        return new BlockOptionalMeta(ctx.getConsumer().getString());
    }

    @Override
    public Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException {
        String arg = ctx.getConsumer().peekString();

        if (!PATTERN.matcher(arg).matches()) {
            // Invalid format; we can't complete this.
            ctx.getConsumer().getString();
            return Stream.empty();
        }

        if (arg.endsWith("]")) {
            // We are already done.
            ctx.getConsumer().getString();
            return Stream.empty();
        }

        if (!arg.contains("[")) {
            // no properties so we are completing the block id
            return ctx.getConsumer().tabCompleteDatatype(BlockById.INSTANCE);
        }

        ctx.getConsumer().getString();

        // destructuring assignment? Please?
        String blockId, properties;
        {
            String[] parts = splitLast(arg, '[');
            blockId = parts[0];
            properties = parts[1];
        }

        Block block = Registry.BLOCK.getOptional(new ResourceLocation(blockId)).orElse(null);
        if (block == null) {
            // This block doesn't exist so there's no properties to complete.
            return Stream.empty();
        }

        String leadingProperties, lastProperty;
        {
            String[] parts = splitLast(properties, ',');
            leadingProperties = parts[0];
            lastProperty = parts[1];
        }

        if (!lastProperty.contains("=")) {
            // The last property-value pair doesn't have a value yet so we are completing its name
            Set<String> usedProps = Stream.of(leadingProperties.split(","))
                    .map(pair -> pair.split("=")[0])
                    .collect(Collectors.toSet());

            String prefix = arg.substring(0, arg.length() - lastProperty.length());
            return new TabCompleteHelper()
                    .append(
                            block.getStateContainer()
                                    .getProperties()
                                    .stream()
                                    .map(Property::getName)
                    )
                    .filter(prop -> !usedProps.contains(prop))
                    .filterPrefix(lastProperty)
                    .sortAlphabetically()
                    .map(prop -> prefix + prop)
                    .stream();
        }

        String lastName, lastValue;
        {
            String[] parts = splitLast(lastProperty, '=');
            lastName = parts[0];
            lastValue = parts[1];
        }

        // We are completing the value of a property
        String prefix = arg.substring(0, arg.length() - lastValue.length());

        Property<?> property = block.getStateContainer().getProperty(lastName);
        if (property == null) {
            // The property does not exist so there's no values to complete
            return Stream.empty();
        }

        return new TabCompleteHelper()
                .append(getValues(property))
                .filterPrefix(lastValue)
                .sortAlphabetically()
                .map(val -> prefix + val)
                .stream();
    }

    /**
     * Always returns exactly two strings.
     * If the separator is not found the FIRST returned string is empty.
     */
    private static String[] splitLast(String string, char chr) {
        int idx = string.lastIndexOf(chr);
        if (idx == -1) {
            return new String[]{"", string};
        }
        return new String[]{string.substring(0, idx), string.substring(idx + 1)};
    }

    // this shouldn't need to be a separate method?
    private static <T extends Comparable<T>> Stream<String> getValues(Property<T> property) {
        return property.getAllowedValues().stream().map(property::getName);
    }
}
