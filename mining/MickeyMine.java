/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.mining;

import baritone.Baritone;
import baritone.inventory.CraftingTask;
import baritone.movement.MovementManager;
import baritone.pathfinding.actions.Action;
import baritone.pathfinding.goals.Goal;
import baritone.pathfinding.goals.GoalBlock;
import baritone.pathfinding.goals.GoalComposite;
import baritone.pathfinding.goals.GoalTwoBlocks;
import baritone.pathfinding.goals.GoalYLevel;
import baritone.ui.LookManager;
import baritone.util.Manager;
import baritone.util.ManagerTick;
import baritone.util.Memory;
import baritone.util.Out;
import java.util.ArrayList;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

/**
 *
 * @author galdara
 */
public class MickeyMine extends ManagerTick {

    public static final int Y_DIAMOND = 13;
    public static final int Y_IRON = 36;
    public static int yLevel = Y_DIAMOND;
    static ArrayList<Block> goalBlocks = null;
    static boolean isGoingToMine = false;
    static boolean isMining = false;
    public static boolean tempDisable = false;
    static EnumFacing miningFacing = EnumFacing.EAST;
    static ArrayList<IntegerTuple> diamondChunks = new ArrayList<IntegerTuple>();
    static ArrayList<BlockPos> hasBeenMined = new ArrayList<BlockPos>();
    static ArrayList<BlockPos> needsToBeMined = new ArrayList<BlockPos>();
    static ArrayList<BlockPos> priorityNeedsToBeMined = new ArrayList<BlockPos>();
    static ArrayList<IntegerTuple> chunkHasDiamonds = new ArrayList<IntegerTuple>();
    static BlockPos branchPosition = null;
    static final String[] ores = {"diamond", "iron", "coal", "gold", "emerald"};
    static final boolean[] enabled = {true, true, false, true, true};
    static boolean mightNeedToGoBackToPath = false;

    public static void notifyFullness(String item, boolean isFull) {
        if (item.equals("stone")) {
            return;
        }
        boolean up = false;
        for (int i = 0; i < ores.length; i++) {
            if (ores[i].endsWith(item)) {
                if (enabled[i] == isFull) {
                    Out.gui((isFull ? "is full" : "not full") + " of " + item + " so therefore " + ores[i], Out.Mode.Minimal);
                    enabled[i] = !isFull;
                    up = true;
                }
            }
        }
        if (up) {
            calculateGoal();
        }
    }

    public static void toggleOre(String ore) {
        String lower = ore.toLowerCase();
        if (lower.trim().length() == 0) {
            for (int i = 0; i < ores.length; i++) {
                Out.gui(ores[i] + ": " + enabled[i], Out.Mode.Minimal);
            }
            return;
        }
        boolean m = false;
        for (int i = 0; i < ores.length; i++) {
            if (!ores[i].contains(lower)) {
                Out.gui(ores[i] + ": " + enabled[i], Out.Mode.Minimal);
                continue;
            }
            m = true;
            enabled[i] = !enabled[i];
            Out.gui(ores[i] + ": " + enabled[i] + " (I toggled this one just now)", Out.Mode.Minimal);
        }
        if (m) {
            goalBlocks = new ArrayList<Block>();
            calculateGoal();
        }
    }

    public static void calculateGoal() {
        goalBlocks = new ArrayList<Block>();
        for (int i = 0; i < ores.length; i++) {
            if (!enabled[i]) {
                continue;
            }
            String oreName = "minecraft:" + ores[i] + "_ore";
            Block block = Block.getBlockFromName(oreName);
            if (block == null) {
                Out.gui(oreName + " doesn't exist bb", Out.Mode.Minimal);
                throw new NullPointerException(oreName + " doesn't exist bb");
            }
            goalBlocks.add(block);
        }
    }

    public static void doMine() {
        if (goalBlocks == null) {
            calculateGoal();
        }
        MovementManager.clearMovement();
        Out.log("Goal blocks: " + goalBlocks);
        Out.log("priority: " + priorityNeedsToBeMined);
        Out.log("needs to be mined: " + needsToBeMined);
        updateBlocksMined();
        if (priorityNeedsToBeMined.isEmpty() && needsToBeMined.isEmpty()) {
            doBranchMine();
        } else if (priorityNeedsToBeMined.isEmpty()) {
            doNormalMine();
        }
        if (ticksSinceBlockMined > 200) {
            Out.gui("Mickey mine stops, its been like 10 seconds and nothing has happened", Out.Mode.Debug);
            Manager.getManager(MickeyMine.class).cancel();
        }
    }

