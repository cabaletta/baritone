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

package baritone.api.utils.command.defaults;

import baritone.api.Settings;
import baritone.api.event.events.RenderEvent;
import baritone.api.schematic.CompositeSchematic;
import baritone.api.schematic.FillBomSchematic;
import baritone.api.schematic.ShellSchematic;
import baritone.api.schematic.WallsSchematic;
import baritone.api.selection.ISelection;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.BlockOptionalMeta;
import baritone.api.utils.IRenderer;
import baritone.api.utils.ISchematic;
import baritone.api.utils.command.Command;
import baritone.api.utils.command.datatypes.ForBlockOptionalMeta;
import baritone.api.utils.command.datatypes.RelativeBlockPos;
import baritone.api.utils.command.exception.CommandInvalidStateException;
import baritone.api.utils.command.exception.CommandInvalidTypeException;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import baritone.api.utils.command.helpers.tabcomplete.TabCompleteHelper;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3i;

import java.awt.Color;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public class SelCommand extends Command {
    private BetterBlockPos pos1 = null;

    public SelCommand() {
        super(asList("sel", "selection", "s"), "WorldEdit-like commands");
    }

    @Override
    protected void executed(String label, ArgConsumer args, Settings settings) {
        Action action = Action.getByName(args.getString());

        if (action == null) {
            throw new CommandInvalidTypeException(args.consumed(), "an action");
        }

        if (action == Action.POS1 || action == Action.POS2) {
            if (action == Action.POS2 && pos1 == null) {
                throw new CommandInvalidStateException("Set pos1 first before using pos2");
            }

            BetterBlockPos playerPos = ctx.playerFeet();
            BetterBlockPos pos = args.has() ? args.getDatatypePost(RelativeBlockPos.class, playerPos) : playerPos;
            args.requireMax(0);

            if (action == Action.POS1) {
                pos1 = pos;
                logDirect("Position 1 has been set");
            } else {
                baritone.getSelectionManager().addSelection(pos1, pos);
                pos1 = null;
                logDirect("Selection added");
            }
        } else if (action == Action.CLEAR) {
            pos1 = null;
            logDirect(String.format(
                "Removed %d selections",
                baritone.getSelectionManager().removeAllSelections().length
            ));
        } else {
            BlockOptionalMeta type = action == Action.CLEARAREA
                ? new BlockOptionalMeta(Blocks.AIR)
                : args.getDatatypeFor(ForBlockOptionalMeta.class);
            args.requireMax(0);
            ISelection[] selections = baritone.getSelectionManager().getSelections();

            if (selections.length == 0) {
                throw new CommandInvalidStateException("No selections");
            }

            BetterBlockPos origin = selections[0].min();
            CompositeSchematic composite = new CompositeSchematic(0, 0, 0);

            for (ISelection selection : selections) {
                BetterBlockPos min = selection.min();
                origin = new BetterBlockPos(
                    Math.min(origin.x, min.x),
                    Math.min(origin.y, min.y),
                    Math.min(origin.z, min.z)
                );
            }

            for (ISelection selection : selections) {
                Vec3i size = selection.size();
                BetterBlockPos min = selection.min();

                ISchematic schematic = new FillBomSchematic(size.getX(), size.getY(), size.getZ(), type);

                if (action == Action.WALLS) {
                    schematic = new WallsSchematic(schematic);
                } else if (action == Action.SHELL) {
                    schematic = new ShellSchematic(schematic);
                }

                composite.put(schematic, min.x - origin.x, min.y - origin.y, min.z - origin.z);
            }

            baritone.getBuilderProcess().build("Fill", composite, origin);
            logDirect("Filling now");
        }
    }

    @Override
    protected Stream<String> tabCompleted(String label, ArgConsumer args, Settings settings) {
        if (args.hasExactlyOne()) {
            return new TabCompleteHelper()
                .append(Action.getAllNames())
                .filterPrefix(args.getString())
                .sortAlphabetically()
                .stream();
        } else {
            Action action = Action.getByName(args.getString());

            if (action != null) {
                if (action == Action.POS1 || action == Action.POS2) {
                    return args.tabCompleteDatatype(RelativeBlockPos.class);
                }
            }
        }

        return Stream.empty();
    }

    @Override
    public List<String> getLongDesc() {
        return asList(
            "",
            "",
            "Usage:",
            "> "
        );
    }

    enum Action {
        POS1("pos1", "p1"),
        POS2("pos2", "p2"),
        CLEAR("clear", "c"),
        SET("set", "fill", "s", "f"),
        WALLS("walls", "w"),
        SHELL("shell", "sh"),
        CLEARAREA("cleararea", "ca");

        private final String[] names;

        Action(String... names) {
            this.names = names;
        }

        public static Action getByName(String name) {
            for (Action action : Action.values()) {
                for (String alias : action.names) {
                    if (alias.equalsIgnoreCase(name)) {
                        return action;
                    }
                }
            }

            return null;
        }

        public static String[] getAllNames() {
            Set<String> names = new HashSet<>();

            for (Action action : Action.values()) {
                names.addAll(asList(action.names));
            }

            return names.toArray(new String[0]);
        }
    }

    @Override
    public void onRenderPass(RenderEvent event) {
        if (!settings.renderSelectionCorners.value || pos1 == null) {
            return;
        }

        Color color = settings.colorSelectionPos1.value;
        float lineWidth = settings.selectionRenderLineWidthPixels.value;
        boolean ignoreDepth = settings.renderSelectionIgnoreDepth.value;

        IRenderer.startLines(color, lineWidth, ignoreDepth);
        IRenderer.drawAABB(new AxisAlignedBB(pos1, pos1.add(1, 1, 1)));
        IRenderer.endLines(ignoreDepth);
    }
}
