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

package baritone.utils.inventory;
import java.util.HashSet;
import net.minecraft.item.Item;

/**
 * @author Matthew Carlson
 */
public class ItemFilter {
    public final Item i;

    public final int priority;

    public ItemFilter(Item i, int priority) {
        this.i = i;
        this.priority = priority;
    }

    public ItemFilter(Item i) {
        this.i = i;
        this.priority = 0;
    }

    public boolean isGarbage() {
        return this.priority == 0;
    }

    @Override
    public int hashCode() {
        return this.i.hashCode();
    }

    @Override
    public String toString() {
        return "(" + this.i.toString() + ", " + this.priority + ")";
    }
}