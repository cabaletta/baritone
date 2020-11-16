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

package baritone.pathing.movement.movements;

import baritone.Baritone;
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.utils.*;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.*;
import baritone.utils.BlockStateInterface;
import baritone.utils.pathing.MutableMoveResult;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.*;

/**
 * Allows baritone to make jumps at different angles (diagonally)
 *
 * @author Zephreo
 */
public class MovementParkourAdv extends Movement {

    private static final BetterBlockPos[] EMPTY = new BetterBlockPos[]{};

    private static final EnumMap<EnumFacing, HashMap<Vec3i, JumpType>> ALL_VALID_DIR = new EnumMap<>(EnumFacing.class);
    private static final HashMap<Vec3i, JumpType> SOUTH_VALID = new HashMap<>();
    private static final HashMap<Vec3i, JumpType> WEST_VALID = new HashMap<>();
    private static final HashMap<Vec3i, JumpType> NORTH_VALID = new HashMap<>();
    private static final HashMap<Vec3i, JumpType> EAST_VALID = new HashMap<>();

    private static final HashMap<Vec3i, Double> DISTANCE_CACHE = new HashMap<>();

    // cost is similar to an equivalent straight flat jump in blocks
    private static final double ASCEND_DIST_PER_BLOCK = 0.6;
    private static final double DESCEND_DIST_PER_BLOCK = -0.2; // its easier to descend
    private static final double TURN_COST_PER_RADIAN = 0.3;

    private static final double PREP_OFFSET = 0.2215; // The default prep location for a jump

    private static final double MAX_JUMP_MOMENTUM = 5.3; // We can't make 1bm momentum jumps greater than this distance
    private static final double MAX_JUMP_SPRINT = 4.6; // We can't make flat sprint jumps greater than this distance
    private static final double MAX_JUMP_WALK = 3.48; // We can make the jump without sprinting below this distance

    private static final double MOVE_COST = SPRINT_ONE_BLOCK_COST; // Since WALK_ONE_BLOCK_COST is heavily penalised it sometimes chose longer sprint jumps over walking jumps. This is now the cost per move distance for all jumps (multiplied for harder ones).
    private static final double COST_ADDER = 3.1; // The amount to add to the cost by (in attempt to increase the cost of small jumps that may be prioritised over faster movements)

    // Calculated using MovementPrediction (add 1.6 ((Player hitbox width + block center) * 2) to get values similar to MAX_JUMP_...), These are flat jumps (12 ticks)
    private static final double WALK_JUMP_DISTANCE = 1.45574 / 12; // (12 ticks) 1.455740609867238, 1 down (14 ticks) = 1.7933772690401462
    private static final double SPRINT_JUMP_DISTANCE = 2.87582 / 12; // (12 ticks) 2.8758288311841866, 1 down (14 ticks) = 3.38866786230939
    // 2.87582 / 12 = 0.23965, 0.23965 * 2 = 0.4793, 3.38866 - 2.87582 = 0.51284, 0.51284 - 0.4793 = 0.03354 (Accurate enough), Average movement per tick used to extrapolate longer jumps vs Actual distance

    // Not 100% accurate
    private static final double MOMENTUM_JUMP_DISTANCE = (MAX_JUMP_MOMENTUM - 1.6) / 12;

    private static final boolean TEST_LOG = false;

    enum JumpType {
        NORMAL(MAX_JUMP_WALK, MAX_JUMP_SPRINT), // Normal run and jump
        NORMAL_STRAIGHT_DESCEND(MAX_JUMP_WALK, MAX_JUMP_SPRINT), // A type that will use the normal jump on descends only (Since MovementParkour doesn't do descends)
        MOMENTUM(-1, MAX_JUMP_MOMENTUM), // An extra momentum jump 1bm
        EDGE(MAX_JUMP_WALK, MAX_JUMP_SPRINT), // No run up (for higher angle jumps)
        EDGE_NEO(-1, 4); // Around the pillar

        final double maxJumpNoSprint;
        final double maxJumpSprint;

        JumpType(double maxJumpNoSprint, double maxJumpSprint) {
            this.maxJumpNoSprint = maxJumpNoSprint;
            this.maxJumpSprint = maxJumpSprint;
        }
    }

    static {
        // The jumps that are valid (forward amount + 1, horizontal amount, the technique to use (defaults to NORMAL))
        int[][] validQuadrant = {{2, 0, JumpType.NORMAL_STRAIGHT_DESCEND.ordinal()}, {3, 0, JumpType.NORMAL_STRAIGHT_DESCEND.ordinal()}, {4, 0, JumpType.NORMAL_STRAIGHT_DESCEND.ordinal()}, {5, 0, JumpType.MOMENTUM.ordinal()},
                {1, 1}, {2, 1}, {3, 1}, {4, 1}, {5, 1, JumpType.MOMENTUM.ordinal()},
                {0, 2, JumpType.EDGE_NEO.ordinal()}, {1, 2, JumpType.EDGE.ordinal()}, {2, 2, JumpType.EDGE.ordinal()}, {3, 2, JumpType.EDGE.ordinal()}, {4, 2, JumpType.MOMENTUM.ordinal()}, {5, 2, JumpType.MOMENTUM.ordinal()},
                {1, 3, JumpType.EDGE.ordinal()}, {2, 3, JumpType.EDGE.ordinal()}};
        for (int[] jump : validQuadrant) {
            int z = jump[0];
            for (int neg = -1; neg <= 1; neg += 2) { // -1 and 1
                int x = neg * jump[1];
                Vec3i southVec = new Vec3i(x, 0, z);
                JumpType type = JumpType.NORMAL;
                if (jump.length > 2) {
                    type = JumpType.values()[jump[2]];
                }
                SOUTH_VALID.put(southVec, type);
                WEST_VALID.put(rotateAroundY(southVec, Math.toRadians(-90)), type);
                NORTH_VALID.put(rotateAroundY(southVec, Math.toRadians(180)), type);
                EAST_VALID.put(rotateAroundY(southVec, Math.toRadians(90)), type);
            }
        }

        ALL_VALID_DIR.put(EnumFacing.SOUTH, SOUTH_VALID);
        ALL_VALID_DIR.put(EnumFacing.WEST, WEST_VALID);
        ALL_VALID_DIR.put(EnumFacing.NORTH, NORTH_VALID);
        ALL_VALID_DIR.put(EnumFacing.EAST, EAST_VALID);

        for (HashMap<Vec3i, JumpType> posbJumpsForDir : ALL_VALID_DIR.values()) {
            for (Vec3i vec : posbJumpsForDir.keySet()) {
                DISTANCE_CACHE.put(vec, vec.getDistance(0, 0, 0));
            }
        }
    }

    private final double moveDist;
    private final int ascendAmount;
    private final Vec3i direction;
    private final EnumFacing simpleDirection;
    private final JumpType type;

