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

package baritone.utils.pathing;

import baritone.Baritone;
import baritone.api.pathing.calc.IPath;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.IPlayerContext;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;

import java.util.List;

public final class Favoring {
    private List<Avoidance> avoidances;
    private final Long2DoubleOpenHashMap favorings;

    public Favoring(IPlayerContext ctx, IPath previous) {
        this(previous);
        avoidances = Avoidance.create(ctx);
        for (Avoidance avoid : avoidances) {
            avoid.applySpherical(favorings);
        }
        System.out.println("Favoring size: " + favorings.size());
    }

    public Favoring(IPath previous) { // create one just from previous path, no mob avoidances
        favorings = new Long2DoubleOpenHashMap();
        favorings.defaultReturnValue(1.0D);
        double coeff = Baritone.settings().backtrackCostFavoringCoefficient.get();
        if (coeff != 1D && previous != null) {
            previous.positions().forEach(pos -> favorings.put(BetterBlockPos.longHash(pos), coeff));
        }
    }

    public boolean isEmpty() {
        return favorings.isEmpty();
    }

    public double calculate(long hash) {
        return favorings.get(hash);
    }
}
