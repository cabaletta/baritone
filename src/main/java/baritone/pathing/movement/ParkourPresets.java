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

package baritone.pathing.movement;

/**
 * Preset definitions for parkour jumps of various distances and shapes.
 * Ordered by increasing difficulty — the cost methods in MovementParkour
 * iterate through these presets in enum order to find the first viable jump.
 *
 * @author Ria
 */
public enum ParkourPresets {

    /** 1-block straight jump (no sprint needed) */
    SIMPLE_1(1, 0, false),

    /** 2-block straight jump (no sprint needed) */
    SIMPLE_2(2, 0, false),

    /** 3-block straight jump (sprint required) */
    SIMPLE_3(3, 0, true),

    /** 1x1 diagonal jump, distance ≈ 1.41 (no sprint needed) */
    DIAG_1(1, 1, false),

    /** 2x2 diagonal jump, distance ≈ 2.83 (sprint required) */
    DIAG_2(2, 2, true),

    /** Mixed 2x1 diagonal jump, distance ≈ 2.24 (sprint may be needed) */
    DIAG_MIXED_2_1(2, 1, true),

    /** Mixed 1x2 diagonal jump, distance ≈ 2.24 (sprint may be needed) */
    DIAG_MIXED_1_2(1, 2, true);

    /** X-axis offset magnitude (always non-negative) */
    public final int dx;

    /** Z-axis offset magnitude (always non-negative) */
    public final int dz;

    /** Euclidean distance sqrt(dx² + dz²) */
    public final double distance;

    /** Whether this jump requires sprinting */
    public final boolean requiresSprint;

    ParkourPresets(int dx, int dz, boolean requiresSprint) {
        this.dx = dx;
        this.dz = dz;
        this.distance = Math.sqrt((double) dx * dx + (double) dz * dz);
        this.requiresSprint = requiresSprint;
    }

    /**
     * @return true if this preset is a cardinal-direction (non-diagonal) jump
     */
    public boolean isCardinal() {
        return dx == 0 || dz == 0;
    }

    /**
     * @return true if this preset is a diagonal jump
     */
    public boolean isDiagonal() {
        return dx != 0 && dz != 0;
    }
}
