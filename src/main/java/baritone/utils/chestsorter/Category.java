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

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

// items only
public interface Category<SUPER extends Item, T extends SUPER> {
    boolean isInCategory(ItemStack stack, SUPER item);
    List<Category<T, ?>> getSubcategories();



    //
    // ItemStack
    //
    @SafeVarargs
    static <SUPER_ARG extends Item, T_ARG extends SUPER_ARG> Category<SUPER_ARG, T_ARG> create(BiPredicate<ItemStack, SUPER_ARG> biPredicate, Category<T_ARG, ?>... subCategories) {
        return new BasicCategory<>(biPredicate, subCategories);
    }

    //
    // ITEM CATEGORIZATION
    //
    @SafeVarargs
    static <SUPER_ARG extends Item, T_ARG extends SUPER_ARG> Category<SUPER_ARG, T_ARG> forItem(Predicate<SUPER_ARG> predicate, Category<T_ARG, ?>... subCategories) {
        return new BasicCategory<>((stack, item) -> predicate.test(item), subCategories);
    }

    @SafeVarargs
    static <SUPER_ARG extends Item, T_ARG extends SUPER_ARG> Category<SUPER_ARG, T_ARG> itemType(Class<T_ARG> type, Category<T_ARG, ?>... subCategories) {
        return forItem(type::isInstance, subCategories);
    }

    @SafeVarargs
    static <SUPER_ARG extends Item, T_ARG extends SUPER_ARG> Category<SUPER_ARG, T_ARG> itemEquals(Item item, Category<T_ARG, ?>... subCategories) {
        return forItem(item::equals, subCategories);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    // Item to enum (unlikely this will be used)
    static <SUPER_ARG extends Item, T_ARG extends SUPER_ARG, ENUM extends Enum<ENUM>> Category<SUPER_ARG, T_ARG> itemEnums(Class<ENUM> enumClass, Function<SUPER_ARG, ENUM> toEnum, Category<T_ARG, ?>... subCategories) {
        final Category<T_ARG, T_ARG>[] categories =
            Stream.of(enumClass.getEnumConstants())
                .map(enom -> Category.<SUPER_ARG, T_ARG>forItem(item -> toEnum.apply(item) == enom))
                .toArray(Category[]::new);

        return forItem(obj -> true,
                categories
            );
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    // ItemStack and item to enum
    static <SUPER_ARG extends Item, T_ARG extends SUPER_ARG, ENUM extends Enum<ENUM>> Category<SUPER_ARG, T_ARG> enumCategories(Class<ENUM> enumClass, BiFunction<ItemStack, SUPER_ARG, ENUM> toEnum, Category<T_ARG, ?>... subCategories) {
        final Category<T_ARG, T_ARG>[] categories =
            Stream.of(enumClass.getEnumConstants())
                .map(enom -> Category.<SUPER_ARG, T_ARG>create((stack, item) -> toEnum.apply(stack, item) == enom))
                .toArray(Category[]::new);

        return create((a, b) -> true,
            categories
        );
    }
}