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
import baritone.api.utils.command.Command;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import baritone.api.utils.command.manager.CommandManager;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class CommandAlias extends Command {
    public final String target;

    public CommandAlias(List<String> names, String shortDesc, String target) {
        super(names, shortDesc);
        this.target = target;
    }

    public CommandAlias(String name, String shortDesc, String target) {
        super(name, shortDesc);
        this.target = target;
    }

    @Override
    protected void executed(String label, ArgConsumer args, Settings settings) {
        CommandManager.execute(String.format("%s %s", target, args.rawRest()));
    }

    @Override
    protected Stream<String> tabCompleted(String label, ArgConsumer args, Settings settings) {
        return CommandManager.tabComplete(String.format("%s %s", target, args.rawRest()));
    }

    @Override
    public List<String> getLongDesc() {
        return Collections.singletonList(String.format("This command is an alias, for: %s ...", target));
    }
}
