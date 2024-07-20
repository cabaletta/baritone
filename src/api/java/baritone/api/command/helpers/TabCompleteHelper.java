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

package baritone.api.command.helpers;

import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.manager.ICommandManager;
import baritone.api.event.events.TabCompleteEvent;
import baritone.api.utils.SettingsUtil;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;

/**
 * The {@link TabCompleteHelper} is a <b>single-use</b> object that helps you handle tab completion. It includes helper
 * methods for appending and prepending streams, sorting, filtering by prefix, and so on.
 * <p>
 * The recommended way to use this class is:
 * <ul>
 * <li>Create a new instance with the empty constructor</li>
 * <li>Use {@code append}, {@code prepend} or {@code add<something>} methods to add completions</li>
 * <li>Sort using {@link #sort(Comparator)} or {@link #sortAlphabetically()} and then filter by prefix using
 * {@link #filterPrefix(String)}</li>
 * <li>Get the stream using {@link #stream()}</li>
 * <li>Pass it up to whatever's calling your tab complete function (i.e.
 * {@link ICommandManager#tabComplete(String)} or {@link IArgConsumer}#tabCompleteDatatype(IDatatype)})</li>
 * </ul>
 * <p>
 * For advanced users: if you're intercepting {@link TabCompleteEvent}s directly, use {@link #build()} instead for an
 * array.
 */
public class TabCompleteHelper {

    private Stream<String> stream;

    public TabCompleteHelper(String[] base) {
        stream = Stream.of(base);
    }

    public TabCompleteHelper(List<String> base) {
        stream = base.stream();
    }

    public TabCompleteHelper() {
        stream = Stream.empty();
    }

    /**
     * Appends the specified stream to this {@link TabCompleteHelper} and returns it for chaining
     *
     * @param source The stream to append
     * @return This {@link TabCompleteHelper} after having appended the stream
     * @see #append(String...)
     * @see #append(Class)
     */
    public TabCompleteHelper append(Stream<String> source) {
        stream = Stream.concat(stream, source);
        return this;
    }

    /**
     * Appends the specified strings to this {@link TabCompleteHelper} and returns it for chaining
     *
     * @param source The stream to append
     * @return This {@link TabCompleteHelper} after having appended the strings
     * @see #append(Stream)
     * @see #append(Class)
     */
    public TabCompleteHelper append(String... source) {
        return append(Stream.of(source));
    }

    /**
     * Appends all values of the specified enum to this {@link TabCompleteHelper} and returns it for chaining
     *
     * @param num The enum to append the values of
     * @return This {@link TabCompleteHelper} after having appended the values
     * @see #append(Stream)
     * @see #append(String...)
     */
    public TabCompleteHelper append(Class<? extends Enum<?>> num) {
        return append(
                Stream.of(num.getEnumConstants())
                        .map(Enum::name)
                        .map(String::toLowerCase)
        );
    }

    /**
     * Prepends the specified stream to this {@link TabCompleteHelper} and returns it for chaining
     *
     * @param source The stream to prepend
     * @return This {@link TabCompleteHelper} after having prepended the stream
     * @see #prepend(String...)
     * @see #prepend(Class)
     */
    public TabCompleteHelper prepend(Stream<String> source) {
        stream = Stream.concat(source, stream);
        return this;
    }

    /**
     * Prepends the specified strings to this {@link TabCompleteHelper} and returns it for chaining
     *
     * @param source The stream to prepend
     * @return This {@link TabCompleteHelper} after having prepended the strings
     * @see #prepend(Stream)
     * @see #prepend(Class)
     */
    public TabCompleteHelper prepend(String... source) {
        return prepend(Stream.of(source));
    }

    /**
     * Prepends all values of the specified enum to this {@link TabCompleteHelper} and returns it for chaining
     *
     * @param num The enum to prepend the values of
     * @return This {@link TabCompleteHelper} after having prepended the values
     * @see #prepend(Stream)
     * @see #prepend(String...)
     */
    public TabCompleteHelper prepend(Class<? extends Enum<?>> num) {
        return prepend(
                Stream.of(num.getEnumConstants())
                        .map(Enum::name)
                        .map(String::toLowerCase)
        );
    }

