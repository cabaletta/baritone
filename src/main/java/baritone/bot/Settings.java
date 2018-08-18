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
    /**
     * It doesn't actually take twenty ticks to place a block, this cost is so high
     * because we want to generally conserve blocks which might be limited
     */
    public Setting<Double> blockPlacementPenalty = new Setting<>(20D);
    public Setting<Boolean> allowWaterBucketFall = new Setting<>(true);
    public Setting<Boolean> allowSprint = new Setting<>(true);
    public Setting<List<Item>> acceptableThrowawayItems = new Setting<>(Arrays.asList(
            Item.getItemFromBlock(Blocks.DIRT),
            Item.getItemFromBlock(Blocks.COBBLESTONE),
            Item.getItemFromBlock(Blocks.NETHERRACK)
    ));

    public Setting<Double> costHeuristic = new <Double>Setting<Double>(4D);

    // obscure internal A* settings that you probably don't want to change
    public Setting<Integer> pathingMaxChunkBorderFetch = new Setting<>(50);
    public Setting<Boolean> backtrackCostFavor = new Setting<>(true);  // see issue #18
    public Setting<Double> backtrackCostFavoringCoefficient = new Setting<>(0.9);  // see issue #18
    public Setting<Boolean> minimumImprovementRepropagation = new Setting<>(true);

    public Setting<Number> pathTimeoutMS = new Setting<>(4000L);

    public Setting<Boolean> slowPath = new Setting<>(false);
    public Setting<Number> slowPathTimeDelayMS = new Setting<>(100L);
    public Setting<Number> slowPathTimeoutMS = new Setting<>(40000L);

    public Setting<Boolean> chuckCaching = new Setting<>(false);

    public Setting<Integer> planningTickLookAhead = new Setting<>(150);

    public Setting<Boolean> chatDebug = new Setting<>(true);
    public Setting<Boolean> chatControl = new Setting<>(true); // probably false in impact

    public Setting<Boolean> renderPath = new Setting<>(true);
    public Setting<Boolean> renderGoal = new Setting<>(true);
    public Setting<Float> pathRenderLineWidth = new Setting<>(5F);
    public Setting<Float> goalRenderLineWidth = new Setting<>(3F);
    public Setting<Boolean> fadePath = new Setting<>(false); // give this a better name in the UI, like "better path fps" idk


    public final Map<String, Setting<?>> byName;
    public final List<Setting<?>> allSettings;

    public class Setting<T> {
        public T value;
        private String name;
        private final Class<T> klass;

        private Setting(T value) {
            if (value == null) {
                throw new IllegalArgumentException("Cannot determine value type class from null");
            }
            this.value = value;
            this.klass = (Class<T>) value.getClass();
        }

        public final <K extends T> K get() {
            return (K) value;
        }

        public final String getName() {
            return name;
        }

        public Class<T> getValueClass() {
            return klass;
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
