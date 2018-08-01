/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
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
import static baritone.inventory.CraftingTask.placeHeldBlockNearby;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiFurnace;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.math.BlockPos;

/**
 *
 * @author leijurv
 */
public class SmeltingTask extends ManagerTick {
    static HashMap<BlockPos, SmeltingTask> furnacesInUse = new HashMap();//smelting tasks that have been put in a furnace are here
    static ArrayList<SmeltingTask> inProgress = new ArrayList();//all smelting tasks will be in here
    public static boolean coalOnly = false;
    public static boolean avoidBreaking(BlockPos pos) {
        return furnacesInUse.containsKey(pos);
    }
    public static Manager createInstance(Class c) {
        return new SmeltingTask();
    }
    private SmeltingTask() {
        toPutInTheFurnace = null;
        desired = null;
        burnTicks = 0;
    }
    @Override
    protected boolean onTick0() {
        for (SmeltingTask task : new ArrayList<SmeltingTask>(inProgress)) {//make a copy because of concurrent modification bs
            if (task.plan != null) {
                task.exec();
                return true;
            }
        }
        for (SmeltingTask task : new ArrayList<SmeltingTask>(inProgress)) {//make a copy because of concurrent modification bs
            if (task.exec()) {
                return false;
            }
        }
        return false;
    }
    public static int tasksFor(Item result) {
        int sum = 0;
        for (SmeltingTask task : inProgress) {
            if (result.equals(task.desired.getItem())) {
                sum += task.desired.stackSize;
            }
        }
        return sum;
    }
    public static void clearInProgress() {
        for (int i = 0; i < inProgress.size(); i++) {
            if (inProgress.get(i).isItDone) {
                inProgress.remove(i);
                i--;
            }
        }
    }
    public static BlockPos getUnusedFurnace() {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : Memory.closest(100, "furnace", "lit_furnace")) {
            if (furnacesInUse.get(pos) != null) {
                continue;
            }
            double dist = dist(pos);
            if (best == null || dist < bestDist) {
                bestDist = dist;
                best = pos;
            }
        }
        return best;
    }
    public static double dist(BlockPos pos) {
        EntityPlayerSP thePlayer = Minecraft.getMinecraft().player;
        double diffX = thePlayer.posX - pos.getX();
        double diffY = thePlayer.posY - pos.getY();
        double diffZ = thePlayer.posZ - pos.getZ();
        return Math.sqrt(diffX * diffX + diffY * diffY + diffZ * diffZ);
    }
    private final ItemStack toPutInTheFurnace;
    private final ItemStack desired;
    private BlockPos furnace = null;
    private boolean didIPutItInAlreadyPhrasing = false;
    private boolean isItDone = false;
    public final int burnTicks;
    public SmeltingTask(ItemStack desired) {
        toPutInTheFurnace = recipe(desired);
        if (toPutInTheFurnace == null) {
            String m = "Babe I can't smelt anyting to make " + desired;
            Out.gui(m, Out.Mode.Minimal);
            throw new IllegalArgumentException(m);
        }
        burnTicks = toPutInTheFurnace.stackSize * 200;
        this.desired = desired;
    }
    public void begin() {
        if (inProgress.contains(this)) {
            return;
        }
        inProgress.add(this);
        //todo: merge different smelting tasks for the same item
    }
    int numTicks = -20;//wait a couple extra ticks, for no reason (I guess server lag maybe)
    int guiWaitTicks = 0;
    int shiftWaitTicks = 0;
    private boolean exec() {
        Out.log(didIPutItInAlreadyPhrasing + " " + isItDone + " " + numTicks + " " + burnTicks + " " + furnace + " " + Minecraft.getMinecraft().currentScreen == null);
        if (!didIPutItInAlreadyPhrasing && Minecraft.getMinecraft().currentScreen == null) {
            BlockPos furnaceLocation = getUnusedFurnace();
            if (furnaceLocation != null) {
                if (LookManager.couldIReach(furnaceLocation)) {
                    LookManager.lookAtBlock(furnaceLocation, true);
                    if (furnaceLocation.equals(Baritone.whatAreYouLookingAt())) {
                        furnace = furnaceLocation;
                        Baritone.currentPath = null;
                        MovementManager.clearMovement();
                        Minecraft.getMinecraft().rightClickMouse();
                    }
                    return true;
                } else {
                    double diffX = furnaceLocation.getX() + 0.5D - Minecraft.getMinecraft().player.posX;
                    double diffY = furnaceLocation.getY() + 0.5D - Minecraft.getMinecraft().player.posY;
                    double diffZ = furnaceLocation.getZ() + 0.5D - Minecraft.getMinecraft().player.posZ;
                    double distXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
                    if (distXZ < 50 && Math.abs(diffY) < 20) {
                        Baritone.goal = new GoalComposite(new GoalBlock(furnaceLocation.up()), new GoalBlock(furnaceLocation.north()), new GoalBlock(furnaceLocation.south()), new GoalBlock(furnaceLocation.east()), new GoalBlock(furnaceLocation.west()), new GoalBlock(furnaceLocation.north().down()), new GoalBlock(furnaceLocation.south().down()), new GoalBlock(furnaceLocation.east().down()), new GoalBlock(furnaceLocation.west().down()));
                        if (Baritone.currentPath == null && !Baritone.isPathFinding()) {
                            Baritone.findPathInNewThread(false);
                        }
                        return true;
                    } else {
                        Out.gui("too far away from closest furnace (" + distXZ + " blocks), crafting another", Out.Mode.Standard);
                    }
                }
            }
            if (putFurnaceOnHotBar()) {
                Out.log("Ready to place!");
                if (placeHeldBlockNearby()) {
                    return true;
                }
                BlockPos player = Baritone.playerFeet;
                if (Baritone.isAir(player.down()) || Baritone.isAir(player.up(2))) {
                    Out.gui("Placing down", Out.Mode.Debug);
                    LookManager.lookAtBlock(Baritone.playerFeet.down(), true);
                    MovementManager.jumping = true;
                    if (Baritone.playerFeet.down().equals(Baritone.whatAreYouLookingAt()) || Baritone.playerFeet.down().down().equals(Baritone.whatAreYouLookingAt())) {
                        Minecraft.getMinecraft().rightClickMouse();
                    }
                    return true;
                }
                return true;
            } else if (hasFurnaceInInventory()) {
                InventoryManager.putOnHotBar(Item.getByNameOrId("furnace"));
                return true;
            }
            return false;
        }
        boolean guiOpen = Minecraft.getMinecraft().currentScreen != null && Minecraft.getMinecraft().currentScreen instanceof GuiFurnace;
        boolean ret = false;
        if (guiOpen) {
            guiWaitTicks++;
            if (guiWaitTicks < 5) {
                guiOpen = false;
            }
            if (!didIPutItInAlreadyPhrasing) {
                if (furnace == null) {
                    furnace = Baritone.whatAreYouLookingAt();
                }
                furnacesInUse.put(furnace, this);
                ret = true;
            }
        } else {
            guiWaitTicks = 0;
        }
        if (guiOpen) {
            GuiFurnace contain = (GuiFurnace) Minecraft.getMinecraft().currentScreen;
            if (!didIPutItInAlreadyPhrasing) {
                Boolean b = realPutItIn_PHRASING(contain);
                if (b != null && b) {
                    didIPutItInAlreadyPhrasing = true;
                    ret = true;//done
                }
                if (b == null) {
                    ret = true;//in progress
                }
                if (b != null && !b) {
                    ret = false;
                }
            }
            if (isItDone && furnace.equals(Baritone.whatAreYouLookingAt())) {//if we are done, and this is our furnace
                ret = true;
                Out.gui("taking it out", Out.Mode.Debug);
                if (isEmpty(contain, 2)) {//make sure
                    if (shiftWaitTicks > 5) {
                        Minecraft.getMinecraft().player.closeScreen();//close the screen
                        inProgress.remove(this);//no longer an in progress smelting dask
                        Out.gui("Smelting " + desired + " totally done m9", Out.Mode.Debug);
                        return false;
                    }
                    shiftWaitTicks++;
                } else {
                    shiftWaitTicks = 0;
                    if (numTicks % 5 == 0) {
                        contain.shiftClick(2);//take out the output
                    }
                }
            }
        }
        if (didIPutItInAlreadyPhrasing) {
            numTicks++;
            if (Memory.blockLoaded(furnace)) {
                Block curr = Minecraft.getMinecraft().world.getBlockState(furnace).getBlock();
                if (!Block.getBlockFromName("furnace").equals(curr) && !Block.getBlockFromName("lit_furnace").equals(curr)) {
                    Out.gui("Furnace at " + furnace + " is now gone. RIP. Was trying to make " + desired + ". Is now " + curr, Out.Mode.Standard);
                    inProgress.remove(this);
                    return false;
                }
            }
            if (!isItDone && numTicks >= burnTicks) {
                isItDone = true;
                Out.gui("Hey we're done. Go to your furnace at " + furnace + " and pick up " + desired, Out.Mode.Debug);
                furnacesInUse.put(furnace, null);
            }
            if (isItDone) {
                MickeyMine.tempDisable = true;
            }
            if (isItDone && !guiOpen) {
                if (LookManager.couldIReach(furnace)) {
                    Baritone.currentPath = null;
                    MovementManager.clearMovement();
                    LookManager.lookAtBlock(furnace, true);
                    if (furnace.equals(Baritone.whatAreYouLookingAt())) {
                        Minecraft.getMinecraft().rightClickMouse();
                    }
                } else {
                    Baritone.goal = new GoalComposite(new GoalBlock(furnace.up()), new GoalBlock(furnace.north()), new GoalBlock(furnace.south()), new GoalBlock(furnace.east()), new GoalBlock(furnace.west()), new GoalBlock(furnace.north().down()), new GoalBlock(furnace.south().down()), new GoalBlock(furnace.east().down()), new GoalBlock(furnace.west().down()));
                    if (Baritone.currentPath == null && !Baritone.isThereAnythingInProgress) {
                        Baritone.findPathInNewThread(false);
                    }
                }
            }
            if (isItDone && (numTicks - 1) % (60 * 20) == 0) {
                Out.gui("DUDE. Go to your furnace at " + furnace + " and pick up " + desired + ". Do /cancelfurnace if you want these notifications to piss off.", Out.Mode.Minimal);
            }
        }
        return ret;
    }
    public static boolean hasFurnaceInInventory() {
        EntityPlayerSP p = Minecraft.getMinecraft().player;
        ItemStack[] inv = p.inventory.mainInventory;
        for (ItemStack item : inv) {
            if (item == null) {
                continue;
            }
            if (Item.getByNameOrId("minecraft:furnace").equals(item.getItem())) {
                return true;
            }
        }
        return false;
    }
    public static boolean putFurnaceOnHotBar() {//shamelessly copied from MickeyMine.torch()
        EntityPlayerSP p = Minecraft.getMinecraft().player;
        ItemStack[] inv = p.inventory.mainInventory;
        for (int i = 0; i < 9; i++) {
            ItemStack item = inv[i];
            if (inv[i] == null) {
                continue;
            }
            if (Item.getByNameOrId("furnace").equals(item.getItem())) {
                p.inventory.currentItem = i;
                return true;
            }
        }
        return false;
    }
    public boolean isInFurnace() {
        return didIPutItInAlreadyPhrasing;
    }
    public boolean lookingForFurnace() {
        return furnace == null;
    }
    private ArrayList<int[]> plan;
    int tickNumber = 0;
    static int ticksBetweenClicks = 4;
    public boolean tickPlan() {
        GuiContainer contain = (GuiContainer) Minecraft.getMinecraft().currentScreen;
        if (tickNumber % ticksBetweenClicks == 0) {
            int index = tickNumber / ticksBetweenClicks;
            if (index >= plan.size()) {
                if (index >= plan.size() + 2) {
                    Out.gui("Plan over", Out.Mode.Debug);
                    plan = null;
                    tickNumber = -40;
                    Minecraft.getMinecraft().player.closeScreen();
                    return true;
                }
                tickNumber++;
                return false;
            }
            if (index >= 0) {
                int[] click = plan.get(index);
                Out.gui(index + " " + click[0] + " " + click[1] + " " + click[2] + " " + desired, Out.Mode.Debug);
                contain.sketchyMouseClick(click[0], click[1], click[2]);
                Out.log("Ticking plan");
            }
        }
        tickNumber++;
        return false;
    }
    private Boolean realPutItIn_PHRASING(GuiFurnace contain) {//null: in progress. false: unable. true: done
        if (plan == null) {
            if (!generatePlan(contain)) {
                return false;
            }
            return null;
        }
        if (tickPlan()) {
            return true;
        }
        return null;
    }
    private boolean generatePlan(GuiFurnace contain) {
        int desiredAmount = toPutInTheFurnace.stackSize;
        if (currentSize1(contain, 0, toPutInTheFurnace.getItem()) == -1) {
            Out.gui("Furnace already in use", Out.Mode.Debug);
            return false;
        }
        ArrayList<Item> burnableItems = new ArrayList<Item>();
        ArrayList<Integer> burnTimes = new ArrayList<Integer>();
        ArrayList<Integer> amountWeHave = new ArrayList<Integer>();
        ArrayList<Integer> amtNeeded = new ArrayList<Integer>();
        for (int i = 3; i < contain.inventorySlots.inventorySlots.size(); i++) {
            Slot slot = contain.inventorySlots.inventorySlots.get(i);
            if (!slot.getHasStack()) {
                continue;
            }
            ItemStack in = slot.getStack();
            if (in == null) {
                continue;
            }
            Item item = in.getItem();
            int ind = burnableItems.indexOf(item);
            if (ind == -1) {
                int time = TileEntityFurnace.getItemBurnTime(in);
                if (time <= 0) {
                    Out.gui(in + " isn't fuel, lol", Out.Mode.Standard);
                    continue;
                }
                burnableItems.add(in.getItem());
                amountWeHave.add(in.stackSize);
                burnTimes.add(time);
                int numRequired = (int) Math.ceil(((double) burnTicks) / (time));
                amtNeeded.add(numRequired);
            } else {
                amountWeHave.set(ind, amountWeHave.get(ind) + in.stackSize);
            }
        }
        for (int i = 0; i < burnableItems.size(); i++) {
            if (amountWeHave.get(i) < amtNeeded.get(i)) {
                Out.gui("Not using fuel " + burnableItems.get(i) + " because not enough (have " + amountWeHave.get(i) + ", need " + amtNeeded.get(i) + ")", Out.Mode.Standard);
                burnableItems.remove(i);
                amountWeHave.remove(i);
                amtNeeded.remove(i);
                burnTimes.remove(i);
                i--;
            }
        }
        if (burnableItems.isEmpty()) {
            Out.gui("lol no fuel", Out.Mode.Standard);
            return false;
        }
        Out.log(burnableItems);
        Out.log(amountWeHave);
        Out.log(amtNeeded);
        Out.log(burnTimes);
        Item bestFuel = null;
        int fuelAmt = Integer.MAX_VALUE;
        int bestExtra = Integer.MAX_VALUE;
        for (int i = 0; i < burnableItems.size(); i++) {
            int amt = amtNeeded.get(i);
            int extra = burnTimes.get(i) * amtNeeded.get(i) - burnTicks;
            boolean better = extra < bestExtra || (extra == bestExtra && amt < fuelAmt);
            boolean thisisCoal = Item.getByNameOrId("coal").equals(burnableItems.get(i));
            boolean bestIsCoal = Item.getByNameOrId("coal").equals(bestFuel);
            if ((better && !coalOnly) || (coalOnly && ((thisisCoal && !bestIsCoal) || (thisisCoal && bestIsCoal && better) || (!thisisCoal && !bestIsCoal && better)))) {
                fuelAmt = amt;
                bestExtra = extra;
                bestFuel = burnableItems.get(i);
            }
        }
        Out.gui("Using " + fuelAmt + " items of " + bestFuel + ", which wastes " + bestExtra + " ticks of fuel.", Out.Mode.Debug);
        int currFuelSize = 0;
        if (currentSize1(contain, 1, bestFuel) == -1) {
            Out.gui("Furnace already in use", Out.Mode.Debug);
            return false;
        }
        if (currentSize1(contain, 0, toPutInTheFurnace.getItem()) == -1) {
            Out.gui("Furnace already in use", Out.Mode.Debug);
            return false;
        }
        plan = new ArrayList();
        tickNumber = -10;
        int currSmeltSize = 0;
        for (int i = 3; i < contain.inventorySlots.inventorySlots.size(); i++) {
            Slot slot = contain.inventorySlots.inventorySlots.get(i);
            if (!slot.getHasStack()) {
                continue;
            }
            ItemStack in = slot.getStack();
            if (in == null) {
                continue;
            }
            if (in.getItem().equals(toPutInTheFurnace.getItem())) {
                int amountHere = in.stackSize;
                int amountNeeded = desiredAmount - currSmeltSize;
                leftClick(i);
                if (amountNeeded >= amountHere) {
                    leftClick(0);
                    currSmeltSize += amountHere;
                    leftClick(i);
                } else {
                    for (int j = 0; j < amountNeeded; j++) {
                        rightClick(0);
                    }
                    leftClick(i);
                    break;
                }
            }
        }
        for (int i = 3; i < contain.inventorySlots.inventorySlots.size(); i++) {
            Slot slot = contain.inventorySlots.inventorySlots.get(i);
            if (!slot.getHasStack()) {
                continue;
            }
            ItemStack in = slot.getStack();
            if (in == null) {
                continue;
            }
            if (in.getItem().equals(bestFuel)) {
                int currentSize = currFuelSize;
                int amountHere = in.stackSize;
                int amountNeeded = fuelAmt - currentSize;
                leftClick(i);
                if (amountNeeded >= amountHere) {
                    leftClick(1);
                    currFuelSize += amountHere;
                    leftClick(i);
                } else {
                    for (int j = 0; j < amountNeeded; j++) {
                        rightClick(1);
                    }
                    leftClick(i);
                    Out.gui("done with fuel", Out.Mode.Debug);
                    break;
                }
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
    private static boolean isEmpty(GuiFurnace contain, int id) {
        Slot slot = contain.inventorySlots.inventorySlots.get(id);
        if (!slot.getHasStack()) {
            return true;
        }
        return slot.getStack() == null;
    }
    private static int currentSize1(GuiFurnace contain, int id, Item item) {
        Slot slot = contain.inventorySlots.inventorySlots.get(id);
        if (!slot.getHasStack()) {
            return 0;
        }
        ItemStack in = slot.getStack();
        if (in == null) {
            return 0;
        }
        if (!in.getItem().equals(item)) {
            return -1;
        }
        return in.stackSize;
    }
    private static ItemStack recipe(ItemStack desired) {
        for (Entry<ItemStack, ItemStack> recipe : getRecipes().entrySet()) {
            ItemStack input = recipe.getKey();
            ItemStack output = recipe.getValue();
            if (output.getItem().equals(desired.getItem())) {
                int desiredQuantity = desired.stackSize;
                int outputQuantity = output.stackSize;
                int totalQuantity = (int) Math.ceil(((double) desiredQuantity) / (outputQuantity));
                int inputQuantity = input.stackSize * totalQuantity;
                Out.log("Recipe from " + input + " to " + output + " " + desiredQuantity + " " + outputQuantity + " " + totalQuantity + " " + inputQuantity);
                if (inputQuantity > 64) {
                    throw new IllegalStateException("lol");
                }
                return new ItemStack(input.getItem(), inputQuantity, input.getMetadata());
            }
        }
        return null;
    }
    @Override
    protected void onCancel() {
        inProgress.clear();
        furnacesInUse.clear();
    }
    @Override
    protected void onStart() {
    }
    @Override
    protected boolean onEnabled(boolean enabled) {
        return true;
    }

    private static class wrapper {//so that people don't try to directly reference recipess
        private static Map<ItemStack, ItemStack> recipes = null;
        public static Map<ItemStack, ItemStack> getRecipes() {
            if (recipes == null) {
                recipes = FurnaceRecipes.instance().getSmeltingList();
            }
            return recipes;
        }
    }
    public static Map<ItemStack, ItemStack> getRecipes() {
        return wrapper.getRecipes();
    }
}
