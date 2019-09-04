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

package baritone.api.schematic;

import baritone.api.IBaritone;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.ISchematic;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSchematic implements ISchematic {
    protected final IBaritone baritone;
    protected final IPlayerContext ctx;
    protected int x;
    protected int y;
    protected int z;

    public AbstractSchematic(@Nullable IBaritone baritone, int x, int y, int z) {
        this.baritone = baritone;
        this.ctx = baritone == null ? null : baritone.getPlayerContext();
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public int widthX() {
        return x;
    }

    @Override
    public int heightY() {
        return y;
    }

    @Override
    public int lengthZ() {
        return z;
    }

    protected IBlockState[] approxPlaceable() {
        EntityPlayerSP player = ctx.player();
        NonNullList<ItemStack> inventory = player.inventory.mainInventory;
        List<IBlockState> placeable = new ArrayList<>();
        placeable.add(Blocks.AIR.getDefaultState());

        // 27 + 9
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.get(i);
            Item item = stack.getItem();

            if (!stack.isEmpty() && stack.getItem() instanceof ItemBlock) {
                ItemBlock itemBlock = (ItemBlock) item;

                // <toxic cloud>
                placeable.add(itemBlock.getBlock().getStateForPlacement(
                    ctx.world(),
                    ctx.playerFeet(),
                    EnumFacing.UP,
                    (float) player.posX,
                    (float) player.posY,
                    (float) player.posZ,
                    itemBlock.getMetadata(stack.getMetadata()),
                    player
                ));
                // </toxic cloud>
            }
        }

        return placeable.toArray(new IBlockState[0]);
    }
}
