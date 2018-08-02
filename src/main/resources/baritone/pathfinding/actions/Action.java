package baritone.pathfinding.actions;

import baritone.Baritone;
import baritone.util.Out;
import baritone.util.ToolSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLilyPad;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockVine;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;

/**
 *
 * @author leijurv
 */
public abstract class Action {

    //These costs are measured roughly in ticks btw
    public static final double WALK_ONE_BLOCK_COST = 20 / 4.317;
    public static final double WALK_ONE_IN_WATER_COST = 20 / 2.2;
    public static final double JUMP_ONE_BLOCK_COST = 5.72854;//see below calculation for fall. 1.25 blocks
    public static final double LADDER_UP_ONE_COST = 20 / 2.35;
    public static final double LADDER_DOWN_ONE_COST = 20 / 3;
    public static final double SNEAK_ONE_BLOCK_COST = 20 / 1.3;
    public static final double SPRINT_ONE_BLOCK_COST = 20 / 5.612;
    /**
     * Doesn't include walking forwards, just the falling
     *
     * Based on a sketchy formula from minecraftwiki
     *
     * d(t) = 3.92 × (99 - 49.50×(0.98^t+1) - t)
     *
     * Solved in mathematica
     */
    public static final double FALL_ONE_BLOCK_COST = 5.11354;
    public static final double FALL_TWO_BLOCK_COST = 7.28283;
    public static final double FALL_THREE_BLOCK_COST = 8.96862;
    /**
     * It doesn't actually take ten ticks to place a block, this cost is so high
     * because we want to generally conserve blocks which might be limited
     */
    public static final double PLACE_ONE_BLOCK_COST = 20;
    /**
     * Add this to the cost of breaking any block. The cost of breaking any
     * block is calculated as the number of ticks that block takes to break with
     * the tools you have. You add this because there's always a little overhead
     * (e.g. looking at the block)
     */
    public static final double BREAK_ONE_BLOCK_ADD = 4;
    public static final double COST_INF = 1000000;
    public final BlockPos from;
    public final BlockPos to;
    private Double cost;
    public boolean finished = false;

    protected Action(BlockPos from, BlockPos to) {
        this.from = from;
        this.to = to;
        this.cost = null;
    }

    /**
     * Get the cost. It's cached
     *
     * @param ts
     * @return
     */
    public double cost(ToolSet ts) {
        if (cost == null) {
            cost = calculateCost0(ts == null ? new ToolSet() : ts);
        }
        if (cost < 1) {
            Out.log("Bad cost " + this + " " + cost);
        }
        return cost;
    }

    public double calculateCost0(ToolSet ts) {
        if (!(this instanceof ActionPillar) && !(this instanceof ActionBridge) && !(this instanceof ActionFall)) {
            Block fromDown = Baritone.get(from.down()).getBlock();
            if (fromDown instanceof BlockLadder || fromDown instanceof BlockVine) {
                return COST_INF;
            }
        }
        return calculateCost(ts);
    }

    protected abstract double calculateCost(ToolSet ts);
    static Block waterFlowing = Block.getBlockById(8);
    static Block waterStill = Block.getBlockById(9);
    static Block lavaFlowing = Block.getBlockById(10);
    static Block lavaStill = Block.getBlockById(11);

    /**
     * Is this block water? Includes both still and flowing
     *
     * @param b
     * @return
     */
    public static boolean isWater(Block b) {
        return waterFlowing.equals(b) || waterStill.equals(b);
    }

    public static boolean isWater(BlockPos bp) {
        return isWater(Baritone.get(bp).getBlock());
    }

    public static boolean isLiquid(Block b) {
        return b instanceof BlockLiquid;
        //return b != null && (waterFlowing.equals(b) || waterStill.equals(b) || lavaFlowing.equals(b) || lavaStill.equals(b));
    }

    public static boolean isFlowing(BlockPos pos, IBlockState state) {
        Block b = state.getBlock();
        Material m = b.getMaterial(state);
        if (b instanceof BlockLiquid) {
            System.out.println("Need to fix get flow check!!!");
            //return BlockLiquid.getFlow(Minecraft.getMinecraft().world, pos, state) != -1000.0D;
        }
        return false;
    }

    public static boolean isLava(Block b) {
        return lavaFlowing.equals(b) || lavaStill.equals(b);
    }

    public static boolean isLiquid(BlockPos p) {
        return isLiquid(Baritone.get(p).getBlock());
    }

    public static boolean avoidBreaking(BlockPos pos) {
        Block b = Baritone.get(pos).getBlock();
        Block below = Baritone.get(new BlockPos(pos.getX(), pos.getY() - 1, pos.getZ())).getBlock();
        return Block.getBlockFromName("minecraft:ice").equals(b)//ice becomes water, and water can mess up the path
                || isLiquid(new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ()))//don't break anything touching liquid on any side
                || isLiquid(new BlockPos(pos.getX() + 1, pos.getY(), pos.getZ()))
                || isLiquid(new BlockPos(pos.getX() - 1, pos.getY(), pos.getZ()))
                || isLiquid(new BlockPos(pos.getX(), pos.getY(), pos.getZ() + 1))
                || isLiquid(new BlockPos(pos.getX(), pos.getY(), pos.getZ() - 1))
                || (!(b instanceof BlockLilyPad && isWater(below)) && isLiquid(below));//if it's a lilypad above water, it's ok to break, otherwise don't break if its liquid
    }

    /**
     * Can I walk through this block? e.g. air, saplings, torches, etc
     *
     * @param pos
     * @return
     */
    public static boolean canWalkThrough(BlockPos pos) {
        IBlockState state = Baritone.get(pos);
        Block block = state.getBlock();
        if (block instanceof BlockLilyPad || block instanceof BlockFire) {//you can't actually walk through a lilypad from the side, and you shouldn't walk through fire
            return false;
        }
        if (isFlowing(pos, state)) {
            return false;//don't walk through flowing liquids
        }
        if (isLiquid(pos.up())) {
            return false;//you could drown
        }
        return block.isPassable(Minecraft.getMinecraft().world, pos);
    }

    public static boolean avoidWalkingInto(BlockPos pos) {
        Block block = Baritone.get(pos).getBlock();
        if (isLava(block)) {
            return true;
        }
        if (block instanceof BlockCactus) {
            return true;
        }
        return block instanceof BlockFire;
    }

    /**
     * Can I walk on this block without anything weird happening like me falling
     * through? Includes water because we know that we automatically jump on
     * lava
     *
     * @param pos
     * @return
     */
    public static boolean canWalkOn(BlockPos pos) {
        IBlockState state = Baritone.get(pos);
        Block block = state.getBlock();
        if (block instanceof BlockLadder || block instanceof BlockVine) {
            return true;
        }
        if (isWater(block)) {
            return isWater(pos.up());//you can only walk on water if there is water above it
        }
        return block.isBlockNormalCube(state) && !isLava(block);
    }

    /**
     * Tick this action
     *
     * @return is it done
     */
    public abstract boolean tick();
}
