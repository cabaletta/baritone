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

package baritone.command.defaults;

import baritone.api.IBaritone;
import baritone.api.cache.IWaypoint;
import baritone.api.cache.Waypoint;
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.exception.CommandException;
import baritone.api.utils.BetterBlockPos;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class SetChestCommand extends Command {

    public SetChestCommand(IBaritone baritone) { super(baritone, "setchest"); }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(0);
        Optional<BlockPos> blockPos = ctx.getSelectedBlock();

        int x = blockPos.get().getX();
        int y = blockPos.get().getY();
        int z = blockPos.get().getZ();

        if (blockPos.isPresent()) {
            Block block = ctx.world().getBlockState(blockPos.get()).getBlock();
            if (block instanceof BlockContainer) {
                baritone.getWorldProvider().getCurrentWorld().getWaypoints().addWaypoint(new Waypoint("", IWaypoint.Tag.CHEST, new BetterBlockPos(blockPos.get())));
                logDirect("Chest selected at " + x + " " + y + " " + z);
            } else {
                logDirect("Block is not a Chest");
            }
        } else {
            logDirect("Please look at a chest");
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Sets chest for mining and farming";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Sets the chest you are currently looking at, as chest to dropoff minned and farmed items ",
                "",
                "Usage:",
                "> setchest - makes a GoalXZ distance blocks in front of you"
        );
    }
}