    private boolean inStartingPosition = false;
    private int atDestTicks = 0;
    private int ticksFromStart = -1;

    private MovementParkourAdv(CalculationContext context, BetterBlockPos src, BetterBlockPos dest, EnumFacing simpleDirection, JumpType type) {
        super(context.baritone, src, dest, EMPTY);
        direction = VecUtils.subtract(dest, src);
        moveDist = calcMoveDist(context, src.x, src.y, src.z, MovementHelper.isBottomSlab(context.get(src.down())) ? -0.5 : 0, direction, simpleDirection);
        this.ascendAmount = dest.y - src.y;
        this.simpleDirection = simpleDirection;
        this.type = type;
    }

    private static double getDistance(Vec3i vec, EnumFacing offset) {
        return getDistance(new Vec3i(vec.getX() - offset.getXOffset(), vec.getY(), vec.getZ() - offset.getZOffset())) + 1;
    }

    private static double getDistance(Vec3i vec) {
        if (DISTANCE_CACHE.containsKey(vec)) {
            return DISTANCE_CACHE.get(vec);
        } else {
            double distance = vec.getDistance(0, 0, 0);
            DISTANCE_CACHE.put(vec, distance);
            return distance;
        }
    }

    /**
     * Rotates the vector anti-clockwise around the y axis, by an angle radians.
     * The result is the block that the vector lies in. (resultant xyz coords floored)
     * Works best with angles that are multiples of pi/2 (90 degrees)
     *
     * @param input the input vector to rotate
     * @param angle amount to rotate anti-clockwise by, in radians
     * @return The rotated vector
     */
    private static Vec3i rotateAroundY(Vec3i input, double angle) {
        double angleCos = Math.cos(angle);
        double angleSin = Math.sin(angle);

        double x = angleCos * input.getX() + angleSin * input.getZ() + 0.1;
        double z = -angleSin * input.getX() + angleCos * input.getZ() + 0.1;

        return new Vec3i(x, input.getY(), z);
    }

    @Override
    public double calculateCost(CalculationContext context) {
        MutableMoveResult res = new MutableMoveResult();
        cost(context, src.x, src.y, src.z, res, simpleDirection);
        if (res.x != dest.x || res.y != dest.y || res.z != dest.z) {
            return COST_INF;
        }
        return res.cost;
    }

    /**
     * Normalizes an integer vector to a double vector.
     *
     * @param vec The integer vector to normalise
     * @return The normalised double vector
     */
    private static Vec3d normalize(Vec3i vec) {
        double length = getDistance(vec);
        double x = vec.getX() / length;
        double y = vec.getY() / length;
        double z = vec.getZ() / length;
        return new Vec3d(x, y, z);
    }

    static final HashMap<Vec3i, HashMap<Double, HashMap<Double, Set<Vec3i>>>> lineApproxCache = new HashMap<>();

    // cache to reduce object allocations?
    private static Set<Vec3i> getLineApprox(Vec3i vec, double overlap, double accPerBlock) {
        HashMap<Double, HashMap<Double, Set<Vec3i>>> lineApproxCache1 = lineApproxCache.get(vec);
        HashMap<Double, Set<Vec3i>> lineApproxCache2;
        Set<Vec3i> out;
        if (lineApproxCache1 == null) {
            lineApproxCache1 = new HashMap<>();
            lineApproxCache2 = null;
        } else {
            lineApproxCache2 = lineApproxCache1.get(overlap);
        }
        if (lineApproxCache2 == null) {
            lineApproxCache2 = new HashMap<>();
            out = null;
        } else {
            out = lineApproxCache2.get(accPerBlock);
        }
        if (!lineApproxCache2.containsKey(accPerBlock)) {
            out = approxBlocks(getLine(vec, accPerBlock), overlap);
            lineApproxCache2.put(accPerBlock, out);
        }
        if (!lineApproxCache1.containsKey(overlap)) {
            lineApproxCache1.put(overlap, lineApproxCache2);
        }
        if (!lineApproxCache.containsKey(vec)) {
            lineApproxCache.put(vec, lineApproxCache1);
        }
        return out;
    }

    public static List<Vec3d> getLine(Vec3i vector, double accPerBlock) {
        double length = getDistance(vector);
        ArrayList<Vec3d> line = new ArrayList<>();
        Vec3d vec = normalize(vector);
        for (double i = 0; i <= length; i += (1 / accPerBlock)) {
            line.add(vec.scale(i).add(0.5, 0.5, 0.5));
        }
        return line;
    }

    /**
     * Checks if each vector is pointing to a location close to the edge of a block. If so also returns the block next to that edge.
     *
     * @param vectors The vectors to approximate
     * @param overlap The size of the edge
     * @return An ordered set with the blocks that the vectors approximately lie in
     */
    public static Set<Vec3i> approxBlocks(Collection<Vec3d> vectors, double overlap) {
        LinkedHashSet<Vec3i> output = new LinkedHashSet<>();
        for (Vec3d vector : vectors) {
            output.addAll(approxBlock(vector, overlap));
        }
        return output;
    }

    /**
     * When the vector is pointing to a location close to the edge of a block also returns the block next to that edge.
     *
     * @param vector  The vector
     * @param overlap The size of the edge
     * @return The set of vectors that correspond to blocks at the approximate location of the input vector
     */
    public static Set<Vec3i> approxBlock(Vec3d vector, double overlap) {
        HashSet<Vec3i> output = new HashSet<>();
        for (int x = (int) (vector.x - overlap); x <= vector.x + overlap; x++) {
            for (int y = (int) (vector.y - overlap); y <= vector.y + overlap; y++) {
                for (int z = (int) (vector.z - overlap); z <= vector.z + overlap; z++) {
                    output.add(new Vec3i(x, y, z));
                }
            }
        }
        return output;
    }

    /**
     * When the vector is pointing to a location close to an XZ edge of a block also returns the block next to that XZ edge.
     *
     * @param vector  The vector
     * @param overlap The size of the edge in the XZ directions
     * @return The set of vectors that correspond to blocks at the approximate location of the input vector
     */
    public static Set<Vec3i> approxBlockXZ(Vec3d vector, double overlap) {
        HashSet<Vec3i> output = new HashSet<>();
        for (int x = (int) (vector.x - overlap); x <= vector.x + overlap; x++) {
            for (int z = (int) (vector.z - overlap); z <= vector.z + overlap; z++) {
                output.add(new Vec3i(x, vector.y, z));
            }
        }
        return output;
    }

    @Override
    public Set<BetterBlockPos> calculateValidPositions() {
        HashSet<BetterBlockPos> out = new HashSet<>();
        for (Vec3i vec : getLineApprox(direction, 2, 3)) {
            BetterBlockPos pos = new BetterBlockPos(src.add(vec));
            out.add(pos); // Jumping from blocks
            out.add(pos.up()); // Jumping into blocks
        }
        out.add(dest);
        out.add(src);
        return out;
    }

