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
import baritone.api.IBaritone;
import baritone.api.pathing.movement.ActionCosts;
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.utils.*;
import baritone.api.utils.input.Input;
import baritone.behavior.InventoryBehavior;
import baritone.pathing.movement.MovementState.MovementTarget;
import baritone.pathing.precompute.Ternary;
import baritone.utils.BlockStateInterface;
import baritone.utils.ToolSet;
import net.minecraft.block.*;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

import static baritone.pathing.movement.Movement.HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP;
import static baritone.pathing.precompute.Ternary.*;

/**
 * Static helpers for cost calculation
 *
 * @author leijurv
 */
public interface MovementHelper extends ActionCosts, Helper {

    static boolean avoidBreaking(BlockStateInterface bsi, int x, int y, int z, IBlockState state) {
        if (!bsi.worldBorder.canPlaceAt(x, z)) {
            return true;
        }
        Block b = state.getBlock();
        return Baritone.settings().blocksToDisallowBreaking.value.contains(b)
                || b == Blocks.ICE // ice becomes water, and water can mess up the path
                || b instanceof BlockSilverfish // obvious reasons
                // call context.get directly with x,y,z. no need to make 5 new BlockPos for no reason
                || avoidAdjacentBreaking(bsi, x, y + 1, z, true)
                || avoidAdjacentBreaking(bsi, x + 1, y, z, false)
                || avoidAdjacentBreaking(bsi, x - 1, y, z, false)
                || avoidAdjacentBreaking(bsi, x, y, z + 1, false)
                || avoidAdjacentBreaking(bsi, x, y, z - 1, false);
    }

    static boolean avoidAdjacentBreaking(BlockStateInterface bsi, int x, int y, int z, boolean directlyAbove) {
        // returns true if you should avoid breaking a block that's adjacent to this one (e.g. lava that will start flowing if you give it a path)
        // this is only called for north, south, east, west, and up. this is NOT called for down.
        // we assume that it's ALWAYS okay to break the block thats ABOVE liquid
        IBlockState state = bsi.get0(x, y, z);
        Block block = state.getBlock();
        if (!directlyAbove // it is fine to mine a block that has a falling block directly above, this (the cost of breaking the stacked fallings) is included in cost calculations
                // therefore if directlyAbove is true, we will actually ignore if this is falling
                && block instanceof BlockFalling // obviously, this check is only valid for falling blocks
                && Baritone.settings().avoidUpdatingFallingBlocks.value // and if the setting is enabled
                && BlockFalling.canFallThrough(bsi.get0(x, y - 1, z))) { // and if it would fall (i.e. it's unsupported)
            return true; // dont break a block that is adjacent to unsupported gravel because it can cause really weird stuff
        }
        if (block instanceof BlockLiquid) {
            if (directlyAbove || Baritone.settings().strictLiquidCheck.value) {
                return true;
            }
            int level = state.getValue(BlockLiquid.LEVEL);
            if (level == 0) {
                return true; // source blocks like to flow horizontally
            }
            // everything else will prefer flowing down
            return !(bsi.get0(x, y - 1, z).getBlock() instanceof BlockLiquid); // assume everything is in a static state
        }
        return false;
    }

    static boolean canWalkThrough(IPlayerContext ctx, BetterBlockPos pos) {
        return canWalkThrough(new BlockStateInterface(ctx), pos.x, pos.y, pos.z);
    }

    static boolean canWalkThrough(BlockStateInterface bsi, int x, int y, int z) {
        return canWalkThrough(bsi, x, y, z, bsi.get0(x, y, z));
    }

    static boolean canWalkThrough(CalculationContext context, int x, int y, int z, IBlockState state) {
        return context.precomputedData.canWalkThrough(context.bsi, x, y, z, state);
    }

    static boolean canWalkThrough(CalculationContext context, int x, int y, int z) {
        return context.precomputedData.canWalkThrough(context.bsi, x, y, z, context.get(x, y, z));
    }

    static boolean canWalkThrough(BlockStateInterface bsi, int x, int y, int z, IBlockState state) {
        Ternary canWalkThrough = canWalkThroughBlockState(state);
        if (canWalkThrough == YES) {
            return true;
        }
        if (canWalkThrough == NO) {
            return false;
        }
        return canWalkThroughPosition(bsi, x, y, z, state);
    }

