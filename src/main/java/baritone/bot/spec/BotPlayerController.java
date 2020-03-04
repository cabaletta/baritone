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

package baritone.bot.spec;

import baritone.api.bot.IBaritoneUser;
import baritone.api.utils.IPlayerController;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCommandBlock;
import net.minecraft.block.BlockStructure;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraft.world.World;

/**
 * @author Brady
 * @since 11/14/2018
 */
public final class BotPlayerController implements IPlayerController {

    private final IBaritoneUser user;
    private GameType gameType;

    private BlockPos currentBlock;
    private ItemStack currentHittingItem;
    private boolean hittingBlock;
    private float blockDamage;
    private int blockHitDelay;
    private int heldItemServer;

    public BotPlayerController(IBaritoneUser user) {
        this.user = user;
        this.currentHittingItem = ItemStack.EMPTY;
        this.currentBlock = new BlockPos(-1, -1, -1);
    }

    @Override
    public boolean onPlayerDamageBlock(BlockPos pos, EnumFacing side) {
        this.syncHeldItem();

        EntityPlayerSP player = this.user.getEntity();
        World world = player.world;

        if (this.blockHitDelay > 0) {
            this.blockHitDelay--;
            return true;
        }

        if (!this.isHittingPosition(pos)) {
            return this.clickBlock(pos, side);
        }

        IBlockState state = world.getBlockState(pos);

        if (state.getMaterial() == Material.AIR) {
            this.hittingBlock = false;
            return false;
        }

        this.blockDamage += state.getPlayerRelativeBlockHardness(player, world, pos);

        if (this.blockDamage >= 1.0F) {
            this.hittingBlock = false;
            this.blockDamage = 0.0F;
            this.blockHitDelay = 5;

            player.connection.sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, side));
            this.handleBreak(pos);
        }
        return true;
    }

    @Override
    public void resetBlockRemoving() {
        if (this.hittingBlock) {
            this.hittingBlock = false;
            this.blockDamage = 0.0F;
            this.user.getEntity().resetCooldown();
            this.user.getEntity().connection.sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, this.currentBlock, EnumFacing.DOWN));
        }
    }

    @Override
    public ItemStack windowClick(int windowId, int slotId, int mouseButton, ClickType type, EntityPlayer player) {
        short transactionID = player.openContainer.getNextTransactionID(player.inventory);
        ItemStack stack = player.openContainer.slotClick(slotId, mouseButton, type, player);
        this.user.getEntity().connection.sendPacket(new CPacketClickWindow(windowId, slotId, mouseButton, type, stack, transactionID));
        return stack;
    }

    @Override
    public void setGameType(GameType type) {
        this.gameType = type;
        this.gameType.configurePlayerCapabilities(this.user.getEntity().capabilities);
    }

    @Override
    public GameType getGameType() {
        return this.gameType;
    }

    @Override
    public boolean clickBlock(BlockPos pos, EnumFacing side) {
        EntityPlayerSP player = this.user.getEntity();
        World world = player.world;

        if (!canBreak(player, pos)) {
            return false;
        }

        if (!this.hittingBlock || !this.isHittingPosition(pos)) {
            if (this.hittingBlock) {
                player.connection.sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, this.currentBlock, side));
            }

            IBlockState state = world.getBlockState(pos);
            player.connection.sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, side));
            boolean flag = state.getMaterial() != Material.AIR;

            if (!flag && this.blockDamage == 0.0F) {
                state.getBlock().onBlockClicked(world, pos, player);
            }

            if (!flag && state.getPlayerRelativeBlockHardness(player, player.world, pos) >= 1.0F) {
                this.handleBreak(pos);
            } else {
                this.hittingBlock = true;
                this.currentBlock = pos;
                this.blockDamage = 0.0F;
                this.currentHittingItem = player.getHeldItemMainhand();
            }
        }

        return true;
    }

    private void handleBreak(BlockPos pos) {
        EntityPlayerSP player = this.user.getEntity();
        World world = player.world;

        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if ((block instanceof BlockCommandBlock || block instanceof BlockStructure) && !player.canUseCommandBlock()) {
            return;
        }

        if (state.getMaterial() == Material.AIR) {
            return;
        }

        block.onBlockHarvested(world, pos, state, player);

        if (world.setBlockState(pos, Blocks.AIR.getDefaultState(), 11)) {
            block.onPlayerDestroy(world, pos, state);
        }

        this.currentBlock = new BlockPos(this.currentBlock.getX(), -1, this.currentBlock.getZ());

        ItemStack stack = player.getHeldItemMainhand();

        if (!stack.isEmpty()) {
            stack.onBlockDestroyed(world, state, pos, player);

            if (stack.isEmpty()) {
                player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
            }
        }
    }

    private boolean canBreak(EntityPlayer player, BlockPos pos) {
        if (this.gameType.isCreative() || this.gameType == GameType.SPECTATOR) {
            return false;
        }

        if (!player.world.getWorldBorder().contains(pos)) {
            return false;
        }

        if (this.gameType.hasLimitedInteractions() && !player.isAllowEdit()) {
            ItemStack stack = player.getHeldItemMainhand();
            return !stack.isEmpty() && stack.canDestroy(player.world.getBlockState(pos).getBlock());
        }

        return true;
    }

    private boolean isHittingPosition(BlockPos pos) {
        ItemStack stack = this.user.getEntity().getHeldItemMainhand();
        boolean itemUnchanged = this.currentHittingItem.isEmpty() && stack.isEmpty();

        if (!this.currentHittingItem.isEmpty() && !stack.isEmpty()) {
            itemUnchanged = stack.getItem() == this.currentHittingItem.getItem()
                    && ItemStack.areItemStackTagsEqual(stack, this.currentHittingItem)
                    && (stack.isItemStackDamageable() || stack.getMetadata() == this.currentHittingItem.getMetadata());
        }

        return pos.equals(this.currentBlock) && itemUnchanged;
    }

    @Override
    public void syncHeldItem() {
        int heldItemClient = this.user.getEntity().inventory.currentItem;

        if (heldItemClient != this.heldItemServer) {
            this.heldItemServer = heldItemClient;
            this.user.getEntity().connection.sendPacket(new CPacketHeldItemChange(this.heldItemServer));
        }
    }

    @Override
    public boolean hasBrokenBlock() {
        return this.currentBlock.getY() == -1;
    }

    @Override
    public EnumActionResult processRightClickBlock(EntityPlayerSP player, World world, BlockPos pos, EnumFacing direction, Vec3d vec, EnumHand hand) {
        this.syncHeldItem();
        if (!world.getWorldBorder().contains(pos)) {
            return EnumActionResult.FAIL;
        }

        ItemStack held = player.getHeldItem(hand);
        Vec3d hitVec = vec.subtract(new Vec3d(pos));
        float hitX = (float) hitVec.x;
        float hitY = (float) hitVec.y;
        float hitZ = (float) hitVec.z;

        boolean usedBlock = false;

        if (this.gameType != GameType.SPECTATOR) {
            IBlockState state = world.getBlockState(pos);
            boolean emptyHands = player.getHeldItemMainhand().isEmpty() && player.getHeldItemOffhand().isEmpty();

            if ((!player.isSneaking() || emptyHands) && state.getBlock().onBlockActivated(world, pos, state, player, hand, direction, hitX, hitY, hitZ)) {
                usedBlock = true;
            }

            if (!usedBlock && held.getItem() instanceof ItemBlock) {
                if (!((ItemBlock) held.getItem()).canPlaceBlockOnSide(world, pos, direction, player, held)) {
                    return EnumActionResult.FAIL;
                }
            }
        }

        this.user.getEntity().connection.sendPacket(
                new CPacketPlayerTryUseItemOnBlock(pos, direction, hand, hitX, hitY, hitZ)
        );

        if (!usedBlock && this.gameType != GameType.SPECTATOR) {
            if (held.isEmpty() || player.getCooldownTracker().hasCooldown(held.getItem())) {
                return EnumActionResult.PASS;
            }

            if (held.getItem() instanceof ItemBlock && !player.canUseCommandBlock()) {
                Block block = ((ItemBlock) held.getItem()).getBlock();

                if (block instanceof BlockCommandBlock || block instanceof BlockStructure) {
                    return EnumActionResult.FAIL;
                }
            }

            return held.onItemUse(player, world, pos, hand, direction, hitX, hitY, hitZ);
        }

        return EnumActionResult.SUCCESS;
    }

    @Override
    public EnumActionResult processRightClick(EntityPlayerSP player, World world, EnumHand hand) {
        if (this.gameType == GameType.SPECTATOR) {
            return EnumActionResult.PASS;
        }

        this.syncHeldItem();
        this.user.getEntity().connection.sendPacket(new CPacketPlayerTryUseItem(hand));

        ItemStack stack = player.getHeldItem(hand);
        if (player.getCooldownTracker().hasCooldown(stack.getItem())) {
            return EnumActionResult.PASS;
        }

        ActionResult<ItemStack> result = stack.useItemRightClick(world, player, hand);
        ItemStack newStack = result.getResult();

        if (newStack != stack || newStack.getCount() != stack.getCount()) {
            player.setHeldItem(hand, newStack);
        }

        return result.getType();
    }

    @Override
    public void setHittingBlock(boolean hittingBlock) {
        this.hittingBlock = hittingBlock;
    }
}
