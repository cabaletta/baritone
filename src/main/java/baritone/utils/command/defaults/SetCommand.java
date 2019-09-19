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

import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.utils.SettingsUtil;
import baritone.api.utils.command.Command;
import baritone.api.utils.command.exception.CommandInvalidTypeException;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import baritone.api.utils.command.helpers.pagination.Paginator;
import baritone.api.utils.command.helpers.tabcomplete.TabCompleteHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static baritone.api.utils.SettingsUtil.settingTypeToString;
import static baritone.api.utils.SettingsUtil.settingValueToString;
import static baritone.api.utils.command.BaritoneChatControl.FORCE_COMMAND_PREFIX;
import static java.util.Arrays.asList;
import static java.util.stream.Stream.of;

public class SetCommand extends Command {

    public SetCommand(IBaritone baritone) {
        super(baritone, asList("set", "setting", "settings"));
    }

    @Override
    protected void executed(String label, ArgConsumer args, Settings settings) {
        String arg = args.has() ? args.getString().toLowerCase(Locale.US) : "list";
        if (asList("s", "save").contains(arg)) {
            SettingsUtil.save(settings);
            logDirect("Settings saved");
            return;
        }
        boolean viewModified = asList("m", "mod", "modified").contains(arg);
        boolean viewAll = asList("all", "l", "list").contains(arg);
        boolean paginate = viewModified || viewAll;
        if (paginate) {
            String search = args.has() && args.peekAsOrNull(Integer.class) == null ? args.getString() : "";
            args.requireMax(1);
            List<? extends Settings.Setting> toPaginate =
                    (viewModified ? SettingsUtil.modifiedSettings(settings) : settings.allSettings).stream()
                            .filter(s -> !s.getName().equals("logger"))
                            .filter(s -> s.getName().toLowerCase(Locale.US).contains(search.toLowerCase(Locale.US)))
                            .sorted((s1, s2) -> String.CASE_INSENSITIVE_ORDER.compare(s1.getName(), s2.getName()))
                            .collect(Collectors.toList());
            Paginator.paginate(
                    args,
                    new Paginator<>(toPaginate),
                    () -> logDirect(
                            !search.isEmpty()
                                    ? String.format("All %ssettings containing the string '%s':", viewModified ? "modified " : "", search)
                                    : String.format("All %ssettings:", viewModified ? "modified " : "")
                    ),
                    setting -> {
                        ITextComponent typeComponent = new TextComponentString(String.format(
                                " (%s)",
                                settingTypeToString(setting)
                        ));
                        typeComponent.getStyle().setColor(TextFormatting.DARK_GRAY);
                        ITextComponent hoverComponent = new TextComponentString("");
                        hoverComponent.getStyle().setColor(TextFormatting.GRAY);
                        hoverComponent.appendText(setting.getName());
                        hoverComponent.appendText(String.format("\nType: %s", settingTypeToString(setting)));
                        hoverComponent.appendText(String.format("\n\nValue:\n%s", settingValueToString(setting)));
                        String commandSuggestion = settings.prefix.value + String.format("set %s ", setting.getName());
                        ITextComponent component = new TextComponentString(setting.getName());
                        component.getStyle().setColor(TextFormatting.GRAY);
                        component.appendSibling(typeComponent);
                        component.getStyle()
                                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponent))
                                .setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, commandSuggestion));
                        return component;
                    },
                    FORCE_COMMAND_PREFIX + "set " + arg + " " + search
            );
            return;
        }
        args.requireMax(1);
        boolean resetting = arg.equalsIgnoreCase("reset");
        boolean toggling = arg.equalsIgnoreCase("toggle");
        boolean doingSomething = resetting || toggling;
        if (resetting) {
            if (!args.has()) {
                logDirect("Please specify 'all' as an argument to reset to confirm you'd really like to do this");
                logDirect("ALL settings will be reset. Use the 'set modified' or 'modified' commands to see what will be reset.");
                logDirect("Specify a setting name instead of 'all' to only reset one setting");
            } else if (args.peekString().equalsIgnoreCase("all")) {
                SettingsUtil.modifiedSettings(settings).forEach(Settings.Setting::reset);
                logDirect("All settings have been reset to their default values");
                SettingsUtil.save(settings);
                return;
            }
        }
        if (toggling) {
            args.requireMin(1);
        }
        String settingName = doingSomething ? args.getString() : arg;
        Settings.Setting<?> setting = settings.allSettings.stream()
                .filter(s -> s.getName().equalsIgnoreCase(settingName))
                .findFirst()
                .orElse(null);
        if (setting == null) {
            throw new CommandInvalidTypeException(args.consumed(), "a valid setting");
        }
        if (!doingSomething && !args.has()) {
            logDirect(String.format("Value of setting %s:", setting.getName()));
            logDirect(settingValueToString(setting));
        } else {
            String oldValue = settingValueToString(setting);
            if (resetting) {
                setting.reset();
            } else if (toggling) {
                if (setting.getValueClass() != Boolean.class) {
                    throw new CommandInvalidTypeException(args.consumed(), "a toggleable setting", "some other setting");
                }
                //noinspection unchecked
                ((Settings.Setting<Boolean>) setting).value ^= true;
                logDirect(String.format(
                        "Toggled setting %s to %s",
                        setting.getName(),
                        Boolean.toString((Boolean) setting.value)
                ));
            } else {
                String newValue = args.getString();
                try {
                    SettingsUtil.parseAndApply(settings, arg, newValue);
                } catch (Throwable t) {
                    t.printStackTrace();
                    throw new CommandInvalidTypeException(args.consumed(), "a valid value", t);
                }
            }
            if (!toggling) {
                logDirect(String.format(
                        "Successfully %s %s to %s",
                        resetting ? "reset" : "set",
                        setting.getName(),
                        settingValueToString(setting)
                ));
            }
            ITextComponent oldValueComponent = new TextComponentString(String.format("Old value: %s", oldValue));
            oldValueComponent.getStyle()
                    .setColor(TextFormatting.GRAY)
                    .setHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new TextComponentString("Click to set the setting back to this value")
                    ))
                    .setClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            FORCE_COMMAND_PREFIX + String.format("set %s %s", setting.getName(), oldValue)
                    ));
            logDirect(oldValueComponent);
            if ((setting.getName().equals("chatControl") && !(Boolean) setting.value && !settings.chatControlAnyway.value) ||
                    setting.getName().equals("chatControlAnyway") && !(Boolean) setting.value && !settings.chatControl.value) {
                logDirect("Warning: Chat commands will no longer work. If you want to revert this change, use prefix control (if enabled) or click the old value listed above.", TextFormatting.RED);
            } else if (setting.getName().equals("prefixControl") && !(Boolean) setting.value) {
                logDirect("Warning: Prefixed commands will no longer work. If you want to revert this change, use chat control (if enabled) or click the old value listed above.", TextFormatting.RED);
            }
        }
        SettingsUtil.save(settings);
    }

    @Override
    protected Stream<String> tabCompleted(String label, ArgConsumer args, Settings settings) {
        if (args.has()) {
            String arg = args.getString();
            if (args.hasExactlyOne() && !asList("s", "save").contains(args.peekString().toLowerCase(Locale.US))) {
                if (arg.equalsIgnoreCase("reset")) {
                    return new TabCompleteHelper()
                            .addModifiedSettings()
                            .prepend("all")
                            .filterPrefix(args.getString())
                            .stream();
                } else if (arg.equalsIgnoreCase("toggle")) {
                    return new TabCompleteHelper()
                            .addToggleableSettings()
                            .filterPrefix(args.getString())
                            .stream();
                }
                Settings.Setting setting = settings.byLowerName.get(arg.toLowerCase(Locale.US));
                if (setting != null) {
                    if (setting.getType() == Boolean.class) {
                        TabCompleteHelper helper = new TabCompleteHelper();
                        if ((Boolean) setting.value) {
                            helper.append(of("true", "false"));
                        } else {
                            helper.append(of("false", "true"));
                        }
                        return helper.filterPrefix(args.getString()).stream();
                    } else {
                        return Stream.of(settingValueToString(setting));
                    }
                }
            } else if (!args.has()) {
                return new TabCompleteHelper()
                        .addSettings()
                        .sortAlphabetically()
                        .prepend("list", "modified", "reset", "toggle", "save")
                        .filterPrefix(arg)
                        .stream();
            }
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "View or change settings";
    }

    @Override
    public List<String> getLongDesc() {
        return asList(
                "Using the set command, you can manage all of Baritone's settings. Almost every aspect is controlled by these settings - go wild!",
                "",
                "Usage:",
                "> set - Same as `set list`",
                "> set list [page] - View all settings",
                "> set modified [page] - View modified settings",
                "> set <setting> - View the current value of a setting",
                "> set <setting> <value> - Set the value of a setting",
                "> set reset all - Reset ALL SETTINGS to their defaults",
                "> set reset <setting> - Reset a setting to its default",
                "> set toggle <setting> - Toggle a boolean setting",
                "> set save - Save all settings (this is automatic tho)"
        );
    }
}
