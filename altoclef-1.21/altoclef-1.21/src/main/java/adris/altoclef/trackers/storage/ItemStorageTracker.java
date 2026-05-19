package adris.altoclef.trackers.storage;

import adris.altoclef.AltoClef;
import adris.altoclef.trackers.Tracker;
import adris.altoclef.trackers.TrackerManager;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Access ALL forms of storage.
 */
public class ItemStorageTracker extends Tracker {

    private final InventorySubTracker _inventory;
    private final ContainerSubTracker _containers;

    public ItemStorageTracker(AltoClef mod, TrackerManager manager, Consumer<ContainerSubTracker> containerTrackerConsumer) {
        super(manager);
        _inventory = new InventorySubTracker(manager);
        _containers = new ContainerSubTracker(manager);
        containerTrackerConsumer.accept(_containers);
    }

    private static Slot[] getCurrentConversionSlots() {
        // TODO: Anvil input, anything else...
        if (StorageHelper.isPlayerInventoryOpen()) {
            return PlayerSlot.CRAFT_INPUT_SLOTS;
        } else if (StorageHelper.isBigCraftingOpen()) {
            return CraftingTableSlot.INPUT_SLOTS;
        } else if (StorageHelper.isFurnaceOpen()) {
            return new Slot[]{FurnaceSlot.INPUT_SLOT_FUEL, FurnaceSlot.INPUT_SLOT_MATERIALS};
        } else if (StorageHelper.isSmokerOpen()) {
            return new Slot[]{SmokerSlot.INPUT_SLOT_FUEL, SmokerSlot.INPUT_SLOT_MATERIALS};
        } else if (StorageHelper.isBlastFurnaceOpen()) {
            return new Slot[]{BlastFurnaceSlot.INPUT_SLOT_FUEL, BlastFurnaceSlot.INPUT_SLOT_MATERIALS};
        }
        return new Slot[0];
    }

    /**
     * Gets the number of items in the player's inventory OR if the player is USING IT in a conversion process
     * (ex. crafting table slots/furnace input, stuff the player is use )
     */
    public int getItemCount(Item... items) {
        int inConversionSlots = Arrays.stream(getCurrentConversionSlots()).mapToInt(slot -> {
            ItemStack stack = StorageHelper.getItemStackInSlot(slot);
            if (ArrayUtils.contains(items, stack.getItem())) {
                return stack.getCount();
            }
            return 0;
        }).reduce(0, Integer::sum);
        return _inventory.getItemCount(true, false, items) + inConversionSlots;
    }

    public int getItemCount(ItemTarget... targets) {
        return Arrays.stream(targets).mapToInt(target -> getItemCount(target.getMatches())).reduce(0, Integer::sum);
    }

    /**
     * Gets the number of items visible on the screen in any slot
     */
    public int getItemCountScreen(Item... items) {
        return _inventory.getItemCount(true, true, items);
    }

    /**
     * Gets the number of items STRICTLY in the player's inventory.
     * <p>
     * ONLY USE THIS when getting an item is the END GOAL. This will
     * NOT count items in a crafting/furnace slot!
     */
    public int getItemCountInventoryOnly(Item... items) {
        return _inventory.getItemCount(true, false, items);
    }

    /**
     * Gets the number of items only in the currently open container, NOT the player's inventory.
     */
    public int getItemCountContainer(Item... items) {
        return _inventory.getItemCount(false, true, items);
    }

    /**
     * Gets whether an item is in the player's inventory OR if the player is USING IT in a conversion process
     * (ex. crafting table slots/furnace input, stuff the player is use )
     */
    public boolean hasItem(Item... items) {
        return Arrays.stream(getCurrentConversionSlots()).anyMatch(slot -> {
            ItemStack stack = StorageHelper.getItemStackInSlot(slot);
            return ArrayUtils.contains(items, stack.getItem());
        }) || _inventory.hasItem(true, items);
    }

    public boolean hasItemInOffhand(Item item) {
        ItemStack offhand = StorageHelper.getItemStackInSlot(PlayerSlot.OFFHAND_SLOT);
        return offhand.getItem() == item;
    }

    public boolean hasItemAll(Item... items) {
        return Arrays.stream(items).allMatch(this::hasItem);
    }

    public boolean hasItem(ItemTarget... targets) {
        return Arrays.stream(targets).anyMatch(target -> hasItem(target.getMatches()));
    }

    /**
     * Returns whether an item is visible on the screen in any slot
     */
    public boolean hasItemScreen(Item... items) {
        return _inventory.hasItem(false, items);
    }

    /**
     * Returns whether the player has an item in its inventory ONLY.
     * <p>
     * ONLY USE THIS when getting an item is the END GOAL. This will
     * NOT count items in a crafting/furnace slot!
     */
    public boolean hasItemInventoryOnly(Item... items) {
        return _inventory.hasItem(true, items);
    }

    /**
     * Returns all slots containing any item given.
     */
    public List<Slot> getSlotsWithItemScreen(Item... items) {
        return _inventory.getSlotsWithItems(true, true, items);
    }

    /**
     * Returns all slots NOT in the player inventory containing any item given.
     */
    public List<Slot> getSlotsWithItemContainer(Item... items) {
        return _inventory.getSlotsWithItems(false, true, items);
    }

