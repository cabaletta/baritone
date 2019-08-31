package baritone.api.utils.command.defaults;

import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import baritone.api.process.IBaritoneProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.command.Command;
import baritone.api.utils.command.exception.CommandInvalidStateException;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

/**
 * Contains the pause, resume, and paused commands.
 *
 * This thing is scoped to hell, private so far you can't even access it using reflection, because you AREN'T SUPPOSED
 * TO USE THIS to pause and resume Baritone. Make your own process that returns {@link PathingCommandType#REQUEST_PAUSE
 * REQUEST_PAUSE} as needed.
 */
public class PauseResumeCommands {
    public static Command pauseCommand;
    public static Command resumeCommand;
    public static Command pausedCommand;

    static {
        // array for mutability, non-field so reflection can't touch it
        final boolean[] paused = {false};

        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingControlManager().registerProcess(
            new IBaritoneProcess() {
                @Override
                public boolean isActive() {
                    return paused[0];
                }

                @Override
                public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
                    return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
                }

                @Override
                public boolean isTemporary() {
                    return true;
                }

                @Override
                public void onLostControl() {
                }

                @Override
                public double priority() {
                    return DEFAULT_PRIORITY + 1;
                }

                @Override
                public String displayName0() {
                    return "Pause/Resume Commands";
                }
            }
        );

        pauseCommand = new Command("pause", "Pauses Baritone until you use resume") {
            @Override
            protected void executed(String label, ArgConsumer args, Settings settings) {
                args.requireMax(0);

                if (paused[0]) {
                    throw new CommandInvalidStateException("Already paused");
                }

                paused[0] = true;
                logDirect("Paused");
            }

            @Override
            protected Stream<String> tabCompleted(String label, ArgConsumer args, Settings settings) {
                return Stream.empty();
            }

            @Override
            public List<String> getLongDesc() {
                return asList(
                    "The pause command tells Baritone to temporarily stop whatever it's doing.",
                    "",
                    "This can be used to pause pathing, building, following, whatever. A single use of the resume command will start it right back up again!",
                    "",
                    "Usage:",
                    "> pause"
                );
            }
        };

        resumeCommand = new Command("resume", "Resumes Baritone after a pause") {
            @Override
            protected void executed(String label, ArgConsumer args, Settings settings) {
                args.requireMax(0);

                if (!paused[0]) {
                    throw new CommandInvalidStateException("Not paused");
                }

                paused[0] = false;
                logDirect("Resumed");
            }

            @Override
            protected Stream<String> tabCompleted(String label, ArgConsumer args, Settings settings) {
                return Stream.empty();
            }

            @Override
            public List<String> getLongDesc() {
                return asList(
                    "The resume command tells Baritone to resume whatever it was doing when you last used pause.",
                    "",
                    "Usage:",
                    "> resume"
                );
            }
        };

        pausedCommand = new Command("paused", "Tells you if Baritone is paused") {
            @Override
            protected void executed(String label, ArgConsumer args, Settings settings) {
                args.requireMax(0);

                logDirect(String.format("Baritone is %spaused", paused[0] ? "" : "not "));
            }

            @Override
            protected Stream<String> tabCompleted(String label, ArgConsumer args, Settings settings) {
                return Stream.empty();
            }

            @Override
            public List<String> getLongDesc() {
                return asList(
                    "The paused command tells you if Baritone is currently paused by use of the pause command.",
                    "",
                    "Usage:",
                    "> paused"
                );
            }
        };
    }
}
