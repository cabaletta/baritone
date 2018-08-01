/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.inventory;

import baritone.movement.MovementManager;
import static baritone.Baritone.whatAreYouLookingAt;
import baritone.util.ManagerTick;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.FoodStats;

/**
 *
 * @author leijurv
 */
public class FoodManager extends ManagerTick {
    @Override
    protected boolean onTick0() {
        EntityPlayerSP p = Minecraft.getMinecraft().player;
        FoodStats fs = p.getFoodStats();
        if (!fs.needFood()) {
            return false;
        }
        int foodNeeded = 20 - fs.getFoodLevel();
        boolean anything = foodNeeded >= 3 && Minecraft.getMinecraft().player.getHealth() < 20;//if this is true, we'll just eat anything to get our health up
        ItemStack[] inv = p.inventory.mainInventory;
        byte slotForFood = -1;
        int worst = 10000;
        for (byte i = 0; i < 9; i++) {
            ItemStack item = inv[i];
            if (inv[i] == null) {
                continue;
            }
            if (item.getItem() instanceof ItemFood && !item.getItem().getUnlocalizedName(item).equals("item.spiderEye")) {
                int healing = ((ItemFood) (item.getItem())).getHealAmount(item);
                if (healing <= foodNeeded) {//whatever heals the least. wait that doesn't make sense. idk
                    slotForFood = i;
                }
                if (anything && healing > foodNeeded && healing < worst) {//whatever wastes the least
                    slotForFood = i;
                }
            }
        }
        if (slotForFood != -1) {
            MovementManager.clearMovement();
            p.inventory.currentItem = slotForFood;
            MovementManager.sneak = true;
            if (whatAreYouLookingAt() == null) {
                MovementManager.isRightClick = true;
            } else {
                if (p.isSneaking()) {//if we are looking at a block, then sneak because you dont know what right click will do
                    MovementManager.isRightClick = true;
                }
            }
            return true;
        }
        return false;
    }
    @Override
    protected void onCancel() {
    }
    @Override
    protected void onStart() {
    }
    @Override
    protected boolean onEnabled(boolean enabled) {
        return true;
    }
}
