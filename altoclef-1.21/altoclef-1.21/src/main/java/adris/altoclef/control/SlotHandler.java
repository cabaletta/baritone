package adris.altoclef.control;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.CursorSlot;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import adris.altoclef.util.time.TimerGame;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.screen.slot.SlotActionType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;


public class SlotHandler {

    private final AltoClef _mod;

    private final TimerGame _slotActionTimer = new TimerGame(0);
    private boolean _overrideTimerOnce = false;

    public SlotHandler(AltoClef mod) {
        _mod = mod;
    }

    private void forceAllowNextSlotAction() {
        _overrideTimerOnce = true;
    }

    public boolean canDoSlotAction() {
        if (_overrideTimerOnce) {
            _overrideTimerOnce = false;
            return true;
        }
        _slotActionTimer.setInterval(_mod.getModSettings().getContainerItemMoveDelay());
        return _slotActionTimer.elapsed();
    }

    public void registerSlotAction() {
        _mod.getItemStorage().registerSlotAction();
        _slotActionTimer.reset();
    }


    public void clickSlot(Slot slot, int mouseButton, SlotActionType type) {
        if (!canDoSlotAction()) return;

        if (slot.getWindowSlot() == -1) {
            clickSlot(PlayerSlot.UNDEFINED, 0, SlotActionType.PICKUP);
            return;
        }
        // NOT THE CASE! We may have something in the cursor slot to place.
        //if (getItemStackInSlot(slot).isEmpty()) return getItemStackInSlot(slot);

        clickWindowSlot(slot.getWindowSlot(), mouseButton, type);
    }

    private void clickSlotForce(Slot slot, int mouseButton, SlotActionType type) {
        forceAllowNextSlotAction();
        clickSlot(slot, mouseButton, type);
    }

    private void clickWindowSlot(int windowSlot, int mouseButton, SlotActionType type) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        registerSlotAction();
        int syncId = player.currentScreenHandler.syncId;

