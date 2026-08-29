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

package baritone.behavior;

import baritone.Baritone;
import baritone.api.BaritoneAPI;
import baritone.api.event.events.PathEvent;
import baritone.api.event.events.TickEvent;
import baritone.api.event.events.WorldEvent;
import baritone.api.event.events.type.EventState;
import baritone.api.process.IBaritoneProcess;
import baritone.api.utils.Helper;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Records the most recent task-starting command and re-executes it after a non-user-initiated interruption.
 * <p>
 * This continues long running tasks after being disconnected from the server (issue #1007), and optionally
 * after Baritone cancelled a task on its own because path calculation kept failing on unloaded chunks
 * (issue #1022). The command line is remembered rather than the process itself, because process state holds
 * world-dependent objects that go stale on disconnect, while the command re-derives everything it needs
 * (position, facing direction, inventory) from the world at execution time. The server preserves the player's
 * rotation across a reconnect, so direction dependent commands like {@code tunnel} continue the same way.
 */
public class ResumeBehavior extends Behavior implements Helper {

    /**
     * Commands that start a task worth resuming.
     */
    private static final Set<String> TASK_COMMANDS = new HashSet<>(Arrays.asList(
            "tunnel", "mine", "goto", "farm", "follow", "explore", "come",
            "thisway", "forward", "axis", "highway", "surface", "top", "litematica", "build"
    ));

    /**
     * Commands that only set a goal without starting to path towards it, so a resumed task has to be
     * followed by {@code path} for movement to actually start.
     */
    private static final Set<String> GOAL_ONLY_COMMANDS = new HashSet<>(Arrays.asList(
            "thisway", "forward", "axis", "highway"
    ));

    /**
     * Commands that signal the user changed their mind about what Baritone should be doing. These drop both
     * a pending resume and the saved command, so a stale task never fires after the new intent.
     */
    private static final Set<String> SUPERSEDING_COMMANDS = new HashSet<>(Arrays.asList(
            "goal", "invert"
    ));

    /**
     * Commands that stop Baritone. These drop a pending resume, but keep the saved command so that
     * {@code #resumelast} can still be used deliberately.
     */
    private static final Set<String> CANCELLING_COMMANDS = new HashSet<>(Arrays.asList(
            "cancel", "c", "stop", "forcecancel"
    ));

    /**
     * How long after the user ran a cancel command an abort is still considered user-initiated even if a path
     * calculation had just failed, since the two often coincide when a task runs into unloaded chunks.
     */
    private static final int USER_CANCEL_GRACE_TICKS = 100;

    /**
     * How many ticks a task process may have been inactive at most when the world unloads for a disconnect
     * to count as interrupting it. Ticks stop advancing while the client is frozen, so this window is safe
     * even when the disconnect itself takes a while.
     */
    private static final int RECENTLY_ACTIVE_TICKS = 10;

    /**
     * How many ticks before the process in control gives up a {@link PathEvent#CALC_FAILED} still counts as
     * the cause. A tick of slack is needed for processes that return a command on the tick they fail and
     * only disappear from control on the next one.
     */
    private static final int CALC_FAILURE_WINDOW_TICKS = 2;

    /**
     * The backoff of {@code resumeAfterCalcFailure} retries is capped at this many ticks (20 minutes).
     */
    private static final int MAX_BACKOFF_TICKS = 24000;

    private String lastTaskCommand;
    private String lastTaskServerId;
    private String pendingResumeCommand;
    private String pendingResumeServerId;
    private int resumeTicksRemaining = -1; // -1 = armed with no countdown yet, otherwise counting down
    private int calcFailureResumeAttempts;
    private int userCancelledAtTick = -1000;
    private int lastCalcFailureTick = -1000;
    private int lastTaskActiveTick = -1000;
    private int tickCounter;
    private boolean playerWasDead;
    private boolean executingAutoResume;
    private IBaritoneProcess inControlPreviousTick;
    private Level lastTickWorld;
    private boolean sawConnectionLossWhileWorldNull;

    public ResumeBehavior(Baritone baritone) {
        super(baritone);
    }

    /**
     * Called by {@code CommandManager} for every executed command. Records task-starting commands and
     * handles the commands that cancel or supersede a resume.
     *
     * @param rawCommand the executed command as it was typed, without the prefix
     */
    public void recordCandidate(String rawCommand) {
        if (rawCommand == null) {
            return;
        }
        String trimmed = rawCommand.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        String label = trimmed.split("\\s+", 2)[0].toLowerCase(Locale.US);
        if (CANCELLING_COMMANDS.contains(label)) {
            this.clearPendingResume();
            this.userCancelledAtTick = this.tickCounter;
            return;
        }
        if (SUPERSEDING_COMMANDS.contains(label)) {
            this.clearPendingResume();
            this.lastTaskCommand = null;
            this.lastTaskServerId = null;
            return;
        }
        if (!TASK_COMMANDS.contains(label)) {
            return;
        }
        if (label.equals("build") && !isResumableBuildCommand(trimmed)) {
            return;
        }
        this.lastTaskCommand = trimmed;
        this.lastTaskServerId = this.currentServerId();
        if (this.executingAutoResume) {
            this.executingAutoResume = false; // re-recording our own retry must not reset the attempt count
        } else {
            this.calcFailureResumeAttempts = 0;
        }
        this.clearPendingResume();
    }

    /**
     * A {@code build} command is only safe to resume if it includes absolute coordinates. Re-running
     * {@code build <file>} or a build with relative coordinates after reconnecting would start a second
     * copy of the schematic at the player's new position instead of continuing the old one.
     */
    private static boolean isResumableBuildCommand(String rawCommand) {
        String[] tokens = rawCommand.split("\\s+");
        if (tokens.length < 5) { // build <file> <x> <y> <z>
            return false;
        }
        for (int i = 2; i < 5; i++) {
            if (tokens[i].startsWith("~")) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onTick(TickEvent event) {
        this.detectDisconnect();
        if (event.getType() != TickEvent.Type.IN) {
            return;
        }
        this.tickCounter++;
        if (ctx.player() != null) {
            this.playerWasDead = !ctx.player().isAlive();
        }
        if (this.anyTaskProcessActive()) {
            this.lastTaskActiveTick = this.tickCounter;
        }
        this.detectCalcFailureAbort();
        if (this.pendingResumeCommand == null) {
            return;
        }
        if (this.resumeTicksRemaining < 0) {
            this.startCountdown();
        } else if (this.resumeTicksRemaining > 0) {
            this.resumeTicksRemaining--;
        } else if (this.readyToFire()) {
            this.fireResume();
        }
    }

    /**
     * Detects the world unloading by comparing the world object across ticks instead of listening for
     * WorldEvent(s), because some servers and mods (proxy reconnects, replaymod, etc.) break those events.
     * A dimension change also unloads the world briefly, but keeps the server connection, so only an
     * unload during which the connection actually dropped counts as a disconnect.
     */
    private void detectDisconnect() {
        Level world = ctx.world();
        if (world == null) {
            if (this.lastTickWorld != null) {
                this.sawConnectionLossWhileWorldNull |= ctx.minecraft().getConnection() == null;
            }
        } else if (this.lastTickWorld == null && this.sawConnectionLossWhileWorldNull) {
            // the world returned after leaving it without the server connection; a real disconnect + rejoin
            this.armResumeOnReconnect();
            this.sawConnectionLossWhileWorldNull = false;
        }
        this.lastTickWorld = world;
    }

    private void startCountdown() {
        // first in-world tick after the disconnect that armed this resume
        this.resumeTicksRemaining = Math.max(0, Baritone.settings().resumeDelayTicks.value);
        logDirect(String.format(
                "Resuming \"%s\" in %.1f seconds. Use %scancel to stop it.",
                this.pendingResumeCommand,
                this.resumeTicksRemaining / 20.0,
                BaritoneAPI.getSettings().prefix.value
        ), ChatFormatting.GRAY);
    }

    /**
     * @return whether the pending resume may fire this tick, logging the reason and disarming it if not
     */
    private boolean readyToFire() {
        if (ctx.player() == null || ctx.world() == null) {
            return false; // not actually in a world yet (e.g. sitting in a join queue); keep waiting
        }
        if (this.playerWasDead) {
            return false; // onPlayerDeath clears the pending resume; don't act while dead regardless
        }
        String serverNow = this.currentServerId();
        // proxy servers can briefly report no server while the world is already loaded; only reject on a
        // provable mismatch, never on a null (unknown) identity
        if (serverNow != null && this.pendingResumeServerId != null
                && !Objects.equals(this.pendingResumeServerId, serverNow)) {
            logDirect(String.format(
                    "Not resuming \"%s\" because a different server or world was joined.",
                    this.pendingResumeCommand
            ), ChatFormatting.RED);
            this.clearPendingResume();
            return false;
        }
        if (this.anyTaskProcessActive()) {
            logDirect(String.format(
                    "Not resuming \"%s\" because another task is already active.",
                    this.pendingResumeCommand
            ), ChatFormatting.RED);
            this.clearPendingResume();
            return false;
        }
        return true;
    }

    private void fireResume() {
        String command = this.pendingResumeCommand;
        boolean isAutoResume = this.calcFailureResumeAttempts > 0;
        this.clearPendingResume();
        if (isAutoResume) {
            this.executingAutoResume = true;
        }
        logDirect(String.format("Resuming: %s", command), ChatFormatting.GRAY);
        baritone.getCommandManager().execute(command);
        if (GOAL_ONLY_COMMANDS.contains(command.split("\\s+", 2)[0].toLowerCase(Locale.US))) {
            baritone.getCommandManager().execute("path");
        }
    }

    @Override
    public void onPathEvent(PathEvent event) {
        if (event == PathEvent.CALC_FAILED) {
            this.lastCalcFailureTick = this.tickCounter;
        }
    }

    @Override
    public void onWorldEvent(WorldEvent event) {
        if (event.getState() == EventState.POST && event.getWorld() == null) {
            // same arming path as the tick-based detection; both may fire, so this must be idempotent
            this.armResumeOnReconnect();
        }
    }

    /**
     * Arms a pending resume if a task was interrupted by the world unloading under it. Must be safe to call
     * multiple times for the same disconnect.
     */
    private void armResumeOnReconnect() {
        if (!Baritone.settings().resumeOnReconnect.value
                || this.playerWasDead
                || this.lastTaskCommand == null
                || this.pendingResumeCommand != null
                || this.tickCounter - this.lastTaskActiveTick > RECENTLY_ACTIVE_TICKS
                || this.tickCounter - this.userCancelledAtTick <= RECENTLY_ACTIVE_TICKS) {
            return;
        }
        this.pendingResumeCommand = this.lastTaskCommand;
        this.pendingResumeServerId = this.lastTaskServerId;
        this.resumeTicksRemaining = -1; // the countdown starts once we are back in a world
        this.calcFailureResumeAttempts = 0; // a fresh connection gets a fresh retry budget
        logDirect(String.format(
                "Ready to resume \"%s\" after reconnecting.",
                this.pendingResumeCommand
        ), ChatFormatting.GRAY);
    }

    @Override
    public void onPlayerDeath() {
        this.clearPendingResume();
    }

    /**
     * Arms a resume when the process that was in control gave up by itself right after a path calculation
     * failure, which is what happens when a task runs into unloaded chunks. A doubling delay and
     * {@code resumeMaxAttempts} bound the retries so a genuinely impossible task doesn't loop forever.
     */
    private void detectCalcFailureAbort() {
        IBaritoneProcess inControlNow = baritone.getPathingControlManager().mostRecentInControl().orElse(null);
        if (Baritone.settings().resumeAfterCalcFailure.value
                && this.tickCounter - this.lastCalcFailureTick <= CALC_FAILURE_WINDOW_TICKS
                && inControlNow == null
                && this.inControlPreviousTick != null
                && !this.inControlPreviousTick.isTemporary()
                && !this.inControlPreviousTick.isActive()
                && this.tickCounter - this.userCancelledAtTick >= USER_CANCEL_GRACE_TICKS
                && this.lastTaskCommand != null
                && this.calcFailureResumeAttempts < Baritone.settings().resumeMaxAttempts.value) {
            int delay = Math.max(0, Baritone.settings().resumeDelayTicks.value);
            for (int i = 0; i < Math.min(this.calcFailureResumeAttempts, 16) && delay < MAX_BACKOFF_TICKS; i++) {
                delay = (int) Math.min((long) delay * 2, MAX_BACKOFF_TICKS);
            }
            this.pendingResumeCommand = this.lastTaskCommand;
            this.pendingResumeServerId = this.lastTaskServerId;
            this.resumeTicksRemaining = delay;
            this.calcFailureResumeAttempts++;
            logDirect(String.format(
                    "Task \"%s\" aborted after a path calculation failure. Retrying (attempt %d/%d) in %.1f seconds.",
                    this.pendingResumeCommand,
                    this.calcFailureResumeAttempts,
                    Baritone.settings().resumeMaxAttempts.value,
                    delay / 20.0
            ), ChatFormatting.GRAY);
        }
        this.inControlPreviousTick = inControlNow;
    }

    private boolean anyTaskProcessActive() {
        return baritone.getCustomGoalProcess().isActive()
                || baritone.getBuilderProcess().isActive()
                || baritone.getMineProcess().isActive()
                || baritone.getGetToBlockProcess().isActive()
                || baritone.getFollowProcess().isActive()
                || baritone.getExploreProcess().isActive()
                || baritone.getFarmProcess().isActive()
                || baritone.getElytraProcess().isActive();
    }

    /**
     * @return an identifier of the currently connected server or singleplayer world, or {@code null} if there
     * is none. Used to make sure a pending resume only fires where the task was running.
     */
    private String currentServerId() {
        if (ctx.minecraft().getSingleplayerServer() != null) {
            return "singleplayer:" + ctx.minecraft().getSingleplayerServer().getWorldPath(LevelResource.ROOT);
        }
        return ctx.minecraft().getCurrentServer() == null ? null : ctx.minecraft().getCurrentServer().ip;
    }

    public void clearPendingResume() {
        this.pendingResumeCommand = null;
        this.pendingResumeServerId = null;
        this.resumeTicksRemaining = -1;
    }

    /**
     * @return the most recent recorded task command, without the prefix, or {@code null} if none was recorded
     */
    public String getLastTaskCommand() {
        return this.lastTaskCommand;
    }

    /**
     * @return whether a resume is currently armed and waiting to fire
     */
    public boolean isResumePending() {
        return this.pendingResumeCommand != null;
    }
}
