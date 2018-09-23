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

import baritone.cache.Waypoint;
import baritone.cache.WorldProvider;
import baritone.api.event.events.BlockInteractEvent;
import baritone.utils.BlockStateInterface;
import baritone.utils.Helper;
import net.minecraft.block.BlockBed;

/**
 * A collection of event methods that are used to interact with Baritone's
 * waypoint system. This class probably needs a better name.
 *
 * @see Waypoint
 *
 * @author Brady
 * @since 8/22/2018
 */
public final class LocationTrackingBehavior extends Behavior implements Helper {

    public static final LocationTrackingBehavior INSTANCE = new LocationTrackingBehavior();

    private LocationTrackingBehavior() {}

    @Override
    public void onBlockInteract(BlockInteractEvent event) {
        if (event.getType() == BlockInteractEvent.Type.USE && BlockStateInterface.getBlock(event.getPos()) instanceof BlockBed) {
            WorldProvider.INSTANCE.getCurrentWorld().waypoints.addWaypoint(new Waypoint("bed", Waypoint.Tag.BED, event.getPos()));
        }
    }

    @Override
    public void onPlayerDeath() {
        WorldProvider.INSTANCE.getCurrentWorld().waypoints.addWaypoint(new Waypoint("death", Waypoint.Tag.DEATH, playerFeet()));
    }
}
