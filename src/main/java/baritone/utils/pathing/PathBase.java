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

package baritone.utils.pathing;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.pathing.path.CutoffPath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.EmptyChunk;

public abstract class PathBase implements IPath {
    @Override
    public IPath cutoffAtLoadedChunks(World world) {
        for (int i = 0; i < positions().size(); i++) {
            BlockPos pos = positions().get(i);
            if (world.getChunk(pos) instanceof EmptyChunk) {
                return new CutoffPath(this, i);
            }
        }
        return this;
    }

    @Override
    public IPath staticCutoff(Goal destination) {
        int min = BaritoneAPI.getSettings().pathCutoffMinimumLength.get();
        if (length() < min) {
            return this;
        }
        if (destination == null || destination.isInGoal(getDest())) {
            return this;
        }
        double factor = BaritoneAPI.getSettings().pathCutoffFactor.get();
        int newLength = (int) ((length() - 1 - min) * factor) + min;
        return new CutoffPath(this, newLength);
    }
}
