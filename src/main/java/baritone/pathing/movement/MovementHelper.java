/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.pathing.movement;

import baritone.Baritone;
import baritone.api.pathing.movement.ActionCosts;
import baritone.api.utils.*;
import baritone.pathing.movement.MovementState.MovementTarget;
import baritone.utils.BlockStateInterface;
import baritone.utils.Helper;
import baritone.utils.InputOverrideHandler;
import baritone.utils.ToolSet;
import net.minecraft.block.*;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.EmptyChunk;

/**
 * Static helpers for cost calculation
 *
 * @author leijurv
 */
public interface MovementHelper extends ActionCosts, Helper {

    static boolean avoidBreaking(int x, int y, int z, IBlockState state) {
        Block b = state.getBlock();
        return b == Blocks.ICE // ice becomes water, and water can mess up the path
                || b instanceof BlockSilverfish // obvious reasons
                // call BlockStateInterface.get directly with x,y,z. no need to make 5 new BlockPos for no reason
                || BlockStateInterface.get(x, y + 1, z).getBlock() instanceof BlockLiquid//don't break anything touching liquid on any side
                || BlockStateInterface.get(x + 1, y, z).getBlock() instanceof BlockLiquid
                || BlockStateInterface.get(x - 1, y, z).getBlock() instanceof BlockLiquid
                || BlockStateInterface.get(x, y, z + 1).getBlock() instanceof BlockLiquid
                || BlockStateInterface.get(x, y, z - 1).getBlock() instanceof BlockLiquid;
    }

    /**
     * Can I walk through this block? e.g. air, saplings, torches, etc
     *
     * @param pos
     * @return
     */
    static boolean canWalkThrough(BetterBlockPos pos) {
        return canWalkThrough(pos.x, pos.y, pos.z, BlockStateInterface.get(pos));
    }

    static boolean canWalkThrough(int x, int y, int z) {
        return canWalkThrough(x, y, z, BlockStateInterface.get(x, y, z));
    }

