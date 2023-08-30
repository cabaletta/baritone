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

package baritone.utils;

import baritone.api.IBaritone;
import baritone.api.process.IBaritoneProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.IExecutionControl;

public class ExecutionControl implements IExecutionControl {
    private final IBaritone baritone;
    private boolean active;

    public ExecutionControl(String name, double priority, IBaritone baritone) {
        this.baritone = baritone;
        baritone.getPathingControlManager().registerProcess(new IBaritoneProcess() {
            @Override
            public boolean isActive() {
                return active;
            }

            @Override
            public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
                baritone.getInputOverrideHandler().clearAllKeys();
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
                return priority;
            }

            @Override
            public String displayName0() {
                return name;
            }
        });
    }

    @Override
    public void pause() {
        active = true;
    }

    @Override
    public void resume() {
        active = false;
    }

    @Override
    public boolean paused() {
        return active;
    }

    @Override
    public void stop() {
        active = false;
        baritone.getPathingBehavior().cancelEverything();
    }
}