    /**
     * Apply the specified {@code transform} to every element <b>currently</b> in this {@link TabCompleteHelper} and
     * return this object for chaining
     *
     * @param transform The transform to apply
     * @return This {@link TabCompleteHelper}
     */
    public TabCompleteHelper map(Function<String, String> transform) {
        stream = stream.map(transform);
        return this;
    }

    /**
     * Apply the specified {@code filter} to every element <b>currently</b> in this {@link TabCompleteHelper} and return
     * this object for chaining
     *
     * @param filter The filter to apply
     * @return This {@link TabCompleteHelper}
     */
    public TabCompleteHelper filter(Predicate<String> filter) {
        stream = stream.filter(filter);
        return this;
    }

    /**
     * Apply the specified {@code sort} to every element <b>currently</b> in this {@link TabCompleteHelper} and return
     * this object for chaining
     *
     * @param comparator The comparator to use
     * @return This {@link TabCompleteHelper}
     */
    public TabCompleteHelper sort(Comparator<String> comparator) {
        stream = stream.sorted(comparator);
        return this;
    }

    /**
     * Sort every element <b>currently</b> in this {@link TabCompleteHelper} alphabetically and return this object for
     * chaining
     *
     * @return This {@link TabCompleteHelper}
     */
    public TabCompleteHelper sortAlphabetically() {
        return sort(String.CASE_INSENSITIVE_ORDER);
    }

    /**
     * Filter out any element that doesn't start with {@code prefix} and return this object for chaining
     *
     * @param prefix The prefix to filter for
     * @return This {@link TabCompleteHelper}
     */
    public TabCompleteHelper filterPrefix(String prefix) {
        return filter(x -> x.toLowerCase(Locale.US).startsWith(prefix.toLowerCase(Locale.US)));
    }

    /**
     * Filter out any element that doesn't start with {@code prefix} and return this object for chaining
     * <p>
     * Assumes every element in this {@link TabCompleteHelper} is a {@link ResourceLocation}
     *
     * @param prefix The prefix to filter for
     * @return This {@link TabCompleteHelper}
     */
    public TabCompleteHelper filterPrefixNamespaced(String prefix) {
        return filterPrefix(ResourceLocation.parse(prefix).toString());
    }

    /**
     * @return An array containing every element in this {@link TabCompleteHelper}
     * @see #stream()
     */
    public String[] build() {
        return stream.toArray(String[]::new);
    }

    /**
     * @return A stream containing every element in this {@link TabCompleteHelper}
     * @see #build()
     */
    public Stream<String> stream() {
        return stream;
    }

    /**
     * Appends every command in the specified {@link ICommandManager} to this {@link TabCompleteHelper}
     *
     * @param manager A command manager
     * @return This {@link TabCompleteHelper}
     */
    public TabCompleteHelper addCommands(ICommandManager manager) {
        return append(manager.getRegistry().descendingStream()
                .flatMap(command -> command.getNames().stream())
                .distinct()
        );
    }

    /**
     * Appends every setting in the {@link Settings} to this {@link TabCompleteHelper}
     *
     * @return This {@link TabCompleteHelper}
     */
    public TabCompleteHelper addSettings() {
        return append(
                BaritoneAPI.getSettings().allSettings.stream()
                        .filter(s -> !s.isJavaOnly())
                        .map(Settings.Setting::getName)
                        .sorted(String.CASE_INSENSITIVE_ORDER)
        );
    }

    /**
     * Appends every modified setting in the {@link Settings} to this {@link TabCompleteHelper}
     *
     * @return This {@link TabCompleteHelper}
     */
    public TabCompleteHelper addModifiedSettings() {
        return append(
                SettingsUtil.modifiedSettings(BaritoneAPI.getSettings()).stream()
                        .map(Settings.Setting::getName)
                        .sorted(String.CASE_INSENSITIVE_ORDER)
        );
    }

    /**
     * Appends every {@link Boolean} setting in the {@link Settings} to this {@link TabCompleteHelper}
     *
     * @return This {@link TabCompleteHelper}
     */
    public TabCompleteHelper addToggleableSettings() {
        return append(
                BaritoneAPI.getSettings().getAllValuesByType(Boolean.class).stream()
                        .map(Settings.Setting::getName)
                        .sorted(String.CASE_INSENSITIVE_ORDER)
        );
    }
}
