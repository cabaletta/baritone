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
import baritone.api.bot.IBaritoneUser;
import baritone.api.bot.connect.IConnectionResult;
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.exception.CommandException;
import baritone.api.command.exception.CommandInvalidTypeException;
import baritone.bot.UserManager;
import net.minecraft.util.Session;

import java.util.*;
import java.util.stream.Stream;

/**
 * @author Brady
 * @since 3/2/2020
 */
public class BotCommand extends Command {

    public BotCommand(IBaritone baritone) {
        super(baritone, "bot");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        if (args.hasExactly(0)) {
            IConnectionResult result = UserManager.INSTANCE.connect(
                    new Session("Bot" + System.currentTimeMillis() % 1000, UUID.randomUUID().toString(), "", ""));
            logDirect(result.toString());
        } else if (args.hasExactly(2)) {
            Action action = Action.getByName(args.getString());
            if (action == null) {
                throw new CommandInvalidTypeException(args.consumed(), "an action");
            }

            Optional<IBaritoneUser> bot = UserManager.INSTANCE.getUserByName(args.getString());
            if (!bot.isPresent()) {
                throw new CommandInvalidTypeException(args.consumed(), "a bot name");
            }

            switch (action) {
                case DISCONNECT: {
                    UserManager.INSTANCE.disconnect(bot.get(), null);
                    break;
                }
            }
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Set a goal to the axes";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Spawns a bot",
                "",
                "Usage:",
                "> bot",
                "> bot <disconnect> <name>"
        );
    }

    private enum Action {
        DISCONNECT("disconnect", "dc");
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
}
