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

package baritone.behavior.impl;

import baritone.behavior.Behavior;
import baritone.chunk.Waypoint;
import baritone.chunk.WorldProvider;
import baritone.event.events.BlockInteractEvent;
import baritone.utils.BlockStateInterface;
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
public final class LocationTrackingBehavior extends Behavior {

    public static final LocationTrackingBehavior INSTANCE = new LocationTrackingBehavior();

    private LocationTrackingBehavior() {}

    @Override
    public void onBlockInteract(BlockInteractEvent event) {
        if (event.getType() == BlockInteractEvent.Type.USE && BlockStateInterface.get(event.getPos()) instanceof BlockBed) {
            createWaypointAtPlayer("bed", Waypoint.Tag.BED);
        }
    }

    @Override
    public void onPlayerDeath() {
        createWaypointAtPlayer("death", Waypoint.Tag.DEATH);
    }

    private void createWaypointAtPlayer(String name, Waypoint.Tag tag) {
        WorldProvider.INSTANCE.getCurrentWorld().waypoints.addWaypoint(new Waypoint(name, tag, playerFeet()));
    }
}
