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
import baritone.api.command.Command;
import baritone.api.command.ICommand;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.exception.CommandException;
import baritone.api.process.ICommandQueueProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.utils.BaritoneProcessHelper;
import baritone.utils.accessor.IAnvilChunkLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandQueueProcess extends BaritoneProcessHelper  implements ICommandQueueProcess {

    private List<Map<ICommand, IArgConsumer>> commandQueue;

    public CommandQueueProcess(Baritone baritone) {
        super(baritone);
    }

    @Override
    public boolean isActive() {
        return !commandQueue.isEmpty();
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {

        return new PathingCommand(null, PathingCommandType.DEFER);
    }

    @Override
    public void onLostControl() {

    }

    @Override
    public double priority() {
        return super.priority();
    }

    @Override
    public String displayName() {
        return super.displayName();
    }

    @Override
    public String displayName0() {
        return null;
    }

    @Override
    public void addNewCommand(ICommand command, IArgConsumer args) {
        Map<ICommand, IArgConsumer> commandArgMap = new HashMap<>();
        commandArgMap.put(command, args);
        commandQueue.add(commandArgMap);
    }

    @Override
    public void removeCommand(int i) {
        try {
            commandQueue.remove(i);
        } catch (IndexOutOfBoundsException e) {
            logDirect("index was out of bounds.");
        }
    }

    @Override
    public void ExecuteNext() throws CommandException {
        commandQueue.remove(0);
        if (!commandQueue.isEmpty()) {
            Map<ICommand, IArgConsumer> map = commandQueue.get(0);
            ICommand command = ((ICommand)map.keySet().toArray()[0]);
            command.execute(command.getNames().get(0),map.get(command));
        } else {
            logDirect("command queue empty");
        }
    }

    @Override
    public void clearQueue() {
        commandQueue.clear();
    }
}
