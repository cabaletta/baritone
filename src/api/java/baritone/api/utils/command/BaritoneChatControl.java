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
import baritone.api.accessor.IGuiScreen;
import baritone.api.event.events.ChatEvent;
import baritone.api.event.events.TabCompleteEvent;
import baritone.api.event.listener.AbstractGameEventListener;
import baritone.api.utils.Helper;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.SettingsUtil;
import baritone.api.utils.command.argument.CommandArgument;
import baritone.api.utils.command.exception.CommandNotFoundException;
import baritone.api.utils.command.execution.CommandExecution;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import baritone.api.utils.command.helpers.tabcomplete.TabCompleteHelper;
import baritone.api.utils.command.manager.CommandManager;
import com.mojang.realmsclient.util.Pair;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class BaritoneChatControl implements Helper, AbstractGameEventListener {

    public final IBaritone baritone;
    public final IPlayerContext ctx;
    public final Settings settings = BaritoneAPI.getSettings();
    public static String FORCE_COMMAND_PREFIX = String.format("<<%s>>", UUID.randomUUID().toString());

    public BaritoneChatControl(IBaritone baritone) {
        this.baritone = baritone;
        this.ctx = baritone.getPlayerContext();
        baritone.getGameEventHandler().registerEventListener(this);
    }

    @Override
    public void onSendChatMessage(ChatEvent event) {
        String msg = event.getMessage();
        String prefix = settings.prefix.value;
        boolean forceRun = msg.startsWith(FORCE_COMMAND_PREFIX);
        if ((settings.prefixControl.value && msg.startsWith(prefix)) || forceRun) {
            event.cancel();
            String commandStr = msg.substring(forceRun ? FORCE_COMMAND_PREFIX.length() : prefix.length());
            if (!runCommand(commandStr) && !commandStr.trim().isEmpty()) {
                new CommandNotFoundException(CommandExecution.expand(commandStr).first()).handle(null, null);
            }
        } else if ((settings.chatControl.value || settings.chatControlAnyway.value) && runCommand(msg)) {
            event.cancel();
        }
    }

    private void logRanCommand(String command, String rest) {
        if (settings.echoCommands.value) {
            String msg = command + rest;
            String toDisplay = settings.censorRanCommands.value ? command + " ..." : msg;
            ITextComponent component = new TextComponentString(String.format("> %s", toDisplay));
            component.getStyle()
                    .setColor(TextFormatting.WHITE)
                    .setHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new TextComponentString("Click to rerun command")
                    ))
                    .setClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            FORCE_COMMAND_PREFIX + msg
                    ));
            logDirect(component);
        }
    }

    public boolean runCommand(String msg) {
        if (msg.trim().equalsIgnoreCase("damn")) {
            logDirect("daniel");
            return false;
        } else if (msg.trim().equalsIgnoreCase("orderpizza")) {
            try {
                ((IGuiScreen) mc.currentScreen).openLink(new URI("https://www.dominos.com/en/pages/order/"));
            } catch (NullPointerException | URISyntaxException ignored) {}
            return false;
        }
        if (msg.isEmpty()) {
            msg = "help";
        }
        Pair<String, List<CommandArgument>> pair = CommandExecution.expand(msg);
        String command = pair.first();
        String rest = msg.substring(pair.first().length());
        ArgConsumer argc = new ArgConsumer(pair.second());
        if (!argc.has()) {
            Settings.Setting setting = settings.byLowerName.get(command.toLowerCase(Locale.US));
            if (setting != null) {
                logRanCommand(command, rest);
                if (setting.getValueClass() == Boolean.class) {
                    CommandManager.execute(String.format("set toggle %s", setting.getName()));
                } else {
                    CommandManager.execute(String.format("set %s", setting.getName()));
                }
                return true;
            }
        } else if (argc.hasExactlyOne()) {
            for (Settings.Setting setting : settings.allSettings) {
                if (setting.getName().equals("logger")) {
                    continue;
                }
                if (setting.getName().equalsIgnoreCase(pair.first())) {
                    logRanCommand(command, rest);
                    CommandManager.execute(String.format("set %s %s", setting.getName(), argc.getString()));
                    return true;
                }
            }
        }
        CommandExecution execution = CommandExecution.from(pair);
        if (isNull(execution)) {
            return false;
        }
        logRanCommand(command, rest);
        CommandManager.execute(execution);
        return true;
    }

    @Override
    public void onPreTabComplete(TabCompleteEvent.Pre event) {
        if (!settings.prefixControl.value) {
            return;
        }
        String prefix = event.prefix.get();
        String commandPrefix = settings.prefix.value;
        if (!prefix.startsWith(commandPrefix)) {
            return;
        }
        String msg = prefix.substring(commandPrefix.length());
        List<CommandArgument> args = CommandArgument.from(msg, true);
        Stream<String> stream = tabComplete(msg);
        if (args.size() == 1) {
            stream = stream.map(x -> commandPrefix + x);
        }
        event.completions.set(stream.toArray(String[]::new));
    }

    public Stream<String> tabComplete(String msg) {
        List<CommandArgument> args = CommandArgument.from(msg, true);
        ArgConsumer argc = new ArgConsumer(args);
        if (argc.hasAtMost(2)) {
            if (argc.hasExactly(1)) {
                return new TabCompleteHelper()
                        .addCommands()
                        .addSettings()
                        .filterPrefix(argc.getString())
                        .stream();
            }
            Settings.Setting setting = settings.byLowerName.get(argc.getString().toLowerCase(Locale.US));
            if (nonNull(setting)) {
                if (setting.getValueClass() == Boolean.class) {
                    TabCompleteHelper helper = new TabCompleteHelper();
                    if ((Boolean) setting.value) {
                        helper.append(Stream.of("true", "false"));
                    } else {
                        helper.append(Stream.of("false", "true"));
                    }
                    return helper.filterPrefix(argc.getString()).stream();
                } else {
                    return Stream.of(SettingsUtil.settingValueToString(setting));
                }
            }
        }
        return CommandManager.tabComplete(msg);
    }
}
