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
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.util.List;

public class Favoring {
    private final LongOpenHashSet backtrackFavored = new LongOpenHashSet();
    private final double backtrackCoefficient;

    private final List<Avoidance> avoidances;

    private final Long2DoubleOpenHashMap favorings = new Long2DoubleOpenHashMap();

    private long mapMethodNano = 0;
    private long hybridMethodNano = 0;


    public Favoring(IPlayerContext ctx, IPath previous) {
        long start = System.currentTimeMillis();
        backtrackCoefficient = Baritone.settings().backtrackCostFavoringCoefficient.get();
        if (backtrackCoefficient != 1D && previous != null) {
            previous.positions().forEach(pos -> backtrackFavored.add(BetterBlockPos.longHash(pos)));
        }

        avoidances = Avoidance.create(ctx);
        long end = System.currentTimeMillis();
        System.out.println("Hybrid method init took " + (end - start) + "ms");

        favorings.defaultReturnValue(1.0D);
        backtrackFavored.forEach(l -> favorings.put((long) l, backtrackCoefficient));
        for (Avoidance avoid : avoidances) {
            avoid.applySpherical(favorings);
        }
        long end2 = System.currentTimeMillis();
        System.out.println("Map method init took " + (end2 - end) + "ms");
        System.out.println("Size: " + favorings.size());
    }

    public void printStats() {
        System.out.println("Map method nanos: " + mapMethodNano);
        System.out.println("Hybrid method nano: " + hybridMethodNano);
    }

    public boolean isEmpty() {
        return backtrackFavored.isEmpty() && favorings.isEmpty() && avoidances.isEmpty();
    }

    public double calculate(int x, int y, int z, long hash) {
        long start = System.nanoTime();
        double result = calculateMap(x, y, z, hash);
        long mid = System.nanoTime();
        double result2 = calculateHybrid(x, y, z, hash);
        long end = System.nanoTime();
        mapMethodNano += mid - start;
        hybridMethodNano += end - mid;
        if (result != result2) {
            System.out.println("NO MATCH " + result + " " + result2);
        }
        return result;
    }


    public double calculateMap(int x, int y, int z, long hash) {
        return favorings.get(hash);
    }

    public double calculateHybrid(int x, int y, int z, long hash) {
        double result = 1.0D;
        if (backtrackFavored.contains(hash)) {
            result *= backtrackCoefficient;
        }
        for (Avoidance avoid : avoidances) {
            result *= avoid.coefficient(x, y, z);
        }
        return result;
    }
}
