package adris.altoclef.util;

import adris.altoclef.Debug;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public enum MiningRequirement implements Comparable<MiningRequirement> {
    HAND(Items.AIR.getDefaultStack()), WOOD(Items.WOODEN_PICKAXE.getDefaultStack()), STONE(Items.STONE_PICKAXE.getDefaultStack()), IRON(Items.IRON_PICKAXE.getDefaultStack()), DIAMOND(Items.DIAMOND_PICKAXE.getDefaultStack()), NETHERITE(Items.NETHERITE_PICKAXE.getDefaultStack());

    private final ItemStack _minPickaxe;

    MiningRequirement(ItemStack minPickaxe) {
        _minPickaxe = minPickaxe;
    }

    public static MiningRequirement getMinimumRequirementForBlock(Block block) {
        if (block.getDefaultState().isToolRequired()) {
            for (MiningRequirement req : MiningRequirement.values()) {
                if (req == MiningRequirement.HAND) continue;
                ItemStack pick = req.getMinimumPickaxe();
                if (pick.isSuitableFor(block.getDefaultState())) {
                    return req;
                }
            }
            Debug.logWarning("Failed to find ANY effective tool against: " + block + ". I assume netherite is not required anywhere, so something else probably went wrong.");
            return MiningRequirement.DIAMOND;
        }
        return MiningRequirement.HAND;
    }

    public ItemStack getMinimumPickaxe() {
        return _minPickaxe;
    }

}
