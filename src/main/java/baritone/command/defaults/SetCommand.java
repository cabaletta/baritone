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
import baritone.api.Settings;
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.datatypes.RelativeFile;
import baritone.api.command.exception.CommandException;
import baritone.api.command.exception.CommandInvalidStateException;
import baritone.api.command.exception.CommandInvalidTypeException;
import baritone.api.command.helpers.Paginator;
import baritone.api.command.helpers.TabCompleteHelper;
import baritone.api.utils.SettingsUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static baritone.api.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;
import static baritone.api.utils.SettingsUtil.*;

public class SetCommand extends Command {

    public SetCommand(IBaritone baritone) {
        super(baritone, "set", "setting", "settings");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        String arg = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
        if (Arrays.asList("s", "save").contains(arg)) {
            SettingsUtil.save(Baritone.settings());
            logDirect("Settings saved");
            return;
        }
        if (Arrays.asList("load", "ld").contains(arg)) {
            String file = SETTINGS_DEFAULT_NAME;
            if (args.hasAny()) {
                file = args.getString();
            }
            // reset to defaults
            SettingsUtil.modifiedSettings(Baritone.settings()).forEach(Settings.Setting::reset);
            // then load from disk
            SettingsUtil.readAndApply(Baritone.settings(), file);
            logDirect("Settings reloaded from " + file);
            return;
        }
        boolean viewModified = Arrays.asList("m", "mod", "modified").contains(arg);
        boolean viewAll = Arrays.asList("all", "l", "list").contains(arg);
        boolean paginate = viewModified || viewAll;
        if (paginate) {
            String search = args.hasAny() && args.peekAsOrNull(Integer.class) == null ? args.getString() : "";
            args.requireMax(1);
            List<? extends Settings.Setting> toPaginate =
                    (viewModified ? SettingsUtil.modifiedSettings(Baritone.settings()) : Baritone.settings().allSettings).stream()
                            .filter(s -> !s.isJavaOnly())
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
                        ITextComponent typeComponent = new StringTextComponent(String.format(
                                " (%s)",
                                settingTypeToString(setting)
                        ));
                        typeComponent.getStyle().setColor(TextFormatting.DARK_GRAY);
                        ITextComponent hoverComponent = new StringTextComponent("");
                        hoverComponent.getStyle().setColor(TextFormatting.GRAY);
                        hoverComponent.appendText(setting.getName());
                        hoverComponent.appendText(String.format("\nType: %s", settingTypeToString(setting)));
                        hoverComponent.appendText(String.format("\n\nValue:\n%s", settingValueToString(setting)));
                        hoverComponent.appendText(String.format("\n\nDefault Value:\n%s", settingDefaultToString(setting)));
                        String commandSuggestion = Baritone.settings().prefix.value + String.format("set %s ", setting.getName());
                        ITextComponent component = new StringTextComponent(setting.getName());
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
            if (!args.hasAny()) {
                logDirect("Please specify 'all' as an argument to reset to confirm you'd really like to do this");
                logDirect("ALL settings will be reset. Use the 'set modified' or 'modified' commands to see what will be reset.");
                logDirect("Specify a setting name instead of 'all' to only reset one setting");
            } else if (args.peekString().equalsIgnoreCase("all")) {
                SettingsUtil.modifiedSettings(Baritone.settings()).forEach(Settings.Setting::reset);
                logDirect("All settings have been reset to their default values");
                SettingsUtil.save(Baritone.settings());
                return;
            }
        }
        if (toggling) {
            args.requireMin(1);
        }
        String settingName = doingSomething ? args.getString() : arg;
        Settings.Setting<?> setting = Baritone.settings().allSettings.stream()
                .filter(s -> s.getName().equalsIgnoreCase(settingName))
                .findFirst()
                .orElse(null);
        if (setting == null) {
            throw new CommandInvalidTypeException(args.consumed(), "a valid setting");
        }
        if (setting.isJavaOnly()) {
            // ideally it would act as if the setting didn't exist
            // but users will see it in Settings.java or its javadoc
            // so at some point we have to tell them or they will see it as a bug
            throw new CommandInvalidStateException(String.format("Setting %s can only be used via the api.", setting.getName()));
        }
        if (!doingSomething && !args.hasAny()) {
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
                Settings.Setting<Boolean> asBoolSetting = (Settings.Setting<Boolean>) setting;
                asBoolSetting.value ^= true;
                logDirect(String.format(
                        "Toggled setting %s to %s",
                        setting.getName(),
                        Boolean.toString((Boolean) setting.value)
                ));
            } else {
                String newValue = args.getString();
                try {
                    SettingsUtil.parseAndApply(Baritone.settings(), arg, newValue);
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
            ITextComponent oldValueComponent = new StringTextComponent(String.format("Old value: %s", oldValue));
            oldValueComponent.getStyle()
                    .setColor(TextFormatting.GRAY)
                    .setHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new StringTextComponent("Click to set the setting back to this value")
                    ))
                    .setClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            FORCE_COMMAND_PREFIX + String.format("set %s %s", setting.getName(), oldValue)
                    ));
            logDirect(oldValueComponent);
            if ((setting.getName().equals("chatControl") && !(Boolean) setting.value && !Baritone.settings().chatControlAnyway.value) ||
                    setting.getName().equals("chatControlAnyway") && !(Boolean) setting.value && !Baritone.settings().chatControl.value) {
                logDirect("Warning: Chat commands will no longer work. If you want to revert this change, use prefix control (if enabled) or click the old value listed above.", TextFormatting.RED);
            } else if (setting.getName().equals("prefixControl") && !(Boolean) setting.value) {
                logDirect("Warning: Prefixed commands will no longer work. If you want to revert this change, use chat control (if enabled) or click the old value listed above.", TextFormatting.RED);
            }
        }
        SettingsUtil.save(Baritone.settings());
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasAny()) {
            String arg = args.getString();
            if (args.hasExactlyOne() && !Arrays.asList("s", "save").contains(args.peekString().toLowerCase(Locale.US))) {
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
                } else if (Arrays.asList("ld", "load").contains(arg.toLowerCase(Locale.US))) {
                    // settings always use the directory of the main Minecraft instance
                    return RelativeFile.tabComplete(args, Minecraft.getInstance().gameDir.toPath().resolve("baritone").toFile());
                }
                Settings.Setting setting = Baritone.settings().byLowerName.get(arg.toLowerCase(Locale.US));
                if (setting != null) {
                    if (setting.getType() == Boolean.class) {
                        TabCompleteHelper helper = new TabCompleteHelper();
                        if ((Boolean) setting.value) {
                            helper.append("true", "false");
                        } else {
                            helper.append("false", "true");
                        }
                        return helper.filterPrefix(args.getString()).stream();
                    } else {
                        return Stream.of(settingValueToString(setting));
                    }
                }
            } else if (!args.hasAny()) {
                return new TabCompleteHelper()
                        .addSettings()
                        .sortAlphabetically()
                        .prepend("list", "modified", "reset", "toggle", "save", "load")
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
        return Arrays.asList(
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
                "> set save - Save all settings (this is automatic tho)",
                "> set load - Load settings from settings.txt",
                "> set load [filename] - Load settings from another file in your minecraft/baritone"
        );
    }
}
