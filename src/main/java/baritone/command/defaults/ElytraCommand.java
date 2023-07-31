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
import baritone.api.command.exception.CommandException;
import baritone.api.command.exception.CommandInvalidStateException;
import baritone.api.command.helpers.TabCompleteHelper;
import baritone.api.pathing.goals.Goal;
import baritone.api.process.ICustomGoalProcess;
import baritone.api.process.IElytraProcess;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class ElytraCommand extends Command {

    public ElytraCommand(IBaritone baritone) {
        super(baritone, "elytra");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        final ICustomGoalProcess customGoalProcess = baritone.getCustomGoalProcess();
        final IElytraProcess elytra = baritone.getElytraProcess();
        if (args.hasExactlyOne() && args.peekString().equals("supported")) {
            logDirect(elytra.isLoaded() ? "yes" : unsupportedSystemMessage());
            return;
        }
        if (!elytra.isLoaded()) {
            throw new CommandInvalidStateException(unsupportedSystemMessage());
        }

        if (!args.hasAny()) {
            if (!Baritone.settings().elytraTermsAccepted.value && !ctx.player().isElytraFlying()) {
                // only gatekeep if they are standing on the ground, don't mess them up in midair lol
                gatekeep();
                return;
            }
            Goal iGoal = customGoalProcess.mostRecentGoal();
            if (iGoal == null) {
                throw new CommandInvalidStateException("No goal has been set");
            }
            if (ctx.player().dimension != -1) {
                throw new CommandInvalidStateException("Only works in the nether");
            }
            try {
                elytra.pathTo(iGoal);
            } catch (IllegalArgumentException ex) {
                throw new CommandInvalidStateException(ex.getMessage());
            }
            return;
        }

        final String action = args.getString();
        switch (action) {
            case "reset": {
                elytra.resetState();
                logDirect("Reset state but still flying to same goal");
                break;
            }
            case "repack": {
                elytra.repackChunks();
                logDirect("Queued all loaded chunks for repacking");
                break;
            }
            default: {
                throw new CommandInvalidStateException("Invalid action");
            }
        }
    }

    private void gatekeep() {
        TextComponentString gatekeep = new TextComponentString("");
        gatekeep.appendText("Once you've read the below, and entered the seed, run ");
        TextComponentString cmd = new TextComponentString(Baritone.settings().prefix.value + "set elytraTermsAccepted true");
        cmd.getStyle().setColor(TextFormatting.GRAY);
        gatekeep.appendSibling(cmd);
        gatekeep.appendText(" and then try again.\n");
        gatekeep.appendText("Baritone Elytra is an experimental feature. It is only intended for long distance travel in the Nether using fireworks for vanilla boost. It will not work with any other mods (\"hacks\") for non-vanilla boost. ");
        TextComponentString gatekeep2 = new TextComponentString("If you want Baritone to attempt to take off from the ground for you, you can enable the elytraAutoJump setting. This may not be advisable on laggy servers. ");
        gatekeep2.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(Baritone.settings().prefix.value + "set elytraAutoJump true")));
        gatekeep.appendSibling(gatekeep2);
        TextComponentString gatekeep3 = new TextComponentString("If you want Baritone to go slower and use less fireworks, enable the elytraConserveFireworks setting and/or decrease the elytraFireworkSpeed setting. ");
        gatekeep3.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(Baritone.settings().prefix.value + "set elytraConserveFireworks true\n" + Baritone.settings().prefix.value + "set elytraFireworkSpeed 0.6\n(the 0.6 number is just an example, tweak to your liking)")));
        gatekeep.appendSibling(gatekeep3);
        TextComponentString gatekeep4 = new TextComponentString("Baritone Elytra ");
        TextComponentString red = new TextComponentString("needs to know the seed");
        red.getStyle().setColor(TextFormatting.RED).setUnderlined(true).setBold(true);
        gatekeep4.appendSibling(red);
        gatekeep4.appendText(" of the world you are in. If it doesn't have the correct seed, it will constantly frustratingly recalculate and backtrack. It uses the seed to generate terrain far beyond what you can see, since terrain obstacles in the Nether can be much larger than your render distance. ");
        gatekeep.appendSibling(gatekeep4);
        TextComponentString gatekeep5 = new TextComponentString("If you're on 2b2t, no need to change anything, since its seed is the default. Otherwise, set it with: " + Baritone.settings().prefix.value + "set elytraNetherSeed seedgoeshere");
        gatekeep5.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("2b2t's nether seed is 146008555100680, so for example you would run\n" + Baritone.settings().prefix.value + "set elytraNetherSeed 146008555100680\n\nAlso, if you're on 2b2t, note that the Nether near spawn is old terrain gen, so you'll see recalculating and backtracking there.\nIt'll only work well further away from spawn/highways.")));
        gatekeep.appendSibling(gatekeep5);
        logDirect(gatekeep);
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        TabCompleteHelper helper = new TabCompleteHelper();
        if (args.hasExactlyOne()) {
            helper.append("reset", "repack", "supported");
        }
        return helper.filterPrefix(args.getString()).stream();
    }

    @Override
    public String getShortDesc() {
        return "elytra time";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "The elytra command tells baritone to, in the nether, automatically fly to the current goal.",
                "",
                "Usage:",
                "> elytra - fly to the current goal",
                "> elytra reset - Resets the state of the process, but will try to keep flying to the same goal.",
                "> elytra repack - Queues all of the chunks in render distance to be given to the native library.",
                "> elytra supported - Tells you if baritone ships a native library that is compatible with your PC."
        );
    }

    private static String unsupportedSystemMessage() {
        final String osArch = System.getProperty("os.arch");
        final String osName = System.getProperty("os.name");
        return String.format(
                "Legacy architectures are not supported. Your CPU is %s and your operating system is %s. " +
                        "Supported architectures are 64 bit x86, and 64 bit ARM. Supported operating systems are Windows, " +
                        "Linux, and Mac",
                osArch, osName
        );
    }
}