    private static final double PLAYER_HEIGHT = 1.8;

    // true if blocks are in the way
    private static boolean checkBlocksInWay(CalculationContext context, int srcX, int srcY, int srcZ, Vec3i jump, int extraAscend, EnumFacing jumpDirection, JumpType type, boolean sprint) {
        if (!MovementHelper.fullyPassable(context, srcX + jump.getX(), srcY + jump.getY() + extraAscend, srcZ + jump.getZ()) || !MovementHelper.fullyPassable(context, srcX + jump.getX(), srcY + jump.getY() + extraAscend + 1, srcZ + jump.getZ())) {
            return true; // Destination is blocked
        }
        Vec3i endPoint = VecUtils.add(jump, -jumpDirection.getXOffset(), extraAscend, -jumpDirection.getZOffset());
        if (type == JumpType.EDGE_NEO) {
            endPoint = VecUtils.add(endPoint, jumpDirection.getDirectionVec());
        }
        Set<Vec3i> jumpLine = getLineApprox(endPoint, 0.2, 1);

        int jumpBoost = getPotionEffectAmplifier(context.getBaritone().getPlayerContext(), MobEffects.JUMP_BOOST);
        double stepSize = sprint ? SPRINT_JUMP_DISTANCE : WALK_JUMP_DISTANCE; // estimates

        if (type == JumpType.MOMENTUM) {
            jumpLine.add(new Vec3i(-jumpDirection.getXOffset(), 2, -jumpDirection.getZOffset())); // The block above the src is entered during a momentum jump (check block above head)
            stepSize = MOMENTUM_JUMP_DISTANCE;
        }
        Iterator<Vec3i> jumpItr = jumpLine.iterator();

        double prevHeight = 0;
        int prevTick = 0;
        for (int i = 0; i < jumpLine.size(); i++) {
            Vec3i jumpVec = jumpItr.next();
            double distance = getDistance(jumpVec, jumpDirection);
            int tick = (int) (distance / stepSize) + 1;
            if (tick == prevTick + 1) {
                prevHeight += calcFallVelocity(tick, true, jumpBoost); // common faster
                prevTick = tick;
            } else if (tick != prevTick) {
                prevHeight = calcFallPosition(tick, true, jumpBoost); // less common slower
                prevTick = tick;
            }
            for (int j = (int) prevHeight; j <= PLAYER_HEIGHT + prevHeight; j++) { // Checks feet, head, for each block. (can double check some blocks on ascends/descends)
                // jumpDirection is subtracted at the beginning (re-added here)
                if (!MovementHelper.fullyPassable(context, jump.getX() + srcX + jumpDirection.getXOffset(), jump.getY() + srcY + j, jump.getZ() + srcZ + jumpDirection.getZOffset())) {
                    // System.out.println("Blocks in the way, block = " + VecUtils.add(jumpVec, srcX + jumpDirection.getXOffset(), srcY + j, srcZ + jumpDirection.getZOffset()) + " jump = " + new Vec3d(srcX, srcY, srcZ) + " -> " + new Vec3d(srcX + jump.getX(), srcY + jump.getY(), srcZ + jump.getZ()) + ", prevHeight = " + prevHeight);
                    return true;
                }
            }
        }
        /* The jump this is referring to (1 block towards src from destination) may not be possible? No (not much) time would be saved either since this is called last in determining whether a jump is possible.
        if (MovementHelper.canWalkOn(context.bsi, endPoint.getX(), endPoint.getY() - 1, endPoint.getZ())) {
            return true; // don't do a jump if you can make an easier jump?
        } //*/
        return false;
    }

    static MutableMoveResult lowestCost = new MutableMoveResult();

    public static MovementParkourAdv cost(CalculationContext context, BetterBlockPos src, EnumFacing simpleDirection) {
        MutableMoveResult res = new MutableMoveResult();
        cost(context, src.x, src.y, src.z, res, simpleDirection);
        JumpType type = ALL_VALID_DIR.get(simpleDirection).get(new Vec3i(res.x - src.x, 0, res.z - src.z));
        // System.out.println("type = " + type + ", jump = " + new Vec3i(res.x - src.x, 0, res.z - src.z) + ", dir = " + simpleDirection + ", cost = " + res.cost);
        return new MovementParkourAdv(context, src, new BetterBlockPos(res.x, res.y, res.z), simpleDirection, type);
    }

