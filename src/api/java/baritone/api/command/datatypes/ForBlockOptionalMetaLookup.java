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
import baritone.api.utils.BlockOptionalMetaLookup;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ForBlockOptionalMetaLookup implements IDatatypeFor<BlockOptionalMetaLookup> {
    INSTANCE;

    /**
     * Matches #?(domain:)?name([(property=value)*])? but the input can be truncated at any position.
     * domain and name are [a-z0-9_.-]+ and [a-z0-9/_.-]+ because that's what mc 1.13+ accepts.
     * property and value use the same format as domain.
     */
    // Good luck reading this.
    private static final Pattern PREFIX_PATTERN = Pattern.compile("#?(?:[a-z0-9_.-]+:)?(?:[a-z0-9/_.-]+(?:\\[(?:(?:[a-z0-9_.-]+=[a-z0-9_.-]+,)*(?:[a-z0-9_.-]+(?:=(?:[a-z0-9_.-]+(?:\\])?)?)?)?|\\])?)?)?");

    // id(\[properties?\])? where id and properties are any text with at least one character
    private static final Pattern FULL_PATTERN = Pattern.compile("^(?<id>.+?)(?:\\[(?<properties>.+?)?\\])?$");

    @Override
    public BlockOptionalMetaLookup get(IDatatypeContext ctx) throws CommandException {
        Matcher matcher = FULL_PATTERN.matcher(ctx.getConsumer().getString());

        if (!matcher.find()) {
            throw new IllegalArgumentException("invalid block/tag selector");
        }

        String props = Optional.ofNullable(matcher.group("properties")).orElse("");
        Map<String, String> properties = props.equals("") ? Collections.emptyMap() : parseProperties(props);

        return new BlockOptionalMetaLookup(parseTagOrBlockId(matcher.group("id"))
                .stream()
                .filter(block -> properties.keySet().stream()
                        .allMatch(name -> Optional.of(block)
                                .map(b -> b.getStateDefinition().getProperty(name))
                                .map(p -> p.getValue(properties.get(name)))
                                .isPresent()
                        )
                )
                .map(BuiltInRegistries.BLOCK::getKey)
                .map(id -> new BlockOptionalMeta(id + "[" + props + "]"))
                .toArray(BlockOptionalMeta[]::new)
        );
    }

    @Override
    public Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException {
        return tabComplete(ctx, true);
    }

    static Stream<String> tabComplete(IDatatypeContext ctx, boolean allowTags) throws CommandException {
        String arg = ctx.getConsumer().peekString();

        if (!PREFIX_PATTERN.matcher(arg).matches()) {
            // Invalid format; we can't complete this.
            return Stream.empty();
        }

        if (arg.startsWith("#") && !allowTags) {
            return Stream.empty();
        }

        if (arg.endsWith("]")) {
            // We are already done.
            return Stream.empty();
        }

        if (!arg.contains("[")) {
            // no properties so we are completing the block or tag id
            TabCompleteHelper helper = new TabCompleteHelper();
            if (arg.startsWith("#") || arg.equals("") && allowTags) {
                helper.append(BuiltInRegistries.BLOCK.getTags()
                                .map(pair -> pair.getFirst().location().toString()))
                        .filterPrefixNamespaced(arg.equals("") ? "" : arg.substring(1))
                        .sortAlphabetically()
                        .map(name -> "#" + name);
            }
            if (!arg.startsWith("#")) {
                helper.append(ctx.getConsumer().tabCompleteDatatype(BlockById.INSTANCE));
            }
            return helper.stream();
        }

        Set<Block> blocks = parseTagOrBlockId(splitLast(arg, '[')[0]);

        String properties = splitLast(arg, '[')[1];
        String previousProps = splitLast(properties, ',')[0];
        String lastProp = splitLast(properties, ',')[1];

        if (!lastProp.contains("=")) {
            // The last property-value pair doesn't have a value yet so we are completing its name
            Set<String> usedProps = Stream.of(previousProps.split(","))
                    .map(pair -> pair.split("=")[0])
                    .collect(Collectors.toSet());

            return blocks.stream()
                    .flatMap(b -> b.getStateDefinition()
                            .getProperties()
                            .stream()
                            .map(p -> p.getName())
                    )
                    .filter(prop -> !usedProps.contains(prop))
                    .filter(prop -> prop.startsWith(lastProp))
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .map(prop -> arg.substring(0, arg.length() - lastProp.length()) + prop);
        }

        String lastName = splitLast(lastProp, '=')[0];
        String lastValue = splitLast(lastProp, '=')[1];

        return blocks.stream()
                .map(b -> b.getStateDefinition().getProperty(lastName))
                .filter(p -> p != null)
                .flatMap(p -> p.getPossibleValues().stream().map(p::getName))
                .filter(v -> v.startsWith(lastValue))
                .distinct()
                .map(v -> arg.substring(0, arg.length() - lastValue.length()) + v);
    }

    private static Map<String, String> parseProperties(String raw) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        for (String pair : raw.split(",")) {
            String[] parts = pair.split("=");
            if (parts.length != 2) {
                throw new IllegalArgumentException(String.format("\"%s\" is not a valid property-value pair", pair));
            }
            builder.put(parts[0], parts[1]);
        }
        return builder.build();
    }

    private static Set<Block> parseTagOrBlockId(String id) {
        ResourceLocation loc = new ResourceLocation(id.replaceAll("^#", ""));
        if (id.startsWith("#")) {
            return BuiltInRegistries.BLOCK
                .getTag(TagKey.create(Registries.BLOCK, loc))
                .orElseThrow(() -> new IllegalArgumentException("No tag " + id))
                .stream()
                .map(h -> h.value())
                .collect(Collectors.toSet());
        } else {
            return BuiltInRegistries.BLOCK.getOptional(loc)
                .map(Set::of)
                .orElseThrow(() -> new IllegalArgumentException("No block " + id));
        }
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
}
