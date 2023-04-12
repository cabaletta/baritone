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

import baritone.Baritone;
import baritone.api.process.IBaritoneProcess;
import baritone.api.utils.Helper;
import baritone.api.utils.IPlayerContext;

public abstract class BaritoneProcessHelper implements IBaritoneProcess, Helper {

    protected final Baritone baritone;
    protected final IPlayerContext ctx;

    public BaritoneProcessHelper(Baritone baritone) {
        this.baritone = baritone;
        this.ctx = baritone.getPlayerContext();
    }

    @Override
    public boolean isTemporary() {
        //When executed from the CommandQueueProcess the new "child" process should be temporary. I think however that
        // this isn't the correct solution. I haven't tested it, but I think if the queue is filled and therefor the
        // CommandQueueProcess is active, all normally executed Processes also would become temporary. This would mean
        // you could bypass the waiting queue and overwrite the current "child" process but after your process is
        // finished the queue would continue as before. I don't think that is the desired behavior.
        return baritone.getCommandQueueProcess().isActive();
    }
}