    public static void cost(CalculationContext context, int srcX, int srcY, int srcZ, MutableMoveResult res, EnumFacing simpleDirection) {
        if (!context.allowParkour || !context.allowParkourAdv) {
            return;
        }

        if (srcY == 256 && !context.allowJumpAt256) {
            return;
        }

        int xDiff = simpleDirection.getXOffset();
        int zDiff = simpleDirection.getZOffset();
        double extraAscend = 0;

        if (!MovementHelper.fullyPassable(context, srcX + xDiff, srcY, srcZ + zDiff)) {
            return; // block in foot in directly adjacent block
        }
        IBlockState adj = context.get(srcX + xDiff, srcY - 1, srcZ + zDiff);
        if (MovementHelper.canWalkOn(context.bsi, srcX + xDiff, srcY - 1, srcZ + zDiff, adj)) {
            return; // don't parkour if we could just traverse
        }
        if (MovementHelper.avoidWalkingInto(adj.getBlock()) && adj.getBlock() != Blocks.WATER && adj.getBlock() != Blocks.FLOWING_WATER) {
            return; // mostly for lava (we shouldn't jump with lava at our feet as we could take damage)
        }
        if (!MovementHelper.fullyPassable(context, srcX + xDiff, srcY + 1, srcZ + zDiff)) {
            return; // common case (makes all jumps in this direction invalid), block in head in directly adjacent block
        }
        if (!MovementHelper.fullyPassable(context, srcX + xDiff, srcY + 2, srcZ + zDiff)) {
            return; // common case (makes all jumps in this direction invalid), block above head in directly adjacent block
        }

        IBlockState standingOn = context.get(srcX, srcY - 1, srcZ);
        if (standingOn.getBlock() == Blocks.VINE || standingOn.getBlock() == Blocks.LADDER || standingOn.getBlock() instanceof BlockLiquid) {
            return; // Can't parkour from these blocks.
        }

        if (standingOn.getBlock() instanceof BlockStairs && standingOn.getValue(BlockStairs.HALF) != BlockStairs.EnumHalf.TOP && standingOn.getValue(BlockStairs.FACING) == simpleDirection.getOpposite()) {
            return; // we can't jump if the lower part of the stair is where we need to jump (we'll fall off)
        }

        if (MovementHelper.isBottomSlab(standingOn)) {
            if (!Baritone.settings().allowWalkOnBottomSlab.value) {
                return;
            }
            extraAscend += 0.5;
        }

        lowestCost.reset();

        for (Vec3i posbJump : ALL_VALID_DIR.get(simpleDirection).keySet()) {
            JumpType type = ALL_VALID_DIR.get(simpleDirection).get(posbJump);

            if ((type == JumpType.MOMENTUM || type == JumpType.EDGE_NEO) && !context.allowParkourMomentumOrNeo) {
                continue;
            }

            int destX = srcX + posbJump.getX();
            int destY = srcY;
            int destZ = srcZ + posbJump.getZ();

            final double maxJump;
            if (context.canSprint) {
                maxJump = type.maxJumpSprint;
            } else {
                maxJump = type.maxJumpNoSprint;
            }

            double moveDis;
            IBlockState destInto = context.bsi.get0(destX, destY, destZ);
            // Must ascend here as foot has block, && no block in head at destination (if ascend)
            if (!MovementHelper.fullyPassable(context.bsi.access, context.bsi.isPassableBlockPos.setPos(destX, destY, destZ), destInto) && type != JumpType.NORMAL_STRAIGHT_DESCEND) {
                moveDis = calcMoveDist(context, srcX, srcY, srcZ, extraAscend + 1, posbJump, simpleDirection);

                if (moveDis > maxJump) {
                    continue; // jump is too long (recalculated with new posbJump)
                }

                if (context.allowParkourAscend && MovementHelper.canWalkOn(context.bsi, destX, destY, destZ, destInto) /* && MovementParkour.checkOvershootSafety(context.bsi, destX + xDiff, srcY + 1, destZ + zDiff) */) {
                    destY += 1;

                    if (checkBlocksInWay(context, srcX, srcY, srcZ, posbJump, 1, simpleDirection, type, moveDis > MAX_JUMP_WALK)) {
                        continue; // Blocks are in the way
                    }

                    getMoveResult(context, srcX, srcY, srcZ, destX, destY, destZ, extraAscend + 1, posbJump, simpleDirection, type, 0, lowestCost, res);
                }
                continue;
            }

            moveDis = calcMoveDist(context, srcX, srcY, srcZ, extraAscend, posbJump, simpleDirection);
            if (moveDis > maxJump) {
                continue; // jump is too long (usually due to ascending (slab) or no sprint)
            }

            for (int descendAmount = type == JumpType.NORMAL_STRAIGHT_DESCEND ? 1 : 0; descendAmount < context.maxFallHeightNoWater; descendAmount++) {
                IBlockState landingOn = context.bsi.get0(destX, destY - descendAmount - 1, destZ);

                // farmland needs to be canWalkOn otherwise farm can never work at all, but we want to specifically disallow ending a jump on farmland
                if (landingOn.getBlock() != Blocks.FARMLAND && MovementHelper.canWalkOn(context.bsi, destX, destY - descendAmount - 1, destZ, landingOn)) {
                    if (checkBlocksInWay(context, srcX, srcY, srcZ, posbJump, -descendAmount, simpleDirection, type, (moveDis + descendAmount * DESCEND_DIST_PER_BLOCK) > MAX_JUMP_WALK)) {
                        continue; // Blocks are in the way
                    }
                    getMoveResult(context, srcX, srcY, srcZ, destX, destY - descendAmount, destZ, extraAscend - descendAmount, posbJump, simpleDirection, type, 0, lowestCost, res);
                }
            }

            // No block to land on, we now test for a parkour place

            if (!context.allowParkourPlace) {
                continue; // Settings don't allow a parkour place
            }

            IBlockState toReplace = context.get(destX, destY - 1, destZ);
            double placeCost = context.costOfPlacingAt(destX, destY - 1, destZ, toReplace);
            if (placeCost >= COST_INF) {
                continue; // Not allowed to place here
            }
            if (!MovementHelper.isReplaceable(destX, destY - 1, destZ, toReplace, context.bsi)) {
                continue; // Not allowed to place here
            }

            // Check if a block side is available/visible to place on
            for (int j = 0; j < 5; j++) {
                int againstX = destX + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[j].getXOffset();
                int againstY = destY - 1 + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[j].getYOffset();
                int againstZ = destZ + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[j].getZOffset();
                if (againstX == srcX + xDiff * 3 && againstZ == srcZ + zDiff * 3) { // we can't turn around that fast
                    continue;
                }
                if (MovementHelper.canPlaceAgainst(context.bsi, againstX, againstY, againstZ)) {
                    getMoveResult(context, srcX, srcY, srcZ, destX, destY, destZ, extraAscend, posbJump, simpleDirection, type, placeCost, lowestCost, res);
                }
            }
        }
        res.x = lowestCost.x;
        res.y = lowestCost.y;
        res.z = lowestCost.z;
        res.cost = lowestCost.cost;
        //*
        if (TEST_LOG && res.cost < COST_INF) {
            Vec3i jumpVec = new Vec3i(res.x - srcX, res.y - srcY, res.z - srcZ);
            System.out.println(new Vec3i(srcX, srcY, srcZ) + " -> " + new Vec3i(res.x, res.y, res.z) + ", Dir = " + simpleDirection + ", Cost: " + res.cost + ", Distance: " + getDistance(jumpVec, simpleDirection) + ", MoveDis: " + calcMoveDist(context, srcX, srcY, srcZ, extraAscend, jumpVec, simpleDirection));
        }
        //*/
    }

    private static void getMoveResult(CalculationContext context, int srcX, int srcY, int srcZ, int destX, int destY, int destZ, double extraAscend, Vec3i jump, EnumFacing jumpDirection, JumpType type, double costModifiers, MutableMoveResult curLowestCost, MutableMoveResult res) {
        res.x = destX;
        res.y = destY;
        res.z = destZ;
        res.cost = costFromJump(context, srcX, srcY, srcZ, jump.getX(), jump.getY() + extraAscend, jump.getZ(), jumpDirection, type) + costModifiers;
        // System.out.println("type = " + type + ", res = " + res.cost + ", curLowest = " + curLowestCost.cost);
        if (res.cost < curLowestCost.cost) {
            curLowestCost.x = res.x;
            curLowestCost.y = res.y;
            curLowestCost.z = res.z;
            curLowestCost.cost = res.cost;
        }
    }

