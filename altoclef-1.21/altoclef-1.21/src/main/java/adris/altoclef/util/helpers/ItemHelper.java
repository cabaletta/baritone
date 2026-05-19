package adris.altoclef.util.helpers;

import adris.altoclef.AltoClef;
import adris.altoclef.util.WoodType;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.MapColor;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.DyeColor;

import java.util.*;

/**
 * Helper functions and definitions for useful groupings of items
 */
public class ItemHelper {
    public static final Item[] COPPER_BLOCKS = new Item[]{Items.COPPER_BLOCK, Items.EXPOSED_COPPER,
            Items.WEATHERED_COPPER, Items.OXIDIZED_COPPER, Items.CUT_COPPER, Items.EXPOSED_CUT_COPPER,
            Items.WEATHERED_CUT_COPPER, Items.OXIDIZED_CUT_COPPER, Items.WAXED_COPPER_BLOCK,
            Items.WAXED_EXPOSED_COPPER, Items.WAXED_WEATHERED_COPPER, Items.WAXED_OXIDIZED_COPPER,
            Items.WAXED_CUT_COPPER, Items.WAXED_EXPOSED_CUT_COPPER, Items.WAXED_WEATHERED_CUT_COPPER,
            Items.WAXED_OXIDIZED_CUT_COPPER};
    public static final Item[] SAPLINGS = new Item[]{Items.OAK_SAPLING, Items.SPRUCE_SAPLING, Items.BIRCH_SAPLING,
            Items.JUNGLE_SAPLING, Items.ACACIA_SAPLING, Items.DARK_OAK_SAPLING, Items.MANGROVE_PROPAGULE,
            Items.CHERRY_SAPLING};
    public static final Block[] SAPLING_SOURCES = new Block[]{Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES,
            Blocks.BIRCH_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES,
            Blocks.MANGROVE_PROPAGULE, Blocks.CHERRY_LEAVES};
    public static final Item[] HOSTILE_MOB_DROPS = new Item[]{Items.BLAZE_ROD, Items.FEATHER, Items.CHICKEN,
            Items.COOKED_CHICKEN, Items.ROTTEN_FLESH, Items.ZOMBIE_HEAD, Items.GUNPOWDER, Items.CREEPER_HEAD,
            Items.TOTEM_OF_UNDYING, Items.EMERALD, Items.PORKCHOP, Items.COOKED_PORKCHOP, Items.LEATHER,
            Items.MAGMA_CREAM, Items.PHANTOM_MEMBRANE, Items.ARROW, Items.SADDLE, Items.SHULKER_SHELL, Items.BONE,
            Items.SKELETON_SKULL, Items.SLIME_BALL, Items.STRING, Items.SPIDER_EYE, Items.SCULK_CATALYST,
            Items.GLASS_BOTTLE, Items.GLOWSTONE_DUST, Items.REDSTONE, Items.STICK, Items.SUGAR, Items.POTION,
            Items.NETHER_STAR, Items.COAL, Items.WITHER_SKELETON_SKULL, Items.GHAST_TEAR, Items.IRON_INGOT,
            Items.CARROT, Items.POTATO, Items.BAKED_POTATO, Items.COPPER_INGOT};
    public static final Item[] DIRTS = new Item[]{Items.DIRT, Items.DIRT_PATH, Items.COARSE_DIRT, Items.ROOTED_DIRT};
    public static final Item[] PLANKS = new Item[]{Items.ACACIA_PLANKS, Items.BIRCH_PLANKS, Items.CRIMSON_PLANKS,
            Items.DARK_OAK_PLANKS, Items.OAK_PLANKS, Items.JUNGLE_PLANKS, Items.SPRUCE_PLANKS, Items.WARPED_PLANKS,
            Items.MANGROVE_PLANKS, Items.CHERRY_PLANKS, Items.BAMBOO_PLANKS, Items.CHERRY_PLANKS};
    public static final Item[] LEAVES = new Item[]{Items.ACACIA_LEAVES, Items.BIRCH_LEAVES, Items.DARK_OAK_LEAVES,
            Items.OAK_LEAVES, Items.JUNGLE_LEAVES, Items.SPRUCE_LEAVES, Items.MANGROVE_LEAVES, Items.CHERRY_LEAVES};
    public static final Item[] WOOD = new Item[]{Items.ACACIA_WOOD, Items.BIRCH_WOOD, Items.CRIMSON_HYPHAE,
            Items.DARK_OAK_WOOD, Items.OAK_WOOD, Items.JUNGLE_WOOD, Items.SPRUCE_WOOD, Items.WARPED_HYPHAE,
            Items.MANGROVE_WOOD};
    public static final Item[] WOOD_BUTTON = new Item[]{Items.ACACIA_BUTTON, Items.BIRCH_BUTTON, Items.CRIMSON_BUTTON,
            Items.DARK_OAK_BUTTON, Items.OAK_BUTTON, Items.JUNGLE_BUTTON, Items.SPRUCE_BUTTON, Items.WARPED_BUTTON,
            Items.MANGROVE_BUTTON, Items.BAMBOO_BUTTON, Items.CHERRY_BUTTON};
    public static final Item[] WOOD_SIGN = new Item[]{Items.ACACIA_SIGN, Items.BIRCH_SIGN, Items.CRIMSON_SIGN,
            Items.DARK_OAK_SIGN, Items.OAK_SIGN, Items.JUNGLE_SIGN, Items.SPRUCE_SIGN, Items.WARPED_SIGN,
            Items.MANGROVE_SIGN, Items.BAMBOO_SIGN, Items.CHERRY_SIGN};
    public static final Item[] WOOD_HANGING_SIGN = new Item[]{Items.ACACIA_HANGING_SIGN, Items.BIRCH_HANGING_SIGN,
            Items.CRIMSON_HANGING_SIGN, Items.DARK_OAK_HANGING_SIGN, Items.OAK_HANGING_SIGN, Items.JUNGLE_HANGING_SIGN,
            Items.SPRUCE_HANGING_SIGN, Items.WARPED_HANGING_SIGN, Items.MANGROVE_HANGING_SIGN, Items.BAMBOO_HANGING_SIGN,
            Items.CHERRY_HANGING_SIGN};
    public static final Item[] WOOD_PRESSURE_PLATE = new Item[]{Items.ACACIA_PRESSURE_PLATE, Items.BIRCH_PRESSURE_PLATE,
            Items.CRIMSON_PRESSURE_PLATE, Items.DARK_OAK_PRESSURE_PLATE, Items.OAK_PRESSURE_PLATE,
            Items.JUNGLE_PRESSURE_PLATE, Items.SPRUCE_PRESSURE_PLATE, Items.WARPED_PRESSURE_PLATE,
            Items.MANGROVE_PRESSURE_PLATE, Items.BAMBOO_PRESSURE_PLATE, Items.CHERRY_PRESSURE_PLATE};
    public static final Item[] WOOD_FENCE = new Item[]{Items.ACACIA_FENCE, Items.BIRCH_FENCE, Items.DARK_OAK_FENCE,
            Items.OAK_FENCE, Items.JUNGLE_FENCE, Items.SPRUCE_FENCE, Items.CRIMSON_FENCE, Items.WARPED_FENCE,
            Items.MANGROVE_FENCE, Items.BAMBOO_FENCE, Items.CHERRY_FENCE};
    public static final Item[] WOOD_FENCE_GATE = new Item[]{Items.ACACIA_FENCE_GATE, Items.BIRCH_FENCE_GATE,
            Items.DARK_OAK_FENCE_GATE, Items.OAK_FENCE_GATE, Items.JUNGLE_FENCE_GATE, Items.SPRUCE_FENCE_GATE,
            Items.CRIMSON_FENCE_GATE, Items.WARPED_FENCE_GATE, Items.MANGROVE_FENCE_GATE, Items.BAMBOO_FENCE_GATE,
            Items.CHERRY_FENCE_GATE};
    public static final Item[] WOOD_BOAT = new Item[]{Items.ACACIA_BOAT, Items.BIRCH_BOAT, Items.DARK_OAK_BOAT,
            Items.OAK_BOAT, Items.JUNGLE_BOAT, Items.SPRUCE_BOAT, Items.MANGROVE_BOAT, Items.CHERRY_BOAT};
    public static final Item[] WOOD_DOOR = new Item[]{Items.ACACIA_DOOR, Items.BIRCH_DOOR, Items.CRIMSON_DOOR,
            Items.DARK_OAK_DOOR, Items.OAK_DOOR, Items.JUNGLE_DOOR, Items.SPRUCE_DOOR, Items.WARPED_DOOR,
            Items.MANGROVE_DOOR, Items.BAMBOO_DOOR, Items.CHERRY_DOOR};
    public static final Item[] WOOD_SLAB = new Item[]{Items.ACACIA_SLAB, Items.BIRCH_SLAB, Items.CRIMSON_SLAB,
            Items.DARK_OAK_SLAB, Items.OAK_SLAB, Items.JUNGLE_SLAB, Items.SPRUCE_SLAB, Items.WARPED_SLAB,
            Items.MANGROVE_SLAB, Items.BAMBOO_SLAB, Items.CHERRY_SLAB};
    public static final Item[] WOOD_STAIRS = new Item[]{Items.ACACIA_STAIRS, Items.BIRCH_STAIRS, Items.CRIMSON_STAIRS,
            Items.DARK_OAK_STAIRS, Items.OAK_STAIRS, Items.JUNGLE_STAIRS, Items.SPRUCE_STAIRS, Items.WARPED_STAIRS,
            Items.MANGROVE_STAIRS, Items.BAMBOO_STAIRS, Items.CHERRY_STAIRS};
    public static final Item[] WOOD_TRAPDOOR = new Item[]{Items.ACACIA_TRAPDOOR, Items.BIRCH_TRAPDOOR,
            Items.CRIMSON_TRAPDOOR, Items.DARK_OAK_TRAPDOOR, Items.OAK_TRAPDOOR, Items.JUNGLE_TRAPDOOR,
            Items.SPRUCE_TRAPDOOR, Items.WARPED_TRAPDOOR, Items.MANGROVE_TRAPDOOR, Items.BAMBOO_TRAPDOOR,
            Items.CHERRY_TRAPDOOR};
    public static final Item[] LOG = new Item[]{Items.ACACIA_LOG, Items.BIRCH_LOG, Items.DARK_OAK_LOG, Items.OAK_LOG, Items.JUNGLE_LOG, Items.SPRUCE_LOG,
            Items.ACACIA_WOOD, Items.BIRCH_WOOD, Items.DARK_OAK_WOOD, Items.OAK_WOOD, Items.JUNGLE_WOOD, Items.SPRUCE_WOOD,
            Items.STRIPPED_ACACIA_LOG, Items.STRIPPED_BIRCH_LOG, Items.STRIPPED_DARK_OAK_LOG, Items.STRIPPED_OAK_LOG, Items.STRIPPED_JUNGLE_LOG, Items.STRIPPED_SPRUCE_LOG,
            Items.STRIPPED_ACACIA_WOOD, Items.STRIPPED_BIRCH_WOOD, Items.STRIPPED_DARK_OAK_WOOD, Items.STRIPPED_OAK_WOOD, Items.STRIPPED_JUNGLE_WOOD, Items.STRIPPED_SPRUCE_WOOD,
            Items.CRIMSON_STEM, Items.WARPED_STEM, Items.CRIMSON_HYPHAE, Items.WARPED_HYPHAE,
            Items.STRIPPED_CRIMSON_STEM, Items.STRIPPED_WARPED_STEM, Items.STRIPPED_CRIMSON_HYPHAE,
            Items.STRIPPED_WARPED_HYPHAE, Items.MANGROVE_LOG, Items.MANGROVE_WOOD, Items.STRIPPED_MANGROVE_LOG,
            Items.STRIPPED_MANGROVE_WOOD, Items.CHERRY_LOG, Items.CHERRY_WOOD, Items.STRIPPED_CHERRY_LOG,
            Items.STRIPPED_CHERRY_WOOD};
    public static final Item[] STRIPPED_LOGS = new Item[]{Items.STRIPPED_ACACIA_LOG, Items.STRIPPED_BIRCH_LOG,
            Items.STRIPPED_DARK_OAK_LOG, Items.STRIPPED_OAK_LOG, Items.STRIPPED_JUNGLE_LOG, Items.STRIPPED_SPRUCE_LOG,
            Items.STRIPPED_CRIMSON_STEM, Items.STRIPPED_WARPED_STEM, Items.STRIPPED_MANGROVE_LOG,
            Items.STRIPPED_CHERRY_LOG};
    public static final Item[] STRIPPABLE_LOGS = new Item[]{Items.ACACIA_LOG, Items.BIRCH_LOG, Items.DARK_OAK_LOG,
            Items.OAK_LOG, Items.JUNGLE_LOG, Items.SPRUCE_LOG, Items.CRIMSON_STEM, Items.WARPED_STEM, Items.MANGROVE_LOG,
            Items.CHERRY_LOG};
    public static final Item[] DYE = new Item[]{Items.WHITE_DYE, Items.BLACK_DYE, Items.BLUE_DYE, Items.BROWN_DYE, Items.CYAN_DYE, Items.GRAY_DYE, Items.GREEN_DYE, Items.LIGHT_BLUE_DYE, Items.LIGHT_GRAY_DYE, Items.LIME_DYE, Items.MAGENTA_DYE, Items.ORANGE_DYE, Items.PINK_DYE, Items.PURPLE_DYE, Items.RED_DYE, Items.YELLOW_DYE};
    public static final Item[] WOOL = new Item[]{Items.WHITE_WOOL, Items.BLACK_WOOL, Items.BLUE_WOOL, Items.BROWN_WOOL, Items.CYAN_WOOL, Items.GRAY_WOOL, Items.GREEN_WOOL, Items.LIGHT_BLUE_WOOL, Items.LIGHT_GRAY_WOOL, Items.LIME_WOOL, Items.MAGENTA_WOOL, Items.ORANGE_WOOL, Items.PINK_WOOL, Items.PURPLE_WOOL, Items.RED_WOOL, Items.YELLOW_WOOL};
    public static final Item[] BED = new Item[]{Items.WHITE_BED, Items.BLACK_BED, Items.BLUE_BED, Items.BROWN_BED, Items.CYAN_BED, Items.GRAY_BED, Items.GREEN_BED, Items.LIGHT_BLUE_BED, Items.LIGHT_GRAY_BED, Items.LIME_BED, Items.MAGENTA_BED, Items.ORANGE_BED, Items.PINK_BED, Items.PURPLE_BED, Items.RED_BED, Items.YELLOW_BED};
    public static final Item[] CARPET = new Item[]{Items.WHITE_CARPET, Items.BLACK_CARPET, Items.BLUE_CARPET, Items.BROWN_CARPET, Items.CYAN_CARPET, Items.GRAY_CARPET, Items.GREEN_CARPET, Items.LIGHT_BLUE_CARPET, Items.LIGHT_GRAY_CARPET, Items.LIME_CARPET, Items.MAGENTA_CARPET, Items.ORANGE_CARPET, Items.PINK_CARPET, Items.PURPLE_CARPET, Items.RED_CARPET, Items.YELLOW_CARPET};
    public static final Item[] SHULKER_BOXES = new Item[]{Items.WHITE_SHULKER_BOX, Items.BLACK_SHULKER_BOX, Items.BLUE_SHULKER_BOX, Items.BROWN_SHULKER_BOX, Items.CYAN_SHULKER_BOX, Items.GRAY_SHULKER_BOX, Items.GREEN_SHULKER_BOX, Items.LIGHT_BLUE_SHULKER_BOX, Items.LIGHT_GRAY_SHULKER_BOX, Items.LIME_SHULKER_BOX, Items.MAGENTA_SHULKER_BOX, Items.ORANGE_SHULKER_BOX, Items.PINK_SHULKER_BOX, Items.PURPLE_SHULKER_BOX, Items.RED_SHULKER_BOX, Items.YELLOW_SHULKER_BOX};
    public static final Item[] FLOWER = new Item[]{Items.ALLIUM, Items.AZURE_BLUET, Items.BLUE_ORCHID, Items.CORNFLOWER, Items.DANDELION, Items.LILAC, Items.LILY_OF_THE_VALLEY, Items.ORANGE_TULIP, Items.OXEYE_DAISY, Items.PINK_TULIP, Items.POPPY, Items.PEONY, Items.RED_TULIP, Items.ROSE_BUSH, Items.SUNFLOWER, Items.WHITE_TULIP};
    public static final Item[] LEATHER_ARMORS = new Item[]{Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_HELMET, Items.LEATHER_BOOTS};
    public static final Item[] GOLDEN_ARMORS = new Item[]{Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_HELMET, Items.GOLDEN_BOOTS};
    public static final Item[] IRON_ARMORS = new Item[]{Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_HELMET, Items.IRON_BOOTS};
    public static final Item[] DIAMOND_ARMORS = new Item[]{Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_HELMET, Items.DIAMOND_BOOTS};
    public static final Item[] NETHERITE_ARMORS = new Item[]{Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_HELMET, Items.NETHERITE_BOOTS};
    public static final Item[] WOODEN_TOOLS = new Item[]{Items.WOODEN_PICKAXE, Items.WOODEN_SHOVEL, Items.WOODEN_SWORD, Items.WOODEN_AXE, Items.WOODEN_HOE};
    public static final Item[] STONE_TOOLS = new Item[]{Items.STONE_PICKAXE, Items.STONE_SHOVEL, Items.STONE_SWORD, Items.STONE_AXE, Items.STONE_HOE};
    public static final Item[] IRON_TOOLS = new Item[]{Items.IRON_PICKAXE, Items.IRON_SHOVEL, Items.IRON_SWORD, Items.IRON_AXE, Items.IRON_HOE};
    public static final Item[] GOLDEN_TOOLS = new Item[]{Items.GOLDEN_PICKAXE, Items.GOLDEN_SHOVEL, Items.GOLDEN_SWORD, Items.GOLDEN_AXE, Items.GOLDEN_HOE};
    public static final Item[] DIAMOND_TOOLS = new Item[]{Items.DIAMOND_PICKAXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.DIAMOND_HOE};
    public static final Item[] NETHERITE_TOOLS = new Item[]{Items.NETHERITE_PICKAXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_SWORD, Items.NETHERITE_AXE, Items.NETHERITE_HOE};
    public static final Block[] WOOD_SIGNS_ALL = new Block[]{Blocks.ACACIA_SIGN, Blocks.BIRCH_SIGN, Blocks.DARK_OAK_SIGN,
            Blocks.OAK_SIGN, Blocks.JUNGLE_SIGN, Blocks.SPRUCE_SIGN, Blocks.ACACIA_WALL_SIGN, Blocks.BIRCH_WALL_SIGN,
            Blocks.DARK_OAK_WALL_SIGN, Blocks.OAK_WALL_SIGN, Blocks.JUNGLE_WALL_SIGN, Blocks.SPRUCE_WALL_SIGN,
            Blocks.MANGROVE_SIGN, Blocks.MANGROVE_WALL_SIGN, Blocks.BAMBOO_SIGN, Blocks.BAMBOO_WALL_SIGN,
            Blocks.CHERRY_SIGN, Blocks.CHERRY_WALL_SIGN};
    public static final Block[] WOOD_HANGING_SIGNS_ALL = new Block[]{Blocks.ACACIA_HANGING_SIGN,
            Blocks.ACACIA_WALL_HANGING_SIGN, Blocks.BIRCH_HANGING_SIGN, Blocks.BIRCH_WALL_HANGING_SIGN,
            Blocks.DARK_OAK_HANGING_SIGN, Blocks.DARK_OAK_WALL_HANGING_SIGN, Blocks.OAK_HANGING_SIGN,
            Blocks.OAK_WALL_HANGING_SIGN, Blocks.JUNGLE_HANGING_SIGN, Blocks.JUNGLE_WALL_HANGING_SIGN,
            Blocks.SPRUCE_HANGING_SIGN, Blocks.SPRUCE_WALL_HANGING_SIGN, Blocks.MANGROVE_HANGING_SIGN,
            Blocks.MANGROVE_WALL_HANGING_SIGN, Blocks.BAMBOO_HANGING_SIGN, Blocks.BAMBOO_WALL_HANGING_SIGN,
            Blocks.CHERRY_HANGING_SIGN, Blocks.CHERRY_WALL_HANGING_SIGN};
    private static final Map<Item, Item> _logToPlanks = new HashMap<>() {
        {
            put(Items.CHERRY_LOG, Items.CHERRY_PLANKS);
            put(Items.CHERRY_WOOD, Items.CHERRY_PLANKS);
            put(Items.STRIPPED_CHERRY_LOG, Items.CHERRY_PLANKS);
            put(Items.STRIPPED_CHERRY_WOOD, Items.CHERRY_PLANKS);
            put(Items.MANGROVE_LOG, Items.MANGROVE_PLANKS);
            put(Items.MANGROVE_WOOD, Items.MANGROVE_PLANKS);
            put(Items.STRIPPED_MANGROVE_LOG, Items.MANGROVE_PLANKS);
            put(Items.STRIPPED_MANGROVE_WOOD, Items.MANGROVE_PLANKS);
            put(Items.ACACIA_LOG, Items.ACACIA_PLANKS);
            put(Items.BIRCH_LOG, Items.BIRCH_PLANKS);
            put(Items.CRIMSON_STEM, Items.CRIMSON_PLANKS);
            put(Items.DARK_OAK_LOG, Items.DARK_OAK_PLANKS);
            put(Items.OAK_LOG, Items.OAK_PLANKS);
            put(Items.JUNGLE_LOG, Items.JUNGLE_PLANKS);
            put(Items.SPRUCE_LOG, Items.SPRUCE_PLANKS);
            put(Items.WARPED_STEM, Items.WARPED_PLANKS);
            put(Items.STRIPPED_ACACIA_LOG, Items.ACACIA_PLANKS);
            put(Items.STRIPPED_BIRCH_LOG, Items.BIRCH_PLANKS);
            put(Items.STRIPPED_CRIMSON_STEM, Items.CRIMSON_PLANKS);
            put(Items.STRIPPED_DARK_OAK_LOG, Items.DARK_OAK_PLANKS);
            put(Items.STRIPPED_OAK_LOG, Items.OAK_PLANKS);
            put(Items.STRIPPED_JUNGLE_LOG, Items.JUNGLE_PLANKS);
            put(Items.STRIPPED_SPRUCE_LOG, Items.SPRUCE_PLANKS);
            put(Items.STRIPPED_WARPED_STEM, Items.WARPED_PLANKS);
            put(Items.ACACIA_WOOD, Items.ACACIA_PLANKS);
            put(Items.BIRCH_WOOD, Items.BIRCH_PLANKS);
            put(Items.CRIMSON_HYPHAE, Items.CRIMSON_PLANKS);
            put(Items.DARK_OAK_WOOD, Items.DARK_OAK_PLANKS);
            put(Items.OAK_WOOD, Items.OAK_PLANKS);
            put(Items.JUNGLE_WOOD, Items.JUNGLE_PLANKS);
            put(Items.SPRUCE_WOOD, Items.SPRUCE_PLANKS);
            put(Items.WARPED_HYPHAE, Items.WARPED_PLANKS);
            put(Items.STRIPPED_ACACIA_WOOD, Items.ACACIA_PLANKS);
            put(Items.STRIPPED_BIRCH_WOOD, Items.BIRCH_PLANKS);
            put(Items.STRIPPED_CRIMSON_HYPHAE, Items.CRIMSON_PLANKS);
            put(Items.STRIPPED_DARK_OAK_WOOD, Items.DARK_OAK_PLANKS);
            put(Items.STRIPPED_OAK_WOOD, Items.OAK_PLANKS);
            put(Items.STRIPPED_JUNGLE_WOOD, Items.JUNGLE_PLANKS);
            put(Items.STRIPPED_SPRUCE_WOOD, Items.SPRUCE_PLANKS);
            put(Items.STRIPPED_WARPED_HYPHAE, Items.WARPED_PLANKS);
        }
    };
    private static final Map<Item, Item> _planksToLogs = new HashMap<>() {
        {
            put(Items.CHERRY_PLANKS, Items.CHERRY_LOG);
            put(Items.MANGROVE_PLANKS, Items.MANGROVE_LOG);
            put(Items.ACACIA_PLANKS, Items.ACACIA_LOG);
            put(Items.BIRCH_PLANKS, Items.BIRCH_LOG);
            put(Items.CRIMSON_PLANKS, Items.CRIMSON_STEM);
            put(Items.DARK_OAK_PLANKS, Items.DARK_OAK_LOG);
            put(Items.OAK_PLANKS, Items.OAK_LOG);
            put(Items.JUNGLE_PLANKS, Items.JUNGLE_LOG);
            put(Items.SPRUCE_PLANKS, Items.SPRUCE_LOG);
            put(Items.WARPED_PLANKS, Items.WARPED_STEM);
        }
    };
    private static final Map<Item, Item> _strippedToLogs = new HashMap<>() {
        {
            put(Items.STRIPPED_CHERRY_LOG, Items.CHERRY_LOG);
            put(Items.STRIPPED_MANGROVE_LOG, Items.MANGROVE_LOG);
            put(Items.STRIPPED_ACACIA_LOG, Items.ACACIA_LOG);
            put(Items.STRIPPED_BIRCH_LOG, Items.BIRCH_LOG);
            put(Items.STRIPPED_CRIMSON_STEM, Items.CRIMSON_STEM);
            put(Items.STRIPPED_DARK_OAK_LOG, Items.DARK_OAK_LOG);
            put(Items.STRIPPED_OAK_LOG, Items.OAK_LOG);
            put(Items.STRIPPED_JUNGLE_LOG, Items.JUNGLE_LOG);
            put(Items.STRIPPED_SPRUCE_LOG, Items.SPRUCE_LOG);
            put(Items.STRIPPED_WARPED_STEM, Items.WARPED_STEM);
        }
    };
    // This is kinda jank ngl
    private static final Map<MapColor, ColorfulItems> _colorMap = new HashMap<MapColor, ColorfulItems>() {
        {
            p(DyeColor.RED, "red", Items.RED_DYE, Items.RED_WOOL, Items.RED_BED, Items.RED_CARPET, Items.RED_STAINED_GLASS, Items.RED_STAINED_GLASS_PANE, Items.RED_TERRACOTTA, Items.RED_GLAZED_TERRACOTTA, Items.RED_CONCRETE, Items.RED_CONCRETE_POWDER, Items.RED_BANNER, Items.RED_SHULKER_BOX, Blocks.RED_WALL_BANNER);
            p(DyeColor.WHITE, "white", Items.WHITE_DYE, Items.WHITE_WOOL, Items.WHITE_BED, Items.WHITE_CARPET, Items.WHITE_STAINED_GLASS, Items.WHITE_STAINED_GLASS_PANE, Items.WHITE_TERRACOTTA, Items.WHITE_GLAZED_TERRACOTTA, Items.WHITE_CONCRETE, Items.WHITE_CONCRETE_POWDER, Items.WHITE_BANNER, Items.WHITE_SHULKER_BOX, Blocks.WHITE_WALL_BANNER);
            p(DyeColor.BLACK, "black", Items.BLACK_DYE, Items.BLACK_WOOL, Items.BLACK_BED, Items.BLACK_CARPET, Items.BLACK_STAINED_GLASS, Items.BLACK_STAINED_GLASS_PANE, Items.BLACK_TERRACOTTA, Items.BLACK_GLAZED_TERRACOTTA, Items.BLACK_CONCRETE, Items.BLACK_CONCRETE_POWDER, Items.BLACK_BANNER, Items.BLACK_SHULKER_BOX, Blocks.BLACK_WALL_BANNER);
            p(DyeColor.BLUE, "blue", Items.BLUE_DYE, Items.BLUE_WOOL, Items.BLUE_BED, Items.BLUE_CARPET, Items.BLUE_STAINED_GLASS, Items.BLUE_STAINED_GLASS_PANE, Items.BLUE_TERRACOTTA, Items.BLUE_GLAZED_TERRACOTTA, Items.BLUE_CONCRETE, Items.BLUE_CONCRETE_POWDER, Items.BLUE_BANNER, Items.BLUE_SHULKER_BOX, Blocks.BLUE_WALL_BANNER);
            p(DyeColor.BROWN, "brown", Items.BROWN_DYE, Items.BROWN_WOOL, Items.BROWN_BED, Items.BROWN_CARPET, Items.BROWN_STAINED_GLASS, Items.BROWN_STAINED_GLASS_PANE, Items.BROWN_TERRACOTTA, Items.BROWN_GLAZED_TERRACOTTA, Items.BROWN_CONCRETE, Items.BROWN_CONCRETE_POWDER, Items.BROWN_BANNER, Items.BROWN_SHULKER_BOX, Blocks.BROWN_WALL_BANNER);
            p(DyeColor.CYAN, "cyan", Items.CYAN_DYE, Items.CYAN_WOOL, Items.CYAN_BED, Items.CYAN_CARPET, Items.CYAN_STAINED_GLASS, Items.CYAN_STAINED_GLASS_PANE, Items.CYAN_TERRACOTTA, Items.CYAN_GLAZED_TERRACOTTA, Items.CYAN_CONCRETE, Items.CYAN_CONCRETE_POWDER, Items.CYAN_BANNER, Items.CYAN_SHULKER_BOX, Blocks.CYAN_WALL_BANNER);
            p(DyeColor.GRAY, "gray", Items.GRAY_DYE, Items.GRAY_WOOL, Items.GRAY_BED, Items.GRAY_CARPET, Items.GRAY_STAINED_GLASS, Items.GRAY_STAINED_GLASS_PANE, Items.GRAY_TERRACOTTA, Items.GRAY_GLAZED_TERRACOTTA, Items.GRAY_CONCRETE, Items.GRAY_CONCRETE_POWDER, Items.GRAY_BANNER, Items.GRAY_SHULKER_BOX, Blocks.GRAY_WALL_BANNER);
            p(DyeColor.GREEN, "green", Items.GREEN_DYE, Items.GREEN_WOOL, Items.GREEN_BED, Items.GREEN_CARPET, Items.GREEN_STAINED_GLASS, Items.GREEN_STAINED_GLASS_PANE, Items.GREEN_TERRACOTTA, Items.GREEN_GLAZED_TERRACOTTA, Items.GREEN_CONCRETE, Items.GREEN_CONCRETE_POWDER, Items.GREEN_BANNER, Items.GREEN_SHULKER_BOX, Blocks.GREEN_WALL_BANNER);
            p(DyeColor.LIGHT_BLUE, "light_blue", Items.LIGHT_BLUE_DYE, Items.LIGHT_BLUE_WOOL, Items.LIGHT_BLUE_BED, Items.LIGHT_BLUE_CARPET, Items.LIGHT_BLUE_STAINED_GLASS, Items.LIGHT_BLUE_STAINED_GLASS_PANE, Items.LIGHT_BLUE_TERRACOTTA, Items.LIGHT_BLUE_GLAZED_TERRACOTTA, Items.LIGHT_BLUE_CONCRETE, Items.LIGHT_BLUE_CONCRETE_POWDER, Items.LIGHT_BLUE_BANNER, Items.LIGHT_BLUE_SHULKER_BOX, Blocks.LIGHT_BLUE_WALL_BANNER);
            p(DyeColor.LIGHT_GRAY, "light_gray", Items.LIGHT_GRAY_DYE, Items.LIGHT_GRAY_WOOL, Items.LIGHT_GRAY_BED, Items.LIGHT_GRAY_CARPET, Items.LIGHT_GRAY_STAINED_GLASS, Items.LIGHT_GRAY_STAINED_GLASS_PANE, Items.LIGHT_GRAY_TERRACOTTA, Items.LIGHT_GRAY_GLAZED_TERRACOTTA, Items.LIGHT_GRAY_CONCRETE, Items.LIGHT_GRAY_CONCRETE_POWDER, Items.LIGHT_GRAY_BANNER, Items.LIGHT_GRAY_SHULKER_BOX, Blocks.LIGHT_GRAY_WALL_BANNER);
            p(DyeColor.LIME, "lime", Items.LIME_DYE, Items.LIME_WOOL, Items.LIME_BED, Items.LIME_CARPET, Items.LIME_STAINED_GLASS, Items.LIME_STAINED_GLASS_PANE, Items.LIME_TERRACOTTA, Items.LIME_GLAZED_TERRACOTTA, Items.LIME_CONCRETE, Items.LIME_CONCRETE_POWDER, Items.LIME_BANNER, Items.LIME_SHULKER_BOX, Blocks.LIME_WALL_BANNER);
            p(DyeColor.MAGENTA, "magenta", Items.MAGENTA_DYE, Items.MAGENTA_WOOL, Items.MAGENTA_BED, Items.MAGENTA_CARPET, Items.MAGENTA_STAINED_GLASS, Items.MAGENTA_STAINED_GLASS_PANE, Items.MAGENTA_TERRACOTTA, Items.MAGENTA_GLAZED_TERRACOTTA, Items.MAGENTA_CONCRETE, Items.MAGENTA_CONCRETE_POWDER, Items.MAGENTA_BANNER, Items.MAGENTA_SHULKER_BOX, Blocks.MAGENTA_WALL_BANNER);
            p(DyeColor.ORANGE, "orange", Items.ORANGE_DYE, Items.ORANGE_WOOL, Items.ORANGE_BED, Items.ORANGE_CARPET, Items.ORANGE_STAINED_GLASS, Items.ORANGE_STAINED_GLASS_PANE, Items.ORANGE_TERRACOTTA, Items.ORANGE_GLAZED_TERRACOTTA, Items.ORANGE_CONCRETE, Items.ORANGE_CONCRETE_POWDER, Items.ORANGE_BANNER, Items.ORANGE_SHULKER_BOX, Blocks.ORANGE_WALL_BANNER);
            p(DyeColor.PINK, "pink", Items.PINK_DYE, Items.PINK_WOOL, Items.PINK_BED, Items.PINK_CARPET, Items.PINK_STAINED_GLASS, Items.PINK_STAINED_GLASS_PANE, Items.PINK_TERRACOTTA, Items.PINK_GLAZED_TERRACOTTA, Items.PINK_CONCRETE, Items.PINK_CONCRETE_POWDER, Items.PINK_BANNER, Items.PINK_SHULKER_BOX, Blocks.PINK_WALL_BANNER);
            p(DyeColor.PURPLE, "purple", Items.PURPLE_DYE, Items.PURPLE_WOOL, Items.PURPLE_BED, Items.PURPLE_CARPET, Items.PURPLE_STAINED_GLASS, Items.PURPLE_STAINED_GLASS_PANE, Items.PURPLE_TERRACOTTA, Items.PURPLE_GLAZED_TERRACOTTA, Items.PURPLE_CONCRETE, Items.PURPLE_CONCRETE_POWDER, Items.PURPLE_BANNER, Items.PURPLE_SHULKER_BOX, Blocks.PURPLE_WALL_BANNER);
            p(DyeColor.RED, "red", Items.RED_DYE, Items.RED_WOOL, Items.RED_BED, Items.RED_CARPET, Items.RED_STAINED_GLASS, Items.RED_STAINED_GLASS_PANE, Items.RED_TERRACOTTA, Items.RED_GLAZED_TERRACOTTA, Items.RED_CONCRETE, Items.RED_CONCRETE_POWDER, Items.RED_BANNER, Items.RED_SHULKER_BOX, Blocks.RED_WALL_BANNER);
            p(DyeColor.YELLOW, "yellow", Items.YELLOW_DYE, Items.YELLOW_WOOL, Items.YELLOW_BED, Items.YELLOW_CARPET, Items.YELLOW_STAINED_GLASS, Items.YELLOW_STAINED_GLASS_PANE, Items.YELLOW_TERRACOTTA, Items.YELLOW_GLAZED_TERRACOTTA, Items.YELLOW_CONCRETE, Items.YELLOW_CONCRETE_POWDER, Items.YELLOW_BANNER, Items.YELLOW_SHULKER_BOX, Blocks.YELLOW_WALL_BANNER);
        }

        void p(DyeColor color, String colorName, Item dye, Item wool, Item bed, Item carpet, Item stainedGlass, Item stainedGlassPane, Item terracotta, Item glazedTerracotta, Item concrete, Item concretePowder, Item banner, Item shulker, Block wallBanner) {
            put(color.getMapColor(), new ColorfulItems(color, colorName, dye, wool, bed, carpet, stainedGlass, stainedGlassPane, terracotta, glazedTerracotta, concrete, concretePowder, banner, shulker, wallBanner));
        }
    };
    private static final Map<WoodType, WoodItems> _woodMap = new HashMap<WoodType, WoodItems>() {
        {
            p(WoodType.CHERRY, "cherry", Items.CHERRY_PLANKS, Items.CHERRY_LOG, Items.STRIPPED_CHERRY_LOG, Items.STRIPPED_CHERRY_WOOD, Items.CHERRY_WOOD, Items.CHERRY_SIGN, Items.CHERRY_HANGING_SIGN, Items.CHERRY_DOOR, Items.CHERRY_BUTTON, Items.CHERRY_STAIRS, Items.CHERRY_SLAB, Items.CHERRY_FENCE, Items.CHERRY_FENCE_GATE, Items.CHERRY_BOAT, Items.CHERRY_SAPLING, Items.CHERRY_LEAVES, Items.CHERRY_PRESSURE_PLATE, Items.CHERRY_TRAPDOOR);
            p(WoodType.BAMBOO, "bamboo", null, null, Items.STRIPPED_BAMBOO_BLOCK, null, null, Items.BAMBOO_SIGN, Items.BAMBOO_HANGING_SIGN, Items.BAMBOO_DOOR, Items.BAMBOO_BUTTON, Items.BAMBOO_STAIRS, Items.BAMBOO_SLAB, Items.BAMBOO_FENCE, Items.BAMBOO_FENCE_GATE, Items.BAMBOO_RAFT, Items.BAMBOO, null, Items.BAMBOO_PRESSURE_PLATE, Items.BAMBOO_TRAPDOOR);
            p(WoodType.MANGROVE, "mangrove", Items.MANGROVE_PLANKS, Items.MANGROVE_LOG, Items.STRIPPED_MANGROVE_LOG, Items.STRIPPED_MANGROVE_WOOD, Items.MANGROVE_WOOD, Items.MANGROVE_SIGN, Items.MANGROVE_HANGING_SIGN, Items.MANGROVE_DOOR, Items.MANGROVE_BUTTON, Items.MANGROVE_STAIRS, Items.MANGROVE_SLAB, Items.MANGROVE_FENCE, Items.MANGROVE_FENCE_GATE, Items.MANGROVE_BOAT, Items.MANGROVE_PROPAGULE, Items.MANGROVE_LEAVES, Items.MANGROVE_PRESSURE_PLATE, Items.MANGROVE_TRAPDOOR);
            p(WoodType.ACACIA, "acacia", Items.ACACIA_PLANKS, Items.ACACIA_LOG, Items.STRIPPED_ACACIA_LOG, Items.STRIPPED_ACACIA_WOOD, Items.ACACIA_WOOD, Items.ACACIA_SIGN, Items.ACACIA_HANGING_SIGN, Items.ACACIA_DOOR, Items.ACACIA_BUTTON, Items.ACACIA_STAIRS, Items.ACACIA_SLAB, Items.ACACIA_FENCE, Items.ACACIA_FENCE_GATE, Items.ACACIA_BOAT, Items.ACACIA_SAPLING, Items.ACACIA_LEAVES, Items.ACACIA_PRESSURE_PLATE, Items.ACACIA_TRAPDOOR);
            p(WoodType.BIRCH, "birch", Items.BIRCH_PLANKS, Items.BIRCH_LOG, Items.STRIPPED_BIRCH_LOG, Items.STRIPPED_BIRCH_WOOD, Items.BIRCH_WOOD, Items.BIRCH_SIGN, Items.BIRCH_HANGING_SIGN, Items.BIRCH_DOOR, Items.BIRCH_BUTTON, Items.BIRCH_STAIRS, Items.BIRCH_SLAB, Items.BIRCH_FENCE, Items.BIRCH_FENCE_GATE, Items.BIRCH_BOAT, Items.BIRCH_SAPLING, Items.BIRCH_LEAVES, Items.BIRCH_PRESSURE_PLATE, Items.BIRCH_TRAPDOOR);
            p(WoodType.CRIMSON, "crimson", Items.CRIMSON_PLANKS, Items.CRIMSON_STEM, Items.STRIPPED_CRIMSON_STEM, Items.STRIPPED_CRIMSON_HYPHAE, Items.CRIMSON_HYPHAE, Items.CRIMSON_SIGN, Items.CRIMSON_HANGING_SIGN, Items.CRIMSON_DOOR, Items.CRIMSON_BUTTON, Items.CRIMSON_STAIRS, Items.CRIMSON_SLAB, Items.CRIMSON_FENCE, Items.CRIMSON_FENCE_GATE, null, Items.CRIMSON_FUNGUS, null, Items.CRIMSON_PRESSURE_PLATE, Items.CRIMSON_TRAPDOOR);
            p(WoodType.DARK_OAK, "dark_oak", Items.DARK_OAK_PLANKS, Items.DARK_OAK_LOG, Items.STRIPPED_DARK_OAK_LOG, Items.STRIPPED_DARK_OAK_WOOD, Items.DARK_OAK_WOOD, Items.DARK_OAK_SIGN, Items.DARK_OAK_HANGING_SIGN, Items.DARK_OAK_DOOR, Items.DARK_OAK_BUTTON, Items.DARK_OAK_STAIRS, Items.DARK_OAK_SLAB, Items.DARK_OAK_FENCE, Items.DARK_OAK_FENCE_GATE, Items.DARK_OAK_BOAT, Items.DARK_OAK_SAPLING, Items.DARK_OAK_LEAVES, Items.DARK_OAK_PRESSURE_PLATE, Items.DARK_OAK_TRAPDOOR);
            p(WoodType.OAK, "oak", Items.OAK_PLANKS, Items.OAK_LOG, Items.STRIPPED_OAK_LOG, Items.STRIPPED_OAK_WOOD, Items.OAK_WOOD, Items.OAK_SIGN, Items.OAK_HANGING_SIGN, Items.OAK_DOOR, Items.OAK_BUTTON, Items.OAK_STAIRS, Items.OAK_SLAB, Items.OAK_FENCE, Items.OAK_FENCE_GATE, Items.OAK_BOAT, Items.OAK_SAPLING, Items.OAK_LEAVES, Items.OAK_PRESSURE_PLATE, Items.OAK_TRAPDOOR);
            p(WoodType.JUNGLE, "jungle", Items.JUNGLE_PLANKS, Items.JUNGLE_LOG, Items.STRIPPED_JUNGLE_LOG, Items.STRIPPED_JUNGLE_WOOD, Items.JUNGLE_WOOD, Items.JUNGLE_SIGN, Items.JUNGLE_HANGING_SIGN, Items.JUNGLE_DOOR, Items.JUNGLE_BUTTON, Items.JUNGLE_STAIRS, Items.JUNGLE_SLAB, Items.JUNGLE_FENCE, Items.JUNGLE_FENCE_GATE, Items.JUNGLE_BOAT, Items.JUNGLE_SAPLING, Items.JUNGLE_LEAVES, Items.JUNGLE_PRESSURE_PLATE, Items.JUNGLE_TRAPDOOR);
            p(WoodType.SPRUCE, "spruce", Items.SPRUCE_PLANKS, Items.SPRUCE_LOG, Items.STRIPPED_SPRUCE_LOG, Items.STRIPPED_SPRUCE_WOOD, Items.SPRUCE_WOOD, Items.SPRUCE_SIGN, Items.SPRUCE_HANGING_SIGN, Items.SPRUCE_DOOR, Items.SPRUCE_BUTTON, Items.SPRUCE_STAIRS, Items.SPRUCE_SLAB, Items.SPRUCE_FENCE, Items.SPRUCE_FENCE_GATE, Items.SPRUCE_BOAT, Items.SPRUCE_SAPLING, Items.SPRUCE_LEAVES, Items.SPRUCE_PRESSURE_PLATE, Items.SPRUCE_TRAPDOOR);
            p(WoodType.WARPED, "warped", Items.WARPED_PLANKS, Items.WARPED_STEM, Items.STRIPPED_WARPED_STEM, Items.STRIPPED_WARPED_HYPHAE, Items.WARPED_HYPHAE, Items.WARPED_SIGN, Items.WARPED_HANGING_SIGN, Items.WARPED_DOOR, Items.WARPED_BUTTON, Items.WARPED_STAIRS, Items.WARPED_SLAB, Items.WARPED_FENCE, Items.WARPED_FENCE_GATE, null, Items.WARPED_FUNGUS, null, Items.WARPED_PRESSURE_PLATE, Items.WARPED_TRAPDOOR);
        }

        void p(WoodType type, String prefix, Item planks, Item log, Item strippedLog, Item strippedWood, Item wood, Item sign, Item hangingSign, Item door, Item button, Item stairs, Item slab, Item fence, Item fenceGate, Item boat, Item sapling, Item leaves, Item pressurePlate, Item trapdoor) {
            put(type, new WoodItems(prefix, planks, log, strippedLog, strippedWood, wood, sign, hangingSign, door, button, stairs, slab, fence, fenceGate, boat, sapling, leaves, pressurePlate, trapdoor));
        }
    };
    private static final HashMap<Item, Item> _cookableFoodMap = new HashMap<>() {
        {
            put(Items.PORKCHOP, Items.COOKED_PORKCHOP);
            put(Items.BEEF, Items.COOKED_BEEF);
            put(Items.CHICKEN, Items.COOKED_CHICKEN); // chicken is best meat, fight me
            put(Items.MUTTON, Items.COOKED_MUTTON);
            put(Items.RABBIT, Items.COOKED_RABBIT);
            put(Items.SALMON, Items.COOKED_SALMON);
            put(Items.COD, Items.COOKED_COD);
            put(Items.POTATO, Items.BAKED_POTATO);
        }
    };
    public static final Item[] RAW_FOODS = _cookableFoodMap.keySet().toArray(Item[]::new);
    private static Map<Item, Integer> _fuelTimeMap = null;