    static Ternary canWalkThroughBlockState(IBlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.AIR) {
            return YES;
        }
        if (block == Blocks.FIRE || block == Blocks.TRIPWIRE || block == Blocks.WEB || block == Blocks.END_PORTAL || block == Blocks.COCOA || block instanceof BlockSkull || block instanceof BlockTrapDoor || block == Blocks.END_ROD) {
            return NO;
        }
        if (Baritone.settings().blocksToAvoid.value.contains(block)) {
            return NO;
        }
        if (block instanceof BlockDoor || block instanceof BlockFenceGate) {
            // TODO this assumes that all doors in all mods are openable
            if (block == Blocks.IRON_DOOR) {
                return NO;
            }
            return YES;
        }
        if (block == Blocks.CARPET) {
            return MAYBE;
        }
        if (block instanceof BlockSnow) {
            // snow layers cached as the top layer of a packed chunk have no metadata, we can't make a decision based on their depth here
            // it would otherwise make long distance pathing through snowy biomes impossible
            return MAYBE;
        }
        if (block instanceof BlockLiquid) {
            if (state.getValue(BlockLiquid.LEVEL) != 0) {
                return NO;
            } else {
                return MAYBE;
            }
        }
        if (block instanceof BlockCauldron) {
            return NO;
        }
        try { // A dodgy catch-all at the end, for most blocks with default behaviour this will work, however where blocks are special this will error out, and we can handle it when we have this information
            if (block.isPassable(null, null)) {
                return YES;
            } else {
                return NO;
            }
        } catch (Throwable exception) {
            System.out.println("The block " + state.getBlock().getLocalizedName() + " requires a special case due to the exception " + exception.getMessage());
            return MAYBE;
        }
    }

    static boolean canWalkThroughPosition(BlockStateInterface bsi, int x, int y, int z, IBlockState state) {
        Block block = state.getBlock();

        if (block == Blocks.CARPET) {
            return canWalkOn(bsi, x, y - 1, z);
        }

        if (block instanceof BlockSnow) {
            // if they're cached as a top block, we don't know their metadata
            // default to true (mostly because it would otherwise make long distance pathing through snowy biomes impossible)
            if (!bsi.worldContainsLoadedChunk(x, z)) {
                return true;
            }
            // the check in BlockSnow.isPassable is layers < 5
            // while actually, we want < 3 because 3 or greater makes it impassable in a 2 high ceiling
            if (state.getValue(BlockSnow.LAYERS) >= 3) {
                return false;
            }
            // ok, it's low enough we could walk through it, but is it supported?
            return canWalkOn(bsi, x, y - 1, z);
        }

        if (block instanceof BlockLiquid) {
            if (isFlowing(x, y, z, state, bsi)) {
                return false;
            }
            // Everything after this point has to be a special case as it relies on the water not being flowing, which means a special case is needed.
            if (Baritone.settings().assumeWalkOnWater.value) {
                return false;
            }

            IBlockState up = bsi.get0(x, y + 1, z);
            if (up.getBlock() instanceof BlockLiquid || up.getBlock() instanceof BlockLilyPad) {
                return false;
            }
            return block == Blocks.WATER || block == Blocks.FLOWING_WATER;
        }

        return block.isPassable(bsi.access, bsi.isPassableBlockPos.setPos(x, y, z));
    }

    static Ternary fullyPassableBlockState(IBlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.AIR) { // early return for most common case
            return YES;
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
                || block instanceof BlockEndPortal
                || block instanceof BlockSkull) {
            return NO;
        }
        // door, fence gate, liquid, trapdoor have been accounted for, nothing else uses the world or pos parameters
        // at least in 1.12.2 vanilla, that is.....
        try { // A dodgy catch-all at the end, for most blocks with default behaviour this will work, however where blocks are special this will error out, and we can handle it when we have this information
            if (block.isPassable(null, null)) {
                return YES;
            } else {
                return NO;
            }
        } catch (Throwable exception) {
            // see PR #1087 for why
            System.out.println("The block " + state.getBlock().getLocalizedName() + " requires a special case due to the exception " + exception.getMessage());
            return MAYBE;
        }
    }

    /**
     * canWalkThrough but also won't impede movement at all. so not including doors or fence gates (we'd have to right click),
     * not including water, and not including ladders or vines or cobwebs (they slow us down)
     */
    static boolean fullyPassable(CalculationContext context, int x, int y, int z) {
        return fullyPassable(context, x, y, z, context.get(x, y, z));
    }

    static boolean fullyPassable(CalculationContext context, int x, int y, int z, IBlockState state) {
        return context.precomputedData.fullyPassable(context.bsi, x, y, z, state);
    }

    static boolean fullyPassable(IPlayerContext ctx, BlockPos pos) {
        IBlockState state = ctx.world().getBlockState(pos);
        Ternary fullyPassable = fullyPassableBlockState(state);
        if (fullyPassable == YES) {
            return true;
        }
        if (fullyPassable == NO) {
            return false;
        }
        return fullyPassablePosition(new BlockStateInterface(ctx), pos.getX(), pos.getY(), pos.getZ(), state); // meh
    }

    static boolean fullyPassablePosition(BlockStateInterface bsi, int x, int y, int z, IBlockState state) {
        return state.getBlock().isPassable(bsi.access, bsi.isPassableBlockPos.setPos(x, y, z));
    }

    static boolean isReplaceable(int x, int y, int z, IBlockState state, BlockStateInterface bsi) {
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
        if (block == Blocks.AIR || isWater(block)) {
            // early return for common cases hehe
            return true;
        }
        if (block instanceof BlockSnow) {
            // as before, default to true (mostly because it would otherwise make long distance pathing through snowy biomes impossible)
            if (!bsi.worldContainsLoadedChunk(x, z)) {
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

    @Deprecated
    static boolean isReplacable(int x, int y, int z, IBlockState state, BlockStateInterface bsi) {
        return isReplaceable(x, y, z, state, bsi);
    }

    static boolean isDoorPassable(IPlayerContext ctx, BlockPos doorPos, BlockPos playerPos) {
        if (playerPos.equals(doorPos)) {
            return false;
        }

        IBlockState state = BlockStateInterface.get(ctx, doorPos);
        if (!(state.getBlock() instanceof BlockDoor)) {
            return true;
        }

        return isHorizontalBlockPassable(doorPos, state, playerPos, BlockDoor.OPEN);
    }

    static boolean isGatePassable(IPlayerContext ctx, BlockPos gatePos, BlockPos playerPos) {
        if (playerPos.equals(gatePos)) {
            return false;
        }

        IBlockState state = BlockStateInterface.get(ctx, gatePos);
        if (!(state.getBlock() instanceof BlockFenceGate)) {
            return true;
        }

        return state.getValue(BlockFenceGate.OPEN);
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
     * <p>
     * If changing something in this function remember to also change it in precomputed data
     *
     * @param bsi   Block state provider
     * @param x     The block's x position
     * @param y     The block's y position
     * @param z     The block's z position
     * @param state The state of the block at the specified location
     * @return Whether or not the specified block can be walked on
     */
    static boolean canWalkOn(BlockStateInterface bsi, int x, int y, int z, IBlockState state) {
        Ternary canWalkOn = canWalkOnBlockState(state);
        if (canWalkOn == YES) {
            return true;
        }
        if (canWalkOn == NO) {
            return false;
        }
        return canWalkOnPosition(bsi, x, y, z, state);
    }

    static Ternary canWalkOnBlockState(IBlockState state) {
        Block block = state.getBlock();
        if (state.isBlockNormalCube() && block != Blocks.MAGMA) {
            return YES;
        }
        if (block == Blocks.LADDER || (block == Blocks.VINE && Baritone.settings().allowVines.value)) { // TODO reconsider this
            return YES;
        }
        if (block == Blocks.FARMLAND || block == Blocks.GRASS_PATH) {
            return YES;
        }
        if (block == Blocks.ENDER_CHEST || block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST) {
            return YES;
        }
        if (block == Blocks.GLASS || block == Blocks.STAINED_GLASS) {
            return YES;
        }
        if (block instanceof BlockStairs) {
            return YES;
        }
        if (isWater(block)) {
            return MAYBE;
        }
        if (MovementHelper.isLava(block) && Baritone.settings().assumeWalkOnLava.value) {
            return MAYBE;
        }
        if (block instanceof BlockSlab) {
            if (!Baritone.settings().allowWalkOnBottomSlab.value) {
                if (((BlockSlab) block).isDouble()) {
                    return YES;
                }
                if (state.getValue(BlockSlab.HALF) != BlockSlab.EnumBlockHalf.BOTTOM) {
                    return YES;
                }
                return NO;
            }
            return YES;
        }
        return NO;
    }

    static boolean canWalkOnPosition(BlockStateInterface bsi, int x, int y, int z, IBlockState state) {
        Block block = state.getBlock();
        if (isWater(block)) {
            // since this is called literally millions of times per second, the benefit of not allocating millions of useless "pos.up()"
            // BlockPos s that we'd just garbage collect immediately is actually noticeable. I don't even think it's a decrease in readability
            Block up = bsi.get0(x, y + 1, z).getBlock();
            if (up == Blocks.WATERLILY || up == Blocks.CARPET) {
                return true;
            }
            if (MovementHelper.isFlowing(x, y, z, state, bsi) || block == Blocks.FLOWING_WATER) {
                // the only scenario in which we can walk on flowing water is if it's under still water with jesus off
                return isWater(up) && !Baritone.settings().assumeWalkOnWater.value;
            }
            // if assumeWalkOnWater is on, we can only walk on water if there isn't water above it
            // if assumeWalkOnWater is off, we can only walk on water if there is water above it
            return isWater(up) ^ Baritone.settings().assumeWalkOnWater.value;
        }

        if (MovementHelper.isLava(block) && !MovementHelper.isFlowing(x, y, z, state, bsi) && Baritone.settings().assumeWalkOnLava.value) { // if we get here it means that assumeWalkOnLava must be true, so put it last
            return true;
        }

        return false; // If we don't recognise it then we want to just return false to be safe.
    }

    static boolean canWalkOn(CalculationContext context, int x, int y, int z, IBlockState state) {
        return context.precomputedData.canWalkOn(context.bsi, x, y, z, state);
    }

    static boolean canWalkOn(CalculationContext context, int x, int y, int z) {
        return canWalkOn(context, x, y, z, context.get(x, y, z));
    }

    static boolean canWalkOn(IPlayerContext ctx, BetterBlockPos pos, IBlockState state) {
        return canWalkOn(new BlockStateInterface(ctx), pos.x, pos.y, pos.z, state);
    }

    static boolean canWalkOn(IPlayerContext ctx, BlockPos pos) {
        return canWalkOn(new BlockStateInterface(ctx), pos.getX(), pos.getY(), pos.getZ());
    }

    static boolean canWalkOn(IPlayerContext ctx, BetterBlockPos pos) {
        return canWalkOn(new BlockStateInterface(ctx), pos.x, pos.y, pos.z);
    }

    static boolean canWalkOn(BlockStateInterface bsi, int x, int y, int z) {
        return canWalkOn(bsi, x, y, z, bsi.get0(x, y, z));
    }

    static boolean canUseFrostWalker(CalculationContext context, IBlockState state) {
        return context.frostWalker != 0
                && (state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.FLOWING_WATER)
                && ((Integer) state.getValue(BlockLiquid.LEVEL)) == 0;
    }

    static boolean canUseFrostWalker(IPlayerContext ctx, BlockPos pos) {
        IBlockState state = BlockStateInterface.get(ctx, pos);
        return EnchantmentHelper.hasFrostWalkerEnchantment(ctx.player())
                && (state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.FLOWING_WATER)
                && ((Integer) state.getValue(BlockLiquid.LEVEL)) == 0;
    }

    /**
     * If movements make us stand/walk on this block, will it have a top to walk on?
     */
    static boolean mustBeSolidToWalkOn(CalculationContext context, int x, int y, int z, IBlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.LADDER || block == Blocks.VINE) {
            return false;
        }
        if (block instanceof BlockLiquid) {
            if (context.assumeWalkOnWater) {
                return false;
            }
            Block blockAbove = context.getBlock(x, y + 1, z);
            if (blockAbove instanceof BlockLiquid) {
                return false;
            }
        }
        return true;
    }

    static boolean canPlaceAgainst(BlockStateInterface bsi, int x, int y, int z) {
        return canPlaceAgainst(bsi, x, y, z, bsi.get0(x, y, z));
    }

    static boolean canPlaceAgainst(BlockStateInterface bsi, BlockPos pos) {
        return canPlaceAgainst(bsi, pos.getX(), pos.getY(), pos.getZ());
    }

    static boolean canPlaceAgainst(IPlayerContext ctx, BlockPos pos) {
        return canPlaceAgainst(new BlockStateInterface(ctx), pos);
    }

    static boolean canPlaceAgainst(BlockStateInterface bsi, int x, int y, int z, IBlockState state) {
        if (!bsi.worldBorder.canPlaceAt(x, z)) {
            return false;
        }
        // can we look at the center of a side face of this block and likely be able to place?
        // (thats how this check is used)
        // therefore dont include weird things that we technically could place against (like carpet) but practically can't
        return state.isBlockNormalCube() || state.isFullBlock() || state.getBlock() == Blocks.GLASS || state.getBlock() == Blocks.STAINED_GLASS;
    }

    static double getMiningDurationTicks(CalculationContext context, int x, int y, int z, boolean includeFalling) {
        return getMiningDurationTicks(context, x, y, z, context.get(x, y, z), includeFalling);
    }

    static double getMiningDurationTicks(CalculationContext context, int x, int y, int z, IBlockState state, boolean includeFalling) {
        Block block = state.getBlock();
        if (!canWalkThrough(context, x, y, z, state)) {
            if (block instanceof BlockLiquid) {
                return COST_INF;
            }
            double mult = context.breakCostMultiplierAt(x, y, z, state);
            if (mult >= COST_INF) {
                return COST_INF;
            }
            if (avoidBreaking(context.bsi, x, y, z, state)) {
                return COST_INF;
            }
            double strVsBlock = context.toolSet.getStrVsBlock(state);
            if (strVsBlock <= 0) {
                return COST_INF;
            }
            double result = 1 / strVsBlock;
            result += context.breakBlockAdditionalCost;
            result *= mult;
            if (includeFalling) {
                IBlockState above = context.get(x, y + 1, z);
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

    /**
     * AutoTool for a specific block
     *
     * @param ctx   The player context
     * @param state The blockstate to mine
     */
    static void switchToBestToolFor(IPlayerContext ctx, IBlockState state) {
        if (ToolSet.isAutoTool()) {
            // TODO: Submit through InventoryBehavior, instead of executing the strategy here
            final InventorySlot slot = new ToolSet(ctx).getBestSlot(state, Baritone.settings().preferSilkTouch.value, null);
            final InventoryBehavior.SelectionStrategy strategy = ((Baritone) ctx.baritone()).getInventoryBehavior().resolveSelectionStrategy(slot);
            if (strategy != null) {
                strategy.run();
            }
        }
    }

    static void moveTowards(IPlayerContext ctx, MovementState state, BlockPos pos) {
        state.setTarget(new MovementTarget(
                RotationUtils.calcRotationFromVec3d(ctx.playerHead(),
                        VecUtils.getBlockPosCenter(pos),
                        ctx.playerRotations()).withPitch(ctx.playerRotations().getPitch()),
                false
        )).setInput(Input.MOVE_FORWARD, true);
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
     * @param ctx The player context
     * @param bp  The block pos
     * @return Whether or not the block is water
     */
    static boolean isWater(IPlayerContext ctx, BlockPos bp) {
        return isWater(BlockStateInterface.getBlock(ctx, bp));
    }

    static boolean isLava(Block b) {
        return b == Blocks.FLOWING_LAVA || b == Blocks.LAVA;
    }

    /**
     * Returns whether or not the specified pos has a liquid
     *
     * @param ctx The player context
     * @param p   The pos
     * @return Whether or not the block is a liquid
     */
    static boolean isLiquid(IPlayerContext ctx, BlockPos p) {
        return BlockStateInterface.getBlock(ctx, p) instanceof BlockLiquid;
    }

    static boolean possiblyFlowing(IBlockState state) {
        // Will be IFluidState in 1.13
        return state.getBlock() instanceof BlockLiquid
                && state.getValue(BlockLiquid.LEVEL) != 0;
    }

    static boolean isFlowing(int x, int y, int z, IBlockState state, BlockStateInterface bsi) {
        if (!(state.getBlock() instanceof BlockLiquid)) {
            return false;
        }
        if (state.getValue(BlockLiquid.LEVEL) != 0) {
            return true;
        }
        return possiblyFlowing(bsi.get0(x + 1, y, z))
                || possiblyFlowing(bsi.get0(x - 1, y, z))
                || possiblyFlowing(bsi.get0(x, y, z + 1))
                || possiblyFlowing(bsi.get0(x, y, z - 1));
    }


    static PlaceResult attemptToPlaceABlock(MovementState state, IBaritone baritone, BlockPos placeAt, boolean preferDown, boolean wouldSneak) {
        IPlayerContext ctx = baritone.getPlayerContext();
        Optional<Rotation> direct = RotationUtils.reachable(ctx, placeAt, wouldSneak); // we assume that if there is a block there, it must be replacable
        boolean found = false;
        if (direct.isPresent()) {
            state.setTarget(new MovementState.MovementTarget(direct.get(), true));
            found = true;
        }
        for (int i = 0; i < 5; i++) {
            BlockPos against1 = placeAt.offset(HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[i]);
            if (MovementHelper.canPlaceAgainst(ctx, against1)) {
                if (!((Baritone) baritone).getInventoryBehavior().selectThrowawayForLocation(false, placeAt.getX(), placeAt.getY(), placeAt.getZ())) { // get ready to place a throwaway block
                    Helper.HELPER.logDebug("bb pls get me some blocks. dirt, netherrack, cobble");
                    state.setStatus(MovementStatus.UNREACHABLE);
                    return PlaceResult.NO_OPTION;
                }
                double faceX = (placeAt.getX() + against1.getX() + 1.0D) * 0.5D;
                double faceY = (placeAt.getY() + against1.getY() + 0.5D) * 0.5D;
                double faceZ = (placeAt.getZ() + against1.getZ() + 1.0D) * 0.5D;
                Rotation place = RotationUtils.calcRotationFromVec3d(wouldSneak ? RayTraceUtils.inferSneakingEyePosition(ctx.player()) : ctx.playerHead(), new Vec3d(faceX, faceY, faceZ), ctx.playerRotations());
                Rotation actual = baritone.getLookBehavior().getAimProcessor().peekRotation(place);
                RayTraceResult res = RayTraceUtils.rayTraceTowards(ctx.player(), actual, ctx.playerController().getBlockReachDistance(), wouldSneak);
                if (res != null && res.typeOfHit == RayTraceResult.Type.BLOCK && res.getBlockPos().equals(against1) && res.getBlockPos().offset(res.sideHit).equals(placeAt)) {
                    state.setTarget(new MovementState.MovementTarget(place, true));
                    found = true;

                    if (!preferDown) {
                        // if preferDown is true, we want the last option
                        // if preferDown is false, we want the first
                        break;
                    }
                }
            }
        }
        if (ctx.getSelectedBlock().isPresent()) {
            BlockPos selectedBlock = ctx.getSelectedBlock().get();
            EnumFacing side = ctx.objectMouseOver().sideHit;
            // only way for selectedBlock.equals(placeAt) to be true is if it's replacable
            if (selectedBlock.equals(placeAt) || (MovementHelper.canPlaceAgainst(ctx, selectedBlock) && selectedBlock.offset(side).equals(placeAt))) {
                if (wouldSneak) {
                    state.setInput(Input.SNEAK, true);
                }
                ((Baritone) baritone).getInventoryBehavior().selectThrowawayForLocation(true, placeAt.getX(), placeAt.getY(), placeAt.getZ());
                return PlaceResult.READY_TO_PLACE;
            }
        }
        if (found) {
            if (wouldSneak) {
                state.setInput(Input.SNEAK, true);
            }
            ((Baritone) baritone).getInventoryBehavior().selectThrowawayForLocation(true, placeAt.getX(), placeAt.getY(), placeAt.getZ());
            return PlaceResult.ATTEMPTING;
        }
        return PlaceResult.NO_OPTION;
    }

    enum PlaceResult {
        READY_TO_PLACE, ATTEMPTING, NO_OPTION;
    }

    static boolean isTransparent(Block b) {

        return b == Blocks.AIR ||
                b == Blocks.FLOWING_LAVA ||
                b == Blocks.FLOWING_WATER ||
                b == Blocks.WATER;
    }
}
