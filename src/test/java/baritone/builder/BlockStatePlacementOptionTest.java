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

package baritone.builder;

import org.junit.Test;

import java.util.List;
import java.util.Optional;

public class BlockStatePlacementOptionTest {

    @Test
    public void sanityCheck() {
        // standing at 1,0,0
        // block to be placed at 0,0,0
        // placing against 0,0,-1

        // eye is at 1, 1.62, 0
        // north or west

        StringBuilder sanity = new StringBuilder();
        for (PlayerVantage vantage : new PlayerVantage[]{PlayerVantage.STRICT_CENTER, PlayerVantage.LOOSE_CENTER}) {
            for (Face playerFacing : new Face[]{Face.NORTH, Face.EAST, Face.WEST}) {
                sanity.append(vantage).append(playerFacing);
                List<Raytracer.Raytrace> traces = BlockStatePlacementOption.get(Face.NORTH, Half.BOTTOM, Optional.of(playerFacing), Optional.empty()).computeTraceOptions(new PlaceAgainstData(Face.SOUTH, Half.EITHER, false), 1, 0, 0, vantage, 4);
                sanity.append(traces.size());
                sanity.append(" ");
                if (!traces.isEmpty()) {
                    for (double d : new double[]{traces.get(0).playerEye.x, traces.get(0).playerEye.z}) {
                        double base = d > 1 ? 1.5 : 0.5;
                        boolean a = d == base - BlockStatePlacementOption.LOOSE_CENTER_DISTANCE;
                        boolean b = d == base;
                        boolean c = d == base + BlockStatePlacementOption.LOOSE_CENTER_DISTANCE;
                        if (!a && !b && !c) {
                            throw new IllegalStateException("Wrong " + d);
                        }
                        sanity.append(a).append(" ").append(b).append(" ").append(c).append(" ");
                    }
                }
                sanity.append(traces.stream().mapToDouble(Raytracer.Raytrace::centerDistApprox).distinct().count());
                sanity.append(";");
            }
        }

        String res = sanity.toString();
        String should = "STRICT_CENTERNORTH0 0;STRICT_CENTEREAST0 0;STRICT_CENTERWEST3 false true false false true false 1;LOOSE_CENTERNORTH2 true false false false true false 1;LOOSE_CENTEREAST0 0;LOOSE_CENTERWEST13 false true false false true false 2;";
        if (!res.equals(should)) {
            System.out.println(res);
            System.out.println(should);
            throw new IllegalStateException(res);
        }
    }
}