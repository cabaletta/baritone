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
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.datatypes.BlockById;
import baritone.api.command.datatypes.ForBlockOptionalMeta;
import baritone.api.command.exception.CommandException;
import baritone.api.command.exception.CommandNotEnoughArgumentsException;
import baritone.api.utils.BlockOptionalMeta;
import baritone.api.utils.BlockOptionalMetaLookup;
import baritone.cache.WorldScanner;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.stream.Stream;

public class MineCommand extends Command {

    public MineCommand(IBaritone baritone) {
        super(baritone, "mine");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        BlockPos startPos = ctx.playerFeet();
        if ("home".equals(args.peekString())) {
            args.getString();
            IWaypoint waypoint = baritone.getWorldProvider().getCurrentWorld().getWaypoints().getMostRecentByTag(IWaypoint.Tag.HOME);
            if (waypoint != null) {
                startPos = waypoint.getLocation();
            } else {
                logDirect("No home waypoint found. Use player position instead");
            }
        }
        int radius = args.getAsOrDefault(Integer.class, 0);
        args.requireMin(1);
        List<BlockOptionalMeta> boms = new ArrayList<>();
        Map<BlockOptionalMeta, Integer> quantity = new HashMap<>();
        while (args.hasAny()) {
            BlockOptionalMeta bom = args.getDatatypeFor(ForBlockOptionalMeta.INSTANCE);
            // check if there is at least 1 argument left
            try {
                args.peek();
                quantity.put(bom, args.getAsOrDefault(Integer.class, 0));
            } catch (CommandNotEnoughArgumentsException e) {
                quantity.put(bom, 0);
            }

            boms.add(bom);
        }
        WorldScanner.INSTANCE.repack(ctx);
        logDirect(String.format("Mining %s", boms.toString()));
        baritone.getMineProcess().mine(startPos, radius, quantity, new BlockOptionalMetaLookup(boms.toArray(new BlockOptionalMeta[0])));
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return args.tabCompleteDatatype(BlockById.INSTANCE);
    }

    @Override
    public String getShortDesc() {
        return "Mine some blocks";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "The mine command allows you to tell Baritone to search for and mine individual blocks.",
                "",
                "The specified blocks can be ores (which are commonly cached), or any other block.",
                "",
                "Also see the legitMine settings (see #set l legitMine).",
                "",
                "Usage:",
                "> mine diamond_ore - Mines all diamonds it can find.",
                "> mine redstone_ore lit_redstone_ore - Mines redstone ore.",
                "> mine log:0 - Mines only oak logs.",
                "> mine iron_ore 64 coal_ore 128 - Mines 64 iron ore blocks and 128 coal/coal ore blocks "
        );
    }
}
