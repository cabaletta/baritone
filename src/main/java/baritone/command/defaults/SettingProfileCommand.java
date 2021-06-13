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
import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.exception.CommandException;
import baritone.api.command.helpers.TabCompleteHelper;
import baritone.api.utils.SettingsUtil;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class SettingProfileCommand extends Command {

    public SettingProfileCommand(IBaritone baritone) {
        super(baritone, "settingProfiles", "settingProfile");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(2);

        if (args.hasAny() && args.peek().getValue().equals("create")) {
            args.get();
            SettingsUtil.createProfile(args.getString(), BaritoneAPI.getSettings());
        } else if (args.hasExactlyOne()) { // we want to set the profile
            String profileName = args.getString();
            if (SettingsUtil.applyProfile(profileName.equals("default") ? "" : profileName, BaritoneAPI.getSettings())) {
                HELPER.logDirect("Profile applied");
            } else {
                ITextComponent component = new TextComponentString("Profile doesn't exist - click to create it");
                component.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, Baritone.settings().prefix.value + "settingProfile create " + profileName));
                HELPER.logDirect(component);
            }
        } else { // Just list the profiles
            List<String> profiles = SettingsUtil.getProfiles();

            for (String profile: profiles) {
                TextComponentString component = new TextComponentString(profile);
                component.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, Baritone.settings().prefix.value + "settingProfile " + profile));
                component.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Switch to this config")));
                HELPER.logDirect(component);
            }
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.getArgs().size() > 1) {
            return Stream.empty();
        }
        List<String> possibleCommands = SettingsUtil.getProfiles();
        possibleCommands.add("create");
        return possibleCommands.stream();
    }

    @Override
    public String getShortDesc() {
        return "Manage your setting profiles";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Manage your setting profiles",
                "",
                "Usage",
                "> settingProfile - list setting profiles",
                "> settingProfile <profile> - switch to another profile",
                "> settingProfile create <profileName> - creates a new profile based on current settings"
        );
    }
}
