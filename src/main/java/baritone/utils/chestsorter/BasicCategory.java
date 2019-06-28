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

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;

public final class BasicCategory<SUPER extends Item, T extends SUPER> implements Category<SUPER, T> {

    private final BiPredicate<ItemStack, SUPER> predicate;
    private final List<Category<T, ?>> subCategories;


    protected BasicCategory(BiPredicate<ItemStack, SUPER> predicate, List<Category<T, ?>> subCategories) {
        this.predicate = predicate;
        this.subCategories = Collections.unmodifiableList(subCategories);
    }

    public BasicCategory(BiPredicate<ItemStack, SUPER> predicate, Category<T, ?>... subCategories) {
        this(predicate, Arrays.asList(subCategories));
    }


    @Override
    public boolean isInCategory(ItemStack stack, SUPER obj) {
        return this.predicate.test(stack, obj);
    }

    @Override
    public final List<Category<T, ?>> getSubcategories() {
        return this.subCategories;
    }
}