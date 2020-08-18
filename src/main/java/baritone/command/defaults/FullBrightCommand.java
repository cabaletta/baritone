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
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.argument.ICommandArgument;
import baritone.api.command.exception.CommandException;
import baritone.api.utils.Helper;
import net.minecraft.client.settings.GameSettings;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class FullBrightCommand extends Command {
    /**
     * full bright command, turns on type of night vision.
     *
     * @param baritone give the current baritone.
     */
    protected FullBrightCommand(IBaritone baritone) {
        super(baritone, "fullBright");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(1);

        if (args.has(1)) {
            ICommandArgument iarg = args.getArgs().get(1);
            approachGamma(Double.parseDouble(iarg.getValue()));
        } else {
            GameSettings options = Helper.mc.gameSettings;
            if (options.gammaSetting == Baritone.settings().fullBrightLevel.value) {
                approachGamma(0.5);
            } else {
                approachGamma(Baritone.settings().fullBrightLevel.value);
            }
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return null;
    }

    @Override
    public String getShortDesc() {
        return "turns on night vision";
    }

    @Override
    public List<String> getLongDesc() {
        return
                Arrays.asList(
                        "This turns on night vision through gamma.",
                        "",
                        "you can also specify a number to set the gamma to.",
                        "",
                        "Usage:",
                        "> fullbright - toggle fullbright on or off",
                        "> fullbright <gamma> - set gamma"
                );
    }


    private void approachGamma(double target) {
        GameSettings options = Helper.mc.gameSettings;


        options.gammaSetting = (float) target;


    }
}
