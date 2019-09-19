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

package baritone.utils.command.defaults;

import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.command.Command;
import baritone.api.utils.command.datatypes.RelativeBlockPos;
import baritone.api.utils.command.datatypes.RelativeFile;
import baritone.api.utils.command.exception.CommandInvalidStateException;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public class BuildCommand extends Command {

    private static final File schematicsDir = new File(Minecraft.getMinecraft().gameDir, "schematics");

    public BuildCommand(IBaritone baritone) {
        super(baritone, "build");
    }

    @Override
    protected void executed(String label, ArgConsumer args, Settings settings) {
        File file = args.getDatatypePost(RelativeFile.class, schematicsDir).getAbsoluteFile();
        if (!file.getName().toLowerCase(Locale.US).endsWith(".schematic")) {
            file = new File(file.getAbsolutePath() + ".schematic");
        }
        BetterBlockPos origin = ctx.playerFeet();
        BetterBlockPos buildOrigin;
        if (args.has()) {
            args.requireMax(3);
            buildOrigin = args.getDatatype(RelativeBlockPos.class).apply(origin);
        } else {
            args.requireMax(0);
            buildOrigin = origin;
        }
        boolean success = baritone.getBuilderProcess().build(file.getName(), file, buildOrigin);
        if (!success) {
            throw new CommandInvalidStateException("Couldn't load the schematic");
        }
        logDirect(String.format("Successfully loaded schematic for building\nOrigin: %s", buildOrigin));
    }

    @Override
    protected Stream<String> tabCompleted(String label, ArgConsumer args, Settings settings) {
        if (args.hasExactlyOne()) {
            return RelativeFile.tabComplete(args, schematicsDir);
        } else if (args.has(2)) {
            args.get();
            return args.tabCompleteDatatype(RelativeBlockPos.class);
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Build a schematic";
    }

    @Override
    public List<String> getLongDesc() {
        return asList(
                "Build a schematic from a file.",
                "",
                "Usage:",
                "> build <filename> - Loads and builds '<filename>.schematic'",
                "> build <filename> <x> <y> <z> - Custom position"
        );
    }
}
