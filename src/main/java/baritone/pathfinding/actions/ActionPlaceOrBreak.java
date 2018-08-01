/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.pathfinding.actions;

import baritone.Baritone;
import baritone.movement.MovementManager;
import baritone.ui.LookManager;
import baritone.util.Out;
import baritone.util.ToolSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;

/**
 *
 * @author leijurv
 */
public abstract class ActionPlaceOrBreak extends Action {

    public final BlockPos[] positionsToBreak;//the positions that need to be broken before this action can ensue
    public final BlockPos[] positionsToPlace;//the positions where we need to place a block before this aciton can ensue
    public final Block[] blocksToBreak;//the blocks at those positions
    public final Block[] blocksToPlace;

    public ActionPlaceOrBreak(BlockPos start, BlockPos end, BlockPos[] toBreak, BlockPos[] toPlace) {
        super(start, end);
        this.positionsToBreak = toBreak;
        this.positionsToPlace = toPlace;
        blocksToBreak = new Block[positionsToBreak.length];
        blocksToPlace = new Block[positionsToPlace.length];
        for (int i = 0; i < blocksToBreak.length; i++) {
            blocksToBreak[i] = Baritone.get(positionsToBreak[i]).getBlock();
        }
        for (int i = 0; i < blocksToPlace.length; i++) {
            blocksToPlace[i] = Baritone.get(positionsToPlace[i]).getBlock();
        }
    }

    public double getTotalHardnessOfBlocksToBreak() {//of all the blocks we need to break before starting this action, what's the sum of how hard they are (phrasing)
        ToolSet ts = new ToolSet();
        return this.getTotalHardnessOfBlocksToBreak(ts);
    }

    public double getTotalHardnessOfBlocksToBreak(ToolSet ts) {
        double sum = 0;
        HashSet<BlockPos> toBreak = new HashSet();
        for (BlockPos positionsToBreak1 : positionsToBreak) {
            toBreak.add(positionsToBreak1);
            if (this instanceof ActionFall) {//if we are digging straight down, assume we have already broken the sand above us
                continue;
            }
            BlockPos tmp = positionsToBreak1.up();
            while (canFall(tmp)) {
                toBreak.add(tmp);
                tmp = tmp.up();
            }
        }
        for (BlockPos pos : toBreak) {
            sum += getHardness(ts, Baritone.get(pos), pos);
            if (sum >= COST_INF) {
                return COST_INF;
            }
        }
        if (!Baritone.allowBreakOrPlace || !Baritone.hasThrowaway) {
            for (int i = 0; i < blocksToPlace.length; i++) {
                if (!canWalkOn(positionsToPlace[i])) {
                    return COST_INF;
                }
            }
        }
        return sum;
    }

    public static boolean canFall(BlockPos pos) {
        return Baritone.get(pos).getBlock() instanceof BlockFalling;
    }

    public static double getHardness(ToolSet ts, IBlockState block, BlockPos position) {
        if (!block.equals(Blocks.AIR) && !canWalkThrough(position)) {
            if (avoidBreaking(position)) {
                return COST_INF;
            }
            if (!Baritone.allowBreakOrPlace) {
                return COST_INF;
            }
            double m = Block.getBlockFromName("minecraft:crafting_table").equals(block) ? 10 : 1;
            return m / ts.getStrVsBlock(block, position) + BREAK_ONE_BLOCK_ADD;
        }
        return 0;
    }

    @Override
    public String toString() {
        return this.getClass() + " place " + Arrays.asList(blocksToPlace) + " break " + Arrays.asList(blocksToBreak) + " cost " + cost(null) + " break cost " + getTotalHardnessOfBlocksToBreak();
    }

