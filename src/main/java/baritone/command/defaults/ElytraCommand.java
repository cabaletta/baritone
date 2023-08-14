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
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static baritone.api.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

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
            if (Baritone.settings().elytraTermsAccepted.value) {
                if (detectOn2b2t()) {
                    warn2b2t();
                }
            } else {
                // only gatekeep if they are standing on the ground, don't mess them up in midair lol
                gatekeep();
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

    private void warn2b2t() {
        if (!Baritone.settings().elytraPredictTerrain.value) {
            if (ctx.playerFeet().distanceSq(0, 0, 0) > 2000 * 2000) {
                TextComponentString clippy = new TextComponentString("");
                clippy.appendText("It looks like you're on 2b2t and more than 2000 blocks from spawn. It is recommended that you ");
                TextComponentString predictTerrain = new TextComponentString("enable elytraPredictTerrain");
                predictTerrain.getStyle().setUnderlined(true).setBold(true).setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(Baritone.settings().prefix.value + "set elytraPredictTerrain true"))).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, FORCE_COMMAND_PREFIX + "set elytraPredictTerrain true"));
                clippy.appendSibling(predictTerrain);
                logDirect(clippy);
            }
        } else {
            long seed = Baritone.settings().elytraNetherSeed.value;
            if (seed != NEW_2B2T_SEED && seed != OLD_2B2T_SEED) {
                logDirect(new TextComponentString("It looks like you're on 2b2t, but elytraNetherSeed is incorrect.")); // match color
                logDirect(suggest2b2tSeeds());
            }
        }
    }

    private ITextComponent suggest2b2tSeeds() {
        TextComponentString clippy = new TextComponentString("");
        clippy.appendText("Within a few hundred blocks of spawn/axis/highways/etc, the terrain is too fragmented to be predictable. Baritone Elytra will still work, just with backtracking. ");
        clippy.appendText("However, once you get more than a few thousand blocks out, you should try ");
        TextComponentString olderSeed = new TextComponentString("the older seed (click here)");
        olderSeed.getStyle().setUnderlined(true).setBold(true).setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(Baritone.settings().prefix.value + "set elytraNetherSeed " + OLD_2B2T_SEED))).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, FORCE_COMMAND_PREFIX + "set elytraNetherSeed " + OLD_2B2T_SEED));
        clippy.appendSibling(olderSeed);
        clippy.appendText(". Once you're further out into newer terrain generation (this includes everything 1.12 and upwards), you should try ");
        TextComponentString newerSeed = new TextComponentString("the newer seed (click here)");
        newerSeed.getStyle().setUnderlined(true).setBold(true).setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(Baritone.settings().prefix.value + "set elytraNetherSeed " + NEW_2B2T_SEED))).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, FORCE_COMMAND_PREFIX + "set elytraNetherSeed " + NEW_2B2T_SEED));
        clippy.appendSibling(newerSeed);
        clippy.appendText(". ");
        return clippy;
    }

    private void gatekeep() {
        TextComponentString gatekeep = new TextComponentString("");
        gatekeep.appendText("To disable this message, enable the setting elytraTermsAccepted\n");
        gatekeep.appendText("Baritone Elytra is an experimental feature. It is only intended for long distance travel in the Nether using fireworks for vanilla boost. It will not work with any other mods (\"hacks\") for non-vanilla boost. ");
        TextComponentString gatekeep2 = new TextComponentString("If you want Baritone to attempt to take off from the ground for you, you can enable the elytraAutoJump setting (not advisable on laggy servers!). ");
        gatekeep2.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(Baritone.settings().prefix.value + "set elytraAutoJump true")));
        gatekeep.appendSibling(gatekeep2);
        TextComponentString gatekeep3 = new TextComponentString("If you want Baritone to go slower, enable the elytraConserveFireworks setting and/or decrease the elytraFireworkSpeed setting. ");
        gatekeep3.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(Baritone.settings().prefix.value + "set elytraConserveFireworks true\n" + Baritone.settings().prefix.value + "set elytraFireworkSpeed 0.6\n(the 0.6 number is just an example, tweak to your liking)")));
        gatekeep.appendSibling(gatekeep3);
        TextComponentString gatekeep4 = new TextComponentString("Baritone Elytra ");
        TextComponentString red = new TextComponentString("wants to know the seed");
        red.getStyle().setColor(TextFormatting.RED).setUnderlined(true).setBold(true);
        gatekeep4.appendSibling(red);
        gatekeep4.appendText(" of the world you are in. If it doesn't have the correct seed, it will frequently backtrack. It uses the seed to generate terrain far beyond what you can see, since terrain obstacles in the Nether can be much larger than your render distance. ");
        gatekeep.appendSibling(gatekeep4);
        gatekeep.appendText("\n");
        if (detectOn2b2t()) {
            TextComponentString gatekeep5 = new TextComponentString("It looks like you're on 2b2t, so elytraPredictTerrain is defaulting to true. ");
            gatekeep5.appendSibling(suggest2b2tSeeds());
            if (Baritone.settings().elytraNetherSeed.value == NEW_2B2T_SEED) {
                gatekeep5.appendText("You are using the newer seed. ");
            } else if (Baritone.settings().elytraNetherSeed.value == OLD_2B2T_SEED) {
                gatekeep5.appendText("You are using the older seed. ");
            } else {
                gatekeep5.appendText("Defaulting to the newer seed. ");
                Baritone.settings().elytraNetherSeed.value = NEW_2B2T_SEED;
            }
            gatekeep.appendSibling(gatekeep5);
        } else {
            if (Baritone.settings().elytraNetherSeed.value == NEW_2B2T_SEED) {
                TextComponentString gatekeep5 = new TextComponentString("Baritone doesn't know the seed of your world. Set it with: " + Baritone.settings().prefix.value + "set elytraNetherSeed seedgoeshere\n");
                gatekeep5.appendText("For the time being, elytraPredictTerrain is defaulting to false since the seed is unknown.");
                gatekeep.appendSibling(gatekeep5);
                Baritone.settings().elytraPredictTerrain.value = false;
            } else {
                if (Baritone.settings().elytraPredictTerrain.value) {
                    TextComponentString gatekeep5 = new TextComponentString("Baritone Elytra is predicting terrain assuming that " + Baritone.settings().elytraNetherSeed.value + " is the correct seed. Change that with " + Baritone.settings().prefix.value + "set elytraNetherSeed seedgoeshere, or disable it with " + Baritone.settings().prefix.value + "set elytraPredictTerrain false");
                    gatekeep.appendSibling(gatekeep5);
                } else {
                    TextComponentString gatekeep5 = new TextComponentString("Baritone Elytra is not predicting terrain. If you don't know the seed, this is the correct thing to do. If you do know the seed, input it with " + Baritone.settings().prefix.value + "set elytraNetherSeed seedgoeshere, and then enable it with " + Baritone.settings().prefix.value + "set elytraPredictTerrain true");
                    gatekeep.appendSibling(gatekeep5);
                }
            }
        }
        logDirect(gatekeep);
    }

    private boolean detect2b2tPlayers() {
        // if any cached worlds are 2b2t worlds
        try (Stream<Path> stream = Files.list(((Baritone) baritone).getDirectory())) {
            return stream.anyMatch(path -> path.getFileName().toString().toLowerCase().contains("2b2t"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean detectOn2b2t() {
        ServerData data = ctx.minecraft().getCurrentServerData();
        return data != null && data.serverIP.toLowerCase().contains("2b2t");
    }

    private static final long OLD_2B2T_SEED = -4100785268875389365L;
    private static final long NEW_2B2T_SEED = 146008555100680L;

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
