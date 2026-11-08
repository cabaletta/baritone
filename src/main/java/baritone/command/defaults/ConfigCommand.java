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
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.exception.CommandException;
import baritone.utils.BaritoneConfigScreen;
import net.minecraft.client.Minecraft;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public final class ConfigCommand extends Command {

    public ConfigCommand(IBaritone baritone) {
        super(baritone, "config", "gui");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(0);
        Minecraft minecraft = Minecraft.getInstance();
        Thread opener = new Thread(() -> {
            try {
                Thread.sleep(100L);
                minecraft.execute(() -> minecraft.setScreen(new BaritoneConfigScreen(null)));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }, "Baritone-Config-Screen");
        opener.setDaemon(true);
        opener.start();
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Open the Baritone settings screen";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Opens an in-game screen with common movement, safety, and path-search settings.",
                "Changes are saved automatically to Baritone's settings file.",
                "",
                "Usage:",
                "> config",
                "> gui"
        );
    }
}
