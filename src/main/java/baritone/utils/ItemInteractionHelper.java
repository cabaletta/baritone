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

package baritone.utils;

import it.unimi.dsi.fastutil.objects.Reference2BooleanMap;
import it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;

/**
 * @author Brady
 */
public final class ItemInteractionHelper {

    private ItemInteractionHelper() {}

    private static final Reference2BooleanMap<Class<? extends Item>> CACHE = new Reference2BooleanOpenHashMap<>();

    public static boolean couldInteract(final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        return CACHE.computeIfAbsent(stack.getItem().getClass(), itemClass -> {
            try {
                final Method onItemUse        = itemClass.getMethod(Helper1.name, Helper1.parameters);
                final Method onItemRightClick = itemClass.getMethod(Helper2.name, Helper2.parameters);

                // If the declaring class isn't Item, then the method is overridden
                return onItemUse.getDeclaringClass() != Item.class
                        || onItemRightClick.getDeclaringClass() != Item.class;
            } catch (NoSuchMethodException ignored) {
                // this shouldn't happen
                return true;
            }
        });
    }

    private static final class Helper1 extends Item {

        public static final String name;
        public static final Class<?>[] parameters;
        static {
            final Method method = Helper1.class.getDeclaredMethods()[0];
            name = method.getName();
            parameters = method.getParameterTypes();
        }

        @Nonnull
        @Override
        public EnumActionResult onItemUse(@Nonnull EntityPlayer player, @Nonnull World worldIn,
                                          @Nonnull BlockPos pos, @Nonnull EnumHand hand,
                                          @Nonnull EnumFacing facing, float hitX, float hitY, float hitZ) {
            return super.onItemUse(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
        }
    }

    private static final class Helper2 extends Item {

        public static final String name;
        public static final Class<?>[] parameters;
        static {
            final Method method = Helper2.class.getDeclaredMethods()[0];
            name = method.getName();
            parameters = method.getParameterTypes();
        }

        @Nonnull
        @Override
        public ActionResult<ItemStack> onItemRightClick(@Nonnull World worldIn, @Nonnull EntityPlayer playerIn,
                                                        @Nonnull EnumHand handIn) {
            return super.onItemRightClick(worldIn, playerIn, handIn);
        }
    }
}
