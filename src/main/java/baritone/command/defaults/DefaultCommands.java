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
import baritone.api.command.ICommand;

import java.util.*;

public final class DefaultCommands {

    private DefaultCommands() {
    }

    public static List<ICommand> createAll(IBaritone baritone) {
        Objects.requireNonNull(baritone);
        List<ICommand> commands = new ArrayList<>(Arrays.asList(
                new HelpCommand(baritone),
                new SetCommand(baritone),
                new CommandAlias(baritone, Arrays.asList("modified", "mod", "baritone", "modifiedsettings"), "List modified settings", "set modified"),
                new CommandAlias(baritone, "reset", "Reset all settings or just one", "set reset"),
                new GoalCommand(baritone),
                new GotoCommand(baritone),
                new PathCommand(baritone),
                new ProcCommand(baritone),
                new ETACommand(baritone),
                new VersionCommand(baritone),
                new RepackCommand(baritone),
                new BuildCommand(baritone),
                new SchematicaCommand(baritone),
                new LitematicaCommand(baritone),
                new ComeCommand(baritone),
                new AxisCommand(baritone),
                new ForceCancelCommand(baritone),
                new GcCommand(baritone),
                new InvertCommand(baritone),
                new TunnelCommand(baritone),
                new RenderCommand(baritone),
                new FarmCommand(baritone),
                new FollowCommand(baritone),
                new ExploreFilterCommand(baritone),
                new ReloadAllCommand(baritone),
                new SaveAllCommand(baritone),
                new ExploreCommand(baritone),
                new BlacklistCommand(baritone),
                new FindCommand(baritone),
                new MineCommand(baritone),
                new ClickCommand(baritone),
                new SurfaceCommand(baritone),
                new ThisWayCommand(baritone),
                new WaypointsCommand(baritone),
                new CommandAlias(baritone, "sethome", "Sets your home waypoint", "waypoints save home"),
                new CommandAlias(baritone, "home", "Path to your home waypoint", "waypoints goto home"),
                new SelCommand(baritone),
                new ElytraCommand(baritone)
        ));
        ExecutionControlCommands prc = new ExecutionControlCommands(baritone);
        commands.add(prc.pauseCommand);
        commands.add(prc.resumeCommand);
        commands.add(prc.pausedCommand);
        commands.add(prc.cancelCommand);
        return Collections.unmodifiableList(commands);
    }
}