    public static String stripItemName(Item item) {
        String[] possibilities = new String[]{"item.minecraft.", "block.minecraft."};
        for (String possible : possibilities) {
            if (item.getTranslationKey().startsWith(possible)) {
                return item.getTranslationKey().substring(possible.length());
            }
        }
        return item.getTranslationKey();
    }

    public static Item[] blocksToItems(Block[] blocks) {
        Item[] result = new Item[blocks.length];
        for (int i = 0; i < blocks.length; ++i) {
            result[i] = blocks[i].asItem();
        }
        return result;
    }

    /* Logs:
        ACACIA
        BIRCH
        CRIMSON
        DARK_OAK
        OAK
        JUNGLE
        SPRUCE
        WARPED
     */

    /* Colors:
        WHITE
        BLACK
        BLUE
        BROWN
        CYAN
        GRAY
        GREEN
        LIGHT_BLUE
        LIGHT_GRAY
        LIME
        MAGENTA
        ORANGE
        PINK
        PURPLE
        RED
        YELLOW
     */

    public static Block[] itemsToBlocks(Item[] items) {
        ArrayList<Block> result = new ArrayList<>();
        for (Item item : items) {
            if (item instanceof BlockItem) {
                Block b = Block.getBlockFromItem(item);
                if (b != null && b != Blocks.AIR) {
                    result.add(b);
                }
            }
        }
        return result.toArray(Block[]::new);
    }

