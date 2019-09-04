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

import baritone.api.Settings;
import baritone.api.event.events.RenderEvent;
import baritone.api.schematic.CompositeSchematic;
import baritone.api.schematic.FillBomSchematic;
import baritone.api.schematic.ReplaceSchematic;
import baritone.api.schematic.ShellSchematic;
import baritone.api.schematic.WallsSchematic;
import baritone.api.selection.ISelection;
import baritone.api.selection.ISelectionManager;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.BlockOptionalMeta;
import baritone.api.utils.BlockOptionalMetaLookup;
import baritone.api.utils.IRenderer;
import baritone.api.utils.ISchematic;
import baritone.api.utils.command.Command;
import baritone.api.utils.command.datatypes.ForBlockOptionalMeta;
import baritone.api.utils.command.datatypes.ForEnumFacing;
import baritone.api.utils.command.datatypes.RelativeBlockPos;
import baritone.api.utils.command.exception.CommandInvalidStateException;
import baritone.api.utils.command.exception.CommandInvalidTypeException;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import baritone.api.utils.command.helpers.tabcomplete.TabCompleteHelper;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3i;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public class SelCommand extends Command {
    private ISelectionManager manager = baritone.getSelectionManager();
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
                manager.addSelection(pos1, pos);
                pos1 = null;
                logDirect("Selection added");
            }
        } else if (action == Action.CLEAR) {
            args.requireMax(0);
            pos1 = null;
            logDirect(String.format("Removed %d selections", manager.removeAllSelections().length));
        } else if (action == Action.UNDO) {
            args.requireMax(0);

            if (pos1 != null) {
                pos1 = null;
                logDirect("Undid pos1");
            } else {
                ISelection[] selections = manager.getSelections();

                if (selections.length < 1) {
                    throw new CommandInvalidStateException("Nothing to undo!");
                } else {
                    pos1 = manager.removeSelection(selections[selections.length - 1]).pos1();
                    logDirect("Undid pos2");
                }
            }
        } else if (action == Action.SET || action == Action.WALLS || action == Action.SHELL || action == Action.CLEARAREA || action == Action.REPLACE) {
            BlockOptionalMeta type = action == Action.CLEARAREA
                ? new BlockOptionalMeta(Blocks.AIR)
                : args.getDatatypeFor(ForBlockOptionalMeta.class);
            BlockOptionalMetaLookup replaces = null;

            if (action == Action.REPLACE) {
                args.requireMin(1);

                List<BlockOptionalMeta> replacesList = new ArrayList<>();

                while (args.has()) {
                    replacesList.add(args.getDatatypeFor(ForBlockOptionalMeta.class));
                }

                replaces = new BlockOptionalMetaLookup(replacesList.toArray(new BlockOptionalMeta[0]));
            } else {
                args.requireMax(0);
            }

            ISelection[] selections = manager.getSelections();

            if (selections.length == 0) {
                throw new CommandInvalidStateException("No selections");
            }

            BetterBlockPos origin = selections[0].min();
            CompositeSchematic composite = new CompositeSchematic(baritone, 0, 0, 0);

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

                ISchematic schematic = new FillBomSchematic(baritone, size.getX(), size.getY(), size.getZ(), type);

                if (action == Action.WALLS) {
                    schematic = new WallsSchematic(baritone, schematic);
                } else if (action == Action.SHELL) {
                    schematic = new ShellSchematic(baritone, schematic);
                } else if (action == Action.REPLACE) {
                    schematic = new ReplaceSchematic(baritone, schematic, replaces);
                }

                composite.put(schematic, min.x - origin.x, min.y - origin.y, min.z - origin.z);
            }

            baritone.getBuilderProcess().build("Fill", composite, origin);
            logDirect("Filling now");
        } else if (action == Action.EXPAND || action == Action.CONTRACT || action == Action.SHIFT) {
            args.requireExactly(3);
            TransformTarget transformTarget = TransformTarget.getByName(args.getString());

            if (transformTarget == null) {
                throw new CommandInvalidStateException("Invalid transform type");
            }

            EnumFacing direction = args.getDatatypeFor(ForEnumFacing.class);
            int blocks = args.getAs(Integer.class);

            ISelection[] selections = manager.getSelections();

            if (selections.length < 1) {
                throw new CommandInvalidStateException("No selections found");
            }

            selections = transformTarget.transform(selections);

            for (ISelection selection : selections) {
                if (action == Action.EXPAND) {
                    manager.expand(selection, direction, blocks);
                } else if (action == Action.CONTRACT) {
                    manager.contract(selection, direction, blocks);
                } else {
                    manager.shift(selection, direction, blocks);
                }
            }

            logDirect(String.format("Transformed %d selections", selections.length));
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
                    if (args.hasAtMost(3)) {
                        return args.tabCompleteDatatype(RelativeBlockPos.class);
                    }
                } else if (action == Action.SET || action == Action.WALLS || action == Action.CLEARAREA || action == Action.REPLACE) {
                    if (args.hasExactlyOne() || action == Action.REPLACE) {
                        while (args.has(2)) {
                            args.get();
                        }

                        return args.tabCompleteDatatype(ForBlockOptionalMeta.class);
                    }
                } else if (action == Action.EXPAND || action == Action.CONTRACT || action == Action.SHIFT) {
                    if (args.hasExactlyOne()) {
                        return new TabCompleteHelper()
                            .append(TransformTarget.getAllNames())
                            .filterPrefix(args.getString())
                            .sortAlphabetically()
                            .stream();
                    } else {
                        TransformTarget target = TransformTarget.getByName(args.getString());

                        if (target != null && args.hasExactlyOne()) {
                            return args.tabCompleteDatatype(ForEnumFacing.class);
                        }
                    }
                }
            }
        }

        return Stream.empty();
    }

    @Override
    public List<String> getLongDesc() {
        return asList(
            "The sel command allows you to manipulate Baritone's selections, similarly to WorldEdit.",
            "",
            "Using these selections, you can clear areas, fill them with blocks, or something else.",
            "",
            "The expand/contract/shift commands use a kind of selector to choose which selections to target. Supported ones are a/all, n/newest, and o/oldest.",
            "",
            "Usage:",
            "> sel pos1/p1/1 - Set position 1 to your current position.",
            "> sel pos1/p1/1 <x> <y> <z> - Set position 1 to a relative position.",
            "> sel pos2/p2/2 - Set position 2 to your current position.",
            "> sel pos2/p2/2 <x> <y> <z> - Set position 2 to a relative position.",
            "",
            "> sel clear/c - Clear the selection.",
            "> sel undo/u - Undo the last action (setting positions, creating selections, etc.)",
            "> sel set/fill/s/f [block] - Completely fill all selections with a block.",
            "> sel walls/w [block] - Fill in the walls of the selection with a specified block.",
            "> sel shell/shl [block] - The same as walls, but fills in a ceiling and floor too.",
            "> sel cleararea/ca - Basically 'set air'.",
            "> sel replace/r <place> <break...> - Replaces, with 'place', all blocks listed after it.",
            "",
            "> sel expand <target> <direction> <blocks> - Expand the targets.",
            "> sel contract <target> <direction> <blocks> - Contract the targets.",
            "> sel shift <target> <direction> <blocks> - Shift the targets (does not resize)."
        );
    }

    enum Action {
        POS1("pos1", "p1", "1"),
        POS2("pos2", "p2", "2"),

        CLEAR("clear", "c"),
        UNDO("undo", "u"),

        SET("set", "fill", "s", "f"),
        WALLS("walls", "w"),
        SHELL("shell", "shl"),
        CLEARAREA("cleararea", "ca"),
        REPLACE("replace", "r"),

        EXPAND("expand", "ex"),
        CONTRACT("contract", "ct"),
        SHIFT("shift", "sh");

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

    enum TransformTarget {
        ALL(sels -> sels, "all", "a"),
        NEWEST(sels -> new ISelection[] {sels[sels.length - 1]}, "newest", "n"),
        OLDEST(sels -> new ISelection[] {sels[0]}, "oldest", "o");

        private final Function<ISelection[], ISelection[]> transform;
        private final String[] names;

        TransformTarget(Function<ISelection[], ISelection[]> transform, String... names) {
            this.transform = transform;
            this.names = names;
        }

        public ISelection[] transform(ISelection[] selections) {
            return transform.apply(selections);
        }

        public static TransformTarget getByName(String name) {
            for (TransformTarget target : TransformTarget.values()) {
                for (String alias : target.names) {
                    if (alias.equalsIgnoreCase(name)) {
                        return target;
                    }
                }
            }

            return null;
        }

        public static String[] getAllNames() {
            Set<String> names = new HashSet<>();

            for (TransformTarget target : TransformTarget.values()) {
                names.addAll(asList(target.names));
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
        float opacity = settings.selectionOpacity.value;
        float lineWidth = settings.selectionLineWidth.value;
        boolean ignoreDepth = settings.renderSelectionIgnoreDepth.value;

        IRenderer.startLines(color, opacity, lineWidth, ignoreDepth);
        IRenderer.drawAABB(new AxisAlignedBB(pos1, pos1.add(1, 1, 1)));
        IRenderer.endLines(ignoreDepth);
    }
}