    private static double calcMoveDist(CalculationContext context, int srcX, int srcY, int srcZ, double extraAscend, Vec3i jump, EnumFacing jumpDirection) {
        double ascendAmount = jump.getY() + extraAscend;

        // Accounting for slab height
        IBlockState destBlock = context.get(jump.getX() + srcX, jump.getY() + srcY - 1, +jump.getZ() + srcZ);
        if (MovementHelper.isBottomSlab(destBlock)) {
            ascendAmount -= 0.5;
        }

        jump = new Vec3i(jump.getX(), 0, jump.getZ());
        double distance = getDistance(jump, jumpDirection);

        // Modifying distance so that ascends have larger distances while descends have smaller
        if (ascendAmount > 0) {
            distance += ascendAmount * ASCEND_DIST_PER_BLOCK;
        } else {
            distance += ascendAmount * -DESCEND_DIST_PER_BLOCK; // ascendAmount is negative
        }

        // Calculating angle between vectors
        Vec3d jumpVec = new Vec3d(jump.getX() - jumpDirection.getXOffset(), 0, jump.getZ() - jumpDirection.getZOffset()).normalize();
        double angle = Math.acos(jumpVec.dotProduct(new Vec3d(jumpDirection.getDirectionVec()))); // in radians
        distance += TURN_COST_PER_RADIAN * angle;

        // reduce distance to approach a minimum value of 0.559 (prevents negatives/or very low distances)
        if (distance < 1.74) { // 1.74 = 3.48 [MAX_JUMP_WALK] / 2
            distance = Math.pow(1.1, distance) + 0.559; // 1.1^1.74 + 0.559 = 1.739
        }

        // This distance is unitless as it contains: modifiers on ascends/descends, and the slowdowns in changing directions midair (angle)
        return distance;
    }

    @Override
    public boolean safeToCancel(MovementState state) {
        // possibly safe if ctx.player().motion is low enough
        return state.getStatus() != MovementStatus.RUNNING;
    }

    private static double costFromJump(CalculationContext context, int srcX, int srcY, int srcZ, int jumpX, double jumpY, int jumpZ, EnumFacing jumpDirection, JumpType type) {
        jumpX -= jumpDirection.getXOffset();
        jumpZ -= jumpDirection.getZOffset();
        double distance = Math.sqrt(jumpX * jumpX + jumpY * jumpY + jumpZ * jumpZ) + 0.8;
        switch (type) {
            case EDGE: // for now edge jumps cost the same as normal ones (just setup differently)
            case NORMAL:
            case NORMAL_STRAIGHT_DESCEND:
                return MOVE_COST * distance + context.jumpPenalty + COST_ADDER;
            case MOMENTUM:
                return MOVE_COST * distance * 2 + context.jumpPenalty * 2 + COST_ADDER; // Momentum jumps are unsafe, therefore should have high costs.
            case EDGE_NEO:
                // dividing by 2 is also an option (but possibly slower? (casting back to int?))
                if (jumpX + jumpDirection.getXOffset() > 0) {
                    jumpX = 1;
                }
                if (jumpX + jumpDirection.getXOffset() < 0) {
                    jumpX = -1;
                }
                if (jumpZ + jumpDirection.getZOffset() > 0) {
                    jumpZ = 1;
                }
                if (jumpX + jumpDirection.getZOffset() < 0) {
                    jumpZ = -1;
                }
                if (MovementHelper.fullyPassable(context, srcX + jumpX, srcY + 1, srcZ + jumpZ)) {
                    return COST_INF; // don't neo if you can just do a normal jump
                }
                return MOVE_COST * distance * 2 + context.jumpPenalty * 3 + COST_ADDER; // due to neo's low distance they need extra multipliers
        }
        throw new UnsupportedOperationException("Add the new JumpType to this switch."); // Will never reach this unless a new type is added
    }

    @Override
    protected boolean prepared(MovementState state) {
        if (inStartingPosition || state.getStatus() == MovementStatus.WAITING) {
            return true;
        }
        Vec3d offset;
        double accuracy;
        switch (type) {
            case NORMAL:
            case NORMAL_STRAIGHT_DESCEND:
                offset = new Vec3d(simpleDirection.getOpposite().getDirectionVec()).scale(PREP_OFFSET);
                accuracy = 0.1;
                break;
            case MOMENTUM:
                offset = new Vec3d(simpleDirection.getOpposite().getDirectionVec()).scale(PREP_OFFSET);
                accuracy = 0.025; // Basically as small as you can get
                break;
            case EDGE:
            case EDGE_NEO:
                EnumFacing destDir = EnumFacing.fromAngle(MathHelper.atan2(src.x - dest.x, -(src.z - dest.z)) * RotationUtils.RAD_TO_DEG);
                offset = new Vec3d(simpleDirection.getDirectionVec()).scale(0.8).add(new Vec3d(destDir.getOpposite().getDirectionVec()).scale(0.2));
                accuracy = 0.025;
                break;
            default:
                throw new UnsupportedOperationException("Add the new JumpType to this switch.");
        }
        Vec3d preJumpPos = offset.add(src.x + 0.5, ctx.player().posY, src.z + 0.5);
        double distance = preJumpPos.distanceTo(ctx.playerFeetAsVec());
        // System.out.println("Distance to prepLoc = " + distance);
        boolean prepLocPassable = MovementHelper.fullyPassable(ctx, src.add(new BlockPos(offset.add(offset.normalize().scale(0.4))))); // Checking 0.4 blocks in the direction of offset for a block (0.3 is the player hitbox width)
        if (((distance > accuracy && prepLocPassable) || (distance > (PREP_OFFSET - (0.2 - accuracy)) && !prepLocPassable)) &&
                (atDestTicks < 8 || accuracy >= 0.1)) { // Accuracies over 0.1 will require additional wait ticks to reduce excess momentum
            if (atDestTicks < 6) {
                MovementHelper.moveTowards(ctx, state, offset.add(VecUtils.getBlockPosCenter(src)));
            }
            if (distance < 0.2) {
                state.setInput(Input.SNEAK, true);
                if (distance < accuracy) {
                    atDestTicks++;
                } else {
                    atDestTicks--;
                }
            } else {
                atDestTicks = 0;
            }
            if (ctx.playerFeet().y < src.y) {
                // we have fallen
                logDebug("Fallen during the prep phase");
                state.setStatus(MovementStatus.UNREACHABLE);
                return true; // to bypass prepare phase
            }
            return false;
        } else {
            logDebug("Achieved '" + (double) Math.round(distance * 10000) / 10000 + "' blocks of accuracy to preploc. Jump Direction = " + simpleDirection + ", Using technique '" + type + "'");
            inStartingPosition = true;
            prevMotionPredict = ctx.playerFeetAsVec();
            prevVelocityPredict = ctx.playerFeetAsVec();
            return true;
        }
    }

