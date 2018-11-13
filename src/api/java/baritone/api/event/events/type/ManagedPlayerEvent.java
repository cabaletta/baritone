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

package baritone.api.event.events.type;

import net.minecraft.client.entity.EntityPlayerSP;

/**
 * An event that has a reference to a locally managed player.
 *
 * @author Brady
 * @since 10/11/2018
 */
public class ManagedPlayerEvent {

    protected final EntityPlayerSP player;

    public ManagedPlayerEvent(EntityPlayerSP player) {
        this.player = player;
    }

    public final EntityPlayerSP getPlayer() {
        return this.player;
    }

    public static class Cancellable extends ManagedPlayerEvent implements ICancellable {

        /**
         * Whether or not this event has been cancelled
         */
        private boolean cancelled;

        public Cancellable(EntityPlayerSP player) {
            super(player);
        }

        @Override
        public final void cancel() {
            this.cancelled = true;
        }

        @Override
        public final boolean isCancelled() {
            return this.cancelled;
        }
    }
}