    public static Item logToPlanks(Item logItem) {
        return _logToPlanks.getOrDefault(logItem, null);
    }

    public static Item planksToLog(Item plankItem) {
        return _planksToLogs.getOrDefault(plankItem, null);
    }

    public static Item strippedToLogs(Item logItem) {
        return _strippedToLogs.getOrDefault(logItem, null);
    }

    public static ColorfulItems getColorfulItems(MapColor color) {
        return _colorMap.get(color);
    }

    public static ColorfulItems getColorfulItems(DyeColor color) {
        return getColorfulItems(color.getMapColor());
    }

    public static Collection<ColorfulItems> getColorfulItems() {
        return _colorMap.values();
    }

    public static WoodItems getWoodItems(WoodType type) {
        return _woodMap.get(type);
    }

    public static Collection<WoodItems> getWoodItems() {
        return _woodMap.values();
    }

    public static Optional<Item> getCookedFood(Item rawFood) {
        return Optional.ofNullable(_cookableFoodMap.getOrDefault(rawFood, null));
    }

    public static String trimItemName(String name) {
        if (name.startsWith("block.minecraft.")) {
            name = name.substring("block.minecraft.".length());
        } else if (name.startsWith("item.minecraft.")) {
            name = name.substring("item.minecraft.".length());
        }
        return name;
    }

