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
import baritone.bot.pathing.calc.AStarPathFinder;
import baritone.bot.pathing.goals.Goal;
import baritone.bot.pathing.goals.GoalBlock;
import baritone.bot.pathing.goals.GoalXZ;
import baritone.bot.pathing.goals.GoalYLevel;
import baritone.bot.pathing.movement.ActionCosts;
import baritone.bot.pathing.movement.CalculationContext;
import baritone.bot.pathing.movement.Movement;
import baritone.bot.utils.pathing.BetterBlockPos;
import baritone.map.Map;
import baritone.map.MapChunk;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
        if (msg.startsWith("/")) {
            msg = msg.substring(1);
        }
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
                        if (params[0].equals("clear") || params[0].equals("none")) {
                            goal = null;
                        } else {
                            goal = new GoalYLevel(Integer.parseInt(params[0]));
                        }
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
        if (msg.toLowerCase().equals("spawn")) {
            BlockPos spawnPoint = player().getBedLocation();
            // for some reason the default spawnpoint is underground sometimes
            Goal goal = new GoalXZ(spawnPoint.getX(), spawnPoint.getY());
            PathingBehavior.INSTANCE.setGoal(goal);
            displayChatMessageRaw("Goal: " + goal);
            event.cancel();
            return;
        }
        if (msg.toLowerCase().equals("costs")) {
            Movement[] movements = AStarPathFinder.getConnectedPositions(new BetterBlockPos(playerFeet()), new CalculationContext());
            ArrayList<Movement> moves = new ArrayList<>(Arrays.asList(movements));
            moves.sort(Comparator.comparingDouble(movement -> movement.getCost(new CalculationContext())));
            for (Movement move : moves) {
                String[] parts = move.getClass().toString().split("\\.");
                double cost = move.getCost(new CalculationContext());
                String strCost = cost + "";
                if (cost >= ActionCosts.COST_INF) {
                    strCost = "IMPOSSIBLE";
                }
                displayChatMessageRaw(parts[parts.length - 1] + " " + move.getDest().getX() + "," + move.getDest().getY() + "," + move.getDest().getZ() + " " + strCost);
            }
            event.cancel();
            return;
        }
        List<Settings.Setting<Boolean>> toggleable = Baritone.settings().getByValueType(Boolean.class);
        for (Settings.Setting<Boolean> setting : toggleable) {
            if (msg.equalsIgnoreCase(setting.getName())) {
                setting.value ^= true;
                event.cancel();
                displayChatMessageRaw("Toggled " + setting.getName() + " to " + setting.value);
                return;
            }
        }
        if (msg.toLowerCase().equals("baritone") || msg.toLowerCase().equals("settings")) {
            for (Settings.Setting<?> setting : Baritone.settings().allSettings) {
                displayChatMessageRaw(setting.toString());
            }
            event.cancel();
            return;
        }
        if (msg.contains(" ")) {
            String[] data = msg.split(" ");
            if (data.length == 2) {
                Settings.Setting setting = Baritone.settings().byLowerName.get(data[0].toLowerCase());
                if (setting != null) {
                    try {
                        if (setting.value.getClass() == Long.class) {
                            setting.value = Long.parseLong(data[1]);
                        }
                        if (setting.value.getClass() == Integer.class) {
                            setting.value = Integer.parseInt(data[1]);
                        }
                        if (setting.value.getClass() == Double.class) {
                            setting.value = Double.parseDouble(data[1]);
                        }
                    } catch (NumberFormatException e) {
                        displayChatMessageRaw("Unable to parse " + data[1]);
                        event.cancel();
                        return;
                    }
                    displayChatMessageRaw(setting.toString());
                    event.cancel();
                    return;
                }
            }
        }
        if(msg.toLowerCase().equals("map")) {
            Map.INSTANCE.writeImage();
        }
        if (Baritone.settings().byLowerName.containsKey(msg.toLowerCase())) {
            Settings.Setting<?> setting = Baritone.settings().byLowerName.get(msg.toLowerCase());
            displayChatMessageRaw(setting.toString());
            event.cancel();
            return;
        }
    }
}
