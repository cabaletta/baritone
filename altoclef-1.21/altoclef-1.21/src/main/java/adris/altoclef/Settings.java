package adris.altoclef;

import adris.altoclef.control.KillAura;
import adris.altoclef.tasks.movement.DefaultGoToDimensionTask;
import adris.altoclef.util.BlockRange;
import adris.altoclef.util.helpers.ConfigHelper;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.serialization.IFailableConfigFile;
import adris.altoclef.util.serialization.ItemDeserializer;
import adris.altoclef.util.serialization.ItemSerializer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Streams;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * The settings file, loaded and used across the codebase.
 * <p>
 * Each setting is documented.
 */
@SuppressWarnings("ALL")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Settings implements IFailableConfigFile {

    public static final String SETTINGS_PATH = "altoclef_settings.json";

    // Internal only.
    // If settings failed to load, this will be set to warn the user.
    @JsonIgnore
    private transient boolean _failedToLoad = false;

    //////////////////////////////////////////////////////////////////////////////////////////
    ////////** BEGIN SETTINGS w/ COMMENTS **//////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////

    /**
     * If true, text will appear on the top left showing the current
     * task chain.
     */
    private boolean showTaskChains = true;

    /**
     * If true, all warning logs will be disabled.
     * <p>
     * NOT RECOMMENDED, as it will make debugging more difficult.
     * But if you know what you're doing, go nuts.
     */
    private boolean hideAllWarningLogs = false;

    /**
     * The prefix for commands (ex. @gamer )
     */
    private String commandPrefix = "@";

    /**
     * When logging to chat, will prepend this to each log.
     */
    private String chatLogPrefix = "[Alto Clef] ";

    /**
     * If true, will show a timer.
     */
    private boolean showTimer = false;

    /**
     * The delay between moving items for crafting/furnace/any kind of inventory movement.
     */
    private float containerItemMoveDelay = 0.2f;

    /**
     * If true, use Minecraft's crafting recipe book to place items into
     * the crafting table (should be much faster as it's almost instant)
     * <p>
     * If false, will place items in each slot manually (the original way)
     */
    private boolean useCraftingBookToCraft = true;

    /**
     * If a dropped resource item is further than this from the player, don't pick it up.
     * <p>
     * -1 (or less than 0) to disable.
     */
    private float resourcePickupDropRange = -1;


    /**
     * minimum amount of food to have in the inventory.
     * if we have less food than this value the bot will go pickup some more.
     */
    private int minimumFoodAllowed = 0;


    /**
     * amount of food to collect when the food in inventory
     * is lower than the value of minimumFoodAllowed
     */
    private int foodUnitsToCollect = 0;


    /**
     * Chests are cached for their contents.
     * <p>
     * If the bot is collecting a resource and finds a chest within this range,
     * it will grab the resource from the chest.
     * <p>
     * Set this to 0 to disable chest pickups.
     * <p>
     * Don't set this too high, as the bot will prioritize chests even if the resource
     * is easily accessible now.
     */
    private float resourceChestLocateRange = 500;

    /**
     * Some block resources are by default obtained through non-mining means.
     * Crafting tables for example, are normally crafted using planks.
     * <p>
     * However, if the block resource is found within this range it may be mined first.
     * <p>
     * Set this to 0 to disable this feature
     * (keep in mind, this will not affect blocks like "dirt" and "cobblestone"
     * that can only be obtained through mining)
     * <p>
     * Set this to -1 to ALWAYS mine a block if it's catalogued.
     * This is not recommended. For example, if the bot happens to track a
     * crafting table 10000 blocks away, and it then tries obtaining one
     * it will travel 10000 blocks to mine that table, even if it finds
     * itself in a forest where the wood is abundant.
     */
    private float resourceMineRange = 100;

    /**
     * When going to the nearest chest to store items, the bot may normally
     * dig up dungeons constantly. If this is set to true, the bot will
     * search around each chest to make sure it's not in a dungeon.
     */
    private boolean avoidSearchingDungeonChests = true;

    /**
     * Will ignore mining/interacting with blocks that are BELOW an ocean (in an ocean biome and below y = 64)
     * <p>
     * This is mainly here because alto-clef does NOT know how to deal with oceans
     */
    private boolean avoidOceanBlocks = true;

    /**
     * How close we must be to attack/interact with an entity.
     * 6 works well for singleplayer
     * 4 works better on more restrictive multiplayer servers
     */
    private float entityReachRange = 4;

    /**
     * Before grabbing ANYTHING, get a pickaxe.
     * <p>
     * Will help with navigation as sometimes dropped items will be underground,
     * but this behaviour only makes sense in regular minecraft worlds.
     */
    private boolean collectPickaxeFirst = true;

    /**
     * If set to true, crops broken when collecting food will be replanted.
     */
    private boolean replantCrops = true;

    /**
     * Uses killaura to move mobs away and performs survival moves including:
     * - Running away from hostile mobs when your health is low
     * - Run away from creepers about to blow up
     * - Avoid wither skeletons and other really dangerous mobs
     * - Attempting to dodge arrows and other projectiles
     */
    private boolean mobDefense = true;

    /**
     * Defines how force field behaves when "mobDefense" is set to true.
     * Note that the force field is not here to KILL mobs, but PUSH THEM AWAY.
     * <p>
     * <p>
     * Strategies:
     * <p>
     * FASTEST: All hostiles are attacked at every possible moment, every frame.
     * DELAY: Closest hostile is attacked with a sword when your attack is charged up
     * SMART: Closest hostile is attacked at max every 0.2 seconds.
     * OFF: Off
     */
    private KillAura.Strategy forceFieldStrategy = KillAura.Strategy.SMART;

    /**
     * Only applies if mobDefense is on.
     * <p>
     * If enabled, will attempt to dodge all incoming projectiles
     */
    private boolean dodgeProjectiles = true;

    /**
     * Skeletons and large groups of mobs are a huge pain.
     * <p>
     * With this set to true, the bot may either
     * kill or run away from mobs that stay too close for too long.
     */
    private boolean killOrAvoidAnnoyingHostiles = true;

    /**
     * If enabled, the bot will avoid going underwater if baritone
     * isn't giving the bot movement instructions.
     * <p>
     * Baritone doesn't know how to move underwater so this should cause
     * no problems, but disable it if you want the bot to be able to sink.
     */
    private boolean avoidDrowning = true;

    /**
     * If enabled, the bot will close the open screen (furnace/crafting/chest/whatever) when the bot detects
     * that its look direction has changed OR that it is mining something.
     * <p>
     * This is here to stop the bot from getting stuck in a screen container.
     */
    private boolean autoCloseScreenWhenLookingOrMining = true;

    /**
     * If enabled, will attempt to extinguish ourselves when on fire (and not immune to fire)
     */
    private boolean extinguishSelfWithWater = true;

    /**
     * If true, eat when we're hungry or in danger.
     */
    private boolean autoEat = true;

    /**
     * If true, MLG/No Fall Bucket if we're knocked off course and falling.
     */
    private boolean autoMLGBucket = true;

    /**
     * If true, will automatically reconnect to the last open server if you get disconnected.
     * <p>
     * If disabled, the bot will stop running when you disconnect from a server.
     */
    private boolean autoReconnect = true;

    /**
     * If true, will automatically respawn instantly if you die.
     * <p>
     * If disabled, the bot will stop running when you die.
     */
    private boolean autoRespawn = true;

    /**
     * This setting lets you configure what the bot should do if it needs to go to the nether
     * but can't find a nether portal immediately.
     * <p>
     * Options:
     * BUILD_PORTAL_VANILLA: Builds a nether portal, either with obsidian or with a water bucket and lava pool.
     * GO_TO_HOME_BASE: Travel to the set home coordinates and assume there's a portal there.
     */
    private DefaultGoToDimensionTask.OVERWORLD_TO_NETHER_BEHAVIOUR overworldToNetherBehaviour = DefaultGoToDimensionTask.OVERWORLD_TO_NETHER_BEHAVIOUR.BUILD_PORTAL_VANILLA;

    /**
     * When fast traveling via the nether, walk to our destination if we somehow end up closer than this range in the overworld.
     * We will normally travel well within this range (to within 100 blocks if not within a few), so keep this value ~decently~ large.
     */
    private int netherFastTravelWalkingRange = 600;

    /**
     * If set, will run this command by default when no other commands are running.
     * <p>
     * For example, try setting this to "idle" to make the bot continue surviving/eating/escaping mobs.
     * Or "follow <Your Username>" to follow you when not doing anything.
     * Or "goto <Home base coords>" to return to home base when the bot finishes its work.
     */
    private String idleCommand = "";


    /**
     * If set, will run this command after death.
     * Also {deathmessage} will be replaced with the death message.
     * <p>
     * For example, try setting this to "@goto <Your base coords>" to make the bot go to the base before continuing the task it was given.
     * Or setting it to "I died!" will send the i died message
     * Or setting it to "/back" will execute /back on the server
     * When running altoclef commands, if the prefix is not @, the set prefix should be used for the command and not @
     * You may run multiple commands even commands of differet type by splitting them with " & ". Example: /home & i died with message: {deathmessage} & @get diamond
     */
    private String deathCommand = "";

    /**
     * If we need to throw away something, throw away these items first.
     */
    @JsonSerialize(using = ItemSerializer.class)
    @JsonDeserialize(using = ItemDeserializer.class)
    private List<Item> throwawayItems = Arrays.asList(
            // Overworld junk
            Items.DRIPSTONE_BLOCK,
            Items.ROOTED_DIRT,
            Items.GRAVEL,
            Items.SAND,
            Items.DIORITE,
            Items.ANDESITE,
            Items.GRANITE,
            Items.TUFF,
            Items.COBBLESTONE,
            Items.DIRT,
            Items.COBBLED_DEEPSLATE,
            Items.ACACIA_LEAVES, Items.BIRCH_LEAVES, Items.DARK_OAK_LEAVES, Items.OAK_LEAVES, Items.JUNGLE_LEAVES, Items.SPRUCE_LEAVES,
            // Nether junk, to be fair it's mostly tuned for the "beat game" task
            Items.NETHERRACK,
            Items.MAGMA_BLOCK,
            Items.SOUL_SOIL,
            Items.SOUL_SAND,
            Items.NETHER_BRICKS,
            Items.NETHER_BRICK,
            Items.BASALT,
            Items.BLACKSTONE,
            Items.END_STONE,
            Items.SANDSTONE,
            Items.STONE_BRICKS
    );

    /**
     * How many throwaway blocks to keep as building blocks.
     */
    private int reservedBuildingBlockCount = 64;

    /**
     * If true, items with custom names will be protected/marked as "important"
     * so they won't be thrown away.
     */
    private boolean dontThrowAwayCustomNameItems = true;
    private boolean dontThrowAwayEnchantedItems = true;

    /**
     * If we need to throw away something but we don't have any "throwaway Items",
     * throw away any unimportant item that's not currently needed in our task chain.
     * <p>
     * Careful with this! If true, any item not in "importantItems" is liable to be thrown away.
     */
    private boolean throwAwayUnusedItems = true;

    /**
     * We will NEVER throw away these items.
     * Even if "throwAwayUnusedItems" is true and one of these items is not used in a task.
     */
    @JsonSerialize(using = ItemSerializer.class)
    @JsonDeserialize(using = ItemDeserializer.class)
    private List<Item> importantItems = Streams.concat(
            Stream.of(
                    Items.TOTEM_OF_UNDYING,
                    Items.ENCHANTED_GOLDEN_APPLE,
                    Items.ENDER_EYE,
                    Items.TRIDENT,
                    Items.DIAMOND,
                    Items.DIAMOND_BLOCK,
                    Items.NETHERITE_SCRAP,
                    Items.NETHERITE_INGOT,
                    Items.NETHERITE_BLOCK
            ),
            Stream.of(ItemHelper.DIAMOND_ARMORS),
            Stream.of(ItemHelper.NETHERITE_ARMORS),
            Stream.of(ItemHelper.DIAMOND_TOOLS),
            Stream.of(ItemHelper.NETHERITE_TOOLS),
            // Don't throw away shulker boxes that would be pretty bad lol
            Stream.of(ItemHelper.SHULKER_BOXES)
    ).toList();

    /**
     * If true, a blast furnace will be used in smelting if an item to smelt is applicable.
     */
    private boolean useBlastFurnace = true;

    /**
     * If true, will only accept items found in `supportedFuels` as fuel when smelting.
     * <p>
     * Be careful when setting this to false, as ALL burnable items are liable to be burned
     * if they're not protected (blaze rods, beds, wooden tools, crafting tables etc.)
     */
    private boolean limitFuelsToSupportedFuels = true;

    /**
     * If `limitFuelsToSupportedFuels` is true, will use these items and ONLY these items as smelting fuel.
     */
    @JsonSerialize(using = ItemSerializer.class)
    @JsonDeserialize(using = ItemDeserializer.class)
    private List<Item> supportedFuels = Streams.concat(
            Stream.of(
                    Items.COAL,
                    Items.CHARCOAL
            )
    ).toList();

    /**
     * Where "home base" is for the bot.
     * Some tasks use this value if you tell them to,
     * but don't worry about changing this unless you NEED it.
     */
    private BlockPos homeBasePosition = new BlockPos(0, 64, 0);

    /**
     * These areas will not be mined.
     * Used to prevent griefing, or to define a "spawn protection" zone so
     * the bot doesn't keep trying to break spawn protected blocks.
     * <p>
     * Example: protects two areas. A "spawn" area from (x=-10 z=-10) to (x=10 z=10) and a home base at around (x = 1100, y = 2050)
     * <p>
     * areasToProtect : [
     * {
     * start: "-10, 0, -10",
     * end: "10, 255, 10"
     * },
     * {
     * start: "1000, 50, 2000",
     * end: "1200, 255, 2100"
     * },
     * ],
     */
    private List<BlockRange> areasToProtect = Collections.emptyList();


    //////////////////////////////////////////////////////////////////////////////////////////
    ////////** END SETTINGS w/ COMMENTS **////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////

    public static void load(Consumer<Settings> onReload) {
        ConfigHelper.loadConfig(SETTINGS_PATH, Settings::new, Settings.class, onReload);
    }

    public boolean shouldShowTaskChain() {
        return showTaskChains;
    }

    public boolean shouldHideAllWarningLogs() {
        return hideAllWarningLogs;
    }

    public String getCommandPrefix() {
        return commandPrefix;
    }

    public String getChatLogPrefix() {
        return chatLogPrefix;
    }

    public boolean shouldShowTimer() {
        return showTimer;
    }

    public float getResourcePickupRange() {
        return resourcePickupDropRange;
    }

    public float getResourceChestLocateRange() {
        return resourceChestLocateRange;
    }

    public float getResourceMineRange() {
        return resourceMineRange;
    }

    public float getContainerItemMoveDelay() {
        return containerItemMoveDelay;
    }

    public boolean shouldUseCraftingBookToCraft() {
        return useCraftingBookToCraft;
    }

    public int getFoodUnitsToCollect() {
        return foodUnitsToCollect;
    }

    public int getMinimumFoodAllowed() {
        return minimumFoodAllowed;
    }

    public boolean isMobDefense() {
        return mobDefense;
    }

    public boolean isDodgeProjectiles() {
        return dodgeProjectiles;
    }

    public boolean isAutoEat() {
        return autoEat;
    }

    public boolean isAutoReconnect() {
        return autoReconnect;
    }

    public boolean isAutoRespawn() {
        return autoRespawn;
    }

    public boolean shouldReplantCrops() {
        return replantCrops;
    }

    public boolean shouldDealWithAnnoyingHostiles() {
        return killOrAvoidAnnoyingHostiles;
    }

    public KillAura.Strategy getForceFieldStrategy() {
        return forceFieldStrategy;
    }

    public String getIdleCommand() {
        return idleCommand;
    }

    public String getDeathCommand() {
        return deathCommand;
    }

    public boolean shouldRunIdleCommandWhenNotActive() {
        return idleCommand != null && !idleCommand.isBlank();
    }

    public boolean shouldAutoMLGBucket() {
        return autoMLGBucket;
    }

    public boolean shouldCollectPickaxeFirst() {
        return collectPickaxeFirst;
    }

    public boolean shouldAvoidDrowning() {
        return avoidDrowning;
    }

    public boolean shouldCloseScreenWhenLookingOrMining() {
        return autoCloseScreenWhenLookingOrMining;
    }

    public boolean shouldExtinguishSelfWithWater() {
        return extinguishSelfWithWater;
    }

    public boolean shouldAvoidSearchingForDungeonChests() {
        return avoidSearchingDungeonChests;
    }

    public boolean shouldAvoidOcean() {
        return avoidOceanBlocks;
    }

    public boolean isThrowaway(Item item) {
        return throwawayItems.contains(item);
    }

    public boolean isImportant(Item item) {
        return importantItems.contains(item);
    }

    public boolean shouldThrowawayUnusedItems() {
        return this.throwAwayUnusedItems;
    }

    public int getReservedBuildingBlockCount() {
        return this.reservedBuildingBlockCount;
    }

    public boolean getDontThrowAwayCustomNameItems() {
        return this.dontThrowAwayCustomNameItems;
    }

    public boolean getDontThrowAwayEnchantedItems() {
        return this.dontThrowAwayEnchantedItems;
    }

    public float getEntityReachRange() {
        return entityReachRange;
    }

    public Item[] getThrowawayItems(AltoClef mod, boolean includeProtected) {
        return throwawayItems.stream().filter(item -> includeProtected || !mod.getBehaviour().isProtected(item)).toArray(Item[]::new);
    }

    public Item[] getThrowawayItems(AltoClef mod) {
        return getThrowawayItems(mod, false);
    }

    public boolean shouldLimitFuelsToSupportedFuels() {
        return limitFuelsToSupportedFuels;
    }

    public boolean shouldUseBlastFurnace() {
        return useBlastFurnace;
    }

    public boolean isSupportedFuel(Item item) {
        return !limitFuelsToSupportedFuels || supportedFuels.contains(item);
    }

    @JsonIgnore
    public Item[] getSupportedFuelItems() {
        return supportedFuels.toArray(Item[]::new);
    }

    public boolean isPositionExplicitlyProtected(BlockPos pos) {
        if (!areasToProtect.isEmpty()) {
            for (BlockRange protection : areasToProtect) {
                if (protection.contains(pos)) return true;
            }
        }
        return false;
    }

    public DefaultGoToDimensionTask.OVERWORLD_TO_NETHER_BEHAVIOUR getOverworldToNetherBehaviour() {
        return overworldToNetherBehaviour;
    }

    public int getNetherFastTravelWalkingRange() {
        return netherFastTravelWalkingRange;
    }

    public BlockPos getHomeBasePosition() {
        return homeBasePosition;
    }

    @Override
    public void onFailLoad() {
        _failedToLoad = true;
    }

    @Override
    public boolean failedToLoad() {
        return _failedToLoad;
    }
}