    public static boolean areShearsEffective(Block b) {
        return b instanceof LeavesBlock
                || b == Blocks.COBWEB
                || b == Blocks.GRASS_BLOCK
                || b == Blocks.TALL_GRASS
                || b == Blocks.LILY_PAD
                || b == Blocks.FERN
                || b == Blocks.DEAD_BUSH
                || b == Blocks.VINE
                || b == Blocks.TRIPWIRE
                || isOfBlockType(b, BlockTags.WOOL)
                || b == Blocks.NETHER_SPROUTS;
    }

    public static boolean isOfBlockType(Block b, TagKey<Block> tag) {
        return Registries.BLOCK.getKey(b).map(e -> Registries.BLOCK.entryOf(e).streamTags().anyMatch(t -> t == tag)).orElse(false);
    }

    private static boolean isStackProtected(AltoClef mod, ItemStack stack) {
        if (stack.hasEnchantments() && mod.getModSettings().getDontThrowAwayEnchantedItems())
            return true;
        if (stack.getItem().getComponents().contains(DataComponentTypes.CUSTOM_NAME) && mod.getModSettings().getDontThrowAwayCustomNameItems())
            return true;
        return mod.getBehaviour().isProtected(stack.getItem()) || mod.getModSettings().isImportant(stack.getItem());
    }

