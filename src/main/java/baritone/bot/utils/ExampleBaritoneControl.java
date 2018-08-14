/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.bot.utils;

import baritone.bot.Baritone;
import baritone.bot.Settings;
import baritone.bot.behavior.Behavior;
import baritone.bot.behavior.impl.PathingBehavior;
import baritone.bot.event.events.ChatEvent;
import baritone.bot.pathing.goals.Goal;
import baritone.bot.pathing.goals.GoalBlock;
import baritone.bot.pathing.goals.GoalXZ;
import baritone.bot.pathing.goals.GoalYLevel;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class ExampleBaritoneControl extends Behavior {
    public static ExampleBaritoneControl INSTANCE = new ExampleBaritoneControl();

    private ExampleBaritoneControl() {

    }

    public void initAndRegister() {
        Baritone.INSTANCE.registerBehavior(this);
    }

    @Override
    public void onSendChatMessage(ChatEvent event) {
        if (!Baritone.settings().chatControl.get()) {
            return;
        }
        String msg = event.getMessage();
        if (msg.toLowerCase().startsWith("goal")) {
            event.cancel();
            String[] params = msg.toLowerCase().substring(4).trim().split(" ");
            if (params[0].equals("")) {
                params = new String[]{};
            }
            Goal goal;
            try {
                switch (params.length) {
                    case 0:
                        goal = new GoalBlock(playerFeet());
                        break;
                    case 1:
                        goal = new GoalYLevel(Integer.parseInt(params[0]));
                        break;
                    case 2:
                        goal = new GoalXZ(Integer.parseInt(params[0]), Integer.parseInt(params[1]));
                        break;
                    case 3:
                        goal = new GoalBlock(new BlockPos(Integer.parseInt(params[0]), Integer.parseInt(params[1]), Integer.parseInt(params[2])));
                        break;
                    default:
                        displayChatMessageRaw("unable to understand lol");
                        return;
                }
            } catch (NumberFormatException ex) {
                displayChatMessageRaw("unable to parse integer " + ex);
                return;
            }
            PathingBehavior.INSTANCE.setGoal(goal);
            displayChatMessageRaw("Goal: " + goal);
            return;
        }
        if (msg.equals("path")) {
            PathingBehavior.INSTANCE.path();
            event.cancel();
            return;
        }
        if (msg.toLowerCase().equals("cancel")) {
            PathingBehavior.INSTANCE.cancel();
            event.cancel();
            displayChatMessageRaw("ok canceled");
            return;
        }
        if (msg.toLowerCase().startsWith("thisway")) {
            Goal goal = GoalXZ.fromDirection(playerFeetAsVec(), player().rotationYaw, Double.parseDouble(msg.substring(7).trim()));
            PathingBehavior.INSTANCE.setGoal(goal);
            displayChatMessageRaw("Goal: " + goal);
            event.cancel();
            return;
        }
        List<Settings.Setting<Boolean>> toggleable = Baritone.settings().getByValueType(Boolean.class);
        for (Settings.Setting<Boolean> setting : toggleable) {
            if (msg.toLowerCase().equals(setting.getName().toLowerCase())) {
                setting.value ^= true;
                event.cancel();
                displayChatMessageRaw("Toggled " + setting.getName() + " to " + setting.value);
                return;
            }
        }
    }
}
