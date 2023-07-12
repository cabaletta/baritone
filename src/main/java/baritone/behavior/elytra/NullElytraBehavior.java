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

package baritone.behavior.elytra;

import baritone.Baritone;
import baritone.api.behavior.IElytraBehavior;
import baritone.behavior.Behavior;
import net.minecraft.util.math.BlockPos;

import java.util.concurrent.CompletableFuture;

/**
 * @author Brady
 */
public final class NullElytraBehavior extends Behavior implements IElytraBehavior {

    public NullElytraBehavior(Baritone baritone) {
        super(baritone);
    }

    @Override
    public CompletableFuture<Void> resetContext() {
        throw new UnsupportedOperationException("Called resetContext() on NullElytraBehavior");
    }

    @Override
    public void repackChunks() {
        throw new UnsupportedOperationException("Called repackChunks() on NullElytraBehavior");
    }

    @Override
    public void pathTo(BlockPos destination) {
        throw new UnsupportedOperationException("Called pathTo() on NullElytraBehavior");
    }

    @Override
    public void cancel() {
        throw new UnsupportedOperationException("Called cancel() on NullElytraBehavior");
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public boolean isLoaded() {
        return false;
    }

    @Override
    public boolean isSafeToCancel() {
        return true;
    }
}
