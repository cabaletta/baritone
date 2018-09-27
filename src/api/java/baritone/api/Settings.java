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

package baritone.api;

import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.text.ITextComponent;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

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
     * Allow Baritone to assume it can walk on still water just like any other block.
     * This functionality is assumed to be provided by a separate library that might have imported Baritone.
     */
    public Setting<Boolean> assumeWalkOnWater = new Setting<>(false);

    /**
     * Assume step functionality; don't jump on an Ascend.
     */
    public Setting<Boolean> assumeStep = new Setting<>(false);

    /**
     * Assume safe walk functionality; don't sneak on a backplace traverse.
     * <p>
     * Warning: if you do something janky like sneak-backplace from an ender chest, if this is true
     * it won't sneak right click, it'll just right click, which means it'll open the chest instead of placing
     * against it. That's why this defaults to off.
     */
    public Setting<Boolean> assumeSafeWalk = new Setting<>(false);

    /**
     * Blocks that Baritone is allowed to place (as throwaway, for sneak bridging, pillaring, etc.)
     */
    public Setting<List<Item>> acceptableThrowawayItems = new Setting<>(new ArrayList<>(Arrays.asList(
            Item.getItemFromBlock(Blocks.DIRT),
            Item.getItemFromBlock(Blocks.COBBLESTONE),
            Item.getItemFromBlock(Blocks.NETHERRACK)
    )));

    /**
     * Enables some more advanced vine features. They're honestly just gimmicks and won't ever be needed in real
     * pathing scenarios. And they can cause Baritone to get trapped indefinitely in a strange scenario.
     */
    public Setting<Boolean> allowVines = new Setting<>(false);

    /**
     * Slab behavior is complicated, disable this for higher path reliability. Leave enabled if you have bottom slabs
     * everywhere in your base.
     */
    public Setting<Boolean> allowWalkOnBottomSlab = new Setting<>(true);

    /**
     * You know what it is
     * <p>
     * But it's very unreliable and falls off when cornering like all the time so.
     */
    public Setting<Boolean> allowParkour = new Setting<>(false);

    /**
     * Like parkour, but even more unreliable!
     */
    public Setting<Boolean> allowParkourPlace = new Setting<>(false);

    /**
     * For example, if you have Mining Fatigue or Haste, adjust the costs of breaking blocks accordingly.
     */
    public Setting<Boolean> considerPotionEffects = new Setting<>(true);

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
    public Setting<Double> costHeuristic = new Setting<>(3.5D);

    // a bunch of obscure internal A* settings that you probably don't want to change
    /**
     * The maximum number of times it will fetch outside loaded or cached chunks before assuming that
     * pathing has reached the end of the known area, and should therefore stop.
     */
    public Setting<Integer> pathingMaxChunkBorderFetch = new Setting<>(50);

    /**
     * Set to 1.0 to effectively disable this feature
     *
     * @see <a href="https://github.com/cabaletta/baritone/issues/18">Issue #18</a>
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
     *
     * @see <a href="https://github.com/cabaletta/baritone/issues/144">Issue #144</a>
     */
    public Setting<Boolean> cutoffAtLoadBoundary = new Setting<>(false);

    /**
     * If a movement's cost increases by more than this amount between calculation and execution (due to changes
     * in the environment / world), cancel and recalculate
     */
    public Setting<Double> maxCostIncrease = new Setting<>(10D);

    /**
     * Stop 5 movements before anything that made the path COST_INF.
     * For example, if lava has spread across the path, don't walk right up to it then recalculate, it might
     * still be spreading lol
     */
    public Setting<Integer> costVerificationLookahead = new Setting<>(5);

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
     * Default size of the Long2ObjectOpenHashMap used in pathing
     */
    public Setting<Integer> pathingMapDefaultSize = new Setting<>(1024);

    /**
     * Load factor coefficient for the Long2ObjectOpenHashMap used in pathing
     * <p>
     * Decrease for faster map operations, but higher memory usage
     */
    public Setting<Float> pathingMapLoadFactor = new Setting<>(0.75f);

    /**
     * How far are you allowed to fall onto solid ground (without a water bucket)?
     * 3 won't deal any damage. But if you just want to get down the mountain quickly and you have
     * Feather Falling IV, you might set it a bit higher, like 4 or 5.
     */
    public Setting<Integer> maxFallHeightNoWater = new Setting<>(3);

    /**
     * How far are you allowed to fall onto solid ground (with a water bucket)?
     * It's not that reliable, so I've set it below what would kill an unarmored player (23)
     */
    public Setting<Integer> maxFallHeightBucket = new Setting<>(20);

    /**
     * Is it okay to sprint through a descend followed by a diagonal?
     * The player overshoots the landing, but not enough to fall off. And the diagonal ensures that there isn't
     * lava or anything that's !canWalkInto in that space, so it's technically safe, just a little sketchy.
     */
    public Setting<Boolean> allowOvershootDiagonalDescend = new Setting<>(true);

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
    public Setting<Long> pathTimeoutMS = new Setting<>(2000L);

    /**
     * Planning ahead while executing a segment can never take longer than this
     */
    public Setting<Long> planAheadTimeoutMS = new Setting<>(4000L);

    /**
     * For debugging, consider nodes much much slower
     */
    public Setting<Boolean> slowPath = new Setting<>(false);

    /**
     * Milliseconds between each node
     */
    public Setting<Long> slowPathTimeDelayMS = new Setting<>(100L);

    /**
     * The alternative timeout number when slowPath is on
     */
    public Setting<Long> slowPathTimeoutMS = new Setting<>(40000L);

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
     * A second override over chatControl to force it on
     */
    public Setting<Boolean> removePrefix = new Setting<>(false);

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

    /**
     * Will cause some minor behavioral differences to ensure that Baritone works on anticheats.
     * <p>
     * At the moment this will silently set the player's rotations when using freeLook so you're not sprinting in
     * directions other than forward, which is picken up by more "advanced" anticheats like AAC, but not NCP.
     */
    public Setting<Boolean> antiCheatCompatibility = new Setting<>(true);

    /**
     * Exclusively use cached chunks for pathing
     */
    public Setting<Boolean> pathThroughCachedOnly = new Setting<>(false);

    /**
     * Whether or not to use the "#" command prefix
     */
    public Setting<Boolean> prefix = new Setting<>(false);

    /**
     * {@code true}: can mine blocks when in inventory, chat, or tabbed away in ESC menu
     * <p>
     * {@code false}: works on cosmic prisons
     * <p>
     * LOL
     */
    public Setting<Boolean> leftClickWorkaround = new Setting<>(true);

    /**
     * Don't stop walking forward when you need to break blocks in your way
     */
    public Setting<Boolean> walkWhileBreaking = new Setting<>(true);

    /**
     * Rescan for the goal once every 5 ticks.
     * Set to 0 to disable.
     */
    public Setting<Integer> mineGoalUpdateInterval = new Setting<>(5);

    /**
     * Cancel the current path if the goal has changed, and the path originally ended in the goal but doesn't anymore.
     * <p>
     * Currently only runs when either MineBehavior or FollowBehavior is active.
     * <p>
     * For example, if Baritone is doing "mine iron_ore", the instant it breaks the ore (and it becomes air), that location
     * is no longer a goal. This means that if this setting is true, it will stop there. If this setting were off, it would
     * continue with its path, and walk into that location. The tradeoff is if this setting is true, it mines ores much faster
     * since it doesn't waste any time getting into locations that no longer contain ores, but on the other hand, it misses
     * some drops, and continues on without ever picking them up.
     * <p>
     * Also on cosmic prisons this should be set to true since you don't actually mine the ore it just gets replaced with stone.
     */
    public Setting<Boolean> cancelOnGoalInvalidation = new Setting<>(true);

    /**
     * The "axis" command (aka GoalAxis) will go to a axis, or diagonal axis, at this Y level.
     */
    public Setting<Integer> axisHeight = new Setting<>(120);

    /**
     * When mining block of a certain type, try to mine two at once instead of one.
     * If the block above is also a goal block, set GoalBlock instead of GoalTwoBlocks
     * If the block below is also a goal block, set GoalBlock to the position one down instead of GoalTwoBlocks
     */
    public Setting<Boolean> forceInternalMining = new Setting<>(true);

    /**
     * Modification to the previous setting, only has effect if forceInternalMining is true
     * If true, only apply the previous setting if the block adjacent to the goal isn't air.
     */
    public Setting<Boolean> internalMiningAirException = new Setting<>(true);

    /**
     * The actual GoalNear is set this distance away from the entity you're following
     * <p>
     * For example, set followOffsetDistance to 5 and followRadius to 0 to always stay precisely 5 blocks north of your follow target.
     */
    public Setting<Double> followOffsetDistance = new Setting<>(0D);

    /**
     * The actual GoalNear is set in this direction from the entity you're following
     */
    public Setting<Float> followOffsetDirection = new Setting<>(0F);

    /**
     * The radius (for the GoalNear) of how close to your target position you actually have to be
     */
    public Setting<Integer> followRadius = new Setting<>(3);

    /**
     * The function that is called when Baritone will log to chat. This function can be added to
     * via {@link Consumer#andThen(Consumer)} or it can completely be overriden via setting
     * {@link Setting#value};
     */
    public Setting<Consumer<ITextComponent>> logger = new Setting<>(Minecraft.getMinecraft().ingameGUI.getChatGUI()::printChatMessage);

    /**
     * The color of the current path
     */
    public Setting<Color> colorCurrentPath = new Setting<>(Color.RED);

    /**
     * The color of the next path
     */
    public Setting<Color> colorNextPath = new Setting<>(Color.MAGENTA);

    /**
     * The color of the blocks to break
     */
    public Setting<Color> colorBlocksToBreak = new Setting<>(Color.RED);

    /**
     * The color of the blocks to place
     */
    public Setting<Color> colorBlocksToPlace = new Setting<>(Color.GREEN);

    /**
     * The color of the blocks to walk into
     */
    public Setting<Color> colorBlocksToWalkInto = new Setting<>(Color.MAGENTA);

    /**
     * The color of the best path so far
     */
    public Setting<Color> colorBestPathSoFar = new Setting<>(Color.BLUE);

    /**
     * The color of the path to the most recent considered node
     */
    public Setting<Color> colorMostRecentConsidered = new Setting<>(Color.CYAN);

    /**
     * The color of the goal box
     */
    public Setting<Color> colorGoalBox = new Setting<>(Color.GREEN);

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
            throw new IllegalStateException(e);
        }
        byLowerName = Collections.unmodifiableMap(tmpByName);
        allSettings = Collections.unmodifiableList(tmpAll);
    }

    @SuppressWarnings("unchecked")
    public <T> List<Setting<T>> getAllValuesByType(Class<T> klass) {
        List<Setting<T>> result = new ArrayList<>();
        for (Setting<?> setting : allSettings) {
            if (setting.getValueClass().equals(klass)) {
                result.add((Setting<T>) setting);
            }
        }
        return result;
    }

    Settings() { }
}
