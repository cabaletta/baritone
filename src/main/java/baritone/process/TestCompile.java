package baritone.process;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.NonNullList;
public class TestCompile {
    public static void test(LocalPlayer player) {
        NonNullList<ItemStack> items = player.getInventory().items;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                int count = stack.getCount();
            }
        }
    }
}
