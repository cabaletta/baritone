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
import baritone.api.utils.command.Command;
import baritone.api.utils.command.exception.CommandNotFoundException;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import baritone.api.utils.command.helpers.pagination.Paginator;
import baritone.api.utils.command.helpers.tabcomplete.TabCompleteHelper;
import baritone.api.utils.command.manager.CommandManager;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static baritone.api.utils.command.BaritoneChatControl.FORCE_COMMAND_PREFIX;
import static baritone.api.utils.command.manager.CommandManager.getCommand;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;

public class HelpCommand extends Command {
    public HelpCommand() {
        super(asList("help", "?"));
    }

    @Override
    protected void executed(String label, ArgConsumer args, Settings settings) {
        args.requireMax(1);

        if (!args.has() || args.is(Integer.class)) {
            Paginator.paginate(
                    args, new Paginator<>(
                            CommandManager.REGISTRY.descendingStream()
                                    .filter(command -> !command.hiddenFromHelp())
                                    .collect(Collectors.toList())
                    ),
                    () -> logDirect("All Baritone commands (clickable):"),
                    command -> {
                        String names = String.join("/", command.names);
                        String name = command.names.get(0);

                        ITextComponent shortDescComponent = new TextComponentString(" - " + command.getShortDesc());
                        shortDescComponent.getStyle().setColor(TextFormatting.DARK_GRAY);

                        ITextComponent namesComponent = new TextComponentString(names);
                        namesComponent.getStyle().setColor(TextFormatting.WHITE);

                        ITextComponent hoverComponent = new TextComponentString("");
                        hoverComponent.getStyle().setColor(TextFormatting.GRAY);
                        hoverComponent.appendSibling(namesComponent);
                        hoverComponent.appendText("\n" + command.getShortDesc());
                        hoverComponent.appendText("\n\nClick to view full help");
                        String clickCommand = FORCE_COMMAND_PREFIX + String.format("%s %s", label, command.names.get(0));

                        ITextComponent component = new TextComponentString(name);
                        component.getStyle().setColor(TextFormatting.GRAY);
                        component.appendSibling(shortDescComponent);
                        component.getStyle()
                                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponent))
                                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, clickCommand));
                        return component;
                    },
                    FORCE_COMMAND_PREFIX + label
            );
        } else {
            String commandName = args.getString().toLowerCase();
            Command command = getCommand(commandName);

            if (isNull(command)) {
                throw new CommandNotFoundException(commandName);
            }

            logDirect(String.format("%s - %s", String.join(" / ", command.names), command.getShortDesc()));
            logDirect("");
            command.getLongDesc().forEach(this::logDirect);
            logDirect("");

            ITextComponent returnComponent = new TextComponentString("Click to return to the help menu");
            returnComponent.getStyle().setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    FORCE_COMMAND_PREFIX + label
            ));

            logDirect(returnComponent);
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
    public String getShortDesc() {
        return "View all commands or help on specific ones";
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
