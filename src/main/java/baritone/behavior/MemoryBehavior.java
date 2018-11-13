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

package baritone.behavior;

import baritone.Baritone;
import baritone.api.behavior.IMemoryBehavior;
import baritone.api.behavior.memory.IRememberedInventory;
import baritone.api.cache.IWorldData;
import baritone.api.event.events.BlockInteractEvent;
import baritone.api.event.events.PacketEvent;
import baritone.api.event.events.PlayerUpdateEvent;
import baritone.api.event.events.type.EventState;
import baritone.cache.Waypoint;
import baritone.utils.BlockStateInterface;
import net.minecraft.block.BlockBed;
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
public final class MemoryBehavior extends Behavior implements IMemoryBehavior {

    private final Map<IWorldData, WorldDataContainer> worldDataContainers = new HashMap<>();

    public MemoryBehavior(Baritone baritone) {
        super(baritone);
    }

    @Override
    public synchronized void onPlayerUpdate(PlayerUpdateEvent event) {
        if (event.getState() == EventState.PRE) {
            updateInventory();
        }
    }

    @Override
    public synchronized void onSendPacket(PacketEvent event) {
        Packet p = event.getPacket();

        if (event.getState() == EventState.PRE) {
            if (p instanceof CPacketPlayerTryUseItemOnBlock) {
                CPacketPlayerTryUseItemOnBlock packet = event.cast();

                TileEntity tileEntity = ctx.world().getTileEntity(packet.getPos());

                // Ensure the TileEntity is a container of some sort
                if (tileEntity instanceof TileEntityLockable) {

                    TileEntityLockable lockable = (TileEntityLockable) tileEntity;
                    int size = lockable.getSizeInventory();

                    this.getCurrentContainer().futureInventories.add(new FutureInventory(System.nanoTime() / 1000000L, size, lockable.getGuiID(), tileEntity.getPos()));
                }
            }

            if (p instanceof CPacketCloseWindow) {
                updateInventory();
            }
        }
    }

    @Override
    public synchronized void onReceivePacket(PacketEvent event) {
        Packet p = event.getPacket();

        if (event.getState() == EventState.PRE) {
            if (p instanceof SPacketOpenWindow) {
                SPacketOpenWindow packet = event.cast();

                WorldDataContainer container = this.getCurrentContainer();

                // Remove any entries that were created over a second ago, this should make up for INSANE latency
                container.futureInventories.removeIf(i -> System.nanoTime() / 1000000L - i.time > 1000);

                container.futureInventories.stream()
                        .filter(i -> i.type.equals(packet.getGuiId()) && i.slots == packet.getSlotCount())
                        .findFirst().ifPresent(matched -> {
                    // Remove the future inventory
                    container.futureInventories.remove(matched);

                    // Setup the remembered inventory
                    RememberedInventory inventory = container.rememberedInventories.computeIfAbsent(matched.pos, pos -> new RememberedInventory());
                    inventory.windowId = packet.getWindowId();
                    inventory.size = packet.getSlotCount();
                });
            }

            if (p instanceof SPacketCloseWindow) {
                updateInventory();
            }
        }
    }

    @Override
    public void onBlockInteract(BlockInteractEvent event) {
        if (event.getType() == BlockInteractEvent.Type.USE && BlockStateInterface.getBlock(ctx, event.getPos()) instanceof BlockBed) {
            baritone.getWorldProvider().getCurrentWorld().getWaypoints().addWaypoint(new Waypoint("bed", Waypoint.Tag.BED, event.getPos()));
        }
    }

    @Override
    public void onPlayerDeath() {
        baritone.getWorldProvider().getCurrentWorld().getWaypoints().addWaypoint(new Waypoint("death", Waypoint.Tag.DEATH, ctx.playerFeet()));
    }

    private Optional<RememberedInventory> getInventoryFromWindow(int windowId) {
        return this.getCurrentContainer().rememberedInventories.values().stream().filter(i -> i.windowId == windowId).findFirst();
    }

    private void updateInventory() {
        getInventoryFromWindow(ctx.player().openContainer.windowId).ifPresent(inventory -> {
            inventory.items.clear();
            inventory.items.addAll(ctx.player().openContainer.getInventory().subList(0, inventory.size));
        });
    }

    private WorldDataContainer getCurrentContainer() {
        return this.worldDataContainers.computeIfAbsent(baritone.getWorldProvider().getCurrentWorld(), data -> new WorldDataContainer());
    }

    @Override
    public final synchronized RememberedInventory getInventoryByPos(BlockPos pos) {
        return this.getCurrentContainer().rememberedInventories.get(pos);
    }

    @Override
    public final synchronized Map<BlockPos, IRememberedInventory> getRememberedInventories() {
        // make a copy since this map is modified from the packet thread
        return new HashMap<>(this.getCurrentContainer().rememberedInventories);
    }

    private static final class WorldDataContainer {

        /**
         * Possible future inventories that we will be able to remember
         */
        private final List<FutureInventory> futureInventories = new ArrayList<>();

        /**
         * The current remembered inventories
         */
        private final Map<BlockPos, RememberedInventory> rememberedInventories = new HashMap<>();
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
     * Associated with a {@link BlockPos} in {@link WorldDataContainer#rememberedInventories}.
     */
    public static class RememberedInventory implements IRememberedInventory {

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

        @Override
        public final List<ItemStack> getContents() {
            return Collections.unmodifiableList(this.items);
        }

        @Override
        public final int getSize() {
            return this.size;
        }
    }
}