    static boolean canWalkThrough(int x, int y, int z, IBlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.AIR) { // early return for most common case
            return true;
        }
        if (block == Blocks.FIRE || block == Blocks.TRIPWIRE || block == Blocks.WEB || block == Blocks.END_PORTAL || block == Blocks.COCOA) {
            return false;
        }
        if (block instanceof BlockDoor || block instanceof BlockFenceGate) {
            // Because there's no nice method in vanilla to check if a door is openable or not, we just have to assume
            // that anything that isn't an iron door isn't openable, ignoring that some doors introduced in mods can't
            // be opened by just interacting.
            return block != Blocks.IRON_DOOR;
        }
        boolean snow = block instanceof BlockSnow;
        boolean trapdoor = block instanceof BlockTrapDoor;
        if (snow || trapdoor) {
            // we've already checked doors and fence gates
            // so the only remaining dynamic isPassables are snow and trapdoor
            // if they're cached as a top block, we don't know their metadata
            // default to true (mostly because it would otherwise make long distance pathing through snowy biomes impossible)
            if (mc.world.getChunk(x >> 4, z >> 4) instanceof EmptyChunk) {
                return true;
            }
            if (snow) {
                // the check in BlockSnow.isPassable is layers < 5
                // while actually, we want < 3 because 3 or greater makes it impassable in a 2 high ceiling
                return state.getValue(BlockSnow.LAYERS) < 3;
            }
            if (trapdoor) {
                return !state.getValue(BlockTrapDoor.OPEN); // see BlockTrapDoor.isPassable
            }
            throw new IllegalStateException();
        }
        if (isFlowing(state)) {
            return false; // Don't walk through flowing liquids
        }
        if (block instanceof BlockLiquid) {
            if (Baritone.settings().assumeWalkOnWater.get()) {
                return false;
            }
            IBlockState up = BlockStateInterface.get(x, y + 1, z);
            if (up.getBlock() instanceof BlockLiquid || up.getBlock() instanceof BlockLilyPad) {
                return false;
            }
            return block == Blocks.WATER || block == Blocks.FLOWING_WATER;
        }
        // every block that overrides isPassable with anything more complicated than a "return true;" or "return false;"
        // has already been accounted for above
        // therefore it's safe to not construct a blockpos from our x, y, z ints and instead just pass null
        return block.isPassable(null, null);
    }

    /**
     * canWalkThrough but also won't impede movement at all. so not including doors or fence gates (we'd have to right click),
     * not including water, and not including ladders or vines or cobwebs (they slow us down)
     *
     * @return
     */
    static boolean fullyPassable(int x, int y, int z) {
        return fullyPassable(BlockStateInterface.get(x, y, z));
    }

    static boolean fullyPassable(IBlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.AIR) { // early return for most common case
            return true;
        }
        // exceptions - blocks that are isPassable true, but we can't actually jump through
        if (block == Blocks.FIRE
                || block == Blocks.TRIPWIRE
                || block == Blocks.WEB
                || block == Blocks.VINE
                || block == Blocks.LADDER
                || block == Blocks.COCOA
                || block instanceof BlockDoor
                || block instanceof BlockFenceGate
                || block instanceof BlockSnow
                || block instanceof BlockLiquid
                || block instanceof BlockTrapDoor
                || block instanceof BlockEndPortal) {
            return false;
        }
        // door, fence gate, liquid, trapdoor have been accounted for, nothing else uses the world or pos parameters
        return block.isPassable(null, null);
    }

    static boolean isReplacable(int x, int y, int z, IBlockState state) {
        // for MovementTraverse and MovementAscend
        // block double plant defaults to true when the block doesn't match, so don't need to check that case
        // all other overrides just return true or false
        // the only case to deal with is snow
        /*
         *  public boolean isReplaceable(IBlockAccess worldIn, BlockPos pos)
         *     {
         *         return ((Integer)worldIn.getBlockState(pos).getValue(LAYERS)).intValue() == 1;
         *     }
         */
        Block block = state.getBlock();
        if (block instanceof BlockSnow) {
            // as before, default to true (mostly because it would otherwise make long distance pathing through snowy biomes impossible)
            if (mc.world.getChunk(x >> 4, z >> 4) instanceof EmptyChunk) {
                return true;
            }
            return state.getValue(BlockSnow.LAYERS) == 1;
        }
        if (block instanceof BlockDoublePlant) {
            BlockDoublePlant.EnumPlantType kek = state.getValue(BlockDoublePlant.VARIANT);
            return kek == BlockDoublePlant.EnumPlantType.FERN || kek == BlockDoublePlant.EnumPlantType.GRASS;
        }
        return state.getMaterial().isReplaceable();
    }

    static boolean isDoorPassable(BlockPos doorPos, BlockPos playerPos) {
        if (playerPos.equals(doorPos)) {
            return false;
        }

        IBlockState state = BlockStateInterface.get(doorPos);
        if (!(state.getBlock() instanceof BlockDoor)) {
            return true;
        }

        return isHorizontalBlockPassable(doorPos, state, playerPos, BlockDoor.OPEN);
    }

    static boolean isGatePassable(BlockPos gatePos, BlockPos playerPos) {
        if (playerPos.equals(gatePos)) {
            return false;
        }

        IBlockState state = BlockStateInterface.get(gatePos);
        if (!(state.getBlock() instanceof BlockFenceGate)) {
            return true;
        }

        return isHorizontalBlockPassable(gatePos, state, playerPos, BlockFenceGate.OPEN);
    }

    static boolean isHorizontalBlockPassable(BlockPos blockPos, IBlockState blockState, BlockPos playerPos, PropertyBool propertyOpen) {
        if (playerPos.equals(blockPos)) {
            return false;
        }

        EnumFacing.Axis facing = blockState.getValue(BlockHorizontal.FACING).getAxis();
        boolean open = blockState.getValue(propertyOpen);

        EnumFacing.Axis playerFacing;
        if (playerPos.north().equals(blockPos) || playerPos.south().equals(blockPos)) {
            playerFacing = EnumFacing.Axis.Z;
        } else if (playerPos.east().equals(blockPos) || playerPos.west().equals(blockPos)) {
            playerFacing = EnumFacing.Axis.X;
        } else {
            return true;
        }

        return (facing == playerFacing) == open;
    }

    static boolean avoidWalkingInto(Block block) {
        return block instanceof BlockLiquid
                || block instanceof BlockDynamicLiquid
                || block == Blocks.MAGMA
                || block == Blocks.CACTUS
                || block == Blocks.FIRE
                || block == Blocks.END_PORTAL
                || block == Blocks.WEB;
    }

    /**
     * Can I walk on this block without anything weird happening like me falling
     * through? Includes water because we know that we automatically jump on
     * water
     *
     * @return
     */
    static boolean canWalkOn(int x, int y, int z, IBlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.AIR || block == Blocks.MAGMA) {
            // early return for most common case (air)
            // plus magma, which is a normal cube but it hurts you
            return false;
        }
        if (state.isBlockNormalCube()) {
            return true;
        }
        if (block == Blocks.LADDER || (block == Blocks.VINE && Baritone.settings().allowVines.get())) { // TODO reconsider this
            return true;
        }
        if (block == Blocks.FARMLAND || block == Blocks.GRASS_PATH) {
            return true;
        }
        if (block == Blocks.ENDER_CHEST || block == Blocks.CHEST) {
            return true;
        }
        if (isWater(block)) {
            // since this is called literally millions of times per second, the benefit of not allocating millions of useless "pos.up()"
            // BlockPos s that we'd just garbage collect immediately is actually noticeable. I don't even think its a decrease in readability
            Block up = BlockStateInterface.get(x, y + 1, z).getBlock();
            if (up == Blocks.WATERLILY) {
                return true;
            }
            if (isFlowing(state) || block == Blocks.FLOWING_WATER) {
                // the only scenario in which we can walk on flowing water is if it's under still water with jesus off
                return isWater(up) && !Baritone.settings().assumeWalkOnWater.get();
            }
            // if assumeWalkOnWater is on, we can only walk on water if there isn't water above it
            // if assumeWalkOnWater is off, we can only walk on water if there is water above it
            return isWater(up) ^ Baritone.settings().assumeWalkOnWater.get();
        }
        if (block == Blocks.GLASS || block == Blocks.STAINED_GLASS) {
            return true;
        }
        if (block instanceof BlockSlab) {
            if (!Baritone.settings().allowWalkOnBottomSlab.get()) {
                if (((BlockSlab) block).isDouble()) {
                    return true;
                }
                return state.getValue(BlockSlab.HALF) != BlockSlab.EnumBlockHalf.BOTTOM;
            }
            return true;
        }
        return block instanceof BlockStairs;
    }

    static boolean canWalkOn(BetterBlockPos pos, IBlockState state) {
        return canWalkOn(pos.x, pos.y, pos.z, state);
    }

    static boolean canWalkOn(BetterBlockPos pos) {
        return canWalkOn(pos.x, pos.y, pos.z, BlockStateInterface.get(pos));
    }

    static boolean canWalkOn(int x, int y, int z) {
        return canWalkOn(x, y, z, BlockStateInterface.get(x, y, z));
    }

    static boolean canPlaceAgainst(int x, int y, int z) {
        return canPlaceAgainst(BlockStateInterface.get(x, y, z));
    }

    static boolean canPlaceAgainst(BlockPos pos) {
        return canPlaceAgainst(BlockStateInterface.get(pos));
    }

    static boolean canPlaceAgainst(IBlockState state) {
        // TODO isBlockNormalCube isn't the best check for whether or not we can place a block against it. e.g. glass isn't normalCube but we can place against it
        return state.isBlockNormalCube();
    }

    static double getMiningDurationTicks(CalculationContext context, int x, int y, int z, boolean includeFalling) {
        return getMiningDurationTicks(context, x, y, z, BlockStateInterface.get(x, y, z), includeFalling);
    }

    static double getMiningDurationTicks(CalculationContext context, int x, int y, int z, IBlockState state, boolean includeFalling) {
        Block block = state.getBlock();
        if (!canWalkThrough(x, y, z, state)) {
            if (!context.canBreakAt(x, y, z)) {
                return COST_INF;
            }
            if (avoidBreaking(x, y, z, state)) {
                return COST_INF;
            }
            if (block instanceof BlockLiquid) {
                return COST_INF;
            }
            double m = Blocks.CRAFTING_TABLE.equals(block) ? 10 : 1; // TODO see if this is still necessary. it's from MineBot when we wanted to penalize breaking its crafting table
            double strVsBlock = context.getToolSet().getStrVsBlock(state);
            if (strVsBlock <= 0) {
                return COST_INF;
            }

            double result = m / strVsBlock;
            result += context.breakBlockAdditionalCost();
            if (includeFalling) {
                IBlockState above = BlockStateInterface.get(x, y + 1, z);
                if (above.getBlock() instanceof BlockFalling) {
                    result += getMiningDurationTicks(context, x, y + 1, z, above, true);
                }
            }
            return result;
        }
        return 0; // we won't actually mine it, so don't check fallings above
    }

    static boolean isBottomSlab(IBlockState state) {
        return state.getBlock() instanceof BlockSlab
                && !((BlockSlab) state.getBlock()).isDouble()
                && state.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.BOTTOM;
    }

    static boolean isBottomSlab(BlockPos pos) {
        return isBottomSlab(BlockStateInterface.get(pos));
    }

    /**
     * AutoTool
     */
    static void switchToBestTool() {
        RayTraceUtils.getSelectedBlock().ifPresent(pos -> {
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
        mc.player.inventory.currentItem = ts.getBestSlot(b.getBlock());
    }

    static boolean throwaway(boolean select) {
        EntityPlayerSP p = Minecraft.getMinecraft().player;
        NonNullList<ItemStack> inv = p.inventory.mainInventory;
        for (byte i = 0; i < 9; i++) {
            ItemStack item = inv.get(i);
            // this usage of settings() is okay because it's only called once during pathing
            // (while creating the CalculationContext at the very beginning)
            // and then it's called during execution
            // since this function is never called during cost calculation, we don't need to migrate
            // acceptableThrowawayItems to the CalculationContext
            if (Baritone.settings().acceptableThrowawayItems.get().contains(item.getItem())) {
                if (select) {
                    p.inventory.currentItem = i;
                }
                return true;
            }
        }
        if (Baritone.settings().acceptableThrowawayItems.get().contains(p.inventory.offHandInventory.get(0).getItem())) {
            // main hand takes precedence over off hand
            // that means that if we have block A selected in main hand and block B in off hand, right clicking places block B
            // we've already checked above ^ and the main hand can't possible have an acceptablethrowawayitem
            // so we need to select in the main hand something that doesn't right click
            // so not a shovel, not a hoe, not a block, etc
            for (byte i = 0; i < 9; i++) {
                ItemStack item = inv.get(i);
                if (item.isEmpty() || item.getItem() instanceof ItemPickaxe) {
                    if (select) {
                        p.inventory.currentItem = i;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    static void moveTowards(MovementState state, BlockPos pos) {
        state.setTarget(new MovementTarget(
                new Rotation(RotationUtils.calcRotationFromVec3d(mc.player.getPositionEyes(1.0F),
                        VecUtils.getBlockPosCenter(pos),
                        new Rotation(mc.player.rotationYaw, mc.player.rotationPitch)).getYaw(), mc.player.rotationPitch),
                false
        )).setInput(InputOverrideHandler.Input.MOVE_FORWARD, true);
    }

    /**
     * Returns whether or not the specified block is
     * water, regardless of whether or not it is flowing.
     *
     * @param b The block
     * @return Whether or not the block is water
     */
    static boolean isWater(Block b) {
        return b == Blocks.FLOWING_WATER || b == Blocks.WATER;
    }

    /**
     * Returns whether or not the block at the specified pos is
     * water, regardless of whether or not it is flowing.
     *
     * @param bp The block pos
     * @return Whether or not the block is water
     */
    static boolean isWater(BlockPos bp) {
        return isWater(BlockStateInterface.getBlock(bp));
    }

    static boolean isLava(Block b) {
        return b == Blocks.FLOWING_LAVA || b == Blocks.LAVA;
    }

    /**
     * Returns whether or not the specified pos has a liquid
     *
     * @param p The pos
     * @return Whether or not the block is a liquid
     */
    static boolean isLiquid(BlockPos p) {
        return BlockStateInterface.getBlock(p) instanceof BlockLiquid;
    }

    static boolean isFlowing(IBlockState state) {
        // Will be IFluidState in 1.13
        return state.getBlock() instanceof BlockLiquid
                && state.getValue(BlockLiquid.LEVEL) != 0;
    }
}