    public static boolean torch() {
        EntityPlayerSP p = Minecraft.getMinecraft().player;
        ItemStack[] inv = p.inventory.mainInventory;
        CraftingTask.ensureCraftingDesired(Item.getByNameOrId("minecraft:torch"), 32);
        for (int i = 0; i < 9; i++) {
            ItemStack item = inv[i];
            if (inv[i] == null) {
                continue;
            }
            if (item.getItem().equals(Item.getByNameOrId("minecraft:torch"))) {
                p.inventory.currentItem = i;
                return true;
            }
        }
        return false;
    }

    public static void doBranchMine() {
        if (branchPosition == null) {
            BlockPos player = Baritone.playerFeet;
            branchPosition = new BlockPos(player.getX(), yLevel, player.getZ());
        }
        if (!Memory.blockLoaded(branchPosition)) {//if this starts before chunks load, this thing just goes on forever
            branchPosition = null;
            return;
        }
        if (!branchPosition.equals(Baritone.playerFeet)) {
            Out.gui("Should be at branch position " + branchPosition + " " + Baritone.playerFeet, Out.Mode.Debug);
            mightNeedToGoBackToPath = true;
        } else if (torch()) {
            if (LookManager.lookAtBlock(branchPosition.down(), true)) {
                Minecraft.getMinecraft().rightClickMouse();
            } else {
                return;
            }
        }
        int i;
        int l = 5;
        for (i = 0; i < l || diamondChunks.contains(tupleFromBlockPos(branchPosition.offset(miningFacing, i))); i++) {
            addNormalBlock(branchPosition.offset(miningFacing, i).up(), true);
            addNormalBlock(branchPosition.offset(miningFacing, i), true);
            Out.log("branche" + i);
            if (i >= l) {
                Out.gui("Not mining " + branchPosition.offset(miningFacing, i) + " because it's in known diamond chunk " + tupleFromBlockPos(branchPosition.offset(miningFacing, i)), Out.Mode.Debug);
            }
        }
        i--;
        Out.gui("Branch distance " + i, Out.Mode.Debug);
        BlockPos futureBranchPosition = branchPosition.offset(miningFacing, i);
        if (futureBranchPosition.getY() != yLevel) {
            onCancel1();
            return;
        }
        Out.log("player reach: " + Minecraft.getMinecraft().playerController.getBlockReachDistance());
        for (int j = 1; j <= Math.ceil(Minecraft.getMinecraft().playerController.getBlockReachDistance()); j++) {
            addNormalBlock(futureBranchPosition.offset(miningFacing.rotateY(), j).up(), false);
        }
        for (int j = 1; j <= Math.ceil(Minecraft.getMinecraft().playerController.getBlockReachDistance()); j++) {
            addNormalBlock(futureBranchPosition.offset(miningFacing.rotateYCCW(), j).up(), false);
        }
        branchPosition = futureBranchPosition;
    }

    public static void doPriorityMine() {
        Goal[] toComposite = new Goal[priorityNeedsToBeMined.size()];
        for (int i = 0; i < toComposite.length; i++) {
            toComposite[i] = new GoalTwoBlocks(priorityNeedsToBeMined.get(i));
        }
        Baritone.goal = new GoalComposite(toComposite);
        if (Baritone.currentPath == null && !Baritone.isPathFinding()) {
            Baritone.findPathInNewThread(Baritone.playerFeet, false);
        } else {
            addNearby();
        }
    }

    public static void addNearby() {
        BlockPos playerFeet = Baritone.playerFeet;
        int searchDist = 4;//why four? idk
        for (int x = playerFeet.getX() - searchDist; x <= playerFeet.getX() + searchDist; x++) {
            for (int y = playerFeet.getY() - searchDist; y <= playerFeet.getY() + searchDist; y++) {
                for (int z = playerFeet.getZ() - searchDist; z <= playerFeet.getZ() + searchDist; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (isGoalBlock(pos)) {
                        if (LookManager.couldIReach(pos)) {//crucial to only add blocks we can see because otherwise this is an x-ray and it'll get caught
                            addPriorityBlock(pos);
                        }
                    }
                }
            }
        }
    }

