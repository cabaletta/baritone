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

package baritone.process;

import baritone.Baritone;
import baritone.api.pathing.goals.GoalFollow;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.utils.BaritoneProcessHelper;
import net.minecraft.world.entity.player.Player;

/**
 * Follows a named player using {@link GoalFollow}, revalidating the path every
 * tick so the route adapts as the target moves.
 *
 * <p>Activated by {@link baritone.command.defaults.FollowPlayerCommand} or
 * directly via {@link #followPlayer(String, int)}.
 * Deactivated by calling {@link #stop()} or by Baritone's standard
 * {@code #cancel} command / cancel hotkey.
 */
public final class FollowPlayerProcess extends BaritoneProcessHelper {

    private String targetName;
    private int    followRange = 3;

    public FollowPlayerProcess(Baritone baritone) {
        super(baritone);
    }

    /**
     * Start following a player by their display name.
     *
     * @param name  case-insensitive display name of the target player
     * @param range how close to approach in blocks (minimum 1)
     */
    public void followPlayer(String name, int range) {
        this.targetName  = name;
        this.followRange = Math.max(1, range);
    }

    /** Cancel following without affecting other active Baritone processes. */
    public void stop() {
        this.targetName = null;
    }

    // -------------------------------------------------------------------------

    @Override
    public boolean isActive() {
        return targetName != null;
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        Player target = findTarget();
        if (target == null) {
            // Target not loaded; pause so we don't spam failed calculations.
            return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
        }
        GoalFollow goal = new GoalFollow(target, followRange);
        return new PathingCommand(goal, PathingCommandType.REVALIDATE_GOAL_AND_PATH);
    }

    @Override
    public void onLostControl() {
        targetName = null;
    }

    @Override
    public String displayName0() {
        return "Following player \"" + targetName + "\"";
    }

    // -------------------------------------------------------------------------

    private Player findTarget() {
        if (targetName == null || ctx.world() == null) return null;
        return ctx.world().players().stream()
                .filter(p -> !p.equals(ctx.player()))
                .filter(p -> p.getName().getString().equalsIgnoreCase(targetName))
                .findFirst()
                .orElse(null);
    }
}
