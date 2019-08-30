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

package baritone.api.event.events;

import baritone.api.event.events.type.Cancellable;
import baritone.api.event.events.type.Overrideable;

/**
 * @author LoganDark
 */
public abstract class TabCompleteEvent extends Cancellable {
    public final Overrideable<String> prefix;
    public final Overrideable<String[]> completions;

    TabCompleteEvent(String prefix, String[] completions) {
        this.prefix = new Overrideable<>(prefix);
        this.completions = new Overrideable<>(completions);
    }

    public boolean wasModified() {
        return prefix.wasModified() || completions.wasModified();
    }

    public static final class Pre extends TabCompleteEvent {
        public Pre(String prefix) {
            super(prefix, null);
        }
    }

    public static final class Post extends TabCompleteEvent {
        public Post(String prefix, String[] completions) {
            super(prefix, completions);
        }
    }
}