    /**
     * Calculates the fall velocity for a regular jump. (no glitches)
     * Does not account for blocks in the way.
     * Can be used to predict the next ticks location to possibly map out a jump tick by tick.
     *
     * @param ticksFromStart Ticks past since the jump began
     * @param jump           If jump is a jump and not a fall.
     * @param jumpBoostLvl   The level of jump boost on the player.
     * @return The (y-direction) velocity in blocks per tick
     */
    private static double calcFallVelocity(int ticksFromStart, boolean jump, int jumpBoostLvl) {
        if (ticksFromStart <= 0) {
            return 0;
        }
        double init = 0.42 + 0.1 * jumpBoostLvl;
        if (!jump) {
            init = -0.0784;
        }
        // V0 = 0
        // V1 = 0.42 (if jumping) OR -0.0784 (if falling)
        // VN+1 = (VN - 0.08) * 0.98
        // If |VN+1| < 0.003, it is 0 instead
        double vel = init * Math.pow(0.98, ticksFromStart - 1) + 4 * Math.pow(0.98, ticksFromStart) - 3.92;
        if (vel < -3.92) {
            vel = -3.92; // TERMINAL_VELOCITY
        }
        if (Math.abs(vel) < 0.003) {
            vel = 0; // MINIMUM_VELOCITY
        }
        return vel;
    }

    /**
     * Calculates the velocity the player would have a specified amount of ticks after a jump
     * Jump at tick 1
     *
     * @param ticksFromStart The amount of ticks since jumping
     * @param ctx            Player context (for jump boost)
     * @return The y velocity calculated (+ is up)
     */
    private static double calcFallVelocity(int ticksFromStart, boolean jump, IPlayerContext ctx) {
        return calcFallVelocity(ticksFromStart, jump, getPotionEffectAmplifier(ctx, MobEffects.JUMP_BOOST));
    }

    /**
     * Calculates the y position of the player relative to the jump y position
     * Jump at tick 1
     *
     * @param ticksFromStart The amount of ticks that have passed since jumping
     * @param jump           If the jump is a jump and not a fall
     * @param jumpBoostLvl   The level of jump boost active on the player
     * @return The relative y position of the player
     */
    private static double calcFallPosition(int ticksFromStart, boolean jump, int jumpBoostLvl) {
        int yPos = 0;
        for (int i = 1; i <= ticksFromStart; i++) {
            yPos += calcFallVelocity(i, jump, jumpBoostLvl);
        }
        return yPos;
    }

    /**
     * Calculates the y position of the player relative to the jump y position
     * Jump at tick 1
     *
     * @param ticksFromStart The amount of ticks that have passed since jumping
     * @param jump           If the jump is a jump and not a fall
     * @param ctx            The player context (for jump boost)
     * @return The relative y position of the player
     */
    private static double calcFallPosition(int ticksFromStart, boolean jump, IPlayerContext ctx) {
        return calcFallPosition(ticksFromStart, jump, getPotionEffectAmplifier(ctx, MobEffects.JUMP_BOOST));

    }

    /**
     * Calculates the time in ticks spent in the air after jumping
     *
     * @param ascendAmount` The y differance of the landing position (can be negative)
     * @param jump          If the jump is a jump and not a fall
     * @param jumpBoostLvl  Tne level of jump boost active on the player
     * @return The jump time in ticks
     */
    private static int calcJumpTime(double ascendAmount, boolean jump, int jumpBoostLvl) {
        if (ascendAmount == 0) {
            return 12; // Most common case
        }
        double maxJumpHeight = calcMaxJumpHeight(jump, jumpBoostLvl);
        if (ascendAmount > maxJumpHeight) {
            return -1; // Jump not possible
        }
        int ticks = 0;
        double prevHeight = -1;
        double newHeight = 0;
        while (prevHeight < newHeight || newHeight > ascendAmount) { // You can only land on a block when you are moving down
            ticks++;
            prevHeight = newHeight;
            newHeight += calcFallVelocity(ticks, jump, jumpBoostLvl);
        }
        return ticks - 1;
    }

    /**
     * Calculates the time in ticks spent in the air after jumping
     *
     * @param height The height we are landing at (relative to the jump height)
     * @param jump   If the jump is a jump and not a fall
     * @param ctx    The player context (for jump boost)
     * @return The jump time in ticks
     */
    private static int calcJumpTime(double height, boolean jump, IPlayerContext ctx) {
        return calcJumpTime(height, jump, getPotionEffectAmplifier(ctx, MobEffects.JUMP_BOOST));
    }

    /**
     * Gets the maximum jump height for a given jump boost lvl
     *
     * @param jump         If the jump is a jump and not a fall
     * @param jumpBoostLvl What level of jump boost is active on the player
     * @return The relative jump height
     */
    private static double calcMaxJumpHeight(boolean jump, int jumpBoostLvl) {
        if (!jump) {
            return 0;
        }
        int ticks = 1;
        double prevHeight = -1;
        double newHeight = 0;
        while (prevHeight < newHeight) {
            prevHeight = newHeight;
            newHeight += calcFallVelocity(ticks, jump, jumpBoostLvl);
            ticks++;
        }
        return prevHeight;
    }

    private static int getPotionEffectAmplifier(IPlayerContext ctx, Potion effect) {
        if (Baritone.settings().considerPotionEffects.value && ctx.player().isPotionActive(effect)) {
            return ctx.player().getActivePotionEffect(effect).getAmplifier() + 1;
        } else {
            return 0;
        }
    }

    /**
     * Move in the direction of moveDirYaw while facing towards playerYaw
     * (excluding MOVE_BACK)
     *
     * @param angleDiff  moveDirYaw - playerYaw
     * @return The input required
     */
    private static Input sideMove(double angleDiff) {
        if (angleDiff > 180) {
            angleDiff = 180 - angleDiff;
        }
        if (angleDiff < -180) {
            angleDiff = -180 - angleDiff;
        }
        // System.out.println("Side move diff of " + diff);
        if (angleDiff >= 20) {
            return Input.MOVE_RIGHT;
        }
        if (angleDiff <= -20) {
            return Input.MOVE_LEFT;
        }
        if (Math.abs(angleDiff) < 20) {
            return Input.MOVE_FORWARD;
        }
        return null; // Not possible
    }

    /**
     * Move in the direction of moveDirYaw while facing towards playerYaw
     * (excluding MOVE_BACK)
     *
     * @param playerYaw  The yaw we are facing towards
     * @param moveDirYaw The yaw (angle) to move towards
     * @return The input required
     */
    private static Input sideMove(double playerYaw, double moveDirYaw) {
        return sideMove(moveDirYaw - playerYaw);
    }

    /**
     * Move in the direction of moveDirYaw while facing towards playerLookDest
     * (excluding MOVE_BACK)
     *
     * @param src            The block we are at
     * @param playerLookDest The block we are facing towards
     * @param moveDest       The block to move towards
     * @return The input required
     */
    private static Input sideMove(Vec3i src, Vec3i playerLookDest, Vec3i moveDest) {
        // Make vectors relative
        playerLookDest = VecUtils.subtract(playerLookDest, src);
        moveDest = VecUtils.subtract(moveDest, src);
        return sideMove(MathHelper.atan2(playerLookDest.getX(), -playerLookDest.getZ()) * RotationUtils.RAD_TO_DEG,
                MathHelper.atan2(moveDest.getX(), -moveDest.getZ()) * RotationUtils.RAD_TO_DEG);
    }

