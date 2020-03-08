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

import baritone.Baritone;
import baritone.api.bot.IBaritoneUser;
import baritone.utils.PlayerMovementInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IMerchant;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.StatisticsManager;
import net.minecraft.tileentity.CommandBlockBaseLogic;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.tileentity.TileEntityStructure;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.GameType;
import net.minecraft.world.IInteractionObject;
import net.minecraft.world.World;

import javax.annotation.Nullable;

// Some Notes:
// onLivingUpdate makes a lot of references to mc fields
//  - playerController

/**
 * @author Brady
 * @since 10/23/2018
 */
@SuppressWarnings("EntityConstructor")
public final class EntityBot extends EntityPlayerSP {

    private final IBaritoneUser user;
    private NetworkPlayerInfo playerInfo;

    public EntityBot(IBaritoneUser user, Minecraft mc, World world, NetHandlerPlayClient netHandlerPlayClient, StatisticsManager statisticsManager, RecipeBook recipeBook) {
        super(mc, world, netHandlerPlayClient, statisticsManager, recipeBook);
        this.user = user;
        this.movementInput = new PlayerMovementInput(this.user.getBaritone().getInputOverrideHandler());
    }

    @Override
    public void onUpdate() {
        this.entityCollisionReduction = Baritone.settings().botCollision.value ? 0.0F : 1.0F;
        super.onUpdate();
    }

    @Override
    public void closeScreenAndDropStack() {
        this.inventory.setItemStack(ItemStack.EMPTY);

        // EntityPlayer#closeScreen
        this.openContainer = this.inventoryContainer;
    }

    @Override
    public void sendStatusMessage(ITextComponent chatComponent, boolean actionBar) {
        // TODO: Custom message handling
    }

    @Override
    public void sendMessage(ITextComponent component) {
        // TODO: Custom message handling
    }

    @Override
    public void openEditSign(TileEntitySign signTile) {
        // TODO: Custom GUI handling
    }

    @Override
    public void displayGuiEditCommandCart(CommandBlockBaseLogic commandBlock) {
        // TODO: Custom GUI handling
    }

    @Override
    public void displayGuiCommandBlock(TileEntityCommandBlock commandBlock) {
        // TODO: Custom GUI handling
    }

    @Override
    public void openEditStructure(TileEntityStructure structure) {
        // TODO: Custom GUI handling
    }

    @Override
    public void openBook(ItemStack stack, EnumHand hand) {
        // TODO: Custom GUI handling
    }

    @Override
    public void displayGUIChest(IInventory chestInventory) {
        // TODO: Custom GUI handling
    }

    @Override
    public void openGuiHorseInventory(AbstractHorse horse, IInventory inventoryIn) {
        // TODO: Custom GUI handling
    }

    @Override
    public void displayGui(IInteractionObject guiOwner) {
        // TODO: Custom GUI handling
    }

    @Override
    public void displayVillagerTradeGui(IMerchant villager) {
        // TODO: Custom GUI handling
    }

    @Override
    public void onCriticalHit(Entity entityHit) {
        // Don't render
    }

    @Override
    public void onEnchantmentCritical(Entity entityHit) {
        // Don't render
    }

    @Override
    protected boolean isCurrentViewEntity() {
        return true;
    }

    @Override
    public boolean isSpectator() {
        NetworkPlayerInfo networkplayerinfo = this.connection.getPlayerInfo(this.getGameProfile().getId());
        // noinspection ConstantConditions
        return networkplayerinfo != null && networkplayerinfo.getGameType() == GameType.SPECTATOR;
    }

    @Override
    public boolean isCreative() {
        NetworkPlayerInfo networkplayerinfo = this.connection.getPlayerInfo(this.getGameProfile().getId());
        // noinspection ConstantConditions
        return networkplayerinfo != null && networkplayerinfo.getGameType() == GameType.CREATIVE;
    }

    @Nullable
    @Override
    protected NetworkPlayerInfo getPlayerInfo() {
        return this.playerInfo == null ? (this.playerInfo = this.connection.getPlayerInfo(this.getUniqueID())) : null;
    }
}
