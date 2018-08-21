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
    /**
     * Allow Baritone to break blocks
     */
    public Setting<Boolean> allowBreak = new Setting<>(true);

    /**
     * Allow Baritone to sprint
     */
    public Setting<Boolean> allowSprint = new Setting<>(true);

    /**
     * Allow Baritone to place blocks
     */
    public Setting<Boolean> allowPlace = new Setting<>(true);

    /**
     * It doesn't actually take twenty ticks to place a block, this cost is so high
     * because we want to generally conserve blocks which might be limited
     */
    public Setting<Double> blockPlacementPenalty = new Setting<>(20D);

    /**
     * Allow Baritone to fall arbitrary distances and place a water bucket beneath it.
     * Reliability: questionable.
     */
    public Setting<Boolean> allowWaterBucketFall = new Setting<>(true);

    /**
     * Blocks that Baritone is allowed to place (as throwaway, for sneak bridging, pillaring, etc.)
     */
    public Setting<List<Item>> acceptableThrowawayItems = new Setting<>(Arrays.asList(
            Item.getItemFromBlock(Blocks.DIRT),
            Item.getItemFromBlock(Blocks.COBBLESTONE),
            Item.getItemFromBlock(Blocks.NETHERRACK)
    ));

    /**
     * Enables some more advanced vine features. They're honestly just gimmicks and won't ever be needed in real
     * pathing scenarios. And they can cause Baritone to get trapped indefinitely in a strange scenario.
     */
    public Setting<Boolean> allowVines = new Setting<>(false);

    /**
     * This is the big A* setting.
     * As long as your cost heuristic is an *underestimate*, it's guaranteed to find you the best path.
     * 3.5 is always an underestimate, even if you are sprinting.
     * If you're walking only (with allowSprint off) 4.6 is safe.
     * Any value below 3.5 is never worth it. It's just more computation to find the same path, guaranteed.
     * (specifically, it needs to be strictly slightly less than ActionCosts.WALK_ONE_BLOCK_COST, which is about 3.56)
     * <p>
     * Setting it at 3.57 or above with sprinting, or to 4.64 or above without sprinting, will result in
     * faster computation, at the cost of a suboptimal path. Any value above the walk / sprint cost will result
     * in it going straight at its goal, and not investigating alternatives, because the combined cost / heuristic
     * metric gets better and better with each block, instead of slightly worse.
     * <p>
     * Finding the optimal path is worth it, so it's the default.
     */
    public Setting<Double> costHeuristic = this.new <Double>Setting<Double>(3.5D);

    // a bunch of obscure internal A* settings that you probably don't want to change
    /**
     * The maximum number of times it will fetch outside loaded or cached chunks before assuming that
     * pathing has reached the end of the known area, and should therefore stop.
     */
    public Setting<Integer> pathingMaxChunkBorderFetch = new Setting<>(50);

    /**
     * See issue #18
     * Set to 1.0 to effectively disable this feature
     */
    public Setting<Double> backtrackCostFavoringCoefficient = new Setting<>(0.9);

    /**
     * Don't repropagate cost improvements below 0.01 ticks. They're all just floating point inaccuracies,
     * and there's no point.
     */
    public Setting<Boolean> minimumImprovementRepropagation = new Setting<>(true);

    /**
     * After calculating a path (potentially through cached chunks), artificially cut it off to just the part that is
     * entirely within currently loaded chunks. Improves path safety because cached chunks are heavily simplified.
     */
    public Setting<Boolean> cutoffAtLoadBoundary = new Setting<>(true);

    /**
     * Static cutoff factor. 0.9 means cut off the last 10% of all paths, regardless of chunk load state
     */
    public Setting<Double> pathCutoffFactor = new Setting<>(0.9);

    /**
     * Only apply static cutoff for paths of at least this length (in terms of number of movements)
     */
    public Setting<Integer> pathCutoffMinimumLength = new Setting<>(30);

    /**
     * Start planning the next path once the remaining movements tick estimates sum up to less than this value
     */
    public Setting<Integer> planningTickLookAhead = new Setting<>(100);

    /**
     * How far are you allowed to fall onto solid ground (without a water bucket)?
     * 3 won't deal any damage. But if you just want to get down the mountain quickly and you have
     * Feather Falling IV, you might set it a bit higher, like 4 or 5.
     */
    public Setting<Integer> maxFallHeight = new Setting<>(3);

    /**
     * If your goal is a GoalBlock in an unloaded chunk, assume it's far enough away that the Y coord
     * doesn't matter yet, and replace it with a GoalXZ to the same place before calculating a path.
     * Once a segment ends within chunk load range of the GoalBlock, it will go back to normal behavior
     * of considering the Y coord. The reasoning is that if your X and Z are 10,000 blocks away,
     * your Y coordinate's accuracy doesn't matter at all until you get much much closer.
     */
    public Setting<Boolean> simplifyUnloadedYCoord = new Setting<>(true);

    /**
     * If a movement takes this many ticks more than its initial cost estimate, cancel it
     */
    public Setting<Integer> movementTimeoutTicks = new Setting<>(100);

    /**
     * Pathing can never take longer than this
     */
    public Setting<Number> pathTimeoutMS = new Setting<>(4000L);

    /**
     * For debugging, consider nodes much much slower
     */
    public Setting<Boolean> slowPath = new Setting<>(false);

    /**
     * Milliseconds between each node
     */
    public Setting<Number> slowPathTimeDelayMS = new Setting<>(100L);

    /**
     * The alternative timeout number when slowPath is on
     */
    public Setting<Number> slowPathTimeoutMS = new Setting<>(40000L);

    /**
     * The big one. Download all chunks in simplified 2-bit format and save them for better very-long-distance pathing.
     */
    public Setting<Boolean> chunkCaching = new Setting<>(true);

    /**
     * Print all the debug messages to chat
     */
    public Setting<Boolean> chatDebug = new Setting<>(true);

    /**
     * Allow chat based control of Baritone. Most likely should be disabled when Baritone is imported for use in
     * something else
     */
    public Setting<Boolean> chatControl = new Setting<>(true);

    /**
     * Render the path
     */
    public Setting<Boolean> renderPath = new Setting<>(true);

    /**
     * Render the goal
     */
    public Setting<Boolean> renderGoal = new Setting<>(true);

    /**
     * Line width of the path when rendered, in pixels
     */
    public Setting<Float> pathRenderLineWidthPixels = new Setting<>(5F);

    /**
     * Line width of the goal when rendered, in pixels
     */
    public Setting<Float> goalRenderLineWidthPixels = new Setting<>(3F);

    /**
     * Start fading out the path at 20 movements ahead, and stop rendering it entirely 30 movements ahead.
     * Improves FPS.
     */
    public Setting<Boolean> fadePath = new Setting<>(false);

    /**
     * Move without having to force the client-sided rotations
     */
    public Setting<Boolean> freeLook = new Setting<>(true);

    public final Map<String, Setting<?>> byLowerName;
    public final List<Setting<?>> allSettings;

    public class Setting<T> {
        public T value;
        private String name;
        private final Class<T> klass;

        @SuppressWarnings("unchecked")
        private Setting(T value) {
            if (value == null) {
                throw new IllegalArgumentException("Cannot determine value type class from null");
            }
            this.value = value;
            this.klass = (Class<T>) value.getClass();
        }

        @SuppressWarnings("unchecked")
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
                    Setting<?> setting = (Setting<?>) field.get(this);
                    String name = field.getName();
                    setting.name = name;
                    name = name.toLowerCase();
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
        byLowerName = Collections.unmodifiableMap(tmpByName);
        allSettings = Collections.unmodifiableList(tmpAll);
    }

    @SuppressWarnings("unchecked")
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