    public static boolean canThrowAwayStack(AltoClef mod, ItemStack stack) {
        // Can't throw away empty stacks!
        if (stack.isEmpty())
            return false;
        if (isStackProtected(mod, stack))
            return false;
        return mod.getModSettings().isThrowaway(stack.getItem()) || mod.getModSettings().shouldThrowawayUnusedItems();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean canStackTogether(ItemStack from, ItemStack to) {
        if (to.isEmpty() && from.getCount() <= from.getMaxCount())
            return true;
        return to.getItem().equals(from.getItem()) && (from.getCount() + to.getCount() < to.getMaxCount());
    }

    private static Map<Item, Integer> getFuelTimeMap() {
        if (_fuelTimeMap == null) {
            _fuelTimeMap = AbstractFurnaceBlockEntity.createFuelTimeMap();
        }
        return _fuelTimeMap;
    }

    public static double getFuelAmount(Item... items) {
        double total = 0;
        for (Item item : items) {
            if (getFuelTimeMap().containsKey(item)) {
                int timeTicks = getFuelTimeMap().get(item);
                // 300 ticks of wood -> 1.5 operations
                // 200 ticks -> 1 operation
                total += (double) timeTicks / 200.0;
            }
        }
        return total;
    }

    public static double getFuelAmount(ItemStack stack) {
        return getFuelAmount(stack.getItem()) * stack.getCount();
    }

    public static boolean isFuel(Item item) {
        return getFuelTimeMap().containsKey(item);
    }

    public boolean isRawFood(Item item) {
        return _cookableFoodMap.containsKey(item);
    }

    public static class ColorfulItems {
        public DyeColor color;
        public String colorName;
        public Item dye;
        public Item wool;
        public Item bed;
        public Item carpet;
        public Item stainedGlass;
        public Item stainedGlassPane;
        public Item terracotta;
        public Item glazedTerracotta;
        public Item concrete;
        public Item concretePowder;
        public Item banner;
        public Item shulker;
        public Block wallBanner;

        public ColorfulItems(DyeColor color, String colorName, Item dye, Item wool, Item bed, Item carpet, Item stainedGlass, Item stainedGlassPane, Item terracotta, Item glazedTerracotta, Item concrete, Item concretePowder, Item banner, Item shulker, Block wallBanner) {
            this.color = color;
            this.colorName = colorName;
            this.dye = dye;
            this.wool = wool;
            this.bed = bed;
            this.carpet = carpet;
            this.stainedGlass = stainedGlass;
            this.stainedGlassPane = stainedGlassPane;
            this.terracotta = terracotta;
            this.glazedTerracotta = glazedTerracotta;
            this.concrete = concrete;
            this.concretePowder = concretePowder;
            this.banner = banner;
            this.shulker = shulker;
            this.wallBanner = wallBanner;
        }
    }

    public static class WoodItems {
        public String prefix;
        public Item planks;
        public Item log;
        public Item strippedLog;
        public Item strippedWood;
        public Item wood;
        public Item sign;
        public Item hangingSign;
        public Item door;
        public Item button;
        public Item stairs;
        public Item slab;
        public Item fence;
        public Item fenceGate;
        public Item boat;
        public Item sapling;
        public Item leaves;
        public Item pressurePlate;
        public Item trapdoor;

        public WoodItems(String prefix, Item planks, Item log, Item strippedLog, Item strippedWood, Item wood, Item sign, Item hangingSign, Item door, Item button, Item stairs, Item slab, Item fence, Item fenceGate, Item boat, Item sapling, Item leaves, Item pressurePlate, Item trapdoor) {
            this.prefix = prefix;
            this.planks = planks;
            this.log = log;
            this.strippedLog = strippedLog;
            this.strippedWood = strippedWood;
            this.wood = wood;
            this.sign = sign;
            this.hangingSign = hangingSign;
            this.door = door;
            this.button = button;
            this.stairs = stairs;
            this.slab = slab;
            this.fence = fence;
            this.fenceGate = fenceGate;
            this.boat = boat;
            this.sapling = sapling;
            this.leaves = leaves;
            this.pressurePlate = pressurePlate;
            this.trapdoor = trapdoor;
        }

        public boolean isNetherWood() {
            return planks == Items.CRIMSON_PLANKS || planks == Items.WARPED_PLANKS;
        }
    }

}
