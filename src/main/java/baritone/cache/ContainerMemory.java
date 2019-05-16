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

package baritone.cache;

import baritone.Baritone;
import baritone.api.cache.IContainerMemory;
import baritone.api.cache.IRememberedInventory;
import baritone.api.utils.IPlayerContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;

public class ContainerMemory implements IContainerMemory {

    private final Path saveTo;
    /**
     * The current remembered inventories
     */
    private final Map<BlockPos, RememberedInventory> inventories = new HashMap<>();


    public ContainerMemory(Path saveTo) {
        this.saveTo = saveTo;
        try {
            read(Files.readAllBytes(saveTo));
        } catch (NoSuchFileException ignored) {
            inventories.clear();
        } catch (Exception ex) {
            ex.printStackTrace();
            inventories.clear();
        }
    }

    private void read(byte[] bytes) throws IOException {
        PacketBuffer in = new PacketBuffer(Unpooled.wrappedBuffer(bytes));
        int chests = in.readInt();
        for (int i = 0; i < chests; i++) {
            int x = in.readInt();
            int y = in.readInt();
            int z = in.readInt();
            RememberedInventory rem = new RememberedInventory();
            rem.items.addAll(readItemStacks(in));
            rem.size = rem.items.size();
            rem.windowId = -1;
            if (rem.items.isEmpty()) {
                continue; // this only happens if the list has no elements, not if the list has elements that are all empty item stacks
            }
            inventories.put(new BlockPos(x, y, z), rem);
        }
    }

    public synchronized void save() throws IOException {
        if (!Baritone.settings().containerMemory.value) {
            return;
        }
        ByteBuf buf = Unpooled.buffer(0, Integer.MAX_VALUE);
        PacketBuffer out = new PacketBuffer(buf);
        out.writeInt(inventories.size());
        for (Map.Entry<BlockPos, RememberedInventory> entry : inventories.entrySet()) {
            out = new PacketBuffer(out.writeInt(entry.getKey().getX()));
            out = new PacketBuffer(out.writeInt(entry.getKey().getY()));
            out = new PacketBuffer(out.writeInt(entry.getKey().getZ()));
            out = writeItemStacks(entry.getValue().getContents(), out);
        }
        Files.write(saveTo, out.array());
    }

    public synchronized void setup(BlockPos pos, int windowId, int slotCount) {
        RememberedInventory inventory = inventories.computeIfAbsent(pos, x -> new RememberedInventory());
        inventory.windowId = windowId;
        inventory.size = slotCount;
    }

    public synchronized Optional<RememberedInventory> getInventoryFromWindow(int windowId) {
        return inventories.values().stream().filter(i -> i.windowId == windowId).findFirst();
    }

    @Override
    public final synchronized RememberedInventory getInventoryByPos(BlockPos pos) {
        return inventories.get(pos);
    }

    @Override
    public final synchronized Map<BlockPos, IRememberedInventory> getRememberedInventories() {
        // make a copy since this map is modified from the packet thread
        return new HashMap<>(inventories);
    }

    public static List<ItemStack> readItemStacks(byte[] bytes) throws IOException {
        PacketBuffer in = new PacketBuffer(Unpooled.wrappedBuffer(bytes));
        return readItemStacks(in);
    }

    public static List<ItemStack> readItemStacks(PacketBuffer in) throws IOException {
        int count = in.readInt();
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(in.readItemStack());
        }
        return result;
    }

    public static byte[] writeItemStacks(List<ItemStack> write) {
        ByteBuf buf = Unpooled.buffer(0, Integer.MAX_VALUE);
        PacketBuffer out = new PacketBuffer(buf);
        out = writeItemStacks(write, out);
        return out.array();
    }

    public static PacketBuffer writeItemStacks(List<ItemStack> write, PacketBuffer out2) {
        PacketBuffer out = out2; // avoid reassigning an argument LOL
        out = new PacketBuffer(out.writeInt(write.size()));
        for (ItemStack stack : write) {
            out = out.writeItemStack(stack);
        }
        return out;
    }

    /**
     * An inventory that we are aware of.
     * <p>
     * Associated with a {@link BlockPos} in {@link ContainerMemory#inventories}.
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

        public void updateFromOpenWindow(IPlayerContext ctx) {
            items.clear();
            items.addAll(ctx.player().openContainer.getInventory().subList(0, size));
        }
    }
}
