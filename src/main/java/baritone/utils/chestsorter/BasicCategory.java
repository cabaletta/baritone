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
import java.util.Comparator;
import java.util.List;
import java.util.function.BiPredicate;
import net.minecraft.util.text.translation.I18n;

final class BasicCategory<SUPER extends Item, T extends SUPER> implements Category<SUPER, T> {

    private final BiPredicate<ItemStack, SUPER> predicate;
    private final List<Category<T, ?>> subCategories;


    private BasicCategory(BiPredicate<ItemStack, SUPER> predicate, List<Category<T, ?>> subCategories) {
        this.predicate = predicate;
        this.subCategories = Collections.unmodifiableList(subCategories);
    }

    @SafeVarargs
    public BasicCategory(BiPredicate<ItemStack, SUPER> predicate, Category<T, ?>... subCategories) {
        this(predicate, Arrays.asList(subCategories));
    }


    @Override
    public boolean isInCategory(ItemStack stack, SUPER obj) {
        return this.predicate.test(stack, obj);
    }

    @Override
    public final List<Category<T, ? extends Item>> getSubcategories() {
        return this.subCategories;
    }

    @Override
    public Comparator<T> comparator() { // TODO: allow comparator to be customized
        return Comparator.comparing(item -> I18n.translateToLocal(item.getTranslationKey()), String.CASE_INSENSITIVE_ORDER);
    }
}