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

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.datatypes.ForBlockOptionalMeta;
import baritone.api.command.exception.CommandException;
import baritone.api.utils.BlockOptionalMeta;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class MineCommand extends Command {

    public MineCommand(IBaritone baritone) {
        super(baritone, "mine");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        int quantity = args.getAsOrDefault(Integer.class, 0);
        args.requireMin(1);

        boolean isRegex = false;
        if (args.hasAny() && args.peekString().equals("-r")) {
            args.getString();
            isRegex = true;
            args.requireMin(1);
        }

        List<BlockOptionalMeta> boms = new ArrayList<>();

        if (isRegex) {
            String regex = args.getString();
            Pattern pattern = Pattern.compile(regex);

            List<Block> matchingBlocks = BuiltInRegistries.BLOCK.keySet().stream()
                    .filter(key -> pattern.matcher(key.toString()).matches())
                    .map(BuiltInRegistries.BLOCK::get)
                    .toList();

            if (matchingBlocks.isEmpty()) {
                logDirect(String.format("No blocks matching the following regular expression were found: %s", regex));
                return;
            }

            for (Block block : matchingBlocks) {
                boms.add(new BlockOptionalMeta(block));
            }

            logDirect(String.format("Found %d blocks matching the regular expression: %s", matchingBlocks.size(), regex));
        } else {
            while (args.hasAny()) {
                boms.add(args.getDatatypeFor(ForBlockOptionalMeta.INSTANCE));
            }
        }

        BaritoneAPI.getProvider().getWorldScanner().repack(ctx);
        logDirect(String.format("Mining %s", boms.toString()));
        baritone.getMineProcess().mine(quantity, boms.toArray(new BlockOptionalMeta[0]));
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        args.getAsOrDefault(Integer.class, 0);

        if (args.hasExactlyOne() && !args.peekString().startsWith("-")) {
            return Stream.concat(
                    Stream.of("-r"),
                    args.tabCompleteDatatype(ForBlockOptionalMeta.INSTANCE)
            );
        }

        if (args.hasExactlyOne() && args.peekString().equals("-r")) {
            args.getString();
            return Stream.of(".*ore.*", ".*log.*", ".*stone.*");
        }

        while (args.has(2)) {
            args.getDatatypeFor(ForBlockOptionalMeta.INSTANCE);
        }
        return args.tabCompleteDatatype(ForBlockOptionalMeta.INSTANCE);
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
                "The specified blocks can be ores, or any other block.",
                "",
                "Also see the legitMine settings (see #set l legitMine).",
                "",
                "Usage:",
                "> mine diamond_ore - Mines all diamonds it can find.",
                "> mine -r .*ore.* - Mines all blocks that match the regular expression '.*ore.*'."
        );
    }
}