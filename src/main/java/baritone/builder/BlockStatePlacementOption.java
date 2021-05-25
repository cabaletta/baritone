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

/**
 * A plane against which this block state can be placed
 * <p>
 * For a normal block, this will be a full face of a block. In that case, this class is no more than an EnumFacing
 * <p>
 * For a block like a slab or a stair, this will contain the information that the placement must be against the top or bottom half of the face
 * <p>
 * For a block like a furnace, this will contain the information that the player must be facing a specific horizontal direction in order to get the desired orientation
 * <p>
 * For a block like a piston, dispenser, or observer, this will contain the information that be player must pass a combination of: specific relative eye coordinate, specific relative X Z, and specific horizontal facing
 */
public class BlockStatePlacementOption {

    /**
     * e.g. a torch placed down on the ground is placed against the bottom of "the torch bounding box", so this would be DOWN for the torch
     */
    public final Face against;
    public final Half half;
    public final Optional<Face> playerMustBeHorizontalFacing; // getHorizontalFacing
    /**
     * IMPORTANT this is the RAW getDirectionFromEntityLiving meaning that it is the OPPOSITE of getHorizontalFacing (when in the horizontal plane)
     */
    public final Optional<Face> playerMustBeEntityFacing; // EnumFacing.getDirectionFromEntityLiving, used by piston, dispenser, observer

    private BlockStatePlacementOption(Face against, Half half, Optional<Face> playerMustBeHorizontalFacing, Optional<Face> playerMustBeEntityFacing) {
        Objects.requireNonNull(against);
        Objects.requireNonNull(half);
        this.against = against;
        this.half = half;
        this.playerMustBeHorizontalFacing = playerMustBeHorizontalFacing;
        this.playerMustBeEntityFacing = playerMustBeEntityFacing;
        validate(against, half, playerMustBeHorizontalFacing, playerMustBeEntityFacing);
    }

    /**
     * This value must be greater than the face projections.
     * <p>
     * Otherwise certain stair placements would not work. This is verified in this class's sanityCheck.
     */
    private static final double LOOSE_CENTER_DISTANCE = 0.15;

