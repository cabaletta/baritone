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

package baritone.api.command.exception;

import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import baritone.api.command.ICommand;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.argument.ICommandArgument;
import baritone.api.utils.SettingsUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import org.lwjgl.opengl.Display;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static baritone.api.utils.Helper.HELPER;

public class CommandUnhandledException extends RuntimeException implements ICommandException {

    public CommandUnhandledException(String message) {
        super(message);
    }

    public CommandUnhandledException(Throwable cause) {
        super(cause);
    }

    @Override
    public void handle(ICommand command, IArgConsumer args) {
        StringBuilder sb = new StringBuilder("https://github.com/cabaletta/baritone/issues/new/?title=");
        StringBuilder title = new StringBuilder();
        if (getCause() != null) {
            title.append(getCause().getClass().getSimpleName());
        } else {
            title.append("CommandUnhandledException");
        }
        title.append(" while executing `#");
        title.append(command.getNames().get(0));
        for (ICommandArgument arg : args.getConsumed()) {
            title.append(" ").append(arg.getValue());
        }
        for (ICommandArgument arg : args.getArgs()) {
            title.append(" ").append(arg.getValue());
        }
        title.append("`");

        StringWriter sw = new StringWriter();
        printStackTrace(new java.io.PrintWriter(sw));
        sw.flush();

        try {
            sb.append(URLEncoder.encode(title.toString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        sb.append("&labels=bug&template=bug.md&body=");

        StringBuilder body = new StringBuilder("## Some information").append("\n")
            .append("Operating system: ").append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.version")).append("\n")
            .append("Java version: ").append(System.getProperty("java.version")).append("\n")
            .append("Minecraft version: ").append(Minecraft.getMinecraft().getVersion()).append(" ").append(Display.getTitle()).append("\n")
            .append("Baritone version: ").append(getClass().getPackage().getImplementationVersion()).append("\n")
            .append("Other mods (if used): ").append("<--please fill out your mods here-->").append("\n\n")

            .append("## Exception, error or logs").append("\n")
            .append("<details><summary>stacktrace</summary><pre>\n").append(sw).append("\n</pre></details>").append("\n\n")

            .append("## How to reproduce").append("\n")
            .append("<--Add your steps to reproduce the issue/bug experienced here.-->").append("\n\n")

            .append("## Modified settings").append("\n");

        for (Settings.Setting<?> setting : SettingsUtil.modifiedSettings(BaritoneAPI.getSettings())) {
            body.append("* `").append(setting.getName()).append("`: `").append(SettingsUtil.settingValueToString(setting)).append("`").append("\n");
        }
        body.append("\n\n")
            .append("## Final checklist").append("\n")
            .append("- [x] I know how to properly use check boxes ").append("\n")
            .append("- [x] I have included the version of Minecraft I'm running, baritone's version and forge mods (if used).").append("\n")
            .append("- [x] I have included logs, exceptions and / or steps to reproduce the issue.").append("\n")
            .append("- [ ] I have not used any OwO's or UwU's in this issue.").append("\n")
            .append("- [ ] I have filled in any useful information the autegenerated issue was missing").append("\n");

        try {
            sb.append(URLEncoder.encode(body.toString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        ITextComponent component = new TextComponentString("An unhandled exception occurred. The error is in your game's log, please report this at ");
        component.getStyle().setColor(TextFormatting.RED);
        ITextComponent link = new TextComponentString("https://github.com/cabaletta/baritone/issues");
        link.getStyle().setColor(TextFormatting.GREEN);
        link.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, sb.toString()));
        link.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Click to report the issue")));
        component.appendSibling(link);

        HELPER.logDirect(component);

        this.printStackTrace();
    }
}
