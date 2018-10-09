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

package baritone.api.pathing.path;

/**
 * The result of a path cut-off operation.
 *
 * @author Brady
 * @since 10/8/2018
 */
public final class CutoffResult {

    /**
     * The resulting path
     */
    private final IPath path;

    /**
     * The amount of movements that were removed
     */
    private final int removed;

    private CutoffResult(IPath path, int removed) {
        this.path = path;
        this.removed = removed;
    }

    /**
     * @return Whether or not the path was cut
     */
    public final boolean wasCut() {
        return this.removed > 0;
    }

    /**
     * @return The amount of movements that were removed
     */
    public final int getRemoved() {
        return this.removed;
    }

    /**
     * @return The resulting path
     */
    public final IPath getPath() {
        return this.path;
    }

    /**
     * Creates a new result from a successful cut-off operation.
     *
     * @param path The input path
     * @param index The index to cut the path at
     * @return The result of the operation
     */
    public static CutoffResult cutoffPath(IPath path, int index) {
        return new CutoffResult(new CutoffPath(path, index), path.positions().size() - index - 1);
    }

    /**
     * Creates a new result in which no cut-off occurred.
     *
     * @param path The input path
     * @return The result of the operation
     */
    public static CutoffResult preservePath(IPath path) {
        return new CutoffResult(path, 0);
    }
}
