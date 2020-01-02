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
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class CommandAlias extends Command {

    private final String shortDesc;
    public final String target;

    public CommandAlias(IBaritone baritone, List<String> names, String shortDesc, String target) {
        super(baritone, names.toArray(new String[0]));
        this.shortDesc = shortDesc;
        this.target = target;
    }

    public CommandAlias(IBaritone baritone, String name, String shortDesc, String target) {
        super(baritone, name);
        this.shortDesc = shortDesc;
        this.target = target;
    }

    @Override
    public void execute(String label, IArgConsumer args) {
        this.baritone.getCommandManager().execute(String.format("%s %s", target, args.rawRest()));
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return this.baritone.getCommandManager().tabComplete(String.format("%s %s", target, args.rawRest()));
    }

    @Override
    public String getShortDesc() {
        return shortDesc;
    }

    @Override
    public List<String> getLongDesc() {
        return Collections.singletonList(String.format("This command is an alias, for: %s ...", target));
    }
}
