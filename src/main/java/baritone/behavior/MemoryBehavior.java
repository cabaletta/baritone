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
import baritone.api.cache.Waypoint;
import baritone.api.event.events.BlockInteractEvent;
import baritone.api.event.events.PacketEvent;
import baritone.api.event.events.PlayerUpdateEvent;
import baritone.api.event.events.TickEvent;
import baritone.api.event.events.type.EventState;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.Helper;
import baritone.cache.ContainerMemory;
import baritone.utils.BlockStateInterface;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketCloseWindow;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.server.SPacketCloseWindow;
import net.minecraft.network.play.server.SPacketOpenWindow;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityLockable;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static baritone.api.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

/**
 * doesn't work for horse inventories :^)
 *
 * @author Brady
 * @since 8/6/2018
 */
public final class MemoryBehavior extends Behavior {

    private final List<FutureInventory> futureInventories = new ArrayList<>(); // this is per-bot

    private Integer enderChestWindowId; // nae nae

    public MemoryBehavior(Baritone baritone) {
        super(baritone);
    }

    @Override
    public synchronized void onTick(TickEvent event) {
        if (!Baritone.settings().containerMemory.value) {
            return;
        }
        if (event.getType() == TickEvent.Type.OUT) {
            enderChestWindowId = null;
            futureInventories.clear();
        }
    }

    @Override
    public synchronized void onPlayerUpdate(PlayerUpdateEvent event) {
        if (event.getState() == EventState.PRE) {
            updateInventory();
        }
    }

    @Override
    public synchronized void onSendPacket(PacketEvent event) {
        if (!Baritone.settings().containerMemory.value) {
            return;
        }
        Packet p = event.getPacket();

        if (event.getState() == EventState.PRE) {
            if (p instanceof CPacketPlayerTryUseItemOnBlock) {
                CPacketPlayerTryUseItemOnBlock packet = event.cast();

                TileEntity tileEntity = ctx.world().getTileEntity(packet.getPos());
                // if tileEntity is an ender chest, we don't need to do anything. ender chests are treated the same regardless of what coordinate right clicked

                // Ensure the TileEntity is a container of some sort
                if (tileEntity instanceof TileEntityLockable) {

                    TileEntityLockable lockable = (TileEntityLockable) tileEntity;
                    int size = lockable.getSizeInventory();
                    BetterBlockPos position = BetterBlockPos.from(tileEntity.getPos());
                    BetterBlockPos adj = BetterBlockPos.from(neighboringConnectedBlock(position));
                    System.out.println(position + " " + adj);
                    if (adj != null) {
                        size *= 2; // double chest or double trapped chest
                        if (adj.getX() < position.getX() || adj.getZ() < position.getZ()) {
                            position = adj; // standardize on the lower coordinate, regardless of which side of the large chest we right clicked
                        }
                    }

                    this.futureInventories.add(new FutureInventory(System.nanoTime() / 1000000L, size, lockable.getGuiID(), position));
                }
            }

            if (p instanceof CPacketCloseWindow) {
                getCurrent().save();
            }
        }
    }

    @Override
    public synchronized void onReceivePacket(PacketEvent event) {
        if (!Baritone.settings().containerMemory.value) {
            return;
        }
        Packet p = event.getPacket();

        if (event.getState() == EventState.PRE) {
            if (p instanceof SPacketOpenWindow) {
                SPacketOpenWindow packet = event.cast();
                // Remove any entries that were created over a second ago, this should make up for INSANE latency
                futureInventories.removeIf(i -> System.nanoTime() / 1000000L - i.time > 1000);

                System.out.println("Received packet " + packet.getGuiId() + " " + packet.getEntityId() + " " + packet.getSlotCount() + " " + packet.getWindowId());
                System.out.println(packet.getWindowTitle());
                if (packet.getWindowTitle() instanceof TextComponentTranslation && ((TextComponentTranslation) packet.getWindowTitle()).getKey().equals("container.enderchest")) {
                    // title is not customized (i.e. this isn't just a renamed shulker)
                    enderChestWindowId = packet.getWindowId();
                    return;
                }
                futureInventories.stream()
                        .filter(i -> i.type.equals(packet.getGuiId()) && i.slots == packet.getSlotCount())
                        .findFirst().ifPresent(matched -> {
                    // Remove the future inventory
                    futureInventories.remove(matched);

                    // Setup the remembered inventory
                    getCurrentContainer().setup(matched.pos, packet.getWindowId(), packet.getSlotCount());
                });
            }

            if (p instanceof SPacketCloseWindow) {
                getCurrent().save();
            }
        }
    }

