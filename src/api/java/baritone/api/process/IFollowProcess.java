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

package baritone.api.process;

import net.minecraft.entity.Entity;

/**
 * @author Brady
 * @since 9/23/2018
 */
public interface IFollowProcess extends IBaritoneProcess {

    /**
     * Set the follow target to the specified entity;
     *
     * @param entity The entity to follow
     */
    void follow(Entity entity);

    /**
     * @return The entity that is currently being followed
     */
    Entity following();

    /**
     * Cancels the follow behavior, this will clear the current follow target.
     */
    default void cancel() {
        onLostControl();
    }
}
