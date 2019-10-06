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

package baritone.api.command;

import baritone.api.IBaritone;
import baritone.api.utils.Helper;
import baritone.api.utils.IPlayerContext;
import baritone.api.command.exception.CommandException;
import baritone.api.command.helpers.arguments.IArgConsumer;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Command implements Helper {

    protected IBaritone baritone;
    protected IPlayerContext ctx;

    /**
     * The names of this command. This is what you put after the command prefix.
     */
    protected final List<String> names;

    /**
     * Creates a new Baritone control command.
     *
     * @param names The names of this command. This is what you put after the command prefix.
     */
    protected Command(IBaritone baritone, String... names) {
        this.names = Collections.unmodifiableList(Stream.of(names)
                .map(s -> s.toLowerCase(Locale.US))
                .collect(Collectors.toList()));
        this.baritone = baritone;
        this.ctx = baritone.getPlayerContext();
    }

    /**
     * Called when this command is executed.
     */
    public abstract void execute(String label, IArgConsumer args) throws CommandException;

    /**
     * Called when the command needs to tab complete. Return a Stream representing the entries to put in the completions
     * list.
     */
    public abstract Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException;

    /**
     * @return A <b>single-line</b> string containing a short description of this command's purpose.
     */
    public abstract String getShortDesc();

    /**
     * @return A list of lines that will be printed by the help command when the user wishes to view them.
     */
    public abstract List<String> getLongDesc();

    /**
     * @return {@code true} if this command should be hidden from the help menu
     */
    public boolean hiddenFromHelp() {
        return false;
    }

    public final List<String> getNames() {
        return this.names;
    }
}
