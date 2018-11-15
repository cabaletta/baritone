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

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.StatisticsManager;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

/**
 * @author Brady
 * @since 11/14/2018
 */
public class BotPlayerController extends PlayerControllerMP {

    public BotPlayerController(Minecraft mcIn, NetHandlerPlayClient netHandler) {
        super(mcIn, netHandler);
    }

    @Override
    public void setPlayerCapabilities(EntityPlayer player) {
        super.setPlayerCapabilities(player);
    }

    @Override
    public boolean isSpectator() {
        return super.isSpectator();
    }

    @Override
    public void setGameType(@Nonnull GameType type) {
        super.setGameType(type);
    }

    @Override
    public void flipPlayer(EntityPlayer playerIn) {
        super.flipPlayer(playerIn);
    }

    @Override
    public boolean shouldDrawHUD() {
        return super.shouldDrawHUD();
    }

    @Override
    public boolean onPlayerDestroyBlock(@Nonnull BlockPos pos) {
        return super.onPlayerDestroyBlock(pos);
    }

    @Override
    public boolean clickBlock(@Nonnull BlockPos loc, @Nonnull EnumFacing face) {
        return super.clickBlock(loc, face);
    }

    @Override
    public void resetBlockRemoving() {
        super.resetBlockRemoving();
    }

    @Override
    public boolean onPlayerDamageBlock(@Nonnull BlockPos posBlock, @Nonnull EnumFacing directionFacing) {
        return super.onPlayerDamageBlock(posBlock, directionFacing);
    }

    @Override
    public float getBlockReachDistance() {
        return super.getBlockReachDistance();
    }

    @Override
    public void updateController() {
        super.updateController();
    }

    @Nonnull
    @Override
    public EnumActionResult processRightClickBlock(EntityPlayerSP player, @Nonnull WorldClient worldIn, BlockPos pos, @Nonnull EnumFacing direction, Vec3d vec, @Nonnull EnumHand hand) {
        return super.processRightClickBlock(player, worldIn, pos, direction, vec, hand);
    }

    @Nonnull
    @Override
    public EnumActionResult processRightClick(@Nonnull EntityPlayer player, @Nonnull World worldIn, @Nonnull EnumHand hand) {
        return super.processRightClick(player, worldIn, hand);
    }

    @Nonnull
    @Override
    public EntityPlayerSP createPlayer(World p_192830_1_, @Nonnull StatisticsManager p_192830_2_, @Nonnull RecipeBook p_192830_3_) {
        return super.createPlayer(p_192830_1_, p_192830_2_, p_192830_3_);
    }

    @Override
    public void attackEntity(@Nonnull EntityPlayer playerIn, Entity targetEntity) {
        super.attackEntity(playerIn, targetEntity);
    }

    @Nonnull
    @Override
    public EnumActionResult interactWithEntity(@Nonnull EntityPlayer player, Entity target, @Nonnull EnumHand hand) {
        return super.interactWithEntity(player, target, hand);
    }

    @Nonnull
    @Override
    public EnumActionResult interactWithEntity(@Nonnull EntityPlayer player, Entity target, RayTraceResult ray, @Nonnull EnumHand hand) {
        return super.interactWithEntity(player, target, ray, hand);
    }

    @Nonnull
    @Override
    public ItemStack windowClick(int windowId, int slotId, int mouseButton, @Nonnull ClickType type, EntityPlayer player) {
        return super.windowClick(windowId, slotId, mouseButton, type, player);
    }

    @Override
    public void func_194338_a(int p_194338_1_, @Nonnull IRecipe p_194338_2_, boolean p_194338_3_, EntityPlayer p_194338_4_) {
        super.func_194338_a(p_194338_1_, p_194338_2_, p_194338_3_, p_194338_4_);
    }

    @Override
    public void sendEnchantPacket(int windowID, int button) {
        super.sendEnchantPacket(windowID, button);
    }

    @Override
    public void sendSlotPacket(@Nonnull ItemStack itemStackIn, int slotId) {
        super.sendSlotPacket(itemStackIn, slotId);
    }

    @Override
    public void sendPacketDropItem(@Nonnull ItemStack itemStackIn) {
        super.sendPacketDropItem(itemStackIn);
    }

    @Override
    public void onStoppedUsingItem(EntityPlayer playerIn) {
        super.onStoppedUsingItem(playerIn);
    }

    @Override
    public boolean gameIsSurvivalOrAdventure() {
        return super.gameIsSurvivalOrAdventure();
    }

    @Override
    public boolean isNotCreative() {
        return super.isNotCreative();
    }

    @Override
    public boolean isInCreativeMode() {
        return super.isInCreativeMode();
    }

    @Override
    public boolean extendedReach() {
        return super.extendedReach();
    }

    @Override
    public boolean isRidingHorse() {
        return super.isRidingHorse();
    }

    @Override
    public boolean isSpectatorMode() {
        return super.isSpectatorMode();
    }

    @Nonnull
    @Override
    public GameType getCurrentGameType() {
        return super.getCurrentGameType();
    }

    @Override
    public boolean getIsHittingBlock() {
        return super.getIsHittingBlock();
    }

    @Override
    public void pickItem(int index) {
        super.pickItem(index);
    }
}
