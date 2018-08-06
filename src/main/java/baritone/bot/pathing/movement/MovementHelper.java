package baritone.bot.pathing.movement;

import baritone.bot.behavior.impl.LookBehaviorUtils;
import baritone.bot.utils.BlockStateInterface;
import baritone.bot.utils.Helper;
import baritone.bot.utils.ToolSet;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;

import java.util.Optional;

/**
 * Static helpers for cost calculation
 *
 * @author leijurv
 */
public interface MovementHelper extends ActionCosts, Helper {

    static boolean avoidBreaking(BlockPos pos) {
        Block b = BlockStateInterface.getBlock(pos);
        Block below = BlockStateInterface.get(new BlockPos(pos.getX(), pos.getY() - 1, pos.getZ())).getBlock();
        return Blocks.ICE.equals(b) // ice becomes water, and water can mess up the path
                || b instanceof BlockSilverfish
                || BlockStateInterface.isLiquid(new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ()))//don't break anything touching liquid on any side
                || BlockStateInterface.isLiquid(new BlockPos(pos.getX() + 1, pos.getY(), pos.getZ()))
                || BlockStateInterface.isLiquid(new BlockPos(pos.getX() - 1, pos.getY(), pos.getZ()))
                || BlockStateInterface.isLiquid(new BlockPos(pos.getX(), pos.getY(), pos.getZ() + 1))
                || BlockStateInterface.isLiquid(new BlockPos(pos.getX(), pos.getY(), pos.getZ() - 1))
                || (!(b instanceof BlockLilyPad && BlockStateInterface.isWater(below)) && BlockStateInterface.isLiquid(below));//if it's a lilypad above water, it's ok to break, otherwise don't break if its liquid
    }

    /**
     * Can I walk through this block? e.g. air, saplings, torches, etc
     *
     * @param pos
     * @return
     */
    static boolean canWalkThrough(BlockPos pos, IBlockState state) {
        Block block = state.getBlock();
        if (block instanceof BlockLilyPad
                || block instanceof BlockFire
                || block instanceof BlockTripWire) {//you can't actually walk through a lilypad from the side, and you shouldn't walk through fire
            return false;
        }
        if (BlockStateInterface.isFlowing(state) || BlockStateInterface.isLiquid(pos.up())) {
            return false; // Don't walk through flowing liquids
        }
        return block.isPassable(mc.world, pos);
    }

    static boolean avoidWalkingInto(Block block) {
        return BlockStateInterface.isLava(block)
                || block instanceof BlockCactus
                || block instanceof BlockFire
                || block instanceof BlockEndPortal
                || block instanceof BlockWeb;
    }

    /**
     * Can I walk on this block without anything weird happening like me falling
     * through? Includes water because we know that we automatically jump on
     * lava
     *
     * @return
     */
    static boolean canWalkOn(BlockPos pos, IBlockState state) {
        Block block = state.getBlock();
        if (block instanceof BlockLadder || block instanceof BlockVine) {
            return true;
        }
        if (block instanceof BlockAir) {
            return false;
        }
        if (BlockStateInterface.isWater(block)) {
            return BlockStateInterface.isWater(pos.up()); // You can only walk on water if there is water above it
        }
        return state.isBlockNormalCube() && !BlockStateInterface.isLava(block);
    }


    static boolean canFall(BlockPos pos) {
        return BlockStateInterface.get(pos).getBlock() instanceof BlockFalling;
    }

    static double getMiningDurationTicks(ToolSet ts, IBlockState block, BlockPos position) {
        if (!block.equals(Blocks.AIR) && !canWalkThrough(position, block)) {
            if (avoidBreaking(position)) {
                return COST_INF;
            }
            //if (!Baritone.allowBreakOrPlace) {
            //    return COST_INF;
            //}
            double m = Blocks.CRAFTING_TABLE.equals(block) ? 10 : 1;
            return m / ts.getStrVsBlock(block, position) + BREAK_ONE_BLOCK_ADD;
        }
        return 0;
    }

    /**
     * The entity the player is currently looking at
     *
     * @return the entity object
     */
    static Optional<Entity> whatEntityAmILookingAt() {
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == RayTraceResult.Type.ENTITY) {
            return Optional.of(mc.objectMouseOver.entityHit);
        }
        return Optional.empty();
    }

    /**
     * AutoTool
     */
    static void switchToBestTool() {
        LookBehaviorUtils.getSelectedBlock().ifPresent(pos -> {
            IBlockState state = BlockStateInterface.get(pos);
            if (state.getBlock().equals(Blocks.AIR)) {
                return;
            }
            switchToBestToolFor(state);
        });
    }

    /**
     * AutoTool for a specific block
     *
     * @param b the blockstate to mine
     */
    static void switchToBestToolFor(IBlockState b) {
        switchToBestToolFor(b, new ToolSet());
    }

    /**
     * AutoTool for a specific block with precomputed ToolSet data
     *
     * @param b  the blockstate to mine
     * @param ts previously calculated ToolSet
     */
    static void switchToBestToolFor(IBlockState b, ToolSet ts) {
        mc.player.inventory.currentItem = ts.getBestSlot(b);
    }
}