    Vec3d prevMotionPredict = ctx.playerFeetAsVec();
    Vec3d prevVelocityPredict = ctx.playerFeetAsVec();
    Vec3d totalMotionDiff = new Vec3d(0, 0, 0);
    Vec3d totalVelocityDiff = new Vec3d(0, 0, 0);

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        if (ticksFromStart > 40) { // Should generally have <= 12 (exceptions are descending jumps)
            logDebug("jump timed out");
            return state.setStatus(MovementStatus.FAILED);
        }

        ticksFromStart++;

        if (ctx.playerFeet().y < (src.y + Math.min(ascendAmount, 0))) {
            // we have fallen
            logDebug("Fallen during jump phase");
            return state.setStatus(MovementStatus.UNREACHABLE);
        }

        if (moveDist > MAX_JUMP_WALK ||
                type == JumpType.MOMENTUM ||
                type == JumpType.EDGE_NEO) {
            state.setInput(Input.SPRINT, true);
        }

        double jumpMod = 0.2; // Amount to shift the jump location by (towards the destination) (0.2 is max if block is present)
        double xJumpMod = jumpMod - Math.abs(simpleDirection.getXOffset()) * jumpMod; // perpendicular to offset
        double zJumpMod = jumpMod - Math.abs(simpleDirection.getZOffset()) * jumpMod;
        if ((dest.getX() - src.getX()) < 0) {
            xJumpMod = -xJumpMod;
        }
        if ((dest.getZ() - src.getZ()) < 0) {
            zJumpMod = -zJumpMod;
        }

        final double JUMP_OFFSET = 0.33;

        Vec3d jumpLoc = new Vec3d(src.getX() + 0.5 + xJumpMod + (simpleDirection.getXOffset() * (0.5 + JUMP_OFFSET)), src.getY(),
                src.getZ() + 0.5 + zJumpMod + (simpleDirection.getZOffset() * (0.5 + JUMP_OFFSET)));
        Vec3d startLoc = new Vec3d(src.getX() + 0.5 - (simpleDirection.getXOffset() * 0.3), src.getY(),
                src.getZ() + 0.5 - (simpleDirection.getZOffset() * 0.3));
        Vec3d destVec = new Vec3d(dest.getX() + 0.5 - ctx.player().posX, dest.getY() - ctx.player().posY, dest.getZ() + 0.5 - ctx.player().posZ); // The vector pointing from the players location to the destination

        double curDist = Math.sqrt(ctx.playerFeetAsVec().squareDistanceTo(dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5));
        double distToJumpXZ = ctx.playerFeetAsVec().distanceTo(jumpLoc.subtract(0, jumpLoc.y - ctx.playerFeetAsVec().y, 0));
        double distFromStart = ctx.playerFeetAsVec().distanceTo(startLoc);
        double distFromStartXZ = ctx.playerFeetAsVec().distanceTo(startLoc.subtract(0, startLoc.y - ctx.playerFeetAsVec().y, 0));

        Vec3d motionVec = new Vec3d(ctx.player().motionX, ctx.player().motionY, ctx.player().motionZ);
        MovementHelper.moveTowards(ctx, state, dest); // set initial look direction (for prediction)
        MovementPrediction.PredictionResult future = MovementPrediction.getFutureLocation(ctx.player(), state, 1); // The predicted location 1 tick in the future

        Vec3d overshootVec = destVec.normalize().scale(2).subtract(motionVec.normalize()).normalize().add(ctx.playerFeetAsVec()); // The unit vector pointing in the ( direction that cancels out motion away from the destination ) + the current position
        double speedXZ = motionVec.distanceTo(new Vec3d(0, motionVec.y, 0));
        if (speedXZ < 0.15) {
            overshootVec = destVec.normalize().add(ctx.playerFeetAsVec());
        }

        /*
        logDebug("Overshoot = " + overshootVec.subtract(ctx.playerFeetAsVec()) + ", og = " + destVec.normalize() + ", difference = " + destVec.normalize().subtract(overshootVec.subtract(ctx.playerFeetAsVec())));
        logDebug("Motion = " + motionVec + ", Speed = " + motionVec.length() + ", XZ = " + speedXZ);
        logDebug("FutureLoc = " + futureLoc);
        //*/

        Vec3d curDest = null; // The current destination (position we are moving towards)

