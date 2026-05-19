package adris.altoclef.util;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

@Deprecated
public enum ArmorRequirement {
    NONE,
    LEATHER(Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS),
    IRON(Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS),
    DIAMOND(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS),
    NETHERITE(Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS);

    private final Item[] _armors;

    ArmorRequirement(Item... armors) {
        _armors = armors;
    }

    public Item[] getArmors() {
        return _armors;
    }
}
