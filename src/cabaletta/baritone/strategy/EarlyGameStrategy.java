/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.strategy;

import baritone.movement.Combat;
import baritone.util.BlockPuncher;
import baritone.mining.MickeyMine;
import baritone.inventory.CraftingTask;
import baritone.util.Manager;
import baritone.util.ManagerTick;
import baritone.util.Out;
import baritone.inventory.SmeltingTask;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;

/**
 * goals:
 *
 * get dirt
 *
 * get wood
 *
 * make a crafting table
 *
 * make a wooden pickaxe
 *
 * get stone
 *
 * make a stone pickaxe
 *
 * get more stone
 *
 * make stone tools and a furnace
 *
 * go mining at level 36
 *
 * craft torches
 *
 * smelt iron
 *
 * make iron pick and iron armor and an iron sword
 *
 * change mining level to 6
 *
 * craft a diamond pickaxe
 *
 * @author leijurv
 */
public class EarlyGameStrategy extends ManagerTick {
    static boolean gotWood_PHRASING = false;
    static int WOOD_AMT = 16;//triggers stopping
    static int MIN_WOOD_AMT = 1;//triggers getting more
    static final int DIRT_AMT = 32;
    static boolean gotDirt = false;
    static boolean cobble = false;
    @Override
    protected boolean onTick0() {
        if (!gotDirt) {
            int dirt = countDirt();
            if (dirt >= DIRT_AMT) {
                Out.gui("Done getting dirt", Out.Mode.Debug);
                gotDirt = true;
                return false;
            }
            if (!BlockPuncher.tick("dirt", "grass")) {
                Out.gui("No dirt or grass nearby =(", Out.Mode.Debug);
            }
            return false;
        }
        int wood = countWood_PHRASING();
        if (wood >= WOOD_AMT) {
            if (!gotWood_PHRASING) {
                Out.gui("Done getting wood", Out.Mode.Debug);
            }
            gotWood_PHRASING = true;
        }
        if (wood < MIN_WOOD_AMT) {
            if (gotWood_PHRASING) {
                Out.gui("Getting more wood", Out.Mode.Debug);
            }
            gotWood_PHRASING = false;
        }
        if (!gotWood_PHRASING) {
            if (!BlockPuncher.tick("log", "log2")) {
                Out.gui("No wood nearby =(", Out.Mode.Debug);
            }
            return false;
        }
        boolean hasWooden = false;
        boolean readyForMining = true;
        boolean hasStone = craftTool(Item.getByNameOrId("minecraft:stone_pickaxe"), 1);
        if (hasStone) {
            dontCraft(Item.getByNameOrId("minecraft:wooden_pickaxe"));
        } else {
            hasWooden = craftTool(Item.getByNameOrId("minecraft:wooden_pickaxe"), 1);
        }
        readyForMining &= hasStone;
        if (hasWooden || hasStone) {
            if (!cobble) {
                if (countCobble() > 16) {
                    cobble = true;
                } else if (!BlockPuncher.tick("stone")) {
                    Out.gui("No stone nearby =(", Out.Mode.Debug);
                }
            }
        }
        if (!cobble) {
            readyForMining = false;
        }
        if (cobble && gotDirt && countCobble() + countDirt() < 10) {//if we have already gotten cobble and dirt, but our amounts have run low, get more
            if (!BlockPuncher.tick("dirt", "grass", "stone")) {
                Out.gui("No dirt, grass, or stone", Out.Mode.Debug);
            }
            readyForMining = false;
        }
        if (countCobble() > 5) {
            boolean axe = craftTool(Item.getByNameOrId("minecraft:stone_axe"), 1);
            if (axe) {
                WOOD_AMT = 64;
                MIN_WOOD_AMT = 16;
            } else {
                readyForMining = false;
            }
            if (!craftTool(Item.getByNameOrId("minecraft:stone_shovel"), 1)) {
                readyForMining = false;
            }
            if (!craftTool(Item.getByNameOrId("minecraft:stone_sword"), 1)) {
                readyForMining = false;
            }
        }
        if (countCobble() > 8) {
            if (!craftTool(Item.getByNameOrId("minecraft:furnace"), 1)) {
                readyForMining = false;
            }
        }
        int miningLevel = MickeyMine.Y_IRON;
        if (readyForMining) {
            int amtIron = 0;
            boolean ironPick = craftTool(Item.getByNameOrId("minecraft:iron_pickaxe"), 1);
            if (ironPick) {
                boolean ironSword = craftTool(Item.getByNameOrId("minecraft:iron_sword"), 1);
                if (ironSword) {
                    boolean ironHelmet = craftTool(Item.getByNameOrId("minecraft:iron_helmet"), 1);
                    boolean ironChestplate = craftTool(Item.getByNameOrId("minecraft:iron_chestplate"), 1);
                    boolean ironLeggings = craftTool(Item.getByNameOrId("minecraft:iron_leggings"), 1);
                    boolean ironBoots = craftTool(Item.getByNameOrId("minecraft:iron_boots"), 1);
                    if (ironHelmet && ironChestplate && ironLeggings && ironBoots) {
                        miningLevel = MickeyMine.Y_DIAMOND;
                    } else {
                        amtIron = (!ironHelmet ? 5 : 0) + (!ironChestplate ? 8 : 0) + (!ironLeggings ? 7 : 0) + (!ironBoots ? 4 : 0);
                    }
                } else {
                    amtIron = 2;
                }
            } else {
                amtIron = 3;
            }
            int currIron = countItem("minecraft:iron_ingot");
            boolean hasOre = countItem("iron_ore") >= amtIron - currIron;
            if (hasOre && currIron < amtIron) {
                int tasksForIron = SmeltingTask.tasksFor(Item.getByNameOrId("iron_ingot"));
                int newTask = amtIron - currIron - tasksForIron;
                if (newTask > 0) {
                    new SmeltingTask(new ItemStack(Item.getByNameOrId("iron_ingot"), Math.min(countItem("iron_ore"), 64))).begin();
                }
                readyForMining = false;
            }
        }
        int numDiamonds = countItem("diamond");
        if (readyForMining && numDiamonds >= 1) {
            if (craftTool(Item.getByNameOrId("diamond_pickaxe"), 1)) {
                if (craftTool(Item.getByNameOrId("diamond_sword"), 1)) {
                    if (craftTool(Item.getByNameOrId("diamond_chestplate"), 1)) {
                        if (craftTool(Item.getByNameOrId("diamond_leggings"), 1)) {
                            if (craftTool(Item.getByNameOrId("diamond_helmet"), 1)) {
                                if (craftTool(Item.getByNameOrId("diamond_boots"), 1)) {
                                    if (craftTool(Item.getByNameOrId("diamond_axe"), 1)) {
                                        if (craftTool(Item.getByNameOrId("diamond_shovel"), 1)) {
                                            Out.gui("My job here is done.", Out.Mode.Minimal);
                                            cancel();
                                            return false;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Manager instance = Manager.getManager(MickeyMine.class);
        if (readyForMining) {
            MickeyMine.yLevel = miningLevel;
            if (!instance.enabled()) {
                instance.toggle();
            }
        } else if (instance.enabled()) {
            instance.toggle();
        }
        return false;
    }
    public static boolean craftTool(Item tool, int amt) {
        if (tool instanceof ItemTool) {
            for (ItemStack stack : Minecraft.getMinecraft().player.inventory.mainInventory) {
                if (stack == null) {
                    continue;
                }
                if (stack.getItem() instanceof ItemTool && stack.getItem().getClass() == tool.getClass()) {
                    ItemTool t = (ItemTool) (stack.getItem());
                    if (t.getToolMaterial().getEfficiencyOnProperMaterial() >= ((ItemTool) tool).getToolMaterial().getEfficiencyOnProperMaterial()) {
                        //Out.gui("Saying has " + new ItemStack(tool, 0) + " because has " + stack);
                        return true;
                    }
                }
            }
            return CraftingTask.ensureCraftingDesired(tool, amt);
        }
        if (tool instanceof ItemArmor) {
            ItemArmor armor = (ItemArmor) tool;
            for (ItemStack stack : Minecraft.getMinecraft().player.inventory.mainInventory) {
                if (stack == null) {
                    continue;
                }
                if (stack.getItem() instanceof ItemArmor) {
                    ItemArmor a = (ItemArmor) (stack.getItem());
                    if (a.armorType == armor.armorType) {
                        if (a.damageReduceAmount >= armor.damageReduceAmount) {
                            //Out.gui("Saying has " + new ItemStack(tool, 0) + " because has " + stack);
                            return true;
                        }
                    }
                }
            }
            for (ItemStack stack : Minecraft.getMinecraft().player.inventory.armorInventory) {
                if (stack == null) {
                    continue;
                }
                ItemArmor a = (ItemArmor) (stack.getItem());
                if (a.armorType == armor.armorType) {
                    if (a.damageReduceAmount >= armor.damageReduceAmount) {
                        //Out.gui("Saying has " + new ItemStack(tool, 0) + " because has " + stack);
                        return true;
                    }
                }
            }
        }
        return CraftingTask.ensureCraftingDesired(tool, amt);
    }
    public static void dontCraft(Item item) {
        CraftingTask task = CraftingTask.findOrCreateCraftingTask(new ItemStack(item, 0));
        if (task.currentlyCrafting().stackSize > 0) {
            task.decreaseNeededAmount(1);
        }
    }
    public static int countItem(String s) {
        Item item = Item.getByNameOrId(s);
        int count = 0;
        for (ItemStack stack : Minecraft.getMinecraft().player.inventory.mainInventory) {
            if (stack == null) {
                continue;
            }
            if (item.equals(stack.getItem())) {
                count += stack.stackSize;
            }
        }
        return count;
    }
    public static int countWood_PHRASING() {
        return countItem("log") + countItem("log2");
    }
    public static int countDirt() {
        return countItem("dirt");
    }
    public static int countCobble() {
        return countItem("cobblestone");
    }
    @Override
    protected void onCancel() {
        gotWood_PHRASING = false;
        WOOD_AMT = 16;
        MIN_WOOD_AMT = 1;
        gotDirt = false;
        cobble = false;
        Combat.mobKilling = false;
        SmeltingTask.coalOnly = false;
        Manager.getManager(MickeyMine.class).cancel();
    }
    @Override
    protected void onStart() {
        gotWood_PHRASING = false;
        WOOD_AMT = 16;
        MIN_WOOD_AMT = 1;
        gotDirt = false;
        cobble = false;
        Combat.mobKilling = true;
        SmeltingTask.coalOnly = true;
    }
}
