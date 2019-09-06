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

package baritone.api.utils.command;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.event.listener.AbstractGameEventListener;
import baritone.api.utils.Helper;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.command.execution.CommandExecution;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import net.minecraft.client.Minecraft;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Command implements Helper, AbstractGameEventListener {
    protected IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
    protected Settings settings = BaritoneAPI.getSettings();
    protected IPlayerContext ctx = baritone.getPlayerContext();
    protected Minecraft MC = mc;

    /**
     * The names of this command. This is what you put after the command prefix.
     */
    public final List<String> names;

    /**
     * Creates a new Baritone control command.
     *
     * @param names The names of this command. This is what you put after the command prefix.
     */
    protected Command(List<String> names) {
        this.names = names.stream()
            .map(s -> s.toLowerCase(Locale.US))
            .collect(Collectors.toList());
        baritone.getGameEventHandler().registerEventListener(this);
    }

    protected Command(String name) {
        this(Collections.singletonList(name));
    }

    /**
     * Executes this command with the specified arguments.
     *
     * @param execution The command execution to execute this command with
     */
    public void execute(CommandExecution execution) {
        executed(execution.label, execution.args, execution.settings);
    }

    /**
     * Tab completes this command with the specified arguments. This won't throw any exceptions ever.
     *
     * @param execution The command execution to tab complete
     * @return The list of completions.
     */
    public Stream<String> tabComplete(CommandExecution execution) {
        try {
            return tabCompleted(execution.label, execution.args, execution.settings);
        } catch (Throwable t) {
            return Stream.empty();
        }
    }

    /**
     * Called when this command is executed.
     */
    protected abstract void executed(String label, ArgConsumer args, Settings settings);

    /**
     * Called when the command needs to tab complete. Return a Stream representing the entries to put in the completions
     * list.
     */
    protected abstract Stream<String> tabCompleted(String label, ArgConsumer args, Settings settings);

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
}