    public static void doNormalMine() {
        if (mightNeedToGoBackToPath) {
            Baritone.goal = new GoalBlock(branchPosition);
            if (Baritone.currentPath == null && !Baritone.isPathFinding()) {
                Baritone.findPathInNewThread(Baritone.playerFeet, false);
                Out.gui("Pathing back to branch", Out.Mode.Standard);
            }
            if (Baritone.playerFeet.equals(branchPosition)) {
                mightNeedToGoBackToPath = false;
                Out.gui("I'm back", Out.Mode.Debug);
            }
            return;
        }
        addNearby();
        BlockPos toMine = needsToBeMined.get(0);
        if (LookManager.lookAtBlock(toMine, true)) {
            if (Action.avoidBreaking(toMine)) {
                miningFacing = miningFacing.rotateY();
                Out.gui("Since I need to avoid breaking " + toMine + ", I'm rotating to " + miningFacing, Out.Mode.Debug);
                needsToBeMined.clear();
                //.priorityNeedsToBeMined.clear();
            } else {
                Baritone.switchToBestTool();
                MovementManager.isLeftClick = true;
                Out.log("Looking");
                if (Baritone.playerFeet.equals(branchPosition)) {
                    Out.log("IN position");
                    if (Baritone.whatAreYouLookingAt() == null) {
                        Out.log("Can't see, going");
                        MovementManager.forward = true;
                    }
                } else {
                    Out.log("Going to position");
                    if (!Action.canWalkOn(Baritone.playerFeet.offset(Minecraft.getMinecraft().player.getHorizontalFacing()).down())) {
                        Out.gui("About to fall off", Out.Mode.Debug);
                        mightNeedToGoBackToPath = true;
                        return;
                    }
                    MovementManager.moveTowardsBlock(branchPosition, false);
                    if (Baritone.playerFeet.getY() != branchPosition.getY()) {
                        Out.gui("wrong Y coordinate", Out.Mode.Debug);
                        mightNeedToGoBackToPath = true;
                    }
                }
            }
        }
    }
    static double ticksSinceBlockMined = 0;

    public static void updateBlocksMined() {
        if (Baritone.currentPath == null) {
            ticksSinceBlockMined++;
        } else {
            ticksSinceBlockMined += 0.1;
        }
        ArrayList<BlockPos> shouldBeRemoved = new ArrayList<BlockPos>();
        for (BlockPos isMined : needsToBeMined) {
            Block block = net.minecraft.client.Minecraft.getMinecraft().world.getBlockState(isMined).getBlock();
            if (isGoalBlock(isMined) || block.equals(Block.getBlockById(0)) || block.equals(Block.getBlockFromName("minecraft:torch")) || block.equals(Blocks.BEDROCK)) {
                hasBeenMined.add(isMined);
                shouldBeRemoved.add(isMined);
                updateBlocks(isMined);
                ticksSinceBlockMined = 0;
            }
        }
        for (BlockPos needsRemoval : shouldBeRemoved) {
            needsToBeMined.remove(needsRemoval);
        }
    }

    public static void updatePriorityBlocksMined() {
        boolean wasEmpty = priorityNeedsToBeMined.isEmpty();
        ArrayList<BlockPos> shouldBeRemoved = new ArrayList<BlockPos>();
        for (BlockPos isMined : priorityNeedsToBeMined) {
            Block block = net.minecraft.client.Minecraft.getMinecraft().world.getBlockState(isMined).getBlock();
            if (block.equals(Block.getBlockById(0)) || block.equals(Block.getBlockFromName("minecraft:torch")) || block.equals(Blocks.BEDROCK)) {
                hasBeenMined.add(isMined);
                shouldBeRemoved.add(isMined);
                updateBlocks(isMined);
                ticksSinceBlockMined = 0;
            }
        }
        for (BlockPos needsRemoval : shouldBeRemoved) {
            priorityNeedsToBeMined.remove(needsRemoval);
        }
        if (priorityNeedsToBeMined.isEmpty() && !wasEmpty) {
            mightNeedToGoBackToPath = true;
            if (!chunkHasDiamonds.isEmpty()) {
                for (IntegerTuple shouldAdd : chunkHasDiamonds) {
                    if (!diamondChunks.contains(shouldAdd)) {
                        diamondChunks.add(shouldAdd);
                    }
                }
                chunkHasDiamonds.clear();
            }
        }
    }