        if (ctx.playerFeet().equals(src) || ctx.playerFeet().equals(src.up()) || (distToJumpXZ < 0.5 && distFromStartXZ < 1.2)) {
            // logDebug("Moving to jump, on src = " + ctx.playerFeet().equals(src) + ", or above = " + ctx.playerFeet().equals(src.up()));

            switch (type) {
                case NORMAL:
                case NORMAL_STRAIGHT_DESCEND:
                    MovementHelper.moveTowards(ctx, state, src.offset(simpleDirection, 2));
                    break;
                case MOMENTUM:
                    if (ticksFromStart == 0) {
                        logDebug("Momentum jump");
                        state.setInput(Input.JUMP, true);
                        state.getInputStates().remove(Input.MOVE_FORWARD);
                    } else if (ticksFromStart >= 1) {
                        MovementHelper.moveTowards(ctx, state, src.offset(simpleDirection, 2));
                        if (ticksFromStart >= 9) {
                            MovementHelper.moveTowards(ctx, state, dest);
                            double xDiff = Math.abs(src.getX() + 0.5 - future.posX);
                            double zDiff = Math.abs(src.getZ() + 0.5 - future.posZ);
                            // logDebug("Pos = " + ctx.playerFeetAsVec() + ", xDiff = " + xDiff + ", zDiff = " + zDiff + ", futureCollision? = " + future.collidedVertically + ", future = " + future.getPosition() + ", test = " + Math.abs(ctx.player().posX - src.getX() - 0.5) + " > " + (0.8 - (0.15 * ((12 - ticksFromStart) * 0.5 + 1))));
                            if (!future.collidedVertically && !(future.posY > src.getY() && xDiff < 0.8 && zDiff < 0.8)) {
                                if (Math.abs(ctx.player().posX - src.getX() - 0.5) > 0.8 - (0.15 * ((12 - ticksFromStart) * 0.5 + 1)) || Math.abs(ctx.player().posZ - src.getZ() - 0.5) > 0.8 - (0.15 * ((12 - ticksFromStart) * 0.5 + 1))) {
                                    // logDebug("Removing walk");
                                    state.getInputStates().remove(Input.MOVE_FORWARD); // we did not land?
                                } else if (Math.abs(ctx.player().posX - src.getX() - 0.5) > 0.8 - SPRINT_JUMP_DISTANCE || Math.abs(ctx.player().posZ - src.getZ() - 0.5) > 0.8 - SPRINT_JUMP_DISTANCE) {
                                    // logDebug("Removing sprint");
                                    state.getInputStates().remove(Input.SPRINT); // The remaining distance should be covered without sprinting
                                }
                            } else if (future.posY < src.getY()) {
                                // logDebug("Back tracking");
                                state.getInputStates().remove(Input.MOVE_FORWARD);
                                state.setInput(Input.MOVE_BACK, true);
                            }
                        } else if (ticksFromStart <= 2) {
                            state.getInputStates().remove(Input.SPRINT); // not sprinting for a few ticks
                            MovementHelper.moveTowards(ctx, state, src.offset(simpleDirection));
                        } else {
                            MovementHelper.moveTowards(ctx, state, src.offset(simpleDirection, 2));
                        }
                    } else {
                        state.getInputStates().remove(Input.MOVE_FORWARD); // don't move for a few ticks
                    }
                    break;
                case EDGE:
                    MovementHelper.moveTowards(ctx, state, dest);
                    break;
                case EDGE_NEO:
                    MovementHelper.moveTowards(ctx, state, new Vec3d(simpleDirection.getDirectionVec()).scale(0.3).add(VecUtils.getBlockPosCenter(dest)));
                    state.setInput(sideMove(src, dest, src.offset(simpleDirection)), true);
                    break;
                default:
                    throw new UnsupportedOperationException("Add new movement to this switch.");
            }
        } else if (curDist < 1 && ctx.player().onGround) {
            if (motionVec.length() < 0.3) {
                atDestTicks++;
                if (atDestTicks >= 3) {
                    logDebug("Canceled momentum for " + atDestTicks + " ticks, distance = " + (double) Math.round(curDist * 10000) / 10000);
                    return state.setStatus(MovementStatus.SUCCESS);
                }
            }
            MovementHelper.moveTowards(ctx, state, overshootVec);
            // logDebug("Cancelling momentum, dis = " + curDist);
        } else {
            MovementHelper.moveTowards(ctx, state, dest);
            curDest = VecUtils.getBlockPosCenter(dest);
            // logDebug("Moving to destination, dis = " + curDist);
            atDestTicks = 0;
        }

        if (curDest != null &&
                type != JumpType.EDGE_NEO && type != JumpType.MOMENTUM) { // EDGE_NEO and MOMENTUM jumps are completed with very low room for error, dodging an obstacle will lead to missing the jump due to the slight decrease in speed
            // This is called after movement to also factor in key presses and look direction
            int ticksRemaining = calcJumpTime(ascendAmount, true, ctx) - ticksFromStart;
            MovementPrediction.PredictionResult future5 = MovementPrediction.getFutureLocation(ctx.player(), state, Math.min(5, ticksRemaining)); // The predicted location a few ticks in the future

            // logDebug("Prediction5 = " + future5.getPosition() + ", ticks = " + Math.min(5, ticksRemaining));

            // adjust movement to attempt to dodge obstacles
            if (future5.collidedHorizontally && future.posY > dest.getY()) {
                double angleDiff = VecUtils.getYaw(destVec.subtract(future5.getPosition())) - ctx.playerRotations().normalize().getYaw();
                if(Math.abs(angleDiff) > 20) {
                    logDebug("Adjusting movement to dodge an obstacle. Predicted collision location = " + future5.getPosition());
                    state.setInput(sideMove(angleDiff), true);
                }
            }
        }

        if (ctx.playerFeet().equals(dest)) {
            Block d = BlockStateInterface.getBlock(ctx, dest);
            if (d == Blocks.VINE || d == Blocks.LADDER) {
                return state.setStatus(MovementStatus.SUCCESS);
            }
        } else if (!ctx.playerFeet().equals(src) && // Don't jump on the src block (too early)
                (((Math.abs(future.posX - (src.x + 0.5)) > 0.8 || Math.abs(future.posZ - (src.z + 0.5)) > 0.8) && distFromStart < 1.2) || // Centre 0.5 + Player hitbox 0.3 = 0.8, if we are this distance from src, jump
                        (type == JumpType.MOMENTUM && distToJumpXZ < 0.6) || // During a momentum jump the momentum jump will position us so just jump whenever possible (i.e. as soon as we land)
                        ((type == JumpType.EDGE || type == JumpType.EDGE_NEO) && distFromStart < 1))  // The prepLoc of an edge jump is on the edge of the block so just jump straight away
                && ctx.player().onGround) { // To only log Jumping when we can actually jump
            if (type != JumpType.EDGE_NEO) {
                MovementHelper.moveTowards(ctx, state, dest); // make sure we are looking at the target when we jump for sprint jump bonuses
                if (type == JumpType.MOMENTUM) {
                    state.setInput(Input.SPRINT, true);
                }
            }
            state.setInput(Input.JUMP, true);
            logDebug("Jumping");
            ticksFromStart = 0; // Reset ticks from momentum/run-up phase
        }

        if (TEST_LOG) {
            MovementPrediction.PredictionResult r = MovementPrediction.getFutureLocation(ctx.player(), state, 1); // Predict location for next tick
            logDebug("Prediction = " + new Vec3d(r.posX, r.posY, r.posZ));

            //*
            Vec3d motionDiff = new Vec3d(Math.abs(prevMotionPredict.x - ctx.playerFeetAsVec().x), Math.abs(prevMotionPredict.y - ctx.playerFeetAsVec().y), Math.abs(prevMotionPredict.z - ctx.playerFeetAsVec().z));
            Vec3d velocityDiff = new Vec3d(Math.abs(prevVelocityPredict.x - ctx.playerFeetAsVec().x), Math.abs(prevVelocityPredict.y - ctx.playerFeetAsVec().y), Math.abs(prevVelocityPredict.z - ctx.playerFeetAsVec().z));
            prevMotionPredict = motionVec.add(ctx.playerFeetAsVec());
            prevVelocityPredict = new Vec3d(r.posX, r.posY, r.posZ);
            totalMotionDiff = totalMotionDiff.add(motionDiff);
            totalVelocityDiff = totalVelocityDiff.add(velocityDiff);

            logDebug("distToJumpXZ = " + distToJumpXZ + ", distFromStart = " + distFromStart + ", distFromStartXZ = " + distFromStartXZ + ", ticksFromStart = " + ticksFromStart);
            logDebug("Player coords = " + ctx.playerFeetAsVec());
            logDebug("MotionDiff = " + motionDiff + ", total = " + totalMotionDiff);
            logDebug("VelocityDiff = " + velocityDiff + ", total = " + totalVelocityDiff);
            //*/
        }

        return state;
    }
}
