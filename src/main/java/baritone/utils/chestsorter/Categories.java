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

package baritone.utils.chestsorter;

import net.minecraft.block.BlockStone;
import net.minecraft.init.Items;
import net.minecraft.item.*;

import java.util.Arrays;
import java.util.List;

import static baritone.utils.chestsorter.Category.*;

public interface Categories {


    // maybe category instead of List?
    List<Category<Item, ? extends Item>> FOOD_CATEGORIES = Arrays.asList(
        itemType(ItemAppleGold.class), // not sure if gold apple dont correctly
        itemEquals(Items.GOLDEN_APPLE),
        itemEquals(Items.GOLDEN_CARROT),
        itemEquals(Items.COOKED_BEEF, itemEquals(Items.BEEF)),
        itemEquals(Items.COOKED_PORKCHOP, itemEquals(Items.PORKCHOP)),
        itemEquals(Items.COOKED_CHICKEN, itemEquals(Items.CHICKEN)),
        itemEquals(Items.COOKED_MUTTON, itemEquals(Items.MUTTON)),
        itemEquals(Items.FISH, itemEquals(Items.COOKED_FISH)),
        itemEquals(Items.RABBIT, itemEquals(Items.COOKED_RABBIT)),
        itemEquals(Items.BAKED_POTATO),
        itemEquals(Items.BREAD),
        itemEquals(Items.MELON),
        itemEquals(Items.COOKIE),
        itemType(ItemFood.class) // TODO: categorize ItemFood better maybe
    );

    Category<Item, ItemBlock> STONE_BLOCK_CATEGORY =
        itemType(ItemBlock.class,
            forItem(itemBlock -> itemBlock.getBlock() instanceof BlockStone,
                enumCategories(BlockStone.EnumType.class, (stack, item) -> item.getBlock().getDefaultState().getValue(BlockStone.VARIANT)) // will probably need to simplify if there's more like this
            )
        );







}