    public static void updateBlocks(BlockPos blockPos) {
        for (int i = 0; i < 4; i++) {
            Out.log(blockPos.offset(miningFacing));
        }
        addPriorityBlock(blockPos);
        addPriorityBlock(blockPos.north());
        addPriorityBlock(blockPos.south());
        addPriorityBlock(blockPos.east());
        addPriorityBlock(blockPos.west());
        addPriorityBlock(blockPos.up());
        addPriorityBlock(blockPos.down());
    }

    public static boolean addNormalBlock(BlockPos blockPos, boolean mainBranch) {
        if (!needsToBeMined.contains(blockPos)) {
            if (Action.avoidBreaking(blockPos) && mainBranch) {//who gives a crap if a side branch will hit lava? lol
                Out.gui("Uh oh, lava nearby", Out.Mode.Debug);
                miningFacing = miningFacing.rotateY();
                return false;
            }
            needsToBeMined.add(blockPos);
            return true;
        }
        return false;
    }

    public static boolean addPriorityBlock(BlockPos blockPos) {
        if (!priorityNeedsToBeMined.contains(blockPos) && isGoalBlock(blockPos)) {
            if (Action.avoidBreaking(blockPos)) {
                Out.gui("Can't break " + Minecraft.getMinecraft().world.getBlockState(blockPos).getBlock() + " at " + blockPos + " because it's near lava", Out.Mode.Debug);
                return false;
            }
            priorityNeedsToBeMined.add(blockPos);
            if (Block.getBlockFromName("minecraft:diamond_ore").equals(Minecraft.getMinecraft().world.getBlockState(blockPos).getBlock())) {
                chunkHasDiamonds.add(tupleFromBlockPos(blockPos));
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            BlockPos oth = new BlockPos(blockPos.getX() + x, blockPos.getY() + y, blockPos.getZ() + z);
                            if (!Action.avoidBreaking(oth) && !priorityNeedsToBeMined.contains(oth)) {
                                priorityNeedsToBeMined.add(oth);
                            }
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    public static boolean isGoalBlock(BlockPos blockPos) {
        return isGoalBlock(Minecraft.getMinecraft().world.getBlockState(blockPos).getBlock());
    }

    public static boolean isGoalBlock(Block block) {
        return goalBlocks.contains(block);
    }

    public static boolean isNull(Object object) {
        try {
            object.toString();
            return false;
        } catch (NullPointerException ex) {
            return true;
        }
    }

    public static IntegerTuple tupleFromChunk(Chunk chunk) {
        return new IntegerTuple(chunk.xPosition, chunk.zPosition);
    }

    public static IntegerTuple tupleFromBlockPos(BlockPos blockPos) {
        return tupleFromChunk(Minecraft.getMinecraft().world.getChunkFromBlockCoords(blockPos));
    }

    @Override
    protected boolean onTick0() {
        if (tempDisable) {
            return false;
        }
        Out.log("mickey" + isGoingToMine + " " + isMining);
        if (!isGoingToMine && !isMining) {
            Baritone.goal = new GoalYLevel(yLevel);
            if (Baritone.currentPath == null && !Baritone.isPathFinding()) {
                Baritone.findPathInNewThread(Baritone.playerFeet, true);
                isGoingToMine = true;
            }
        }
        if (isGoingToMine && Baritone.playerFeet.getY() == yLevel) {
            isGoingToMine = false;
            isMining = true;
        }
        updatePriorityBlocksMined();
        if (isMining) {
            doMine();
        }
        if (!priorityNeedsToBeMined.isEmpty()) {
            doPriorityMine();
        }
        Out.log("mickey done");
        return false;
    }

    @Override
    protected void onCancel() {
        onCancel1();
    }

    private static void onCancel1() {
        isGoingToMine = false;
        isMining = false;
        needsToBeMined.clear();
        priorityNeedsToBeMined.clear();
        branchPosition = null;
        mightNeedToGoBackToPath = false;
        ticksSinceBlockMined = 0;
    }

    @Override
    protected void onStart() {
    }

    @Override
    protected void onTickPre() {
        tempDisable = false;
    }

    public static class IntegerTuple {//why not use the normal net.minecraft.util.Tuple? Because it doesn't implement equals or hashCode

        private final int a;
        private final int b;

        public IntegerTuple(int a, int b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public String toString() {
            return a + "," + b;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 73 * hash + this.a;
            hash = 73 * hash + this.b;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final IntegerTuple other = (IntegerTuple) obj;
            if (this.a != other.a) {
                return false;
            }
            return this.b == other.b;
        }
    }
}