    @Override
    public void onBlockInteract(BlockInteractEvent event) {
        if (event.getType() == BlockInteractEvent.Type.USE && BlockStateInterface.getBlock(ctx, event.getPos()) instanceof BlockBed) {
            baritone.getWorldProvider().getCurrentWorld().getWaypoints().addWaypoint(new Waypoint("bed", Waypoint.Tag.BED, BetterBlockPos.from(event.getPos())));
        }
    }

    @Override
    public void onPlayerDeath() {
        Waypoint deathWaypoint = new Waypoint("death", Waypoint.Tag.DEATH, ctx.playerFeet());
        baritone.getWorldProvider().getCurrentWorld().getWaypoints().addWaypoint(deathWaypoint);
        ITextComponent component = new TextComponentString("Death position saved.");
        component.getStyle()
                .setColor(TextFormatting.WHITE)
                .setHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new TextComponentString("Click to goto death")
                ))
                .setClickEvent(new ClickEvent(
                        ClickEvent.Action.RUN_COMMAND,
                        String.format(
                                "%s%s goto %s @ %d",
                                FORCE_COMMAND_PREFIX,
                                "wp",
                                deathWaypoint.getTag().getName(),
                                deathWaypoint.getCreationTimestamp()
                        )
                ));
        Helper.HELPER.logDirect(component);
    }


    private void updateInventory() {
        if (!Baritone.settings().containerMemory.value) {
            return;
        }
        int windowId = ctx.player().openContainer.windowId;
        if (enderChestWindowId != null) {
            if (windowId == enderChestWindowId) {
                getCurrent().contents = ctx.player().openContainer.getInventory().subList(0, 27);
            } else {
                getCurrent().save();
                enderChestWindowId = null;
            }
        }
        if (getCurrentContainer() != null) {
            getCurrentContainer().getInventoryFromWindow(windowId).ifPresent(inventory -> inventory.updateFromOpenWindow(ctx));
        }
    }

    private ContainerMemory getCurrentContainer() {
        if (baritone.getWorldProvider().getCurrentWorld() == null) {
            return null;
        }
        return (ContainerMemory) baritone.getWorldProvider().getCurrentWorld().getContainerMemory();
    }

    private BlockPos neighboringConnectedBlock(BlockPos in) {
        BlockStateInterface bsi = baritone.bsi;
        Block block = bsi.get0(in).getBlock();
        if (block != Blocks.TRAPPED_CHEST && block != Blocks.CHEST) {
            return null; // other things that have contents, but can be placed adjacent without combining
        }
        for (int i = 0; i < 4; i++) {
            BlockPos adj = in.offset(EnumFacing.byHorizontalIndex(i));
            if (bsi.get0(adj).getBlock() == block) {
                return adj;
            }
        }
        return null;
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
            // betterblockpos has censoring
            System.out.println("Future inventory created " + time + " " + slots + " " + type + " " + BetterBlockPos.from(pos));
        }
    }

    public Optional<List<ItemStack>> echest() {
        return Optional.ofNullable(getCurrent().contents).map(Collections::unmodifiableList);
    }

    public EnderChestMemory getCurrent() {
        Path path = baritone.getWorldProvider().getCurrentWorld().directory;
        return EnderChestMemory.getByServerAndPlayer(path.getParent(), ctx.player().getUniqueID());
    }

    public static class EnderChestMemory {

        private static final Map<Path, EnderChestMemory> memory = new HashMap<>();
        private final Path enderChest;
        private List<ItemStack> contents;

        private EnderChestMemory(Path enderChest) {
            this.enderChest = enderChest;
            System.out.println("Echest storing in " + enderChest);
            try {
                this.contents = ContainerMemory.readItemStacks(Files.readAllBytes(enderChest));
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("CANNOT read echest =( =(");
                this.contents = null;
            }
        }

        public synchronized void save() {
            System.out.println("Saving");
            if (contents != null) {
                try {
                    enderChest.getParent().toFile().mkdir();
                    Files.write(enderChest, ContainerMemory.writeItemStacks(contents));
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("CANNOT save echest =( =(");
                }
            }
        }

        private static synchronized EnderChestMemory getByServerAndPlayer(Path serverStorage, UUID player) {
            return memory.computeIfAbsent(serverStorage.resolve("echests").resolve(player.toString()), EnderChestMemory::new);
        }
    }
}
