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
import baritone.api.bot.IBaritoneUser;
import baritone.api.bot.connect.IConnectionResult;
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.exception.CommandException;
import baritone.api.command.exception.CommandInvalidTypeException;
import baritone.api.event.events.ChatEvent;
import baritone.bot.UserManager;
import baritone.bot.impl.BotGuiInventory;
import net.minecraft.util.Session;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static baritone.api.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

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
        final Action action = Action.getByName(args.getString());
        if (action == null) {
            throw new CommandInvalidTypeException(args.consumed(), "an action");
        }

        if (action == Action.ADD) {
            final String username = args.hasAny() ? args.getString() : "Bot" + System.currentTimeMillis() % 1000;
            final Session session = new Session(username, UUID.randomUUID().toString(), "", "");
            final IConnectionResult result = UserManager.INSTANCE.connect(session);
            logDirect(result.toString());
        } else if (action.requiresBotSelector()) {
            final String selector = args.getString();
            final List<IBaritoneUser> bots;

            if (selector.equals("*")) {
                bots = UserManager.INSTANCE.getUsers();
            } else if (selector.contains(",")) {
                bots = Arrays.stream(selector.split(","))
                        .map(UserManager.INSTANCE::getUserByName)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());
            } else {
                bots = UserManager.INSTANCE.getUserByName(selector)
                        .map(Collections::singletonList)
                        .orElseGet(Collections::emptyList);
            }

            if (bots.isEmpty()) {
                throw new CommandInvalidTypeException(args.consumed(), "selector didn't match any bots");
            }

            if (action == Action.INVENTORY) {
                // Only display one inventory lol
                final IBaritoneUser bot = bots.get(0);
                ((Baritone) baritone).showScreen(new BotGuiInventory(bot));
                return;
            }

            switch (action) {
                case DISCONNECT: {
                    bots.forEach(bot -> UserManager.INSTANCE.disconnect(bot, null));
                    break;
                }
                case SAY: {
                    final String message = args.rawRest();
                    bots.forEach(bot -> bot.getPlayerContext().player().sendChatMessage(message));
                    break;
                }
                case EXECUTE: {
                    final String command = FORCE_COMMAND_PREFIX + args.rawRest();
                    bots.forEach(bot -> bot.getBaritone().getGameEventHandler().onSendChatMessage(new ChatEvent(command)));
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
        return "Manage bots";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Spawns a bot",
                "",
                "Usage:",
                "> bot add/a <name>",
                "> bot inventory/i [bot]",
                "> bot disconnect/d [bot]",
                "> bot say/s [bot] [args...]",
                "> bot execute/e [bot] [args...]"
        );
    }

    private enum Action {
        ADD("add", "a"),
        INVENTORY("inventory", "i"),
        DISCONNECT("disconnect", "d"),
        SAY("say", "s"),
        EXECUTE("execute", "e");
        private final String[] names;

        Action(String... names) {
            this.names = names;
        }

        public boolean requiresBotSelector() {
            return this == INVENTORY || this == DISCONNECT || this == SAY || this == EXECUTE;
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
