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

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A plane against which this block state can be placed
 * <p>
 * For a normal block, this will be a full face of a block. In that case, this class is no more than an EnumFacing
 * <p>
 * For a block like a slab or a stair, this will contain the information that the placement must be against the top or bottom half of the face
 */
public class BlockStatePlacementOption {

    /**
     * e.g. a torch placed down on the ground is placed against the bottom of "the torch bounding box", so this would be DOWN for the torch
     */
    public final Face against;
    public final Half half;
    public final Optional<Face> playerMustBeFacing;

    private BlockStatePlacementOption(Face against, Half half, Optional<Face> playerMustBeFacing) {
        Objects.requireNonNull(against);
        Objects.requireNonNull(half);
        this.against = against;
        this.half = half;
        this.playerMustBeFacing = playerMustBeFacing;
        validate(against, half, playerMustBeFacing);
    }

    public Optional<Raytracer.Raytrace> computeTraceOptions(Half againstHalf, int playerSupportingX, double playerEyeY, int playerSupportingZ, PlayerVantage vantage, double blockReachDistance) {
        if (againstHalf != half && half != Half.EITHER) { // narrowing is ok (EITHER -> TOP/BOTTOM) but widening isn't (TOP/BOTTOM -> EITHER)
            throw new IllegalStateException();
        }
        List<Vec2d> acceptableVantages = new ArrayList<>();
        Vec2d center = Vec2d.HALVED_CENTER.plus(playerSupportingX, playerSupportingZ);
        switch (vantage) {
            case LOOSE_CENTER: {
                acceptableVantages.add(center.plus(0.15, 0));
                acceptableVantages.add(center.plus(-0.15, 0));
                acceptableVantages.add(center.plus(0, 0.15));
                acceptableVantages.add(center.plus(0, -0.15));
                // no break!
            } // FALLTHROUGH!
            case STRICT_CENTER: {
                acceptableVantages.add(center);
                break;
            }
            case SNEAK_BACKPLACE: {
                if (playerSupportingX != against.x || playerSupportingZ != against.z) {
                    throw new IllegalStateException();
                }
                // in a sneak backplace, there is exactly one location where the player will be
                acceptableVantages.add(Vec2d.HALVED_CENTER.plus(0.25 * against.x, 0.25 * against.z));
                break;
            }
            default:
                throw new IllegalStateException();
        }
        // direction from placed block to place-against block = this.against
        long blockPlacedAt = 0;
        long placeAgainstPos = against.offset(blockPlacedAt);
        double sq = blockReachDistance * blockReachDistance;

        return acceptableVantages
                .stream()
                .map(playerEyeXZ -> new Vec3d(playerEyeXZ.x, playerEyeY, playerEyeXZ.z))
                .flatMap(eye ->
                        Stream.of(FACE_PROJECTION_CACHE[against.index])
                                .filter(hit -> eye.distSq(hit) < sq)
                                .<Supplier<Optional<Raytracer.Raytrace>>>map(hit -> () -> Raytracer.runTrace(eye, placeAgainstPos, against.opposite(), hit))
                )
                .collect(Collectors.toList())
                .parallelStream() // wrap it like this because flatMap forces .sequential() on the interior child stream, defeating the point
                .map(Supplier::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .min(Comparator.naturalOrder());
    }

    private static final Vec3d[][] FACE_PROJECTION_CACHE;

    static {
        Vec2d center = new Vec2d(0.5, 0.5);
        double[] diffs = {-0.4, 0, +0.4};
        FACE_PROJECTION_CACHE = new Vec3d[Face.NUM_FACES][diffs.length * diffs.length];
        for (Face face : Face.VALUES) {
            int i = 0;
            for (double dx : diffs) {
                for (double dz : diffs) {
                    FACE_PROJECTION_CACHE[face.index][i++] = new Vec3d(project(center.plus(dx, dz).toArr(), face));
                }
            }
        }
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

    public static BlockStatePlacementOption get(Face against, Half half, Optional<Face> playerMustBeFacing) {
        BlockStatePlacementOption ret = PLACEMENT_OPTION_SINGLETON_CACHE[against.index][half.ordinal()][playerMustBeFacing.map(face -> face.index).orElse(Face.NUM_FACES)];
        if (ret == null) {
            throw new IllegalStateException(against + " " + half + " " + playerMustBeFacing);
        }
        return ret;
    }

    private static final BlockStatePlacementOption[][][] PLACEMENT_OPTION_SINGLETON_CACHE;

    static {
        PLACEMENT_OPTION_SINGLETON_CACHE = new BlockStatePlacementOption[Face.NUM_FACES][Half.values().length][Face.NUM_FACES + 1];
        for (Face against : Face.VALUES) {
            for (Half half : Half.values()) {
                BlockStatePlacementOption[] saveInto = PLACEMENT_OPTION_SINGLETON_CACHE[against.index][half.ordinal()];
                for (Face player : Face.VALUES) {
                    try {
                        saveInto[player.index] = new BlockStatePlacementOption(against, half, Optional.of(player));
                    } catch (RuntimeException ex) {
                    }
                }
                try {
                    saveInto[Face.NUM_FACES] = new BlockStatePlacementOption(against, half, Optional.empty());
                } catch (RuntimeException ex) {
                }
            }
        }
    }

    private void validate(Face against, Half half, Optional<Face> playerMustBeFacing) {
        if ((against == Face.DOWN || against == Face.UP) && half != Half.EITHER) {
            throw new IllegalArgumentException();
        }
        if (Main.STRICT_Y && against == Face.UP) {
            throw new IllegalStateException();
        }
        playerMustBeFacing.ifPresent(face -> {
            if (face == Face.DOWN || face == Face.UP) {
                throw new IllegalArgumentException();
            }
            if (face == against.opposite()) {
                throw new IllegalStateException();
            }
        });
    }
}
