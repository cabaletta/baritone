/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.inventory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import baritone.Baritone;
import baritone.mining.MickeyMine;
import baritone.util.Manager;
import baritone.util.Out;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.math.BlockPos;

/**
 *
 * @author leijurv
 */
public class InventoryManager extends Manager {
    static HashMap<String, Integer> maximumAmounts = null;
    static HashMap<String, Integer> minimumAmounts = null;
    static HashSet<Item> onHotbar = new HashSet();
    public static void initMax() {
        maximumAmounts = new HashMap();
        minimumAmounts = new HashMap();
        addBounds("cobblestone", 128, 64);
        addBounds("coal", 128, 64);
        //addBounds("redstone", 0, 0);//no one wants stinking redstone
        addBounds("stone", 64, 32);
        addBounds("gravel", 64, 32);
        addBounds("dirt", 128, 64);
    }
    public static void addBounds(String itemName, int max, int min) {
        Item item = Item.getByNameOrId("minecraft:" + itemName);
        if (item == null) {
            Out.gui(itemName + " doesn't exist", Out.Mode.Minimal);
            throw new NullPointerException(itemName + " doesn't exist");
        }
        maximumAmounts.put(itemName, max);
        minimumAmounts.put(itemName, min);
    }
    static boolean openedInvYet = false;
    /**
     * Place the best instance of itemType in hot bar slot position
     *
     * @param pos hot bar slot to put in
     * @param check what block to check break time against
     * @param itemType the class of the item
     * @param doThrowaway throw away all but the best two
     * @return
     */
    public static boolean place(int pos, Block check, Class<?> itemType, boolean doThrowaway) {
        ItemStack[] stacks = Minecraft.getMinecraft().player.inventory.mainInventory;
        int itemPos = -1;
        float bestStrength = Float.MIN_VALUE;
        for (int i = 0; i < stacks.length; i++) {
            ItemStack stack = stacks[i];
            if (stack == null) {
                continue;
            }
            Item item = stack.getItem();
            if (item.getClass() == itemType) {
                float strength = item.getStrVsBlock(stack, check);
                if (strength > bestStrength) {
                    bestStrength = strength;
                    itemPos = i;
                }
            }
        }
        if (itemPos == -1) {//there are none
            return false;
        }
        if (itemPos == pos) {//there is one and this is the best
            if (!doThrowaway) {
                return false;
            }
            int seconditemPos = -1;
            float secondbestStrength = Float.MIN_VALUE;
            for (int i = 0; i < stacks.length; i++) {
                if (i == itemPos) {
                    continue;
                }
                ItemStack stack = stacks[i];
                if (stack == null) {
                    continue;
                }
                Item item = stack.getItem();
                if (item.getClass() == itemType) {
                    float strength = item.getStrVsBlock(stack, check);
                    if (strength > secondbestStrength) {
                        secondbestStrength = strength;
                        seconditemPos = i;
                    }
                }
            }
            if (seconditemPos == -1) {
                return false;
            }
            for (int i = 0; i < stacks.length; i++) {
                if (i == itemPos || i == seconditemPos) {
                    continue;
                }
                ItemStack stack = stacks[i];
                if (stack == null) {
                    continue;
                }
                Item item = stack.getItem();
                if (item.getClass() == itemType) {
                    int j = i;
                    if (j < 9) {
                        j += 36;
                    }
                    if (!openedInvYet) {
                        Baritone.slowOpenInventory();
                        openedInvYet = true;
                    }
                    dropOne(j);
                    return true;
                }
            }
            return false;
        }
        if (itemPos < 9) {
            itemPos += 36;
        }
        if (!openedInvYet) {
            Baritone.slowOpenInventory();
            openedInvYet = true;
        }
        switchWithHotBar(itemPos, pos);
        return true;
    }
    /**
     * Find items in the player's inventory
     *
     * @param items
     * @return
     */
    public static int find(Item... items) {
        ItemStack[] stacks = Minecraft.getMinecraft().player.inventory.mainInventory;
        int bestPosition = -1;
        int bestSize = 0;
        for (int i = 0; i < stacks.length; i++) {
            ItemStack stack = stacks[i];
            if (stack == null) {
                continue;
            }
            for (Item it : items) {
                if (it.equals(stack.getItem())) {
                    if (stack.stackSize > bestSize) {
                        bestSize = stack.stackSize;
                        bestPosition = i;
                    }
                }
            }
        }
        return bestPosition;
    }
    /**
     * Put items in the hot bar slot. If the current item in that slot matches,
     * don't do anything
     *
     * @param hotbarslot
     * @param items
     * @return
     */
    public static boolean putItemInSlot(int hotbarslot, Item... items) {
        int currPos = find(items);
        ItemStack curr = Minecraft.getMinecraft().player.inventory.mainInventory[hotbarslot];
        if (curr != null) {
            for (Item item : items) {
                if (item.equals(curr.getItem())) {
                    return false;
                }
            }
        }
        if (currPos == -1) {
            return false;
        }
        if (currPos < 9) {
            currPos += 36;
        }
        if (!openedInvYet) {
            Baritone.slowOpenInventory();
            openedInvYet = true;
        }
        switchWithHotBar(currPos, hotbarslot);
        return true;
    }
    /**
     * Randomize an array. You know I just kinda realized that I don't know who
     * originally came up with this method for randomizing arrays. I copied it
     * from something we used in Terry G's web programming class when we were
     * making the game of 15/16 4x4 square thingy, and we used it to randomize a
     * 16 length array. I copied it from there and have been using it ever since
     *
     * @param array
     * @param random
     */
    public static void randomize(int[] array, Random random) {
        for (int i = 0; i < array.length; i++) {
            int j = random.nextInt(array.length);
            int tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }
    public static void putOnHotBar(Item item) {
        if (item == null) {
            throw new NullPointerException();
        }
        onHotbar.add(item);
    }
    @Override
    protected void onTick() {
        if (maximumAmounts == null) {
            initMax();
        }
        if (Minecraft.getMinecraft().currentScreen != null && !(Minecraft.getMinecraft().currentScreen instanceof GuiInventory)) {
            return;
        }
        if (openedInvYet && Minecraft.getMinecraft().currentScreen == null) {
            openedInvYet = false;
            return;
        }
        BlockPos look = Baritone.whatAreYouLookingAt();
        boolean doThrowAway = true;
        if (look != null) {
            int xDiff = look.getX() - Minecraft.getMinecraft().player.getPosition0().getX();
            int zDiff = look.getZ() - Minecraft.getMinecraft().player.getPosition0().getZ();
            if (Math.abs(xDiff) + Math.abs(zDiff) <= 2) {
                doThrowAway = false;//dont throw away if we are looking at a wall and we are close, because we'll probably just pick it right back up again
            }
        }
        if (checkArmor()) {
            return;
        }
        if (doThrowAway && throwAwayOldArmor()) {
            return;
        }
        Random random = new Random(Minecraft.getMinecraft().player.getName().hashCode());
        int[] slots = {0, 1, 2, 3, 4, 5, 6, 7, 8};
        randomize(slots, random);
        if (place(slots[0], Block.getBlockFromName("stone"), ItemPickaxe.class, doThrowAway)) {
            return;
        }
        if (placeSword(slots[1], doThrowAway)) {
            return;
        }
        if (placeFood(slots[2])) {
            return;
        }
        if (place(slots[3], Block.getBlockFromName("log"), ItemAxe.class, doThrowAway)) {
            return;
        }
        if (place(slots[4], Block.getBlockFromName("dirt"), ItemSpade.class, doThrowAway)) {
            return;
        }
        if (putItemInSlot(slots[5], Item.getByNameOrId("minecraft:dirt"), Item.getByNameOrId("minecraft:cobblestone"))) {
            return;
        }
        if (putItemInSlot(slots[6], Item.getByNameOrId("minecraft:torch"))) {
            return;
        }
        if (!onHotbar.isEmpty()) {
            Out.gui("Hotbar: " + onHotbar, Out.Mode.Debug);
        }
        int slotIndex = 7;
        for (Item item : onHotbar) {
            if (putItemInSlot(slots[slotIndex], item)) {
                return;
            }
            slotIndex++;
            if (slotIndex > 8) {
                break;
            }
        }
        onHotbar.clear();
        HashMap<Item, Integer> amounts = countItems();
        for (String itemName : maximumAmounts.keySet()) {
            if (!doThrowAway) {
                continue;
            }
            Item item = Item.getByNameOrId("minecraft:" + itemName);
            if (amounts.get(item) == null) {
                amounts.put(item, 0);
            }
            //Out.log(amounts.get(item));
            int toThrowAway = amounts.get(item) > maximumAmounts.get(itemName) ? amounts.get(item) - minimumAmounts.get(itemName) : 0;
            if (amounts.get(item) <= minimumAmounts.get(itemName)) {
                MickeyMine.notifyFullness(itemName, false);
            }
            if (amounts.get(item) > ((minimumAmounts.get(itemName) + maximumAmounts.get(itemName)) / 2)) {
                MickeyMine.notifyFullness(itemName, true);
            }
            if (toThrowAway <= 0) {
                continue;
            }
            if (!openedInvYet) {
                Baritone.slowOpenInventory();
                openedInvYet = true;
            }
            GuiContainer c = (GuiContainer) Minecraft.getMinecraft().currentScreen;
            if (Minecraft.getMinecraft().currentScreen == null) {
                Out.gui("Null container", Out.Mode.Debug);
                openedInvYet = false;
                return;
            }
            int bestPos = -1;
            int bestSize = 0;
            for (int i = 0; i < c.inventorySlots.inventorySlots.size(); i++) {
                Slot slot = c.inventorySlots.inventorySlots.get(i);
                if (slot == null) {
                    continue;
                }
                ItemStack is = slot.getStack();
                if (is == null) {
                    continue;
                }
                if (item.equals(is.getItem())) {
                    if (is.stackSize > bestSize && is.stackSize <= toThrowAway) {
                        bestSize = is.stackSize;
                        bestPos = i;
                    }
                }
            }
            if (bestPos != -1) {
                dropAll(bestPos);//throw away the largest stack that's smaller than toThrowAway, if it exists
                return;
            }
            for (int i = 0; i < c.inventorySlots.inventorySlots.size(); i++) {
                Slot slot = c.inventorySlots.inventorySlots.get(i);
                if (slot == null) {
                    continue;
                }
                ItemStack is = slot.getStack();
                if (is == null) {
                    continue;
                }
                if (item.equals(is.getItem())) {
                    if (is.stackSize <= toThrowAway) {
                        toThrowAway -= is.stackSize;
                        dropAll(i);
                        return;
                    } else {
                        for (int j = 0; j < toThrowAway; j++) {
                            dropOne(i);
                            return;
                        }
                        toThrowAway = 0;
                    }
                    if (toThrowAway <= 0) {
                        break;
                    }
                }
            }
        }
        if (openedInvYet) {
            Minecraft.getMinecraft().player.closeScreen();
            openedInvYet = false;
        }
    }
    public static HashMap<Item, Integer> countItems() {
        HashMap<Item, Integer> amounts = new HashMap();
        for (ItemStack is : Minecraft.getMinecraft().player.inventory.mainInventory) {
            if (is != null && is.getItem() != null) {
                if (amounts.get(is.getItem()) == null) {
                    amounts.put(is.getItem(), is.stackSize);
                } else {
                    amounts.put(is.getItem(), is.stackSize + amounts.get(is.getItem()));
                }
            }
        }
        return amounts;
    }
    public static boolean placeSword(int slot, boolean doThrowaway) {
        ItemStack[] stacks = Minecraft.getMinecraft().player.inventory.mainInventory;
        int swordPos = -1;
        float bestStrength = Float.MIN_VALUE;
        for (int i = 0; i < stacks.length; i++) {
            ItemStack stack = stacks[i];
            if (stack == null) {
                continue;
            }
            Item item = stack.getItem();
            if (item instanceof ItemSword) {
                ItemSword sword = (ItemSword) item;
                float strength = sword.getDamageVsEntity();
                if (strength > bestStrength) {
                    bestStrength = strength;
                    swordPos = i;
                }
            }
        }
        if (swordPos == -1) {
            return false;
        }
        if (swordPos == slot) {
            if (!doThrowaway) {
                return false;
            }
            int seconditemPos = -1;
            float secondbestStrength = Float.MIN_VALUE;
            for (int i = 0; i < stacks.length; i++) {
                if (i == swordPos) {
                    continue;
                }
                ItemStack stack = stacks[i];
                if (stack == null) {
                    continue;
                }
                Item item = stack.getItem();
                if (item instanceof ItemSword) {
                    float strength = ((ItemSword) item).getDamageVsEntity();
                    if (strength > secondbestStrength) {
                        secondbestStrength = strength;
                        seconditemPos = i;
                    }
                }
            }
            if (seconditemPos == -1) {
                return false;
            }
            for (int i = 0; i < stacks.length; i++) {
                if (i == swordPos || i == seconditemPos) {
                    continue;
                }
                ItemStack stack = stacks[i];
                if (stack == null) {
                    continue;
                }
                Item item = stack.getItem();
                if (item instanceof ItemSword) {
                    int j = i;
                    if (j < 9) {
                        j += 36;
                    }
                    if (!openedInvYet) {
                        Baritone.slowOpenInventory();
                        openedInvYet = true;
                    }
                    dropOne(j);
                    return true;
                }
            }
            return false;
        }
        if (swordPos < 9) {
            swordPos += 36;
        }
        if (!openedInvYet) {
            Baritone.slowOpenInventory();
            openedInvYet = true;
        }
        switchWithHotBar(swordPos, slot);
        return true;
    }
    public static boolean placeFood(int slot) {
        ItemStack[] stacks = Minecraft.getMinecraft().player.inventory.mainInventory;
        int foodPos = -1;
        float bestStrength = Float.MIN_VALUE;
        for (int i = 0; i < stacks.length; i++) {
            ItemStack stack = stacks[i];
            if (stack == null) {
                continue;
            }
            Item item = stack.getItem();
            if (item instanceof ItemFood && !item.getUnlocalizedName(stack).equals("item.spiderEye")) {
                ItemFood food = (ItemFood) item;
                float strength = food.getHealAmount(stack);
                if (strength > bestStrength) {
                    bestStrength = strength;
                    foodPos = i;
                }
            }
        }
        if (foodPos == -1) {
            return false;
        }
        if (foodPos == slot) {
            return false;
        }
        if (foodPos < 9) {
            foodPos += 36;
        }
        if (!openedInvYet) {
            Baritone.slowOpenInventory();
            openedInvYet = true;
        }
        switchWithHotBar(foodPos, slot);
        return true;
    }
    public static boolean playerHasOpenSlot() {
        ItemStack[] stacks = Minecraft.getMinecraft().player.inventory.mainInventory;
        for (ItemStack stack : stacks) {
            if (stack == null) {
                return true;
            }
        }
        return false;
    }
    public static boolean throwAwayOldArmor() {
        for (int i = 0; i < 4; i++) {
            int betterInd = bestArmor(i, true);
            if (betterInd == -1) {
                continue;
            }
            if (!openedInvYet) {
                Baritone.slowOpenInventory();
                openedInvYet = true;
            }
            if (betterInd < 9) {
                betterInd += 36;
            }
            dropOne(betterInd);
            return true;
        }
        return false;
    }
    public static boolean checkArmor() {
        //helmet, inv container slot 5, armorType 0, armorInventory 3
        //chestplate, inv container slot 6, armorType 1, armorInventory 2
        //leggings, inv container slot 7, armorType 2, armorInventory 1
        //boots, inv container slot 8, armorType 3, armorInventory 0
        for (int i = 0; i < 4; i++) {
            int betterInd = bestArmor(i, false);
            if (betterInd == -1) {
                continue;
            }
            if (!openedInvYet) {
                Baritone.slowOpenInventory();
                openedInvYet = true;
            }
            ItemStack currentArmor = Minecraft.getMinecraft().player.inventory.armorInventory[3 - i];
            if (currentArmor != null) {
                if (playerHasOpenSlot()) {
                    shiftClick(i + 5);
                    return true;
                } else {
                    dropOne(i + 5);//if we don't have space, drop the inferior armor
                    return true;
                }
            }
            if (betterInd < 9) {
                betterInd += 36;
            }
            shiftClick(betterInd);
            return true;
        }
        return false;
    }
    public static int bestArmor(int type, boolean onlyMainInv) {
        ItemStack[] stacks = Minecraft.getMinecraft().player.inventory.mainInventory;
        int bestInd = -1;
        int bestDamageReduce = Integer.MIN_VALUE;
        for (int i = 0; i < stacks.length; i++) {
            ItemStack stack = stacks[i];
            if (stack == null) {
                continue;
            }
            Item item = stack.getItem();
            if (item instanceof ItemArmor) {
                ItemArmor armor = (ItemArmor) item;
                if (armor.armorType == type) {
                    if (armor.damageReduceAmount > bestDamageReduce) {
                        bestDamageReduce = armor.damageReduceAmount;
                        bestInd = i;
                    }
                }
            }
        }
        if (onlyMainInv) {
            return bestInd;
        }
        ItemStack currentlyInSlot = Minecraft.getMinecraft().player.inventory.armorInventory[3 - type];
        if (currentlyInSlot != null) {
            ItemArmor armor = (ItemArmor) currentlyInSlot.getItem();
            if (armor.armorType != type) {
                throw new IllegalStateException(currentlyInSlot + " should be " + type + ", is " + armor.armorType);
            }
            if (armor.damageReduceAmount >= bestDamageReduce) {
                return -1;//if we are already wearing better armor, pretend there is no good armor of this type in main inv
            }
        }
        return bestInd;
    }
    public static void switchWithHotBar(int slotNumber, int hotbarPosition) {
        GuiContainer contain = (GuiContainer) Minecraft.getMinecraft().currentScreen;
        contain.sketchyMouseClick(slotNumber, hotbarPosition, 2);
    }
    public static void dropAll(int slotNumber) {
        GuiContainer contain = (GuiContainer) Minecraft.getMinecraft().currentScreen;
        contain.sketchyMouseClick(slotNumber, 1, 4);
    }
    public static void dropOne(int slotNumber) {
        GuiContainer contain = (GuiContainer) Minecraft.getMinecraft().currentScreen;
        contain.sketchyMouseClick(slotNumber, 0, 4);
    }
    public static void shiftClick(int slotNumber) {
        GuiContainer contain = (GuiContainer) Minecraft.getMinecraft().currentScreen;
        contain.shiftClick(slotNumber);
    }
    @Override
    protected void onCancel() {
    }
    @Override
    protected void onStart() {
    }
    @Override
    protected boolean onEnabled(boolean enabled) {
        return Baritone.tickNumber % 10 == 0 && !Minecraft.getMinecraft().player.capabilities.isCreativeMode;
    }
}
