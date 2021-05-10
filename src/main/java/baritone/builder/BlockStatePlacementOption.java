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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

    /**
     * This value must be greater than the face projections.
     * <p>
     * Otherwise certain stair placements would not work. This is verified in this class's sanityCheck.
     */
    private static final double LOOSE_CENTER_DISTANCE = 0.15;

    public List<Raytracer.Raytrace> computeTraceOptions(PlaceAgainstData placingAgainst, int playerSupportingX, double playerEyeY, int playerSupportingZ, PlayerVantage vantage, double blockReachDistance) {
        if (placingAgainst.half != half && half != Half.EITHER) { // narrowing is ok (EITHER -> TOP/BOTTOM) but widening isn't (TOP/BOTTOM -> EITHER)
            throw new IllegalStateException();
        }
        List<Vec2d> acceptableVantages = new ArrayList<>();
        Vec2d center = Vec2d.HALVED_CENTER.plus(playerSupportingX, playerSupportingZ);
        switch (vantage) {
            case LOOSE_CENTER: {
                acceptableVantages.add(center.plus(LOOSE_CENTER_DISTANCE, 0));
                acceptableVantages.add(center.plus(-LOOSE_CENTER_DISTANCE, 0));
                acceptableVantages.add(center.plus(0, LOOSE_CENTER_DISTANCE));
                acceptableVantages.add(center.plus(0, -LOOSE_CENTER_DISTANCE));
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

        return sanityCheckTraces(acceptableVantages
                .stream()
                .map(playerEyeXZ -> new Vec3d(playerEyeXZ.x, playerEyeY, playerEyeXZ.z))
                .flatMap(eye ->
                        Stream.of(FACE_PROJECTION_CACHE[against.index])
                                .filter(hit -> hitOk(placingAgainst, hit))
                                .filter(hit -> eye.distSq(hit) < blockReachDistance * blockReachDistance)
                                .filter(hit -> directionOk(eye, hit))
                                .<Supplier<Optional<Raytracer.Raytrace>>>map(hit -> () -> Raytracer.runTrace(eye, placeAgainstPos, against.opposite(), hit))
                )
                .collect(Collectors.toList())
                .parallelStream() // wrap it like this because flatMap forces .sequential() on the interior child stream, defeating the point
                .map(Supplier::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted()
                .collect(Collectors.toList()));
    }

    private static boolean hitOk(PlaceAgainstData placingAgainst, Vec3d hit) {
        if (placingAgainst.half == Half.EITHER) {
            return true;
        } else if (hit.y == 0.1) {
            return placingAgainst.half == Half.BOTTOM;
        } else if (hit.y == 0.5) {
            return false;
        } else if (hit.y == 0.9) {
            return placingAgainst.half == Half.TOP;
        } else {
            throw new IllegalStateException();
        }
    }

    private boolean directionOk(Vec3d eye, Vec3d hit) {
        if (playerMustBeFacing.isPresent()) {
            return eye.flatDirectionTo(hit) == playerMustBeFacing.get();
        }
        // TODO include the other conditions for stupid blocks like pistons
        return true;
    }

    private static final Vec3d[][] FACE_PROJECTION_CACHE;

    static {
        double[] diffs = {0.1, 0.5, 0.9};
        FACE_PROJECTION_CACHE = new Vec3d[Face.NUM_FACES][diffs.length * diffs.length];
        for (Face face : Face.VALUES) {
            int i = 0;
            for (double dx : diffs) {
                for (double dz : diffs) {
                    FACE_PROJECTION_CACHE[face.index][i++] = new Vec3d(project(new double[]{dx, dz}, face));
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

    static {
        if (Main.DEBUG) {
            sanityCheck();
        }
    }

    public static void sanityCheck() {
        // standing at 1,0,0
        // block to be placed at 0,0,0
        // placing against 0,0,-1

        // eye is at 1, 1.62, 0
        // north or west

        StringBuilder sanity = new StringBuilder();
        for (PlayerVantage vantage : new PlayerVantage[]{PlayerVantage.STRICT_CENTER, PlayerVantage.LOOSE_CENTER}) {
            for (Face playerFacing : new Face[]{Face.NORTH, Face.EAST, Face.WEST}) {
                sanity.append(vantage).append(playerFacing);
                List<Raytracer.Raytrace> traces = BlockStatePlacementOption.get(Face.NORTH, Half.BOTTOM, Optional.of(playerFacing)).computeTraceOptions(PlaceAgainstData.get(Half.BOTTOM, false), 1, 1.62, 0, vantage, 4);
                sanity.append(traces.size());
                sanity.append(" ");
                if (!traces.isEmpty()) {
                    for (double d : new double[]{traces.get(0).playerEye.x, traces.get(0).playerEye.z}) {
                        double base = d > 1 ? 1.5 : 0.5;
                        boolean a = d == base - LOOSE_CENTER_DISTANCE;
                        boolean b = d == base;
                        boolean c = d == base + LOOSE_CENTER_DISTANCE;
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

    private static List<Raytracer.Raytrace> sanityCheckTraces(List<Raytracer.Raytrace> traces) {
        if (Main.DEBUG && traces.stream().mapToDouble(Raytracer.Raytrace::centerDistApprox).distinct().count() > 2) {
            throw new IllegalStateException();
        }
        return traces;
    }
}
