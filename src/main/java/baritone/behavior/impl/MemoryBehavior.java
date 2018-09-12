package baritone.behavior.impl;

import baritone.api.event.events.PacketEvent;
import baritone.api.event.events.PlayerUpdateEvent;
import baritone.api.event.events.type.EventState;
import baritone.api.behavior.Behavior;
import baritone.utils.Helper;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketCloseWindow;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.server.SPacketCloseWindow;
import net.minecraft.network.play.server.SPacketOpenWindow;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityLockable;
import net.minecraft.util.math.BlockPos;

import java.util.*;

/**
 * @author Brady
 * @since 8/6/2018 9:47 PM
 */
public class MemoryBehavior extends Behavior implements Helper {

    public static MemoryBehavior INSTANCE = new MemoryBehavior();

    private MemoryBehavior() {}

    /**
     * Possible future inventories that we will be able to remember
     */
    private final List<FutureInventory> futureInventories = new ArrayList<>();

    /**
     * The current remembered inventories
     */
    private final Map<BlockPos, RememberedInventory> rememberedInventories = new HashMap<>();

    @Override
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (event.getState() == EventState.PRE) {
            updateInventory();
        }
    }

    @Override
    public void onSendPacket(PacketEvent event) {
        Packet p = event.getPacket();

        switch (event.getState()) {
            case PRE: {
                if (p instanceof CPacketPlayerTryUseItemOnBlock) {
                    CPacketPlayerTryUseItemOnBlock packet = event.cast();

                    TileEntity tileEntity = world().getTileEntity(packet.getPos());

                    // Ensure the TileEntity is a container of some sort
                    if (tileEntity instanceof TileEntityLockable) {

                        TileEntityLockable lockable = (TileEntityLockable) tileEntity;
                        int size = lockable.getSizeInventory();

                        this.futureInventories.add(new FutureInventory(System.nanoTime() / 1000000L, size, lockable.getGuiID(), tileEntity.getPos()));
                    }
                }

                if (p instanceof CPacketCloseWindow) {
                    updateInventory();
                }
                break;
            }
        }
    }

    @Override
    public void onReceivePacket(PacketEvent event) {
        Packet p = event.getPacket();

        switch (event.getState()) {
            case PRE: {
                if (p instanceof SPacketOpenWindow) {
                    SPacketOpenWindow packet = event.cast();

                    // Remove any entries that were created over a second ago, this should make up for INSANE latency
                    this.futureInventories.removeIf(i -> System.nanoTime() / 1000000L - i.time > 1000);

                    this.futureInventories.stream()
                            .filter(i -> i.type.equals(packet.getGuiId()) && i.slots == packet.getSlotCount())
                            .findFirst().ifPresent(matched -> {
                        // Remove the future inventory
                        this.futureInventories.remove(matched);

                        // Setup the remembered inventory
                        RememberedInventory inventory = this.rememberedInventories.computeIfAbsent(matched.pos, pos -> new RememberedInventory());
                        inventory.windowId = packet.getWindowId();
                        inventory.size = packet.getSlotCount();
                    });
                }

                if (p instanceof SPacketCloseWindow) {
                    updateInventory();
                }
                break;
            }
        }
    }

    private Optional<RememberedInventory> getInventoryFromWindow(int windowId) {
        return this.rememberedInventories.values().stream().filter(i -> i.windowId == windowId).findFirst();
    }

    private void updateInventory() {
        getInventoryFromWindow(player().openContainer.windowId).ifPresent(inventory -> {
            inventory.items.clear();
            inventory.items.addAll(player().openContainer.getInventory().subList(0, inventory.size));
        });
    }

    public final RememberedInventory getInventoryByPos(BlockPos pos) {
        return this.rememberedInventories.get(pos);
    }

    /**
     * An inventory that we are not yet fully aware of, but are expecting to exist at some point in the future.
     */
    private static final class FutureInventory {

        /**
         * The time that we initially expected the inventory to be provided, in milliseconds
         */
        private final long time;

        /**
         * The amount of slots in the inventory
         */
        private final int slots;

        /**
         * The type of inventory
         */
        private final String type;

        /**
         * The position of the inventory container
         */
        private final BlockPos pos;

        private FutureInventory(long time, int slots, String type, BlockPos pos) {
            this.time = time;
            this.slots = slots;
            this.type = type;
            this.pos = pos;
        }
    }

    /**
     * An inventory that we are aware of.
     * <p>
     * Associated with a {@link BlockPos} in {@link MemoryBehavior#rememberedInventories}.
     */
    public static class RememberedInventory {

        /**
         * The list of items in the inventory
         */
        private final List<ItemStack> items;

        /**
         * The last known window ID of the inventory
         */
        private int windowId;

        /**
         * The size of the inventory
         */
        private int size;

        private RememberedInventory() {
            this.items = new ArrayList<>();
        }

        /**
         * @return The list of items in the inventory
         */
        public final List<ItemStack> getItems() {
            return this.items;
        }
    }
}
