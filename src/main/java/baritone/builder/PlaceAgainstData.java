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

import java.util.stream.DoubleStream;
import java.util.stream.Stream;

/**
 * If you want to place against me, there's some things you gotta know
 */
public class PlaceAgainstData {

    public final Face against;
    public final boolean mustSneak; // like if its a crafting table
    private final Vec3d[] hits;
    private final boolean top;
    private final boolean bottom;

    public PlaceAgainstData(Face against, Vec3d[] hits, boolean mustSneak) {
        this.mustSneak = mustSneak;
        this.against = against;
        boolean top = false;
        boolean bottom = false;
        for (Vec3d hit : hits) {
            if (!validatePossibleHit(hit)) {
                throw new IllegalArgumentException();
            }
            if (!against.vertical) {
                if (BlockStatePlacementOption.hitOk(Half.BOTTOM, hit)) {
                    bottom = true;
                }
                if (BlockStatePlacementOption.hitOk(Half.TOP, hit)) {
                    top = true;
                }
            }
        }
        this.top = top;
        this.bottom = bottom;
        this.hits = hits;
        if (!streamRelativeToMyself().allMatch(Vec3d::inOriginUnitVoxel)) {
            throw new IllegalStateException();
        }
        if (!streamRelativeToPlace().allMatch(Vec3d::inOriginUnitVoxel)) {
            throw new IllegalStateException();
        }
        if (hits.length == 0) {
            throw new IllegalStateException();
        }
    }

    public PlaceAgainstData(Face against, Half half, boolean mustSneak) {
        this(against, project(against.opposite(), half), mustSneak);
        if (against.vertical && half != Half.EITHER) {
            throw new IllegalStateException();
        }
    }

    public Stream<Vec3d> streamRelativeToPlace() {
        return Stream.of(hits);
    }

    public Stream<Vec3d> streamRelativeToMyself() {
        return streamRelativeToPlace().map(v -> v.plus(against.x, against.y, against.z));
    }

    private boolean validatePossibleHit(Vec3d hit) {
        double[] h = {hit.x, hit.y, hit.z};
        for (int i = 0; i < 3; i++) {
            switch (against.vec[i]) {
                case -1: {
                    if (h[i] != 1) {
                        return false;
                    }
                    break;
                }
                case 0: {
                    if (h[i] < 0.05 || h[i] > 0.95) {
                        return false;
                    }
                    break;
                }
                case 1: {
                    if (h[i] != 0) {
                        return false;
                    }
                    break;
                }
            }
        }
        return true;
    }

    // TODO for playerMustBeHorizontalFacing, do i need something like andThatOptionExtendsTheFullHorizontalSpaceOfTheVoxel()?

    public boolean presentsAnOptionStrictlyInTheTopHalfOfTheStandardVoxelPlane() {
        if (Main.DEBUG && against.vertical) {
            throw new IllegalStateException();
        }
        return top;
    }

    public boolean presentsAnOptionStrictlyInTheBottomHalfOfTheStandardVoxelPlane() {
        if (Main.DEBUG && against.vertical) {
            throw new IllegalStateException();
        }
        return bottom;
    }

    private static final double[] LOCS = {0.1, 0.5, 0.9};

    private static Vec3d[] project(Face ontoFace, Half filterHalf) {
        return DoubleStream
                .of(LOCS)
                .boxed()
                .flatMap(dx -> DoubleStream.of(LOCS).mapToObj(dz -> new double[]{dx, dz}))
                .map(faceHit -> project(faceHit, ontoFace))
                .map(Vec3d::new)
                .filter(vec -> ontoFace.vertical || BlockStatePlacementOption.hitOk(filterHalf, vec))
                .toArray(Vec3d[]::new);
    }

    private static double[] project(double[] faceHit, Face ontoFace) {
        double[] ret = new double[3];
        int j = 0;
        for (int i = 0; i < 3; i++) {
            if (ontoFace.vec[i] == 0) {
                ret[i] = faceHit[j++];
            } else {
                if (ontoFace.vec[i] == 1) {
                    ret[i] = 1;
                } // else leave it as zero
            }
        }
        return ret;
    }
}
