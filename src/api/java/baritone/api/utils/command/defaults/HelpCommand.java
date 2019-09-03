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
import baritone.api.utils.command.exception.CommandNotFoundException;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import baritone.api.utils.command.helpers.pagination.Paginator;
import baritone.api.utils.command.helpers.tabcomplete.TabCompleteHelper;
import baritone.api.utils.command.manager.CommandManager;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static baritone.api.utils.command.BaritoneChatControl.FORCE_COMMAND_PREFIX;
import static baritone.api.utils.command.manager.CommandManager.getCommand;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;

public class HelpCommand extends Command {
    public HelpCommand() {
        super(asList("help", "?"), "View all commands or help on specific ones");
    }

    @Override
    protected void executed(String label, ArgConsumer args, Settings settings) {
        args.requireMax(1);

        if (!args.has() || args.is(Integer.class)) {
            Paginator.paginate(
                args, new Paginator<>(
                    CommandManager.REGISTRY.descendingStream()
                        .filter(command -> !command.hiddenFromHelp())
                        .collect(Collectors.toCollection(ArrayList::new))
                ),
                () -> logDirect("All Baritone commands (clickable):"),
                command -> {
                    String names = String.join("/", command.names);
                    String name = command.names.get(0);

                    return new TextComponentString(name) {{
                        getStyle()
                            .setColor(TextFormatting.GRAY)
                            .setHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                new TextComponentString("") {{
                                    getStyle().setColor(TextFormatting.GRAY);

                                    appendSibling(new TextComponentString(names + "\n") {{
                                        getStyle().setColor(TextFormatting.WHITE);
                                    }});

                                    appendText(command.shortDesc);
                                    appendText("\n\nClick to view full help");
                                }}
                            ))
                            .setClickEvent(new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                FORCE_COMMAND_PREFIX + String.format("help %s", command.names.get(0))
                            ));

                        appendSibling(new TextComponentString(" - " + command.shortDesc) {{
                            getStyle().setColor(TextFormatting.DARK_GRAY);
                        }});
                    }};
                },
                FORCE_COMMAND_PREFIX + "help"
            );
        } else {
            String commandName = args.getString().toLowerCase();
            Command command = getCommand(commandName);

            if (isNull(command)) {
                throw new CommandNotFoundException(commandName);
            }

            logDirect(String.format("%s - %s", String.join(" / ", command.names), command.shortDesc));
            logDirect("");
            command.getLongDesc().forEach(this::logDirect);
            logDirect("");
            logDirect(new TextComponentString("Click to return to the help menu") {{
                getStyle().setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    FORCE_COMMAND_PREFIX + "help"
                ));
            }});
        }
    }

    @Override
    protected Stream<String> tabCompleted(String label, ArgConsumer args, Settings settings) {
        if (args.hasExactlyOne()) {
            return new TabCompleteHelper().addCommands().filterPrefix(args.getString()).stream();
        }

        return Stream.empty();
    }

    @Override
    public List<String> getLongDesc() {
        return asList(
            "Using this command, you can view detailed help information on how to use certain commands of Baritone.",
            "",
            "Usage:",
            "> help - Lists all commands and their short descriptions.",
            "> help <command> - Displays help information on a specific command."
        );
    }
}
