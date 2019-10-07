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
import baritone.api.utils.BetterBlockPos;
import baritone.cache.ContainerMemory;
import baritone.utils.BlockStateInterface;
import net.minecraft.block.BedBlock;
import net.minecraft.item.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * doesn't work for horse inventories :^)
 *
 * @author Brady
 * @since 8/6/2018
 */
public final class MemoryBehavior extends Behavior {

    public MemoryBehavior(Baritone baritone) {
        super(baritone);
    }

    @Override
    public void onBlockInteract(BlockInteractEvent event) {
        if (event.getType() == BlockInteractEvent.Type.USE && BlockStateInterface.getBlock(ctx, event.getPos()) instanceof BedBlock) {
            baritone.getWorldProvider().getCurrentWorld().getWaypoints().addWaypoint(new Waypoint("bed", Waypoint.Tag.BED, BetterBlockPos.from(event.getPos())));
        }
    }

    @Override
    public void onPlayerDeath() {
        baritone.getWorldProvider().getCurrentWorld().getWaypoints().addWaypoint(new Waypoint("death", Waypoint.Tag.DEATH, ctx.playerFeet()));
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