    public List<Raytracer.Raytrace> computeTraceOptions(PlaceAgainstData placingAgainst, int playerSupportingX, int playerFeetBlips, int playerSupportingZ, PlayerVantage vantage, double blockReachDistance) {
        if (!BlockStateCachedData.possible(this, placingAgainst)) {
            throw new IllegalStateException();
        }
        if (Main.DEBUG && placingAgainst.streamRelativeToPlace().noneMatch(hit -> hitOk(half, hit))) {
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
                .map(playerEyeXZ -> new Vec3d(playerEyeXZ.x, Blip.playerEyeFromFeetBlips(playerFeetBlips, placingAgainst.mustSneak), playerEyeXZ.z))
                .flatMap(eye ->
                        placingAgainst.streamRelativeToPlace()
                                .filter(hit -> hitOk(half, hit))
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

    public static boolean hitOk(Half half, Vec3d hit) {
        if (half == Half.EITHER) {
            return true;
        } else if (hit.y == 0.1) {
            return half == Half.BOTTOM;
        } else if (hit.y == 0.5) {
            return false;
        } else if (hit.y == 0.9) {
            return half == Half.TOP;
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * In EnumFacing.getDirectionFromEntityLiving, it checks if the player feet is within 2 blocks of the center of the block to be placed.
     * Normally, this is a nonissue, but a problem arises because we are considering hypothetical placements where the player stands at the exact +0.5,+0.5 center of a block.
     * In that case, it's possible for our hypothetical to have the player at precisely 2 blocks away, i.e. precisely on the edge of this condition being true or false.
     * For that reason, we treat those exact cases as "ambiguous". So, if the distance is within this tolerance of 2 (so, 1.99 to 2.01), we treat it as a "could go either way",
     * because when we really get there in-game, floating point inaccuracy could indeed actually make it go either way.
     */
    private static final double ENTITY_FACING_TOLERANCE = 0.01;

    private boolean directionOk(Vec3d eye, Vec3d hit) {
        if (playerMustBeHorizontalFacing.isPresent()) {
            return eye.flatDirectionTo(hit) == playerMustBeHorizontalFacing.get();
        }
        if (playerMustBeEntityFacing.isPresent()) { // handle piston, dispenser, dropper, observer
            if (!hit.inOriginUnitVoxel()) {
                throw new IllegalStateException();
            }
            Face entFace = playerMustBeEntityFacing.get();
            // see EnumFacing.getDirectionFromEntityLiving
            double dx = Math.abs(eye.x - 0.5);
            double dz = Math.abs(eye.z - 0.5);
            if (dx < 2 - ENTITY_FACING_TOLERANCE && dz < 2 - ENTITY_FACING_TOLERANCE) { // < 1.99
                if (eye.y < 0) { // eye below placement level = it will be facing down, so this is only okay if we want that
                    return entFace == Face.DOWN;
                }
                if (eye.y > 2) { // same for up, if y>2 then it will be facing up
                    return entFace == Face.UP;
                }
            } else if (!(dx > 2 + ENTITY_FACING_TOLERANCE || dz > 2 + ENTITY_FACING_TOLERANCE)) { // > 2.01
                // this is the ambiguous case, because we are neither unambiguously both-within-2 (previous case), nor unambiguously either-above-two (this elseif condition).
                // UP/DOWN are impossible, but that's caught by flat check
                if (eye.y < 0 || eye.y > 2) { // this check is okay because player eye height is not an even multiple of blips, therefore there's no way for it to == 0 or == 2, so using > and < is safe
                    return false; // anything that could cause up/down instead of horizontal is also not allowed sadly
                }
            } // else we are in unambiguous either-above-two, putting us in simple horizontal mode, so fallthrough to flat condition is correct, yay
            return eye.flatDirectionTo(hit) == entFace.opposite();
        }
        return true;
    }

    public static BlockStatePlacementOption get(Face against, Half half, Optional<Face> playerMustBeHorizontalFacing, Optional<Face> playerMustBeEntityFacing) {
        BlockStatePlacementOption ret = PLACEMENT_OPTION_SINGLETON_CACHE[against.index][half.ordinal()][Face.OPTS.indexOf(playerMustBeHorizontalFacing)][Face.OPTS.indexOf(playerMustBeEntityFacing)];
        if (ret == null) {
            throw new IllegalStateException(against + " " + half + " " + playerMustBeHorizontalFacing + " " + playerMustBeEntityFacing);
        }
        return ret;
    }

    private static final BlockStatePlacementOption[][][][] PLACEMENT_OPTION_SINGLETON_CACHE;

    static {
        PLACEMENT_OPTION_SINGLETON_CACHE = new BlockStatePlacementOption[Face.NUM_FACES][Half.values().length][Face.OPTS.size()][Face.OPTS.size()];
        for (Face against : Face.VALUES) {
            for (Half half : Half.values()) {
                for (Optional<Face> horizontalFacing : Face.OPTS) {
                    for (Optional<Face> entityFacing : Face.OPTS) {
                        try {
                            PLACEMENT_OPTION_SINGLETON_CACHE[against.index][half.ordinal()][Face.OPTS.indexOf(horizontalFacing)][Face.OPTS.indexOf(entityFacing)] = new BlockStatePlacementOption(against, half, horizontalFacing, entityFacing);
                        } catch (RuntimeException ex) {}
                    }
                }
            }
        }
    }

    static {
        for (int i = 0; i < Face.OPTS.size(); i++) {
            if (Face.OPTS.indexOf(Face.OPTS.get(i)) != i) {
                throw new IllegalStateException();
            }
            if (Face.OPTS.get(i).map(face -> face.index).orElse(Face.NUM_FACES) != i) {
                throw new IllegalStateException();
            }
        }
    }

    private void validate(Face against, Half half, Optional<Face> playerMustBeHorizontalFacing, Optional<Face> playerMustBeEntityFacing) {
        if (playerMustBeEntityFacing.isPresent() && playerMustBeHorizontalFacing.isPresent()) {
            throw new IllegalStateException();
        }
        if (against.vertical && half != Half.EITHER) {
            throw new IllegalArgumentException();
        }
        if (Main.STRICT_Y && against == Face.UP) {
            throw new IllegalStateException();
        }
        playerMustBeHorizontalFacing.ifPresent(face -> {
            if (face.vertical) {
                throw new IllegalArgumentException();
            }
            if (face == against.opposite()) {
                throw new IllegalStateException();
            }
        });
        playerMustBeEntityFacing.ifPresent(face -> {
            if (half != Half.EITHER) {
                throw new IllegalStateException();
            }
            if (against == face) { // impossible because EnumFacing inverts the horizontal facing AND because the down and up require the eye to be <0 and >2 respectively
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
                List<Raytracer.Raytrace> traces = BlockStatePlacementOption.get(Face.NORTH, Half.BOTTOM, Optional.of(playerFacing), Optional.empty()).computeTraceOptions(new PlaceAgainstData(Face.SOUTH, Half.EITHER, false), 1, 0, 0, vantage, 4);
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
