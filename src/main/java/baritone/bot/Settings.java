/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.bot;

import net.minecraft.init.Blocks;
import net.minecraft.item.Item;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Baritone's settings
 *
 * @author leijurv
 */
public class Settings {
    public Setting<Boolean> allowBreak = new Setting<>(true);
    public Setting<Boolean> allowPlaceThrowaway = new Setting<>(true);
    public Setting<Double> costHeuristic = new <Double>Setting<Double>(4D);
    public Setting<Boolean> chuckCaching = new Setting<>(false);
    public Setting<Boolean> allowWaterBucketFall = new Setting<>(true);
    public Setting<Integer> planningTickLookAhead = new Setting<>(150);
    public Setting<Boolean> renderPath = new Setting<>(true);
    public Setting<Boolean> chatDebug = new Setting<>(true);
    public Setting<Boolean> chatControl = new Setting<>(true); // probably false in impact
    public Setting<Boolean> fadePath = new Setting<>(false); // give this a better name in the UI, like "better path fps" idk
    public Setting<Boolean> slowPath = new Setting<>(false);
    public Setting<List<Item>> acceptableThrowAwayItems = new Setting<>(Arrays.asList(
            Item.getItemFromBlock(Blocks.DIRT),
            Item.getItemFromBlock(Blocks.COBBLESTONE),
            Item.getItemFromBlock(Blocks.NETHERRACK)
    ));

    public final Map<String, Setting<?>> byName;
    public final List<Setting<?>> allSettings;

    public class Setting<T> {
        public T value;
        private String name;
        private final Class<T> klass;

        private Setting(T value) {
            this.value = value;
            this.klass = (Class<T>) value.getClass();
        }

        public final T get() {
            return value;
        }

        public final String getName() {
            return name;
        }

        public String toString() {
            return name + ": " + value;
        }
    }

    // here be dragons

    {
        Field[] temp = getClass().getFields();
        HashMap<String, Setting<?>> tmpByName = new HashMap<>();
        List<Setting<?>> tmpAll = new ArrayList<>();
        try {
            for (Field field : temp) {
                if (field.getType().equals(Setting.class)) {
                    Setting<?> setting = (Setting<? extends Object>) field.get(this);
                    String name = field.getName();
                    setting.name = name;
                    if (tmpByName.containsKey(name)) {
                        throw new IllegalStateException("Duplicate setting name");
                    }
                    tmpByName.put(name, setting);
                    tmpAll.add(setting);
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        byName = Collections.unmodifiableMap(tmpByName);
        allSettings = Collections.unmodifiableList(tmpAll);
    }

    public <T> List<Setting<T>> getByValueType(Class<T> klass) {
        ArrayList<Setting<T>> result = new ArrayList<>();
        for (Setting<?> setting : allSettings) {
            if (setting.klass.equals(klass)) {
                result.add((Setting<T>) setting);
            }
        }
        return result;
    }

    Settings() { }
}
