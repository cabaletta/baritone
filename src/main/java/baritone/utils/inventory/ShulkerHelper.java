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

package baritone.utils.inventory;

import baritone.Baritone;
import baritone.api.utils.Helper;
import baritone.api.utils.IPlayerContext;
import baritone.utils.BlockStateInterface;
import net.minecraft.block.BlockState;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;

/**
 * @author Matthew Carlson
 */
public class ShulkerHelper implements Helper {
    private final IPlayerContext ctx;

    private final Baritone baritone;

    public ShulkerHelper(IPlayerContext ctx, Baritone baritone) {
        this.ctx = ctx;
        this.baritone = baritone;
    }

    public static boolean isShulkerBoxWithSpace(ItemStack stack) {
        if (!isShulkerBox(stack))
            return false;
        return getEmptySpaceInShulkerBox(stack) > 0;
    }

    public static boolean isShulkerBox(ItemStack stack) {
        if (stack == null)
            return false;
        if (stack.isEmpty())
            return false;
        return ShulkerHelper.isShulkerBox(stack.getItem());
    }

    public static boolean isShulkerBox(Item i) {
        return i.equals(Items.SHULKER_BOX) || i.equals(Items.BLACK_SHULKER_BOX) || i.equals(Items.BLUE_SHULKER_BOX)
                || i.equals(Items.BROWN_SHULKER_BOX) || i.equals(Items.CYAN_SHULKER_BOX)
                || i.equals(Items.GRAY_SHULKER_BOX) || i.equals(Items.GREEN_SHULKER_BOX)
                || i.equals(Items.LIME_SHULKER_BOX) || i.equals(Items.MAGENTA_SHULKER_BOX)
                || i.equals(Items.ORANGE_SHULKER_BOX) || i.equals(Items.PINK_SHULKER_BOX)
                || i.equals(Items.PURPLE_SHULKER_BOX) || i.equals(Items.RED_SHULKER_BOX)
                || i.equals(Items.WHITE_SHULKER_BOX) || i.equals(Items.YELLOW_SHULKER_BOX);
    }

    public BlockPos findPlaceToPutShulkerBox() {
        BlockPos player_pos = ctx.player().getPosition();
        BlockPos player_lower = player_pos.down();

        BlockPos places[] = {
                // upper half
                player_pos.add(1, 0, 1), player_pos.add(1, 0, 0), player_pos.add(0, 0, 1), player_pos.add(-1, 0, 0),
                player_pos.add(0, 0, -1), player_pos.add(-1, 0, 1), player_pos.add(1, 0, -1), player_pos.add(-1, 0, -1),
                // lower half
                player_lower.add(1, 0, 1), player_lower.add(1, 0, 0), player_lower.add(0, 0, 1),
                player_lower.add(-1, 0, 0), player_lower.add(0, 0, -1), player_lower.add(-1, 0, 1),
                player_lower.add(1, 0, -1), player_lower.add(-1, 0, -1) };
        BlockStateInterface bsi = baritone.bsi;
        for (BlockPos place : places) {
            BlockState upper = bsi.get0(place.up());
            BlockState lower = bsi.get0(place.down());
            if (bsi.get0(place).isAir() && (upper.isTransparent() || upper.isAir()) && lower.isSolid()) // Make sure we
                                                                                                        // can place the
                                                                                                        // thing
                // there and we can open it later
                return place;
        }
        System.out.println("We failed to find a spot to place the shulker box");
        logDirect("We failed to find a spot to place the shulker box");
        return null;

    }

    /**
     * Returns the number of empty slots in a shulker
     */
    public static int getEmptySpaceInShulkerBox(ItemStack stack) {
        if (!ShulkerHelper.isShulkerBox(stack))
            return 0;
        return ShulkerHelper.getEmptySpaceInShulkerBox(stack.getTag());
    }

    /**
     * @return returns the number of slots available in a given shulker
     */
    public static int getEmptySpaceInShulkerBox(CompoundNBT data) {
        int defaultSize = 27;
        if (data == null)
            return defaultSize;
        CompoundNBT blockEntityTag = data.getCompound("BlockEntityTag");
        if (blockEntityTag == null)
            return defaultSize;
        ListNBT items = blockEntityTag.getList("Items", 10);
        if (items == null)
            return defaultSize;
        // TODO: figure out how much we can fit of what we currently have
        return defaultSize - items.size();
    }

    public boolean isCurrentHeldItemShulkerBox() {
        if (isShulkerBox(ctx.player().getHeldItemMainhand()))
            return true;
        if (isShulkerBox(ctx.player().getHeldItemOffhand()))
            return true;
        return false;
    }

}