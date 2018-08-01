/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.inventory;

import baritone.ui.LookManager;
import baritone.util.Memory;
import java.util.ArrayList;
import baritone.Baritone;
import baritone.movement.MovementManager;
import baritone.pathfinding.goals.GoalComposite;
import baritone.pathfinding.goals.GoalGetToBlock;
import baritone.util.Autorun;
import baritone.util.ChatCommand;
import baritone.util.ChatCommand;
import baritone.util.Manager;
import baritone.util.Manager;
import baritone.util.Memory;
import baritone.util.Out;
import baritone.util.Out;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

/**
 *
 * @author avecowa
 */
public class AnotherStealer extends Manager {
    protected static Manager newInstance() {
        return null;
    }
    public static ArrayList<BlockPos> alreadyStolenFrom = new ArrayList<BlockPos>();
    public static boolean chestStuff = false;
    public static boolean stuff = false;
    public static BlockPos current = null;
    private static final Block CHEST = Block.getBlockFromName("chest");
    private static boolean positionArmor = false;
    private static int positionSlot = 0;
    private static int positionStatus = 0;
    @Override
    public void onTick() {
        //try{
        if (invFull()) {
            ChatCommand.stealer("stealer");
            return;
        }
        if (Baritone.isThereAnythingInProgress || Baritone.currentPath != null) {
            Out.log(Baritone.currentPath);
            return;
        }
        if (stuff) {
            stuff = false;
            ArrayList<BlockPos> chests = Memory.closest(100, "chest");
            chests.removeAll(alreadyStolenFrom);
            if (chests.isEmpty()) {
                return;
            }
            BlockPos[] goals = GoalGetToBlock.ajacentBlocks(chests.get(0));
            for (int i = 1; i < chests.size(); i++) {
                goals = Autorun.concat(goals, GoalGetToBlock.ajacentBlocks(chests.get(i)));
            }
            Baritone.goal = new GoalComposite(goals);
            ChatCommand.path("path false");
            return;
        }
        if (positionArmor) {
            if (!(Minecraft.getMinecraft().currentScreen instanceof GuiInventory)) {
                Out.gui("BAD GUI", Out.Mode.Debug);
                positionArmor = false;
                return;
            }
            Out.gui("Position Armor:" + positionSlot, Out.Mode.Debug);
            if (positionStatus == 0) {
                Container inv = Minecraft.getMinecraft().player.inventoryContainer;
                Out.gui("Position Status 0:" + inv.inventorySlots.size(), Out.Mode.Debug);
                for (int i = positionSlot; i < 45; i++) {
                    Out.gui((inv.getSlot(i).getHasStack() ? inv.getSlot(i).getStack().getItem().toString() : "NULL STACK") + " :" + i, Out.Mode.Debug);
                    if (inv.getSlot(i).getHasStack() && inv.getSlot(i).getStack().getItem() instanceof ItemArmor) {
                        Out.gui("ITEM IS ARMOR", Out.Mode.Debug);
                        ItemArmor armor = (ItemArmor) inv.getSlot(i).getStack().getItem();
                        if (inv.getSlot(armor.armorType).getHasStack() && ((ItemArmor) inv.getSlot(armor.armorType).getStack().getItem()).damageReduceAmount < armor.damageReduceAmount) {
                            positionSlot = i;
                            positionStatus = 1;
                            Minecraft.getMinecraft().playerController.windowClick(((GuiContainer) Minecraft.getMinecraft().currentScreen).inventorySlots.windowId, 103 - armor.armorType, 0, 1, Minecraft.getMinecraft().player);
                            return;
                        }
                    }
                }
                positionArmor = false;
                Minecraft.getMinecraft().player.closeScreen();
                return;
            }
            if (positionStatus == 1) {
                Minecraft.getMinecraft().playerController.windowClick(((GuiContainer) Minecraft.getMinecraft().currentScreen).inventorySlots.windowId, positionSlot, 0, 1, Minecraft.getMinecraft().player);
                positionStatus = 0;
                return;
            }
        }
        BlockPos near = getAjacentChest();
        if (near == null) {
            stuff = true;
            return;
        }
        if (near.equals(Baritone.whatAreYouLookingAt())) {
            if (chestStuff) {
                Out.gui("CHEST STUFF", Out.Mode.Debug);
                EntityPlayerSP player = Minecraft.getMinecraft().player;
                WorldClient world = Minecraft.getMinecraft().world;
                if (Minecraft.getMinecraft().currentScreen == null) {
                    chestStuff = false;
                    Out.gui("NULL GUI", Out.Mode.Debug);
                    return;
                }
                if (!(Minecraft.getMinecraft().currentScreen instanceof GuiChest)) {
                    Out.gui("NOT CHEST GUI", Out.Mode.Debug);
                    return;
                }
                GuiChest contain = (GuiChest) Minecraft.getMinecraft().currentScreen;
                Slot slot = getFilledSlot(contain);
                Out.gui(slot == null ? "null slot" : slot.getHasStack() ? slot.getStack().getItem().toString() : "empty slot", Out.Mode.Debug);
                if (slot == null) {
                    Out.gui("CLOSING THE SCREEN", Out.Mode.Debug);
                    alreadyStolenFrom.add(near);
                    positionArmor = true;
                    positionSlot = 9;
                    positionStatus = 0;
                    Baritone.slowOpenInventory();
                    return;
                }
                contain.shiftClick(slot.slotNumber);
                return;
            }
            Out.gui("NO CHEST STUFF", Out.Mode.Debug);
            chestStuff = true;
            MovementManager.isRightClick = true;
            current = Baritone.whatAreYouLookingAt();
            return;
        }
        LookManager.lookAtBlock(near, true);
        return;
    }
    public static BlockPos getAjacentChest() {
        BlockPos[] pos = GoalGetToBlock.ajacentBlocks(Baritone.playerFeet);
        WorldClient w = Minecraft.getMinecraft().world;
        for (BlockPos p : pos) {
            if (!alreadyStolenFrom.contains(p) && w.getBlockState(p).getBlock().equals(CHEST)) {
                return p;
            }
        }
        return null;
    }
    public static Slot getFilledSlot(GuiChest chest) {
        for (int i = 0; i < chest.lowerChestInventory.getSizeInventory(); i++) {
            if (chest.lowerChestInventory.getStackInSlot(i) != null) {
                return chest.inventorySlots.getSlotFromInventory(chest.lowerChestInventory, i);
            }
        }
        return null;
    }
    public static boolean invFull() {
        ItemStack[] inv = Minecraft.getMinecraft().player.inventory.mainInventory;
        for (ItemStack i : inv) {
            if (i == null) {
                return false;
            }
        }
        return true;
    }
    @Override
    protected void onCancel() {
    }
    @Override
    protected void onStart() {
        alreadyStolenFrom = new ArrayList<BlockPos>();
        chestStuff = false;
        stuff = false;
        current = null;
        positionArmor = false;
        positionSlot = 0;
    }
}