    @Override
    public boolean tick() {
        //breaking first
        for (int i = 0; i < positionsToBreak.length; i++) {
            if (!canWalkThrough(positionsToBreak[i])) {
                if (!Baritone.allowBreakOrPlace) {
                    Out.gui("BB I can't break this =(((", Out.Mode.Standard);
                    return false;
                }
                //Out.log("Breaking " + blocksToBreak[i] + " at " + positionsToBreak[i]);
                boolean lookingInCorrectDirection = LookManager.lookAtBlock(positionsToBreak[i], true);
                //TODO decide if when actuallyLookingAtTheBlock==true then should it still keep moving the look direction to the exact center of the block
                boolean actuallyLookingAtTheBlock = positionsToBreak[i].equals(Baritone.whatAreYouLookingAt());
                if (!(lookingInCorrectDirection || actuallyLookingAtTheBlock)) {
                    return false;
                }
                /*if (!positionsToBreak[i].equals(Baritone.whatAreYouLookingAt())) {//hmmm, our crosshairs are looking at the wrong block
                 //TODO add a timer here, and if we are stuck looking at the wrong block for more than 1 second, do something
                 //(it cant take longer than twenty ticks, because the Baritone.MAX_YAW_CHANGE_PER_TICK=18, and 18*20 = 360°
                 Out.log("Wrong");
                 return false;
                 }*/
                if (Baritone.whatAreYouLookingAt() != null) {
                    Baritone.switchtotool(Baritone.get(Baritone.whatAreYouLookingAt()));
                }
                MovementManager.isLeftClick = true;//hold down left click
                if (canWalkThrough(positionsToBreak[i])) {
                    MovementManager.letGoOfLeftClick();
                    Out.log("Done breaking " + blocksToBreak[i] + " at " + positionsToBreak[i]);
                }
                return false;
            }
        }
        MovementManager.letGoOfLeftClick();//sometimes it keeps on left clicking so we need this here (yes it scares me too)
        for (BlockPos positionsToPlace1 : positionsToPlace) {
            if (!canWalkOn(positionsToPlace1)) {
                if (!Baritone.allowBreakOrPlace) {
                    Out.gui("BB I can't place this =(((", Out.Mode.Standard);
                    return false;
                }
                //Baritone.lookAtBlock(positionsToPlace[i], true);
                //Out.log("CANT DO IT. CANT WALK ON " + blocksToPlace[i] + " AT " + positionsToPlace[i]);
                //one of the blocks that needs to be there isn't there
                //so basically someone mined out our path from under us
                //
                //this doesn't really do anything, because all the cases for positionToPlace are handled in their respective action tick0s (e.g. pillar and bridge)
            }
        }
        return tick0();
    }
    //I dont want to make this static, because then it might be executed before Item gets initialized
    private static List<Item> ACCEPTABLE_THROWAWAY_ITEMS = null;

    private static void set() {
        if (ACCEPTABLE_THROWAWAY_ITEMS != null) {
            return;
        }
        ACCEPTABLE_THROWAWAY_ITEMS = Arrays.asList(new Item[]{Item.getByNameOrId("minecraft:dirt"), Item.getByNameOrId("minecraft:cobblestone")});
    }

    public static boolean switchtothrowaway(boolean message) {
        set();
        EntityPlayerSP p = Minecraft.getMinecraft().player;
        NonNullList<ItemStack> inv = p.inventory.mainInventory;
        for (byte i = 0; i < 9; i++) {
            ItemStack item = inv.get(i);
            if (item == null) {
                item = new ItemStack(Item.getByNameOrId("minecraft:apple"));
            }
            if (ACCEPTABLE_THROWAWAY_ITEMS.contains(item.getItem())) {
                p.inventory.currentItem = i;
                return true;
            }
        }
        if (message) {
            Out.gui("bb pls get me some blocks. dirt or cobble", Out.Mode.Minimal);
        }
        return false;
    }

    public static boolean hasthrowaway() {
        set();
        EntityPlayerSP p = Minecraft.getMinecraft().player;
        NonNullList<ItemStack> inv = p.inventory.mainInventory;
        for (byte i = 0; i < 9; i++) {
            ItemStack item = inv.get(i);
            if (item == null) {
                item = new ItemStack(Item.getByNameOrId("minecraft:apple"));
            }
            if (ACCEPTABLE_THROWAWAY_ITEMS.contains(item.getItem())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Do the actual tick. This function can assume that all blocks in
     * positionsToBreak are now walk-through-able.
     *
     * @return
     */
    protected abstract boolean tick0();

    public ArrayList<BlockPos> toMine() {
        ArrayList<BlockPos> result = new ArrayList<>();
        for (BlockPos positionsToBreak1 : positionsToBreak) {
            if (!canWalkThrough(positionsToBreak1)) {
                result.add(positionsToBreak1);
            }
        }
        return result;
    }

    public ArrayList<BlockPos> toPlace() {
        ArrayList<BlockPos> result = new ArrayList<>();
        for (BlockPos positionsToPlace1 : positionsToPlace) {
            if (!canWalkOn(positionsToPlace1)) {
                result.add(positionsToPlace1);
            }
        }
        return result;
    }
}
