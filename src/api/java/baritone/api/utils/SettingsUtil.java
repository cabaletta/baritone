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
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SettingsUtil {
    private static final File settingsFile = new File(new File(Minecraft.getMinecraft().gameDir, "baritone"), "settings.txt");

    private static final Map<Class<?>, SettingsIO> map;

    public static void readAndApply(Settings settings) {
        try (Scanner scan = new Scanner(settingsFile)) {
            while (scan.hasNextLine()) {
                String line = scan.nextLine();
                if (line.isEmpty()) {
                    continue;
                }
                if (line.startsWith("#") || line.startsWith("//")) {
                    continue;
                }
                int space = line.indexOf(" ");
                if (space == -1) {
                    System.out.println("Skipping invalid line with no space: " + line);
                    continue;
                }
                String settingName = line.substring(0, space).trim().toLowerCase();
                String settingValue = line.substring(space).trim();
                try {
                    parseAndApply(settings, settingName, settingValue);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.out.println("Unable to parse line " + line);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Exception while reading Baritone settings, some settings may be reset to default values!");
        }
    }

    public static synchronized void save(Settings settings) {
        try (FileOutputStream out = new FileOutputStream(settingsFile)) {
            for (Settings.Setting setting : settings.allSettings) {
                if (setting.get() == null) {
                    System.out.println("NULL SETTING?" + setting.getName());
                    continue;
                }
                if (setting.getName().equals("logger")) {
                    continue; // NO
                }
                SettingsIO io = map.get(setting.getValueClass());
                if (io == null) {
                    throw new IllegalStateException("Missing " + setting.getValueClass() + " " + setting + " " + setting.getName());
                }
                out.write((setting.getName() + " " + io.toString.apply(setting.get()) + "\n").getBytes());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Exception while saving Baritone settings!");
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
