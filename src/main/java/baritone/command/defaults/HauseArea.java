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

import baritone.Baritone;
import baritone.api.IBaritone;
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.datatypes.ForEnumFacing;
import baritone.api.command.datatypes.RelativeBlockPos;
import baritone.api.command.exception.CommandException;
import baritone.api.command.exception.CommandInvalidStateException;
import baritone.api.command.exception.CommandInvalidTypeException;
import baritone.api.command.helpers.TabCompleteHelper;
import baritone.api.event.events.RenderEvent;
import baritone.api.event.listener.AbstractGameEventListener;
import baritone.api.selection.ISelection;
import baritone.api.selection.ISelectionManager;
import baritone.api.utils.BetterBlockPos;
import baritone.utils.IRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public class HauseArea extends Command {

    private ISelectionManager manager = baritone.getHomeAreaSelectionManager();
    private BetterBlockPos pos1 = null;

    public HauseArea(IBaritone baritone) {
        super(baritone, "homearea", "hausearea", "ha");
        baritone.getGameEventHandler().registerEventListener(new AbstractGameEventListener() {
            @Override
            public void onRenderPass(RenderEvent event) {
                if (!Baritone.settings().renderHomeAreaSelectionCorners.value || pos1 == null) {
                    return;
                }
                Color color = Baritone.settings().colorHomeAreaSelectionPos1.value;
                float opacity = Baritone.settings().selectionHomeAreaOpacity.value;
                float lineWidth = Baritone.settings().selectionHomeAreaLineWidth.value;
                boolean ignoreDepth = Baritone.settings().renderHomeAreaSelectionIgnoreDepth.value;
                IRenderer.startLines(color, opacity, lineWidth, ignoreDepth);
                IRenderer.drawAABB(new AxisAlignedBB(pos1, pos1.add(1, 1, 1)));
                IRenderer.endLines(ignoreDepth);
            }
        });
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        Action action = Action.getByName(args.getString());
        if (action == null) {
            throw new CommandInvalidTypeException(args.consumed(), "an action");
        }
        if (action == Action.POS1 || action == Action.POS2) {
            if (action == Action.POS2 && pos1 == null) {
                throw new CommandInvalidStateException("Set pos1 first before using pos2");
            }
            BetterBlockPos playerPos = mc.getRenderViewEntity() != null ? BetterBlockPos.from(new BlockPos(mc.getRenderViewEntity())) : ctx.playerFeet();
            BetterBlockPos pos = args.hasAny() ? args.getDatatypePost(RelativeBlockPos.INSTANCE, playerPos) : playerPos;
            args.requireMax(0);
            if (action == Action.POS1) {
                pos1 = pos;
                logDirect("Position 1 has been set");
            } else {
                manager.addSelection(pos1, pos);
                pos1 = null;
                logDirect("Selection added");
            }
        }else if (action == Action.UNDO) {
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
        } else if (action == Action.EXPAND || action == Action.CONTRACT || action == Action.SHIFT) {
            args.requireExactly(3);
            TransformTarget transformTarget = TransformTarget.getByName(args.getString());
            if (transformTarget == null) {
                throw new CommandInvalidStateException("Invalid transform type");
            }
            EnumFacing direction = args.getDatatypeFor(ForEnumFacing.INSTANCE);
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
        else if (action == Action.CLEAR) {
            args.requireMax(0);
            pos1 = null;
            logDirect(String.format("Removed %d selections", manager.removeAllSelections().length));
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
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
                        return args.tabCompleteDatatype(RelativeBlockPos.INSTANCE);
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
                            return args.tabCompleteDatatype(ForEnumFacing.INSTANCE);
                        }
                    }
                }
            }
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Home area command";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Can be used as #homearea or #ha",
                "",
                "Using these home area selections, you can keep baritone from breaking your walls or placing blocks in the middle of your base, BEWHARE ANY BUILD PROCESS WILL IGNORE IT",
                "",
                "Also they are saved so you don't have to worry about disconnection or closing your minecraft instance",
                "",
                "The expand/contract/shift commands use a kind of selector to choose which selections to target. Supported ones are a/all, n/newest, and o/oldest.",
                "",
                "Usage:",
                "> ha pos1/p1/1 - Set position 1 to your current position.",
                "> ha pos1/p1/1 <x> <y> <z> - Set position 1 to a relative position.",
                "> ha pos2/p2/2 - Set position 2 to your current position.",
                "> ha pos2/p2/2 <x> <y> <z> - Set position 2 to a relative position.",
                "",
                "PS: It's a blatant and shameful #sel copy"
        );
    }

    enum Action {
        POS1("pos1", "p1", "1"),
        POS2("pos2", "p2", "2"),
        UNDO("undo", "u"),
        CONTRACT("contract", "ct"),
        SHIFT("shift", "sh"),
        EXPAND("expand", "ex"),
        CLEAR("clear");
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
                names.addAll(Arrays.asList(action.names));
            }
            return names.toArray(new String[0]);
        }
    }

    enum TransformTarget {
        ALL(sels -> sels, "all", "a"),
        NEWEST(sels -> new ISelection[]{sels[sels.length - 1]}, "newest", "n"),
        OLDEST(sels -> new ISelection[]{sels[0]}, "oldest", "o");
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
                names.addAll(Arrays.asList(target.names));
            }
            return names.toArray(new String[0]);
        }
    }
}