    /**
     * Returns all slots in our player inventory containing any item given.
     */
    public List<Slot> getSlotsWithItemPlayerInventory(boolean includeCraftArmorOffhand, Item... items) {
        List<Slot> results = _inventory.getSlotsWithItems(true, false, items);
        // Check other slots
        if (includeCraftArmorOffhand) {
            HashSet<Item> toCheck = new HashSet<>(Arrays.asList(items));
            for (Slot otherSlot : StorageHelper.INACCESSIBLE_PLAYER_SLOTS) {
                if (toCheck.contains(StorageHelper.getItemStackInSlot(otherSlot).getItem())) {
                    results.add(otherSlot);
                }
            }
        }
        return results;
    }

    public List<ItemStack> getItemStacksPlayerInventory(boolean includeCursorSlot) {
        return _inventory.getInventoryStacks(includeCursorSlot);
    }

    /**
     * Get all slots in the player's inventory that can fit an item stack.
     *
     * @param stack         The stack to "fit"/place in the inventory.
     * @param acceptPartial If true, is OK with fitting PART of the stack. If false, requires 100% of the stack to fit.
     */
    public List<Slot> getSlotsThatCanFitInPlayerInventory(ItemStack stack, boolean acceptPartial) {
        return _inventory.getSlotsThatCanFit(true, false, stack, acceptPartial);
    }

    public Optional<Slot> getSlotThatCanFitInPlayerInventory(ItemStack stack, boolean acceptPartial) {
        List<Slot> slots = getSlotsThatCanFitInPlayerInventory(stack, acceptPartial);
        if (!slots.isEmpty()) {
            for (Slot slot : slots) {
                return Optional.ofNullable(slot);
            }
        }
        return Optional.empty();
    }

    /**
     * Get all slots in the currently open container that can fit an item stack, EXCLUDING the player inventory.
     *
     * @param stack         The stack to "fit"/place in the inventory.
     * @param acceptPartial If true, is OK with fitting PART of the stack. If false, requires 100% of the stack to fit.
     */
    public List<Slot> getSlotsThatCanFitInOpenContainer(ItemStack stack, boolean acceptPartial) {
        return _inventory.getSlotsThatCanFit(false, true, stack, acceptPartial);
    }

    public Optional<Slot> getSlotThatCanFitInOpenContainer(ItemStack stack, boolean acceptPartial) {
        List<Slot> slots = getSlotsThatCanFitInOpenContainer(stack, acceptPartial);
        if (!slots.isEmpty()) {
            for (Slot slot : slots) {
                return Optional.ofNullable(slot);
            }
        }
        return Optional.empty();
    }

    /**
     * Get all slots that can fit an item stack.
     *
     * @param stack         The stack to "fit"/place in the inventory.
     * @param acceptPartial If true, is OK with fitting PART of the stack. If false, requires 100% of the stack to fit.
     */
    public List<Slot> getSlotsThatCanFitScreen(ItemStack stack, boolean acceptPartial) {
        return _inventory.getSlotsThatCanFit(true, true, stack, acceptPartial);
    }

    public boolean hasEmptyInventorySlot() {
        return _inventory.hasEmptySlot(true);
    }

    public void registerSlotAction() {
        _inventory.setDirty();
    }

    /**
     * Returns whether an item is present in a container. You can filter out containers
     * you don't like.
     */
    public boolean hasItemContainer(Predicate<ContainerCache> accept, Item... items) {
        return _containers.hasItem(accept, items);
    }

    /**
     * Returns whether an item is present in ANY container, no matter how far.
     */
    public boolean hasItemContainer(Item... items) {
        return _containers.hasItem(items);
    }

    public Optional<ContainerCache> getContainerAtPosition(BlockPos pos) {
        return _containers.getContainerAtPosition(pos);
    }

    public boolean isContainerCached(BlockPos pos) {
        return getContainerAtPosition(pos).isPresent();
    }

    public Optional<ContainerCache> getEnderChestStorage() {
        return _containers.getEnderChestStorage();
    }

    public List<ContainerCache> getCachedContainers(Predicate<ContainerCache> accept) {
        return _containers.getCachedContainers(accept);
    }

    public List<ContainerCache> getCachedContainers(ContainerType... types) {
        return _containers.getCachedContainers(types);
    }

    public List<ContainerCache> getCachedContainers() {
        return getCachedContainers(cache -> true);
    }

    public Optional<ContainerCache> getContainerClosestTo(Vec3d pos, Predicate<ContainerCache> accept) {
        return _containers.getClosestTo(pos, accept);
    }

    public Optional<ContainerCache> getContainerClosestTo(Vec3d pos, ContainerType... types) {
        return _containers.getClosestTo(pos, types);
    }

    public Optional<ContainerCache> getContainerClosestTo(Vec3d pos) {
        return getContainerClosestTo(pos, cache -> true);
    }

    public List<ContainerCache> getContainersWithItem(Item... items) {
        return _containers.getContainersWithItem(items);
    }

    public Optional<ContainerCache> getClosestContainerWithItem(Vec3d pos, Item... items) {
        return _containers.getClosestWithItem(pos, items);
    }

    public Optional<BlockPos> getLastBlockPosInteraction() {
        return Optional.ofNullable(_containers.getLastBlockPosInteraction());
    }

    @Override
    protected void updateState() {
        _inventory.updateState();
        _containers.updateState();
    }

    @Override
    protected void reset() {
        _inventory.reset();
        _containers.reset();
    }
}

