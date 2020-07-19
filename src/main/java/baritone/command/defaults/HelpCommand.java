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
import baritone.api.command.ICommand;
import baritone.api.command.exception.CommandException;
import baritone.api.command.exception.CommandNotFoundException;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.helpers.Paginator;
import baritone.api.command.helpers.TabCompleteHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static baritone.api.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

public class HelpCommand extends Command {

    public HelpCommand(IBaritone baritone) {
        super(baritone, "help", "?");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(1);
        if (!args.hasAny() || args.is(Integer.class)) {
            Paginator.paginate(
                    args, new Paginator<>(
                            this.baritone.getCommandManager().getRegistry().descendingStream()
                                    .filter(command -> !command.hiddenFromHelp())
                                    .collect(Collectors.toList())
                    ),
                    () -> logDirect("All Baritone commands (clickable):"),
                    command -> {
                        String names = String.join("/", command.getNames());
                        String name = command.getNames().get(0);
                        StringTextComponent shortDescComponent = new StringTextComponent(" - " + command.getShortDesc());
                        Style shortDescComponentStyle = Style.EMPTY;
                        shortDescComponentStyle = shortDescComponentStyle.setFormatting(TextFormatting.DARK_GRAY);
                        shortDescComponent.func_230530_a_(shortDescComponentStyle);
                        StringTextComponent namesComponent = new StringTextComponent(names);
                        Style namesComponentStyle = Style.EMPTY;
                        namesComponentStyle = namesComponentStyle.setFormatting(TextFormatting.WHITE);
                        namesComponent.func_230530_a_(namesComponentStyle);
                        StringTextComponent hoverComponent = new StringTextComponent("");
                        Style hoverComponentStyle = Style.EMPTY;
                        hoverComponentStyle = hoverComponentStyle.setFormatting(TextFormatting.GRAY);
                        hoverComponent.func_230530_a_(hoverComponentStyle);
                        hoverComponent.func_230529_a_(namesComponent);
                        hoverComponent.func_240702_b_("\n" + command.getShortDesc());
                        hoverComponent.func_240702_b_("\n\nClick to view full help");
                        String clickCommand = FORCE_COMMAND_PREFIX + String.format("%s %s", label, command.getNames().get(0));
                        StringTextComponent component = new StringTextComponent(name);
                        Style componentStyle = Style.EMPTY;
                        componentStyle = componentStyle.setFormatting(TextFormatting.GRAY);
                        component.func_230530_a_(componentStyle);
                        component.func_230529_a_(shortDescComponent);
                        componentStyle = componentStyle
                                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponent))
                                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, clickCommand));
                        component.func_230530_a_(componentStyle);
                        return component;
                    },
                    FORCE_COMMAND_PREFIX + label
            );
        } else {
            String commandName = args.getString().toLowerCase();
            ICommand command = this.baritone.getCommandManager().getCommand(commandName);
            if (command == null) {
                throw new CommandNotFoundException(commandName);
            }
            logDirect(String.format("%s - %s", String.join(" / ", command.getNames()), command.getShortDesc()));
            logDirect("");
            command.getLongDesc().forEach(this::logDirect);
            logDirect("");
            StringTextComponent returnComponent = new StringTextComponent("Click to return to the help menu");
            Style returnComponentStyle = Style.EMPTY;
            returnComponentStyle = returnComponentStyle.setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    FORCE_COMMAND_PREFIX + label
            ));
            returnComponent.func_230530_a_(returnComponentStyle);
            logDirect(returnComponent);
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasExactlyOne()) {
            return new TabCompleteHelper()
                    .addCommands(this.baritone.getCommandManager())
                    .filterPrefix(args.getString())
                    .stream();
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "View all commands or help on specific ones";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Using this command, you can view detailed help information on how to use certain commands of Baritone.",
                "",
                "Usage:",
                "> help - Lists all commands and their short descriptions.",
                "> help <command> - Displays help information on a specific command."
        );
    }
}
