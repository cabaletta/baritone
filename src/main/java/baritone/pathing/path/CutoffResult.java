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

package baritone.pathing.path;

/**
 * @author Brady
 * @since 10/8/2018
 */
public final class CutoffResult {

    private final IPath path;

    private final int removed;

    private CutoffResult(IPath path, int removed) {
        this.path = path;
        this.removed = removed;
    }

    public final boolean wasCut() {
        return this.removed > 0;
    }

    public final int getRemoved() {
        return this.removed;
    }

    public final IPath getPath() {
        return this.path;
    }

    public static CutoffResult cutoffPath(IPath path, int index) {
        return new CutoffResult(new CutoffPath(path, index), path.positions().size() - index - 1);
    }

    public static CutoffResult preservePath(IPath path) {
        return new CutoffResult(path, 0);
    }
}
