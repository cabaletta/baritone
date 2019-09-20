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
public interface Category<T extends Item, SUB extends T> {
    // responsible for type checks
    // if this function returns true then SUPER should assignable to T
    boolean isInCategory(ItemStack stack, T item);
    //Class<? extends T> getSubType(); // TODO: use this for type checking

    List<Category<SUB, ? extends Item>> getSubcategories();
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
    static Category<?, ?> findBestCategory(ItemStack stack, Category<?, ?> category) {
        // cast to raw type
        if (!((Category)category).isInCategory(stack, stack.getItem())) return null; // this should only be done for root category

        for (Category iter : category.getSubcategories()) {
            if (iter.isInCategory(stack, stack.getItem())) {
                return findBestCategory(stack, iter);
            }
        }
        return category;
    }

    static void postOrderTraverse(Category<?, ?> category, Consumer<Category<?, ?>> consumer) {
        for (Category<?, ?> iter : category.getSubcategories()) {
            postOrderTraverse(iter, consumer);
        }
        consumer.accept(category);
    }



    //
    // ItemStack
    //
    @SafeVarargs
    //@Deprecated // biPredicate must do type checking
    static <T extends Item, SUB extends T> Category<T, SUB> predicate(BiPredicate<ItemStack, T> biPredicate, Category<SUB, ?>... subCategories) {
        return new BasicCategory<>(biPredicate, subCategories);
    }

    // subcategories ignored
    // unsure about this function
    @SafeVarargs
    static <T extends Item> Category<T, T> notMatching(Category<T, ?> category, Category<T, T>... subCategories) {
        return predicate((stack, item) -> !category.isInCategory(stack, item), subCategories);
    }

    //
    // Item
    //
    @SafeVarargs
    //@Deprecated // predicate must do type checking
    static <T extends Item, SUB extends T> Category<T, SUB> itemPredicate(Predicate<T> predicate, Category<SUB, ?>... subCategories) {
        return predicate((stack, item) -> predicate.test(item), subCategories);
    }

    @SafeVarargs
    static <T extends Item, SUB extends T> Category<T, SUB> itemType(Class<? extends SUB> type, Category<SUB, ?>... subCategories) {
        return itemPredicate(type::isInstance, subCategories);
    }

    @SafeVarargs
    static <T extends Item> Category<T, T> itemEquals(Item item, Category<T, T>... subCategories) {
        return itemPredicate(item::equals, subCategories);
    }

    //
    // ItemBlock
    //
    @SafeVarargs
    static <T extends ItemBlock> Category<T, T> itemBlockType(Class<? extends Block> type, Category<T, T>... subCategories) {
        return itemPredicate(itemBlock -> type.isInstance(itemBlock.getBlock()), subCategories);
    }



    @SafeVarargs
    @SuppressWarnings("unchecked")
    // Item to enum
    static <T extends Item, ENUM extends Enum<ENUM>> Category<T, T> itemEnums(Class<ENUM> enumClass, Function<T, ENUM> toEnum, Category<T, T>... subCategories) {
        final Category<T, T>[] categories =
            Stream.of(enumClass.getEnumConstants())
                //.peek(enom -> System.out.println("Enum: " + enom))
                .map(enom -> Category.<T, T>itemPredicate(item -> toEnum.apply(item) == enom))
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