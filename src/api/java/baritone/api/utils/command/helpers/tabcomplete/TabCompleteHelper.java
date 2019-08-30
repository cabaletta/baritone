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

package baritone.api.utils.command.helpers.tabcomplete;

import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import baritone.api.utils.SettingsUtil;
import baritone.api.utils.command.manager.CommandManager;
import net.minecraft.util.ResourceLocation;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

public class TabCompleteHelper {
    private Stream<String> stream;

    public TabCompleteHelper(String[] base) {
        stream = Arrays.stream(base);
    }

    public TabCompleteHelper(List<String> base) {
        stream = base.stream();
    }

    public TabCompleteHelper() {
        this(new String[0]);
    }

    public TabCompleteHelper append(Stream<String> source) {
        stream = concat(stream, source);

        return this;
    }

    public TabCompleteHelper append(String... source) {
        return append(of(source));
    }

    public TabCompleteHelper append(Class<? extends Enum<?>> num) {
        return append(
            Arrays.stream(num.getEnumConstants())
                .map(Enum::name)
                .map(String::toLowerCase)
        );
    }

    public TabCompleteHelper prepend(Stream<String> source) {
        stream = concat(source, stream);

        return this;
    }

    public TabCompleteHelper prepend(String... source) {
        return prepend(of(source));
    }

    public TabCompleteHelper prepend(Class<? extends Enum<?>> num) {
        return prepend(
            Arrays.stream(num.getEnumConstants())
                .map(Enum::name)
                .map(String::toLowerCase)
        );
    }

    public TabCompleteHelper map(Function<String, String> transform) {
        stream = stream.map(transform);

        return this;
    }

    public TabCompleteHelper filter(Predicate<String> filter) {
        stream = stream.filter(filter);

        return this;
    }

    public TabCompleteHelper sort(Comparator<String> comparator) {
        stream = stream.sorted(comparator);

        return this;
    }

    public TabCompleteHelper sortAlphabetically() {
        return sort(String.CASE_INSENSITIVE_ORDER);
    }

    public TabCompleteHelper filterPrefix(String prefix) {
        return filter(x -> x.toLowerCase(Locale.US).startsWith(prefix.toLowerCase(Locale.US)));
    }

    public TabCompleteHelper filterPrefixNamespaced(String prefix) {
        return filterPrefix(new ResourceLocation(prefix).toString());
    }

    public String[] build() {
        return stream.toArray(String[]::new);
    }

    public Stream<String> stream() {
        return stream;
    }

    public TabCompleteHelper addCommands() {
        return append(
            CommandManager.REGISTRY.descendingStream()
                .flatMap(command -> command.names.stream())
                .distinct()
        );
    }

    public TabCompleteHelper addSettings() {
        return append(
            BaritoneAPI.getSettings().allSettings.stream()
                .map(Settings.Setting::getName)
                .filter(s -> !s.equalsIgnoreCase("logger"))
                .sorted(String.CASE_INSENSITIVE_ORDER)
        );
    }

    public TabCompleteHelper addModifiedSettings() {
        return append(
            SettingsUtil.modifiedSettings(BaritoneAPI.getSettings()).stream()
                .map(Settings.Setting::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
        );
    }

    public TabCompleteHelper addToggleableSettings() {
        return append(
            BaritoneAPI.getSettings().getAllValuesByType(Boolean.class).stream()
                .map(Settings.Setting::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
        );
    }
}
