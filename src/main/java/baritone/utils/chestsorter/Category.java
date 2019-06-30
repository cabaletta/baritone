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

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.*;
import java.util.stream.Stream;

// items only
// TODO: make more type safe
public interface Category<SUPER extends Item, T extends SUPER> {
    // responsible for type checks
    // if this function returns true then SUPER should assignable to T
    boolean isInCategory(ItemStack stack, SUPER item);
    //Class<? extends T> getSubType(); // TODO: use this for type checking

    List<Category<T, ? extends Item>> getSubcategories();
    Comparator<T> comparator();



    // returns -1 if stack is not in category
    static int indexOf(ItemStack stack, Category<Item, Item> root) {
        final Category<? extends Item, ? extends Item> category = findBestCategory(stack, root);
        if (category != null) {
            // tfw no coroutines
            final List<Category<? extends Item, ? extends Item>> flatList = new ArrayList<>();
            postOrderTraverse(root, flatList::add);

            return flatList.indexOf(category);
        } else {
            return -1;
        }

    }


    @SuppressWarnings("unchecked")
    @Nullable
    // breadth first search
    static Category<? extends Item, ? extends Item> findBestCategory(ItemStack stack, Category<? extends Item, ? extends Item> category) {
        // cast to raw type
        if (!((Category)category).isInCategory(stack, stack.getItem())) return null; // this should only be done for root category

        for (Category iter : category.getSubcategories()) {
            if (iter.isInCategory(stack, stack.getItem())) {
                return findBestCategory(stack, iter);
            }
        }
        return category;
    }

    static void postOrderTraverse(Category<? extends Item, ? extends Item> category, Consumer<Category<? extends Item, ? extends Item>> consumer) {
        for (Category<? extends Item, ? extends Item> iter : category.getSubcategories()) {
            postOrderTraverse(iter, consumer);
        }
        consumer.accept(category);
    }



    //
    // ItemStack
    //
    @SafeVarargs
    @Deprecated // biPredicate must do type checking
    static <SUPER extends Item, T extends SUPER> Category<SUPER, T> predicate(BiPredicate<ItemStack, SUPER> biPredicate, Category<T, ?>... subCategories) {
        return new BasicCategory<>(biPredicate, subCategories);
    }

    // subcategories ignored
    // unsure about this function
    @SafeVarargs
    static <SUPER extends Item> Category<SUPER, SUPER> notMatching(Category<SUPER, ?> category, Category<SUPER, SUPER>... subCategories) {
        return predicate((stack, item) -> !category.isInCategory(stack, item), subCategories);
    }

    //
    // Item
    //
    @SafeVarargs
    @Deprecated // predicate must do type checking
    static <SUPER extends Item, T extends SUPER> Category<SUPER, T> itemPredicate(Predicate<SUPER> predicate, Category<T, ?>... subCategories) {
        return predicate((stack, item) -> predicate.test(item), subCategories);
    }

    @SafeVarargs
    static <SUPER extends Item, T extends SUPER> Category<SUPER, T> itemType(Class<T> type, Category<T, ?>... subCategories) {
        return itemPredicate(type::isInstance, subCategories);
    }

    @SafeVarargs
    static <SUPER extends Item> Category<SUPER, SUPER> itemEquals(Item item, Category<SUPER, SUPER>... subCategories) {
        return itemPredicate(item::equals, subCategories);
    }

    //
    // ItemBlock
    //
    @SafeVarargs
    static <SUPER extends ItemBlock> Category<SUPER, SUPER> itemBlockType(Class<? extends Block> type, Category<SUPER, SUPER>... subCategories) {
        return itemPredicate(itemBlock -> type.isInstance(itemBlock.getBlock()), subCategories);
    }



    @SafeVarargs
    @SuppressWarnings("unchecked")
    // Item to enum
    static <SUPER extends Item, ENUM extends Enum<ENUM>> Category<SUPER, SUPER> itemEnums(Class<ENUM> enumClass, Function<SUPER, ENUM> toEnum, Category<SUPER, SUPER>... subCategories) {
        final Category<SUPER, SUPER>[] categories =
            Stream.of(enumClass.getEnumConstants())
                //.peek(enom -> System.out.println("Enum: " + enom))
                .map(enom -> Category.<SUPER, SUPER>itemPredicate(item -> toEnum.apply(item) == enom))
                .toArray(Category[]::new);

        return itemPredicate(obj -> true,
                categories
            );
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    // ItemStack and item to enum (unlikely this will be used)
    static <SUPER extends Item, T extends SUPER, ENUM extends Enum<ENUM>> Category<SUPER, T> enumCategories(Class<ENUM> enumClass, BiFunction<ItemStack, SUPER, ENUM> toEnum, Category<T, ?>... subCategories) {
        final Category<T, T>[] categories =
            Stream.of(enumClass.getEnumConstants())
                .map(enom -> Category.<SUPER, T>predicate((stack, item) -> toEnum.apply(stack, item) == enom))
                .toArray(Category[]::new);

        return predicate((a, b) -> true,
            categories
        );
    }
}