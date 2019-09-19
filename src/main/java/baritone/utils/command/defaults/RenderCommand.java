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
import baritone.api.utils.command.helpers.arguments.ArgConsumer;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public class RenderCommand extends Command {

    public RenderCommand(IBaritone baritone) {
        super(baritone, "render");
    }

    @Override
    protected void executed(String label, ArgConsumer args, Settings settings) {
        args.requireMax(0);

        BetterBlockPos origin = ctx.playerFeet();
        int renderDistance = (MC.gameSettings.renderDistanceChunks + 1) * 16;
        MC.renderGlobal.markBlockRangeForRenderUpdate(
                origin.x - renderDistance,
                0,
                origin.z - renderDistance,
                origin.x + renderDistance,
                255,
                origin.z + renderDistance
        );

        logDirect("Done");
    }

    @Override
    protected Stream<String> tabCompleted(String label, ArgConsumer args, Settings settings) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Fix glitched chunks";
    }

    @Override
    public List<String> getLongDesc() {
        return asList(
                "The render command fixes glitched chunk rendering without having to reload all of them.",
                "",
                "Usage:",
                "> render"
        );
    }
}