        try {
            _mod.getController().clickSlot(syncId, windowSlot, mouseButton, type, player);
        } catch (Exception e) {
            Debug.logWarning("Slot Click Error (ignored)");
            e.printStackTrace();
        }
    }

    public void forceEquipItemToOffhand(Item toEquip) {
        if (StorageHelper.getItemStackInSlot(PlayerSlot.OFFHAND_SLOT).getItem() == toEquip) {
            return;
        }
        List<Slot> currentItemSlot = _mod.getItemStorage().getSlotsWithItemPlayerInventory(false,
                toEquip);
        for (Slot CurrentItemSlot : currentItemSlot) {
            if (!Slot.isCursor(CurrentItemSlot)) {
                _mod.getSlotHandler().clickSlot(CurrentItemSlot, 0, SlotActionType.PICKUP);
            } else {
                _mod.getSlotHandler().clickSlot(PlayerSlot.OFFHAND_SLOT, 0, SlotActionType.PICKUP);
            }
        }
    }

    public boolean forceEquipItem(Item toEquip) {

        // Already equipped
        if (StorageHelper.getItemStackInSlot(PlayerSlot.getEquipSlot()).getItem() == toEquip) return true;

        // Always equip to the second slot. First + last is occupied by baritone.
        _mod.getPlayer().getInventory().selectedSlot = 1;

        // If our item is in our cursor, simply move it to the hotbar.
        boolean inCursor = StorageHelper.getItemStackInSlot(CursorSlot.SLOT).getItem() == toEquip;

        List<Slot> itemSlots = _mod.getItemStorage().getSlotsWithItemScreen(toEquip);
        if (itemSlots.size() != 0) {
            for (Slot ItemSlots : itemSlots) {
                int hotbar = 1;
                //_mod.getPlayer().getInventory().swapSlotWithHotbar();
                clickSlotForce(Objects.requireNonNull(ItemSlots), inCursor ? 0 : hotbar, inCursor ? SlotActionType.PICKUP : SlotActionType.SWAP);
                //registerSlotAction();
            }
            return true;
        }
        return false;
    }

    public boolean forceDeequipHitTool() {
        return forceDeequip(stack -> stack.getItem() instanceof ToolItem);
    }

    public void forceDeequipRightClickableItem() {
        forceDeequip(stack -> {
                    Item item = stack.getItem();
                    return item instanceof BucketItem // water,lava,milk,fishes
                            || item instanceof EnderEyeItem
                            || item == Items.BOW
                            || item == Items.CROSSBOW
                            || item == Items.FLINT_AND_STEEL || item == Items.FIRE_CHARGE
                            || item == Items.ENDER_PEARL
                            || item instanceof FireworkRocketItem
                            || item instanceof SpawnEggItem
                            || item == Items.END_CRYSTAL
                            || item == Items.EXPERIENCE_BOTTLE
                            || item instanceof PotionItem // also includes splash/lingering
                            || item == Items.TRIDENT
                            || item == Items.WRITABLE_BOOK
                            || item == Items.WRITTEN_BOOK
                            || item instanceof FishingRodItem
                            || item instanceof OnAStickItem
                            || item == Items.COMPASS
                            || item instanceof EmptyMapItem
                            || item instanceof Equipment
                            || item == Items.LEAD
                            || item == Items.SHIELD;
                }
        );
    }

    /**
     * Tries to de-equip any item that we don't want equipped.
     *
     * @param isBad: Whether an item is bad/shouldn't be equipped
     * @return Whether we successfully de-equipped, or if we didn't have the item equipped at all.
     */
    public boolean forceDeequip(Predicate<ItemStack> isBad) {
        ItemStack equip = StorageHelper.getItemStackInSlot(PlayerSlot.getEquipSlot());
        ItemStack cursor = StorageHelper.getItemStackInSlot(CursorSlot.SLOT);
        if (isBad.test(cursor)) {
            // Throw away cursor slot OR move
            Optional<Slot> fittableSlots = _mod.getItemStorage().getSlotThatCanFitInPlayerInventory(equip, false);
            if (fittableSlots.isEmpty()) {
                // Try to swap items with the first non-bad slot.
                for (Slot slot : Slot.getCurrentScreenSlots()) {
                    if (!isBad.test(StorageHelper.getItemStackInSlot(slot))) {
                        clickSlotForce(slot, 0, SlotActionType.PICKUP);
                        return false;
                    }
                }
                if (ItemHelper.canThrowAwayStack(_mod, cursor)) {
                    clickSlotForce(PlayerSlot.UNDEFINED, 0, SlotActionType.PICKUP);
                    return true;
                }
                // Can't throw :(
                return false;
            } else {
                // Put in the empty/available slot.
                clickSlotForce(fittableSlots.get(), 0, SlotActionType.PICKUP);
                return true;
            }
        } else if (isBad.test(equip)) {
            // Pick up the item
            clickSlotForce(PlayerSlot.getEquipSlot(), 0, SlotActionType.PICKUP);
            return false;
        } else if (equip.isEmpty() && !cursor.isEmpty()) {
            // cursor is good and equip is empty, so finish filling it in.
            clickSlotForce(PlayerSlot.getEquipSlot(), 0, SlotActionType.PICKUP);
            return true;
        }
        // We're already de-equipped
        return true;
    }

    public void forceEquipSlot(Slot slot) {
        Slot target = PlayerSlot.getEquipSlot();
        clickSlotForce(slot, target.getInventorySlot(), SlotActionType.SWAP);
    }

    public boolean forceEquipItem(Item[] matches, boolean unInterruptable) {
        return forceEquipItem(new ItemTarget(matches, 1), unInterruptable);
    }

    public boolean forceEquipItem(ItemTarget toEquip, boolean unInterruptable) {
        if (toEquip == null) return false;

        //If the bot try to eat
        if (_mod.getFoodChain().needsToEat() && !unInterruptable) { //unless we really need to force equip the item
            return false; //don't equip the item for now
        }

        Slot target = PlayerSlot.getEquipSlot();
        // Already equipped
        if (toEquip.matches(StorageHelper.getItemStackInSlot(target).getItem())) return true;

        for (Item item : toEquip.getMatches()) {
            if (_mod.getItemStorage().hasItem(item)) {
                if (forceEquipItem(item)) return true;
            }
        }
        return false;
    }

    // By default, don't force equip if the bot is eating.
    public boolean forceEquipItem(Item... toEquip) {
        return forceEquipItem(toEquip, false);
    }

    public void refreshInventory() {
        if (MinecraftClient.getInstance().player == null)
            return;
        for (int i = 0; i < MinecraftClient.getInstance().player.getInventory().main.size(); ++i) {
            Slot slot = Slot.getFromCurrentScreenInventory(i);
            clickSlotForce(slot, 0, SlotActionType.PICKUP);
            clickSlotForce(slot, 0, SlotActionType.PICKUP);
        }
    }
}
