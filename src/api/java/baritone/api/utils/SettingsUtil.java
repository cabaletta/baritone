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

package baritone.api.utils;

import baritone.api.Settings;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.minecraft.client.Minecraft.getMinecraft;

public class SettingsUtil {

    private static final Path settingsFile = getMinecraft().gameDir.toPath().resolve("baritone").resolve("settings.txt");
    private static final Pattern SETTING_PATTERN = Pattern.compile("^(?<setting>[^ ]+) +(?<value>[^ ]+)");// 2 words separated by spaces

    private static final Map<Class<?>, SettingsIO> map;


    private static boolean isComment(String line) {
        return line.startsWith("#") || line.startsWith("//");
    }

    private static void forEachLine(Path file, Consumer<String> consumer) throws IOException {
        try (BufferedReader scan = Files.newBufferedReader(file)) {
            String line;
            while ((line = scan.readLine()) != null) {
                if (line.isEmpty() || isComment(line)) continue;

                consumer.accept(line);
            }
        }
    }

    public void readAndApply(Settings settings) {
        try {
            forEachLine(settingsFile, line -> {
                Matcher matcher = SETTING_PATTERN.matcher(line);
                if (!matcher.matches()) {
                    System.out.println("Invalid syntax in setting file: " + line);
                    return;
                }

                String settingName = matcher.group("setting").toLowerCase();
                String settingValue = matcher.group("value").toLowerCase();
                try {
                    parseAndApply(settings, settingName, settingValue);
                } catch (Exception ex) {
                    System.out.println("Unable to parse line " + line);
                    ex.printStackTrace();
                }
            });
        } catch (Exception ex) {
            System.out.println("Exception while reading Baritone settings, some settings may be reset to default values!");
            ex.printStackTrace();
        }
    }

    public static synchronized void save(Settings settings) {
        try (BufferedWriter out = Files.newBufferedWriter(settingsFile)) {
            for (Settings.Setting setting : settings.allSettings) {
                if (setting.get() == null) {
                    System.out.println("NULL SETTING?" + setting.getName());
                    continue;
                }
                if (setting.getName().equals("logger")) {
                    continue; // NO
                }
                if (setting.value == setting.defaultValue) {
                    continue;
                }
                SettingsIO io = map.get(setting.getValueClass());
                if (io == null) {
                    throw new IllegalStateException("Missing " + setting.getValueClass() + " " + setting + " " + setting.getName());
                }
                out.write(setting.getName() + " " + io.toString.apply(setting.get()) + "\n");
            }
        } catch (Exception ex) {
            System.out.println("Exception thrown while saving Baritone settings!");
            ex.printStackTrace();
        }
    }

    private static void parseAndApply(Settings settings, String settingName, String settingValue) throws IllegalStateException, NumberFormatException {
        Settings.Setting setting = settings.byLowerName.get(settingName);
        if (setting == null) {
            throw new IllegalStateException("No setting by that name");
        }
        Class intendedType = setting.getValueClass();
        SettingsIO ioMethod = map.get(intendedType);
        Object parsed = ioMethod.parser.apply(settingValue);
        if (!intendedType.isInstance(parsed)) {
            throw new IllegalStateException(ioMethod + " parser returned incorrect type, expected " + intendedType + " got " + parsed + " which is " + parsed.getClass());
        }
        setting.value = parsed;
    }

    private enum SettingsIO {
        DOUBLE(Double.class, Double::parseDouble),
        BOOLEAN(Boolean.class, Boolean::parseBoolean),
        INTEGER(Integer.class, Integer::parseInt),
        FLOAT(Float.class, Float::parseFloat),
        LONG(Long.class, Long::parseLong),

        ITEM_LIST(ArrayList.class, str -> Stream.of(str.split(",")).map(Item::getByNameOrId).collect(Collectors.toCollection(ArrayList::new)), list -> ((ArrayList<Item>) list).stream().map(Item.REGISTRY::getNameForObject).map(ResourceLocation::toString).collect(Collectors.joining(","))),
        COLOR(Color.class, str -> new Color(Integer.parseInt(str.split(",")[0]), Integer.parseInt(str.split(",")[1]), Integer.parseInt(str.split(",")[2])), color -> color.getRed() + "," + color.getGreen() + "," + color.getBlue());


        Class<?> klass;
        Function<String, Object> parser;
        Function<Object, String> toString;

        <T> SettingsIO(Class<T> klass, Function<String, T> parser) {
            this(klass, parser, Object::toString);
        }

        <T> SettingsIO(Class<T> klass, Function<String, T> parser, Function<T, String> toString) {
            this.klass = klass;
            this.parser = parser::apply;
            this.toString = x -> toString.apply((T) x);
        }
    }

    static {
        HashMap<Class<?>, SettingsIO> tempMap = new HashMap<>();
        for (SettingsIO type : SettingsIO.values()) {
            tempMap.put(type.klass, type);
        }
        map = Collections.unmodifiableMap(tempMap);
    }
}
