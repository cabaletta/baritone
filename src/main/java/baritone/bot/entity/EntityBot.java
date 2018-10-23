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

package baritone.bot.entity;

import baritone.bot.IBaritoneUser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
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
import net.minecraft.world.IInteractionObject;
import net.minecraft.world.World;

// Some Notes:
// TODO: Make custom movement input
// startRiding references the sound manager
// onUpdateWalkingPlayer references the gameSettings autoJump flag
// notifyDataManagerChange references the sound manager
// onLivingUpdate makes a lot of references to mc fields
//  - playerController
//  - currentScreen
//  - gameSettings
//  - tutorial
// What needs to be considered
//  - The server tells us what our entity id should be, the bot entity should respect this.

/**
 * @author Brady
 * @since 10/23/2018
 */
@SuppressWarnings("EntityConstructor")
public class EntityBot extends EntityPlayerSP {

    private final IBaritoneUser user;

    public EntityBot(IBaritoneUser user, Minecraft mc, World world, NetHandlerPlayClient netHandlerPlayClient, StatisticsManager statisticsManager, RecipeBook recipeBook) {
        super(mc, world, netHandlerPlayClient, statisticsManager, recipeBook);
        this.user = user;
    }

    @Override
    public void closeScreenAndDropStack() {
        this.inventory.setItemStack(ItemStack.EMPTY);
        super.closeScreen();
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
        // Don't you even try @leijurv
        return false;
    }
}
