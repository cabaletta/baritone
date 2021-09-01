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

package baritone.api.utils;

public enum ToolPriorityType {
    DEFAULT("default"),
    CAN_BREAK_BLOCK("can_break_block"),
    BEST_SILKTOUCH_FOR_BLOCK("best_silktouch_for_block"),
    FASTEST("fastest"),
    LEAST_DURABLE("least_durable"),
    EXPENSIVE("expensive"),
    CHEAP("cheap"),
    ENCHANTED("enchanted"),
    NOT_ENCHANTED("not_enchanted");

    private final String name;

    ToolPriorityType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ToolPriorityType fromString(String text) {
        for (ToolPriorityType tpt : ToolPriorityType.values()) {
            if (tpt.name.equalsIgnoreCase(text)) {
                return tpt;
            }
        }
        throw new IllegalArgumentException(String.format("Unknown ToolPriorityType %s", text));
    }
}