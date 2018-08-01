/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.inventory;

import java.util.ArrayList;
import java.util.List;
import baritone.inventory.InventoryManager;
import baritone.ui.LookManager;
import baritone.util.Memory;
import baritone.Baritone;
import baritone.movement.MovementManager;
import baritone.mining.MickeyMine;
import baritone.pathfinding.goals.GoalBlock;
import baritone.pathfinding.goals.GoalComposite;
import baritone.util.Manager;
import baritone.util.ManagerTick;
import baritone.util.Out;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;

/**
 *
 * @author galdara
 */
public class CraftingTask extends ManagerTick {
    static ArrayList<CraftingTask> overallCraftingTasks = new ArrayList<CraftingTask>();
    ArrayList<CraftingTask> subCraftingTasks = new ArrayList<CraftingTask>();
    private Item currentlyCrafting = null;
    private int stackSize;
    private int alreadyHas;
    private CraftingTask(ItemStack craftStack) {
        this.currentlyCrafting = craftStack.getItem();
        this.stackSize = 0;
        buildTasks();
        increaseNeededAmount(craftStack.stackSize);
    }
    public static int map(int id, int width, int height, int craftingSize) {//shamelessly copied from Objectives
        int yPos = id / width;
        int xPos = id % width;
        int z = xPos + craftingSize * yPos;
        return z + 1;
    }
    /**
     * @param item
     * @return recipe for that item, or null if item has no recipe
     */
    public static IRecipe getRecipeFromItem(Item item) {
        List<IRecipe> recipes = CraftingManager.getInstance().getRecipeList();
        for (IRecipe currRecipe : recipes) {
            if (currRecipe == null) {
                continue;
            }
            if (currRecipe.getRecipeOutput() == null) {
                continue;
            }
            if (currRecipe.getRecipeOutput().getItem() == null) {
                continue;//probably not all of these are necessary, but when I added all three it stopped a nullpointerexception somewhere in this function
            }
            if (currRecipe.getRecipeOutput().getItem().equals(item)) {
                if (isRecipeOkay(currRecipe)) {
                    return currRecipe;
                }
            }
        }
        return null;
    }
    public static boolean isRecipeOkay(IRecipe recipe) {
        if (recipe instanceof ShapedRecipes) {
            if (((ShapedRecipes) recipe).recipeItems.length > 1) {
                return true;
            }
            for (ItemStack stack : ((ShapedRecipes) recipe).recipeItems) {
                if (stack == null) {
                    continue;
                }
                if (stack.toString().toLowerCase().contains("block")) {
                    Out.log("Not doing " + stack);
                    return false;
                }
            }
            return true;
        }
        if (recipe instanceof ShapelessRecipes) {
            if (((ShapelessRecipes) recipe).recipeItems.size() > 1) {
                return true;
            }
            for (ItemStack stack : ((ShapelessRecipes) recipe).recipeItems) {
                if (stack.toString().toLowerCase().contains("block")) {
                    Out.log("Not doing " + stack);
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    public static boolean recipeNeedsCraftingTable(IRecipe recipe) {
        return (recipe instanceof ShapelessRecipes && recipe.getRecipeSize() > 4) || (recipe instanceof ShapedRecipes && (((ShapedRecipes) recipe).recipeHeight > 2 || ((ShapedRecipes) recipe).recipeWidth > 2));
    }
    ArrayList<int[]> plan = null;
    int tickNumber = 0;
    static int ticksBetweenClicks = 4;
    public void tickPlan() {
        GuiContainer contain = (GuiContainer) Minecraft.getMinecraft().currentScreen;
        if (tickNumber % ticksBetweenClicks == 0) {
            int index = tickNumber / ticksBetweenClicks;
            if (index >= plan.size()) {
                Out.gui("Plan over", Out.Mode.Debug);
                plan = null;
                tickNumber = -40;
                return;
            }
            if (index >= 0) {
                int[] click = plan.get(index);
                Out.gui(index + " " + click[0] + " " + click[1] + " " + click[2] + " " + currentlyCrafting(), Out.Mode.Debug);
                contain.sketchyMouseClick(click[0], click[1], click[2]);
                Out.log("Ticking plan");
            }
        }
        tickNumber++;
    }
    public boolean onTick1() {
        if (plan != null) {
            if (Minecraft.getMinecraft().currentScreen == null || !(Minecraft.getMinecraft().currentScreen instanceof GuiContainer)) {
                plan = null;
                tickNumber = 0;
                return true;
            }
            tickPlan();
            return true;
        }
        if (isDone()) {
            return false;
        }
        if (stackSize != 0) {
            Out.log(currentlyCrafting() + " " + alreadyHas + " " + isDone());
        }
        boolean hasMaterials = actualDoCraft(1, false, true) != null;
        //Out.log("materials " + this + " " + currentlyCrafting() + " " + hasMaterials);
        if (!hasMaterials) {
            return false;
        }
        boolean isCraftingTable = Minecraft.getMinecraft().currentScreen != null && Minecraft.getMinecraft().currentScreen instanceof GuiCrafting;
        if (isCraftingTable) {
            findOrCreateCraftingTask(new ItemStack(Item.getByNameOrId("minecraft:crafting_table"), 0)).clearAll();
        }
        if (!recipeNeedsCraftingTable(getRecipeFromItem(currentlyCrafting)) && !isCraftingTable) {
            craftAsManyAsICan(true);
            return true;//if this doesn't need a crafting table, return no matter what
        }
        //at this point we know that we need a crafting table
        if (isCraftingTable) {
            craftAsManyAsICan(false);
            return true;//since we are already in a crafting table, return so we don't run the code to get into a crafting table repeatedly
        }
        if (!recipeNeedsCraftingTable(getRecipeFromItem(currentlyCrafting))) {
            return false;
        }
        //at this point we know that we need a crafting table and we aren't in one at this moment
        BlockPos craftingTableLocation = Memory.closestOne("crafting_table");
        if (craftingTableLocation != null) {
            MickeyMine.tempDisable = true;
            if (LookManager.couldIReach(craftingTableLocation)) {
                LookManager.lookAtBlock(craftingTableLocation, true);
                if (craftingTableLocation.equals(Baritone.whatAreYouLookingAt())) {
                    Baritone.currentPath = null;
                    MovementManager.clearMovement();
                    Minecraft.getMinecraft().rightClickMouse();
                    findOrCreateCraftingTask(new ItemStack(Item.getByNameOrId("minecraft:crafting_table"), 0)).clearAll();
                }
                return true;
            } else {
                double diffX = craftingTableLocation.getX() + 0.5D - Minecraft.getMinecraft().player.posX;
                double diffY = craftingTableLocation.getY() + 0.5D - Minecraft.getMinecraft().player.posY;
                double diffZ = craftingTableLocation.getZ() + 0.5D - Minecraft.getMinecraft().player.posZ;
                double distXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
                if (distXZ < 50 && Math.abs(diffY) < 20) {
                    Baritone.goal = new GoalComposite(new GoalBlock(craftingTableLocation.up()), new GoalBlock(craftingTableLocation.north()), new GoalBlock(craftingTableLocation.south()), new GoalBlock(craftingTableLocation.east()), new GoalBlock(craftingTableLocation.west()), new GoalBlock(craftingTableLocation.north().down()), new GoalBlock(craftingTableLocation.south().down()), new GoalBlock(craftingTableLocation.east().down()), new GoalBlock(craftingTableLocation.west().down()));
                    if (Baritone.currentPath == null && !Baritone.isPathFinding()) {
                        Baritone.findPathInNewThread(false);
                    }
                    return true;
                } else {
                    Out.gui("too far away from closest crafting table (" + distXZ + " blocks), crafting another", Out.Mode.Debug);
                }
            }
        }
        if (Baritone.whatAreYouLookingAt() != null && Block.getBlockFromName("crafting_table").equals(Minecraft.getMinecraft().world.getBlockState(Baritone.whatAreYouLookingAt()).getBlock())) {
            Baritone.currentPath = null;
            MovementManager.clearMovement();
            Minecraft.getMinecraft().rightClickMouse();
            findOrCreateCraftingTask(new ItemStack(Item.getByNameOrId("minecraft:crafting_table"), 0)).clearAll();
            return true;
        }
        //at this point we know that we need a crafting table and we aren't in one and there isn't one nearby
        if (putCraftingTableOnHotBar()) {
            MickeyMine.tempDisable = true;
            findOrCreateCraftingTask(new ItemStack(Item.getByNameOrId("minecraft:crafting_table"), 0)).clearAll();
            Out.log("Ready to place!");
            if (placeHeldBlockNearby()) {
                return true;
            }
            BlockPos player = Minecraft.getMinecraft().player.getPosition0();
            if (Baritone.isAir(player.down()) || Baritone.isAir(player.up(2))) {
                Out.gui("Placing down", Out.Mode.Debug);
                LookManager.lookAtBlock(Minecraft.getMinecraft().player.getPosition0().down(), true);
                MovementManager.jumping = true;
                MovementManager.sneak = true;
                if (Minecraft.getMinecraft().player.getPosition0().down().equals(Baritone.whatAreYouLookingAt()) || Minecraft.getMinecraft().player.getPosition0().down().down().equals(Baritone.whatAreYouLookingAt())) {
                    Minecraft.getMinecraft().rightClickMouse();
                }
                return true;
            }
            /*
             LookManager.lookAtBlock(Minecraft.getMinecraft().player.getPosition0().down().north(), true);
             LookManager.beSketchy();
             Baritone.forward = new Random().nextBoolean();
             Baritone.backward = new Random().nextBoolean();
             Baritone.left = new Random().nextBoolean();
             Baritone.right = new Random().nextBoolean();
             Baritone.jumping = true;*/
            return true;
        } else if (hasCraftingTableInInventory()) {
            InventoryManager.putOnHotBar(Item.getByNameOrId("crafting_table"));
            return true;
        }
        //at this point we know that we need a crafting table and we aren't in one and there isn't one nearby and we don't have one
        ensureCraftingDesired(Item.getByNameOrId("crafting_table"), 1);
        //at this point we know that we need a crafting table and we aren't in one and there isn't one nearby and we don't have one and we don't have the materials to make one
        //so just rip at this point
        return false;
    }
    public static boolean placeHeldBlockNearby() {
        BlockPos player = Minecraft.getMinecraft().player.getPosition0();
        for (int x = player.getX() - 3; x <= player.getX() + 3; x++) {
            for (int y = player.getY() - 2; y <= player.getY() + 1; y++) {
                for (int z = player.getZ() - 3; z <= player.getZ() + 3; z++) {
                    if (x == player.getX() && z == player.getZ()) {
                        continue;
                    }
                    BlockPos pos = new BlockPos(x, y, z);
                    if (Minecraft.getMinecraft().world.getBlockState(pos).getBlock().equals(Block.getBlockFromName("crafting_table"))) {
                        Memory.scanBlock(pos);
                    }
                    if (Baritone.isAir(pos)) {
                        for (EnumFacing f : EnumFacing.values()) {
                            BlockPos placeAgainst = pos.offset(f);
                            if (!Baritone.isAir(placeAgainst) && Minecraft.getMinecraft().world.getBlockState(placeAgainst).getBlock().isBlockNormalCube()) {
                                if (LookManager.couldIReach(placeAgainst, f.getOpposite())) {
                                    MovementManager.sneak = true;
                                    double faceX = (pos.getX() + placeAgainst.getX() + 1.0D) * 0.5D;
                                    double faceY = (pos.getY() + placeAgainst.getY()) * 0.5D;
                                    double faceZ = (pos.getZ() + placeAgainst.getZ() + 1.0D) * 0.5D;
                                    if (LookManager.lookAtCoords(faceX, faceY, faceZ, true) && Minecraft.getMinecraft().player.isSneaking()) {
                                        Minecraft.getMinecraft().rightClickMouse();
                                    }
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
    public static boolean hasCraftingTableInInventory() {
        EntityPlayerSP p = Minecraft.getMinecraft().player;
        ItemStack[] inv = p.inventory.mainInventory;
        for (ItemStack item : inv) {
            if (item == null) {
                continue;
            }
            if (Item.getByNameOrId("minecraft:crafting_table").equals(item.getItem())) {
                return true;
            }
        }
        return false;
    }
    public static boolean putCraftingTableOnHotBar() {//shamelessly copied from MickeyMine.torch()
        EntityPlayerSP p = Minecraft.getMinecraft().player;
        ItemStack[] inv = p.inventory.mainInventory;
        for (int i = 0; i < 9; i++) {
            ItemStack item = inv[i];
            if (inv[i] == null) {
                continue;
            }
            if (Item.getByNameOrId("minecraft:crafting_table").equals(item.getItem())) {
                p.inventory.currentItem = i;
                return true;
            }
        }
        return false;
    }
    public void clearAll() {
        if (stackSize != 0) {
            decreaseNeededAmount(stackSize);
        }
    }
    /**
     *
     * @param inInventory
     * @return did I actually craft some
     */
    public boolean craftAsManyAsICan(boolean inInventory) {
        int amtCurrentlyCraftable = stackSize - alreadyHas;
        if (amtCurrentlyCraftable > 64) {
            amtCurrentlyCraftable = 64;
        }
        while (true) {
            Boolean b = actualDoCraft(amtCurrentlyCraftable, inInventory, false);
            if (b != null) {
                return b;
            }
            amtCurrentlyCraftable--;
            if (amtCurrentlyCraftable <= 0) {
                return false;
            }
        }
    }
    /**
     *
     * @param outputQuantity
     * @param inInventory
     * @param justChecking
     * @return true if it was able to craft and did, null if it was unable to
     * craft because of a lack of input items, false for anything else
     * (including being unable to craft for other reasons)
     */
    public Boolean actualDoCraft(int outputQuantity, boolean inInventory, boolean justChecking) {
        IRecipe currentRecipe = getRecipeFromItem(currentlyCrafting);
        int outputVolume = currentRecipe.getRecipeOutput().stackSize;
        int inputQuantity = (int) Math.ceil(((double) outputQuantity) / (outputVolume));
        if (currentRecipe instanceof ShapedRecipes) {
            ShapedRecipes shaped = (ShapedRecipes) currentRecipe;
            if (!inInventory || (inInventory && shaped.recipeHeight <= 2 && shaped.recipeWidth <= 2)) {
                int numNotNull = 0;
                for (ItemStack recipeItem : shaped.recipeItems) {
                    if (recipeItem != null) {
                        numNotNull++;
                    }
                }
                Item[] items = new Item[numNotNull];
                int[] positions = new int[items.length];
                int index = 0;
                for (int i = 0; i < shaped.recipeItems.length; i++) {
                    if (shaped.recipeItems[i] == null) {
                        continue;
                    }
                    items[index] = shaped.recipeItems[i].getItem();
                    positions[index] = map(i, shaped.recipeWidth, shaped.recipeHeight, inInventory ? 2 : 3);
                    index++;
                }
                return actualDoCraftOne(items, positions, inputQuantity, inInventory, justChecking);
            }
        }
        if (currentRecipe instanceof ShapelessRecipes) {
            ShapelessRecipes shapeless = (ShapelessRecipes) currentRecipe;
            if (!inInventory || (inInventory && shapeless.getRecipeSize() < 4)) {
                Item[] items = new Item[shapeless.getRecipeSize()];
                int[] positions = new int[items.length];
                for (int i = 0; i < items.length; i++) {
                    items[i] = shapeless.recipeItems.get(i).getItem();
                    positions[i] = i + 1;
                }
                return actualDoCraftOne(items, positions, inputQuantity, inInventory, justChecking);
            }
        }
        return justChecking ? null : false;
    }
    /**
     *
     * @param items
     * @param positions
     * @param amount
     * @param inv
     * @param justChecking
     * @return true if it was able to craft and did, null if it was unable to
     * craft because of a lack of input items, false for anything else
     * (including being unable to craft for other reasons)
     */
    public Boolean actualDoCraftOne(Item[] items, int[] positions, int amount, boolean inv, boolean justChecking) {
        int[] amounts = new int[items.length];
        for (int i = 0; i < items.length; i++) {
            amounts[i] = amount;
        }
        int[] count = new int[items.length];
        for (ItemStack in : Minecraft.getMinecraft().player.inventory.mainInventory) {
            if (in == null) {
                continue;
            }
            Item item = in.getItem();
            int size = in.stackSize;
            for (int j = 0; j < items.length; j++) {
                if (items[j].equals(item)) {
                    int amountRemain = amounts[j] - count[j];
                    if (amountRemain >= size) {
                        count[j] += size;
                        size = 0;
                    } else {
                        count[j] += amountRemain;
                        size -= amountRemain;
                    }
                }
            }
        }
        for (int i = 0; i < count.length; i++) {
            if (count[i] != amounts[i]) {
                //Out.gui("Not enough " + items[i], true);
                return null;
            }
        }
        if (justChecking) {
            return false;
        }
        if (inv) {
            if (Minecraft.getMinecraft().currentScreen == null || !(Minecraft.getMinecraft().currentScreen instanceof GuiInventory)) {
                Out.log("Opening");
                Baritone.slowOpenInventory();
            }
            didIOpenMyInventory = true;
        } else if (Minecraft.getMinecraft().currentScreen == null || !(Minecraft.getMinecraft().currentScreen instanceof GuiCrafting)) {
            Out.gui("Not in crafting table", Out.Mode.Debug);
            return false;
        } else {
            didIOpenMyInventory = true;
        }
        GuiContainer contain = (GuiContainer) Minecraft.getMinecraft().currentScreen;
        for (int i = 1; i < (inv ? 5 : 10); i++) {
            if (contain.inventorySlots.inventorySlots.get(i).getHasStack()) {
                return false;
            }
        }
        Out.gui("Crafting amount " + amount + " of " + currentlyCrafting(), Out.Mode.Debug);
        plan = new ArrayList();
        tickNumber = -10;
        for (int i = inv ? 9 : 10; i < contain.inventorySlots.inventorySlots.size(); i++) {
            Slot slot = contain.inventorySlots.inventorySlots.get(i);
            if (!slot.getHasStack()) {
                continue;
            }
            ItemStack in = slot.getStack();
            if (in == null) {
                continue;
            }
            Item item = in.getItem();
            int size = in.stackSize;
            for (int j = 0; j < items.length; j++) {
                if (amounts[j] <= 0) {
                    continue;
                }
                if (items[j].equals(item)) {
                    leftClick(i);
                    if (size <= amounts[j]) {
                        leftClick(positions[j]);
                        amounts[j] -= size;
                        size = 0;
                    } else {
                        for (int k = 0; k < amounts[j]; k++) {
                            rightClick(positions[j]);
                        }
                        size -= amounts[j];
                        leftClick(i);
                        amounts[j] = 0;
                    }
                }
            }
        }
        Out.gui("shift clicking " + contain.inventorySlots.inventorySlots.get(0).getStack(), Out.Mode.Debug);
        shiftClick(0);
        for (int i = 0; i < amounts.length; i++) {
            if (amounts[i] > 0) {
                Out.gui("Not enough " + i + " " + amounts[i] + " " + items[i] + " " + positions[i], Out.Mode.Debug);//this detects if it didn't have enough, but you shouldn't call this function unless you have already made sure you have enough
            }
        }
        return true;
    }
    public void leftClick(int slot) {
        if (!plan.isEmpty()) {
            int[] last = plan.get(plan.size() - 1);
            if (last[0] == slot && last[1] == 0 && last[2] == 0) {
                plan.remove(plan.size() - 1);
                return;
            }
        }
        plan.add(new int[]{slot, 0, 0});
    }
    public void rightClick(int slot) {
        plan.add(new int[]{slot, 1, 0});
    }
    public void shiftClick(int slot) {
        plan.add(new int[]{slot, 0, 1});
    }
    static boolean didIOpenMyInventory = false;
    static boolean waitingToClose = false;
    static int TUC = 20;
    @Override
    protected boolean onTick0() {
        for (CraftingTask craftingTask : overallCraftingTasks) {
            if (craftingTask.plan != null) {
                Out.log(craftingTask + " " + craftingTask.currentlyCrafting() + " " + craftingTask.plan);
                if (!craftingTask.onTick1()) {
                    didIOpenMyInventory = true;
                }
                return true;
            }
        }
        for (CraftingTask craftingTask : overallCraftingTasks) {
            if (craftingTask.onTick1()) {
                return false;
            }
        }
        if (didIOpenMyInventory) {
            waitingToClose = true;
            TUC = 3;
            didIOpenMyInventory = false;
        }
        if (waitingToClose) {
            TUC--;
            if (TUC <= 0) {
                Out.gui("Closing screen!!!", Out.Mode.Debug);
                Minecraft.getMinecraft().player.closeScreen();
                waitingToClose = false;
                TUC = 3;
            }
            return true;
        }
        return false;
    }
    public final void buildTasks() {
        IRecipe currentRecipe = getRecipeFromItem(currentlyCrafting);
        if (!(currentRecipe == null)) {
            if (currentRecipe instanceof ShapedRecipes) {
                ShapedRecipes shapedRecipe = (ShapedRecipes) currentRecipe;
                for (ItemStack input : shapedRecipe.recipeItems) {
                    if (input == null) {
                        continue;
                    }
                    IRecipe inputRecipe = getRecipeFromItem(input.getItem());
                    if (!(inputRecipe == null)) {
                        Out.log("As a part of " + currentlyCrafting + ", getting " + input);
                        CraftingTask newTask = CraftingTask.findOrCreateCraftingTask(new ItemStack(input.getItem(), 0));
                        subCraftingTasks.add(newTask);
                        //newTask.execute();
                    }
                }
            } else if (currentRecipe instanceof ShapelessRecipes) {
                ShapelessRecipes shapelessRecipe = (ShapelessRecipes) currentRecipe;
                for (ItemStack input : shapelessRecipe.recipeItems) {
                    IRecipe inputRecipe = getRecipeFromItem(input.getItem());
                    if (!(inputRecipe == null)) {
                        Out.log("As a part of " + currentlyCrafting + ", getting " + input);
                        CraftingTask newTask = CraftingTask.findOrCreateCraftingTask(new ItemStack(input.getItem(), 0));
                        subCraftingTasks.add(newTask);
                        //newTask.execute();
                    }
                }
            } else {
                throw new IllegalStateException("Current recipe isn't shapeless or shaped");
            }
        } else {
            throw new IllegalArgumentException("no recipe for this");
        }
    }
    public static CraftingTask findOrCreateCraftingTask(ItemStack itemStack) {
        //Out.log("Getting a task for " + itemStack);
        for (CraftingTask selectedTask : overallCraftingTasks) {
            if (selectedTask.currentlyCrafting().getItem().equals(itemStack.getItem())) {
                if (itemStack.stackSize != 0) {
                    selectedTask.increaseNeededAmount(itemStack.stackSize);
                }
                return selectedTask;
            }
        }
        CraftingTask newTask = new CraftingTask(itemStack);
        overallCraftingTasks.add(newTask);
        return newTask;
    }
    public ItemStack currentlyCrafting() {
        return new ItemStack(currentlyCrafting, stackSize);
    }
    public final void increaseNeededAmount(int amount) {
        //Out.gui(currentlyCrafting() + " inc " + amount);
        int stackSizeBefore = stackSize;
        stackSize += amount;
        IRecipe currentRecipe = getRecipeFromItem(currentlyCrafting);
        int outputVolume = currentRecipe.getRecipeOutput().stackSize;
        int inputQuantityBefore = (int) Math.ceil(((double) stackSizeBefore) / outputVolume);
        int inputQuantityNew = (int) Math.ceil(((double) stackSize) / outputVolume);
        int change = inputQuantityNew - inputQuantityBefore;
        if (change != 0) {
            /*for (CraftingTask craftingTask : subCraftingTasks) {
             Out.gui("> inc sub " + craftingTask.currentlyCrafting() + " " + change);
             }*/
            for (CraftingTask craftingTask : subCraftingTasks) {
                craftingTask.increaseNeededAmount(change);
            }
        }
    }
    public void decreaseNeededAmount(int amount) {
        //Out.gui(currentlyCrafting() + " dec " + amount);
        int stackSizeBefore = stackSize;
        stackSize -= amount;
        IRecipe currentRecipe = getRecipeFromItem(currentlyCrafting);
        int outputVolume = currentRecipe.getRecipeOutput().stackSize;
        int inputQuantityBefore = (int) Math.ceil(((double) stackSizeBefore) / (outputVolume));
        int inputQuantityNew = (int) Math.ceil(((double) stackSize) / outputVolume);
        int change = inputQuantityBefore - inputQuantityNew;
        if (change != 0) {
            /*for (CraftingTask craftingTask : subCraftingTasks) {
             Out.gui("> dec sub " + craftingTask.currentlyCrafting() + " " + change);
             }*/
            for (CraftingTask craftingTask : subCraftingTasks) {
                craftingTask.decreaseNeededAmount(change);
            }
        }
    }
    public void calculateAlreadyHasAmount() {
        int count = 0;
        for (ItemStack armor : Minecraft.getMinecraft().player.inventory.armorInventory) {
            if (armor == null) {
                continue;
            }
            if (currentlyCrafting.equals(armor.getItem())) {
                count += armor.stackSize;
            }
        }
        for (int i = 0; i < Minecraft.getMinecraft().player.inventory.getSizeInventory(); i++) {
            if (Minecraft.getMinecraft().player.inventory.getStackInSlot(i) == null) {
                continue;
            }
            if (Minecraft.getMinecraft().player.inventory.getStackInSlot(i).getItem().equals(currentlyCrafting)) {
                count += Minecraft.getMinecraft().player.inventory.getStackInSlot(i).stackSize;
            }
        }
        alreadyHas = count;
    }
    public int alreadyHas() {
        return alreadyHas;
    }
    public boolean isDone() {
        calculateAlreadyHasAmount();
        return stackSize <= alreadyHas;
    }
    public static boolean ensureCraftingDesired(Item item, int quantity) {
        if (item == null) {
            throw new NullPointerException();
        }
        CraftingTask craftingTableTask = CraftingTask.findOrCreateCraftingTask(new ItemStack(item, 0));
        //Out.log(craftingTableTask.currentlyCrafting() + " " + quantity + " " + craftingTableTask.stackSize + " " + craftingTableTask.alreadyHas + " " + craftingTableTask.isDone());
        if (craftingTableTask.isDone() && craftingTableTask.alreadyHas >= quantity) {
            if (craftingTableTask.stackSize > 0) {
                craftingTableTask.decreaseNeededAmount(1);
            }
            return true;
        }
        if (craftingTableTask.stackSize < quantity) {
            craftingTableTask.increaseNeededAmount(quantity - craftingTableTask.stackSize);
        }
        return craftingTableTask.alreadyHas >= quantity;
    }
    public static Manager createInstance(Class c) {
        return new CraftingTask();
    }
    private CraftingTask() {
    }
    @Override
    protected void onCancel() {
        overallCraftingTasks.clear();
    }
    @Override
    protected void onStart() {
    }
    @Override
    protected boolean onEnabled(boolean enabled) {
        return true;
    }
}
