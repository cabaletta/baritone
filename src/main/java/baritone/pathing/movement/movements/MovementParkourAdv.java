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
    private static final double DESCEND_DIST_PER_BLOCK = -0.3; // its easier to descend
    private static final double TURN_COST_PER_RADIAN = 0.3;

    private static final double PREP_OFFSET = 0.2215; // The default prep location for a jump

    private static final double MAX_JUMP_MOMENTUM = 5.3; // We can't make 1bm momentum jumps greater than this distance
    private static final double MAX_JUMP_SPRINT = 4.6; // We can't make flat sprint jumps greater than this distance
    private static final double MAX_JUMP_WALK = 3.48; // We can make the jump without sprinting below this distance

    private static final double MOVE_COST = SPRINT_ONE_BLOCK_COST; // Since WALK_ONE_BLOCK_COST is heavily penalised it sometimes chose longer sprint jumps over walking jumps. This is now the cost per move distance for all jumps (multiplied for harder ones).
    private static final double COST_MODIFIER = 1.3; // The amount to multiply the cost by (in attempt to magnify the differences between jumps)

    enum JumpType {
        NORMAL(MAX_JUMP_WALK, MAX_JUMP_SPRINT), // Normal run and jump
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
        int[][] validQuadrant = {{5, 0, JumpType.MOMENTUM.ordinal()},
                {1, 1}, {2, 1}, {3, 1}, {4, 1}, {5, 1, JumpType.MOMENTUM.ordinal()},
                {0, 2, JumpType.EDGE_NEO.ordinal()}, {1, 2, JumpType.EDGE.ordinal()}, {2, 2, JumpType.EDGE.ordinal()}, {3, 2, JumpType.EDGE.ordinal()}, {4, 2, JumpType.MOMENTUM.ordinal()}, {5, 2, JumpType.MOMENTUM.ordinal()},
                {1, 3, JumpType.EDGE.ordinal()}};
        for (int[] jump : validQuadrant) {
            int z = jump[0];
            for (int neg = -1; neg < 2; neg += 2) { // -1 and 1
                int x = neg * jump[1];
                Vec3i southVec = new Vec3i(x, 0, z);
                JumpType type = JumpType.NORMAL;
                if(jump.length > 2) {
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

    private static double getDistance(Vec3i vec, EnumFacing offset) {
        return getDistance(new Vec3i(vec.getX() - offset.getXOffset(), vec.getY(), vec.getZ() - offset.getZOffset())) + 1;
    }

    private static double getDistance(Vec3i vec) {
        if(DISTANCE_CACHE.containsKey(vec)) {
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
     * @return      The rotated vector
     */
    private static Vec3i rotateAroundY(Vec3i input, double angle) {
        double angleCos = Math.cos(angle);
        double angleSin = Math.sin(angle);

        double x = angleCos * input.getX() + angleSin * input.getZ() + 0.1;
        double z = -angleSin * input.getX() + angleCos * input.getZ() + 0.1;

        return new Vec3i(x, input.getY(), z);
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
        moveDist = calcMoveDist(context, src, MovementHelper.isBottomSlab(context.get(src.down())) ? -0.5 : 0, direction, simpleDirection);
        this.ascendAmount = dest.y - src.y;
        this.simpleDirection = simpleDirection;
        this.type = type;
    }

    @Override
    public double calculateCost(CalculationContext context) {
        MutableMoveResult res = new MutableMoveResult();
        cost(context, src, res, simpleDirection);
        if (res.x != dest.x || res.y != dest.y || res.z != dest.z) {
            return COST_INF;
        }
        return res.cost;
    }

    /**
     * Normalizes an integer vector to a double vector.
     *
     * @param vec   The integer vector to normalise
     * @return      The normalised double vector
     */
    private static Vec3d normalize(Vec3i vec) {
        double length = getDistance(vec);
        double x = vec.getX() / length;
        double y = vec.getY() / length;
        double z = vec.getZ() / length;
        return new Vec3d(x, y, z);
    }

    private static Set<Vec3i> getLineApprox(Vec3i vec, double overlap, double accPerBlock) {
        return approxBlocks(getLine(vec, accPerBlock), overlap);
    }

    public static List<Vec3d> getLine(Vec3i vector, double accPerBlock) {
        double length = Math.ceil(getDistance(vector));
        ArrayList<Vec3d> line = new ArrayList<>();
        for (double i = 0; i <= length - (1 / accPerBlock); i += (1 / accPerBlock)) {
            line.add(normalize(vector).scale(i).add(0.5, 0.5, 0.5));
        }
        return line;
    }

    /**
     * Checks if each vector is pointing to a location close to the edge of a block. If so also returns the block next to that edge.
     *
     * @param vectors   The vectors to approximate
     * @param overlap   The size of the edge
     * @return          An ordered set with the blocks that the vectors approximately lie in
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
     * @param vector    The vector
     * @param overlap   The size of the edge
     * @return          The set of vectors that correspond to blocks at the approximate location of the input vector
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
     * @param vector    The vector
     * @param overlap   The size of the edge in the XZ directions
     * @return          The set of vectors that correspond to blocks at the approximate location of the input vector
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
    private static boolean checkBlocksInWay(CalculationContext context, BetterBlockPos src, Vec3i jump, EnumFacing jumpDirection, JumpType type) {
        BlockPos dest = src.add(jump);
        if (!MovementHelper.fullyPassable(context, dest.getX(), dest.getY(), dest.getZ()) ||  !MovementHelper.fullyPassable(context, dest.getX(), dest.getY() + 1, dest.getZ())) {
            return true; // Destination is blocked
        }
        Vec3i endPoint = VecUtils.subtract(jump, jumpDirection.getDirectionVec());
        if(type == JumpType.EDGE_NEO) {
            endPoint = VecUtils.add(endPoint, jumpDirection.getDirectionVec());
        }
        Set<Vec3i> jumpLine = getLineApprox(endPoint, 0.2, 1);
        jumpLine.add(endPoint); // Check the destination for blocks
        jumpLine.remove(VecUtils.add(endPoint, 0, -1, 0)); // Landing block
        jumpLine.add(new Vec3i(-jumpDirection.getXOffset(), 0, -jumpDirection.getZOffset())); // Src block
        if (type == JumpType.MOMENTUM) {
           jumpLine.add(new Vec3i(-jumpDirection.getXOffset(), 1, -jumpDirection.getZOffset())); // The block above the src is entered during a momentum jump (check block above head)
        }
        Iterator<Vec3i> jumpItr = jumpLine.iterator();
        double stepSize = (double) jumpLine.size() / calcJumpTime(jump.getY(), true, context.baritone.getPlayerContext());
        double prevHeight = 0;
        int prevTick = 0;
        for (int i = 0; i < jumpLine.size(); i++) {
            Vec3i jumpVec = jumpItr.next();
            int tick = (int) (i / stepSize) + 1;
            if (tick > prevTick) {
                prevHeight += calcFallVelocity(tick, context.getBaritone().getPlayerContext());
                prevTick = tick;
            }
            for (int j = (int) prevHeight; j <= PLAYER_HEIGHT + prevHeight; j++) { // Checks feet, head, for each block. (can double check some blocks on ascends/descends)
                // jumpDirection is subtracted at the beginning (re-added here)
                Vec3i vec = VecUtils.add(VecUtils.add(jumpVec, jumpDirection.getDirectionVec()), src.up(j));
                if (!MovementHelper.fullyPassable(context, vec.getX(), vec.getY(), vec.getZ())) {
                    // System.out.println("Blocks in the way, block = " + vec);
                    return true;
                }
            }
        }
        return false;
    }

    public static MovementParkourAdv cost(CalculationContext context, BetterBlockPos src, EnumFacing simpleDirection) {
        MutableMoveResult res = new MutableMoveResult();
        cost(context, src, res, simpleDirection);
        JumpType type = ALL_VALID_DIR.get(simpleDirection).get(new Vec3i(res.x - src.x, 0, res.z - src.z));
        // System.out.println("type = " + type + ", jump = " + new Vec3i(res.x - src.x, 0, res.z - src.z) + ", dir = " + simpleDirection);
        return new MovementParkourAdv(context, src, new BetterBlockPos(res.x, res.y, res.z), simpleDirection, type);
    }

    public static void cost(CalculationContext context, BetterBlockPos src, MutableMoveResult res, EnumFacing simpleDirection) {
        if (!context.allowParkour || !context.allowParkourAdv) {
            return;
        }

        if (src.getY() == 256 && !context.allowJumpAt256) {
            return;
        }

        int xDiff = simpleDirection.getXOffset();
        int zDiff = simpleDirection.getZOffset();
        double extraAscend = 0;

        if (!MovementHelper.fullyPassable(context, src.offset(simpleDirection))) { // block in foot in directly adjacent block
            return;
        }
        IBlockState adj = context.get(src.offset(simpleDirection).down());
        if (MovementHelper.canWalkOn(context.bsi, src.offset(simpleDirection).down(), adj)) { // don't parkour if we could just traverse
            // second most common case -- we could just traverse not parkour
            return;
        }
        if (MovementHelper.avoidWalkingInto(adj.getBlock()) && adj.getBlock() != Blocks.WATER && adj.getBlock() != Blocks.FLOWING_WATER) {
            return; // mostly for lava (we shouldn't jump with lava at our feet as we could take damage)
        }
        if (!MovementHelper.fullyPassable(context, src.offset(simpleDirection).up())) { // block in head in directly adjacent block
            return; // common case (makes all jumps in this direction invalid)
        }
        if (!MovementHelper.fullyPassable(context, src.offset(simpleDirection).up(2))) { // Block above head in directly adjacent block
            return; // common case (makes all jumps in this direction invalid)
        }

        IBlockState standingOn = context.get(src.down());
        if (standingOn.getBlock() == Blocks.VINE || standingOn.getBlock() == Blocks.LADDER || standingOn.getBlock() instanceof BlockStairs || standingOn.getBlock() instanceof BlockLiquid) {
            return; // Can't parkour from these blocks.
        }

        if(MovementHelper.isBottomSlab(standingOn)) {
            if (!Baritone.settings().allowWalkOnBottomSlab.value) {
                return;
            }
            extraAscend += 0.5;
        }

        MutableMoveResult lowestCost = res;

        for (Vec3i posbJump : ALL_VALID_DIR.get(simpleDirection).keySet()) {
            JumpType type = ALL_VALID_DIR.get(simpleDirection).get(posbJump);
            BetterBlockPos dest = new BetterBlockPos(src.add(posbJump));

            final double maxJump;
            if (context.canSprint) {
                maxJump = type.maxJumpSprint;
            } else {
                maxJump = type.maxJumpNoSprint;
            }

            if (!MovementHelper.fullyPassable(context, dest.up())) { // block in head at destination
                continue;
            }

            if (calcMoveDist(context, src, extraAscend, posbJump, simpleDirection) > maxJump) {
                continue; // jump is too long (usually due to ascending (slab) or no sprint)
            }

            IBlockState destInto = context.bsi.get0(dest);
            // Must ascend here as foot has block, && no block in head at destination (if ascend)
            if (!MovementHelper.fullyPassable(context.bsi.access, context.bsi.isPassableBlockPos.setPos(dest), destInto) && MovementHelper.fullyPassable(context, dest.up(2))) {
                posbJump = VecUtils.add(posbJump, 0, 1, 0);

                if (calcMoveDist(context, src, extraAscend, posbJump, simpleDirection) > maxJump) {
                    continue; // jump is too long (recalculated with new posbJump)
                }

                if (context.allowParkourAscend && MovementHelper.canWalkOn(context.bsi, dest, destInto) /* && MovementParkour.checkOvershootSafety(context.bsi, destX + xDiff, srcY + 1, destZ + zDiff) */) {
                    dest = dest.up();

                    if (checkBlocksInWay(context, src, posbJump, simpleDirection, type)) {
                        continue; // Blocks are in the way
                    }

                    lowestCost = getMoveResult(context, dest, extraAscend, posbJump, simpleDirection, type, 0, lowestCost, res);
                }
                continue;
            }

            for (int descendAmount = 0; descendAmount < context.maxFallHeightNoWater; descendAmount++) {
                Vec3i decJump = VecUtils.add(posbJump, 0, -descendAmount, 0);

                if (checkBlocksInWay(context, src, decJump, simpleDirection, type)) {
                    continue; // Blocks are in the way
                }

                IBlockState landingOn = context.bsi.get0(dest.down(descendAmount + 1));

                // farmland needs to be canWalkOn otherwise farm can never work at all, but we want to specifically disallow ending a jump on farmland
                if (landingOn.getBlock() != Blocks.FARMLAND && MovementHelper.canWalkOn(context.bsi, dest.down(descendAmount + 1), landingOn)) {
                    lowestCost = getMoveResult(context, dest.down(descendAmount), extraAscend, decJump, simpleDirection, type, 0, lowestCost, res);
                }
            }

            // No block to land on, we now test for a parkour place

            if (!context.allowParkourPlace) {
                continue; // Settings don't allow a parkour place
            }

            IBlockState toReplace = context.get(dest.down());
            double placeCost = context.costOfPlacingAt(dest.getX(), dest.getY() - 1, dest.getZ(), toReplace);
            if (placeCost >= COST_INF) {
                continue; // Not allowed to place here
            }
            if (!MovementHelper.isReplaceable(dest.getX(), dest.getY() - 1, dest.getZ(), toReplace, context.bsi)) {
                continue; // Not allowed to place here
            }

            // Check if a block side is available/visible to place on
            for (int j = 0; j < 5; j++) {
                int againstX = dest.getX() + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[j].getXOffset();
                int againstY = dest.getY() - 1 + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[j].getYOffset();
                int againstZ = dest.getZ() + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[j].getZOffset();
                if (againstX == src.getX() + xDiff * 3 && againstZ == src.getZ() + zDiff * 3) { // we can't turn around that fast
                    continue;
                }
                if (MovementHelper.canPlaceAgainst(context.bsi, againstX, againstY, againstZ)) {
                    lowestCost = getMoveResult(context, dest, extraAscend, posbJump, simpleDirection, type, placeCost, lowestCost, res);
                }
            }
        }
        res = lowestCost;
        //*
        if (res.cost < 1000) {
            Vec3i jumpVec = new Vec3i(res.x - src.getX(), res.y - src.getY(), res.z - src.getZ());
            System.out.println(src + ", Dir = " + simpleDirection + ", Cost: " + res.cost + ", Distance: " + getDistance(jumpVec, simpleDirection) + ", MoveDis: " + calcMoveDist(context, src, extraAscend, jumpVec, simpleDirection));
        }
        //*/
    }

    private static MutableMoveResult getMoveResult(CalculationContext context, BetterBlockPos dest, double extraAscend, Vec3i jump, EnumFacing jumpDirection, JumpType type, double costModifiers, MutableMoveResult curLowestCost, MutableMoveResult res) {
        res.reset();
        res.x = dest.getX();
        res.y = dest.getY();
        res.z = dest.getZ();
        res.cost = costFromJump(context, res.x - jump.getX(), res.y - jump.getY(), res.z - jump.getZ(), extraAscend, jump, jumpDirection, type) + costModifiers;
        if(res.cost < curLowestCost.cost) {
            return res;
        }
        return curLowestCost;
    }

    private static double calcMoveDist(CalculationContext context, BetterBlockPos src, double extraAscend, Vec3i jump, EnumFacing jumpDirection) {
        double ascendAmount = jump.getY() + extraAscend;

        // Accounting for slab height
        IBlockState destBlock = context.get(jump.getX() + src.getX(), jump.getY() + src.getY() - 1, + jump.getZ() + src.getZ());
        if(MovementHelper.isBottomSlab(destBlock)) {
            ascendAmount -= 0.5;
        }

        jump = new Vec3i(jump.getX(), 0, jump.getZ());
        double distance = getDistance(jump, jumpDirection);

        // Modifying distance so that ascends have larger distances while descends have smaller
        if(ascendAmount > 0) {
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

    private static double costFromJump(CalculationContext context, int srcX, int srcY, int srcZ, double extraAscend, Vec3i jump, EnumFacing jumpDirection, JumpType type) {
        double distance = new Vec3d(jump.getX() - jumpDirection.getXOffset(), jump.getY() + extraAscend, jump.getZ() - jumpDirection.getZOffset()).length();
        switch (type) {
            case EDGE: // for now edge jumps cost the same as normal ones (just setup differently)
            case NORMAL:
                return (MOVE_COST * distance + context.jumpPenalty) * COST_MODIFIER; // Walk jumps now cost the same as sprint ones.
            case MOMENTUM:
                return (MOVE_COST * distance * 2 + context.jumpPenalty * 2) * COST_MODIFIER; // Momentum jumps are unsafe, therefore should have high costs.
            case EDGE_NEO:
                return (MOVE_COST * distance + context.jumpPenalty * 2) * COST_MODIFIER;
        }
        return 0; // Will never reach this unless a new type is added
    }

    @Override
    protected boolean prepared(MovementState state) {
        if (inStartingPosition || state.getStatus() == MovementStatus.WAITING) {
            return true;
        }
        Vec3d offset = new Vec3d(simpleDirection.getOpposite().getDirectionVec()).scale(PREP_OFFSET); // defaults for a NORMAL JumpType
        double accuracy = 0.1; // defaults for a NORMAL JumpType
        switch (type) {
            case MOMENTUM:
                accuracy = 0.025; // Basically as small as you can get
                break;
            case EDGE:
                offset = new Vec3d(simpleDirection.getDirectionVec()).scale(0.8);
                accuracy = 0.025; // Don't want to fall off
                break;
            case EDGE_NEO:
                EnumFacing destDir = EnumFacing.fromAngle(MathHelper.atan2(src.x - dest.x, -(src.z - dest.z)) * RotationUtils.RAD_TO_DEG);
                offset = new Vec3d(simpleDirection.getDirectionVec()).scale(0.8).add(new Vec3d(destDir.getOpposite().getDirectionVec()).scale(0.2));
                accuracy = 0.025;
                break;
        }
        Vec3d preJumpPos = offset.add(src.x + 0.5, ctx.player().posY, src.z + 0.5);
        double distance = preJumpPos.distanceTo(ctx.playerFeetAsVec());
        // System.out.println("Distance to prepLoc = " + distance);
        boolean prepLocPassable = MovementHelper.fullyPassable(ctx, src.add(new BlockPos(offset.add(offset.normalize().scale(0.4))))); // Checking 0.4 blocks in the direction of offset for a block (0.3 is the player hitbox width)
        if (((distance > accuracy && prepLocPassable) || (distance > (PREP_OFFSET - (0.2 - accuracy)) && !prepLocPassable)) &&
                (atDestTicks < 7 || accuracy >= 0.1)) { // Accuracies over 0.1 will require additional wait ticks to reduce excess momentum
            if (atDestTicks < 5) {
                MovementHelper.moveTowards(ctx, state, offset.add(VecUtils.getBlockPosCenter(src)));
            }
            if(distance < 0.2) {
                state.setInput(Input.SNEAK, true);
                if(distance < accuracy) {
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
     * @param ticksFromStart    Ticks past since the jump began
     * @param jump              If jump is a jump and not a fall.
     * @param jumpBoostLvl      The level of jump boost on the player.
     * @return      The (y-direction) velocity in blocks per tick
     */
    private static double calcFallVelocity(int ticksFromStart, boolean jump, int jumpBoostLvl) {
        if(ticksFromStart <= 0) {
            return 0;
        }
        double init = 0.42 + 0.1 * jumpBoostLvl;
        if(!jump) {
            init = -0.0784;
        }
        // V0 = 0
        // V1 = 0.42 (if jumping) OR -0.0784 (if falling)
        // VN+1 = (VN - 0.08) * 0.98
        // If |VN+1| < 0.003, it is 0 instead
        double vel = init * Math.pow(0.98, ticksFromStart - 1) + 4 * Math.pow(0.98, ticksFromStart) - 3.92;
        if(vel < -3.92) {
            vel = -3.92; // TERMINAL_VELOCITY
        }
        if(Math.abs(vel) < 0.003) {
            vel = 0; // MINIMUM_VELOCITY
        }
        return vel;
    }

    /**
     * Calculates the velocity the player would have a specified amount of ticks after a jump
     * Jump at tick 1
     *
     * @param ticksFromStart    The amount of ticks since jumping
     * @param ctx               Player context (for jump boost)
     * @return                  The y velocity calculated (+ is up)
     */
    private static double calcFallVelocity(int ticksFromStart, IPlayerContext ctx) {
        if (Baritone.settings().considerPotionEffects.value && ctx.player().isPotionActive(MobEffects.JUMP_BOOST)) {
            return calcFallVelocity(ticksFromStart, true, ctx.player().getActivePotionEffect(MobEffects.JUMP_BOOST).getAmplifier());
        } else {
            return calcFallVelocity(ticksFromStart, true, 0);
        }
    }

    /**
     * Calculates the y position of the player relative to the jump y position
     * Jump at tick 1
     *
     * @param ticksFromStart    The amount of ticks that have passed since jumping
     * @param jump              If the jump is a jump and not a fall
     * @param jumpBoostLvl      The level of jump boost active on the player
     * @return                  The relative y position of the player
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
     * @param ticksFromStart    The amount of ticks that have passed since jumping
     * @param jump              If the jump is a jump and not a fall
     * @param ctx               The player context (for jump boost)
     * @return                  The relative y position of the player
     */
    private static double calcFallPosition(int ticksFromStart, boolean jump, IPlayerContext ctx) {
        if (Baritone.settings().considerPotionEffects.value && ctx.player().isPotionActive(MobEffects.JUMP_BOOST)) {
            return calcFallPosition(ticksFromStart, jump, ctx.player().getActivePotionEffect(MobEffects.JUMP_BOOST).getAmplifier());
        } else {
            return calcFallPosition(ticksFromStart, jump, 0);
        }
    }

    /**
     * Calculates the time in ticks spent in the air after jumping
     *
     * @param ascendAmount` The y differance of the landing position (can be negative)
     * @param jump          If the jump is a jump and not a fall
     * @param jumpBoostLvl  Tne level of jump boost active on the player
     * @return              The jump time in ticks
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
     * @param height   The height we are landing at (relative to the jump height)
     * @param jump     If the jump is a jump and not a fall
     * @param ctx      The player context (for jump boost)
     * @return         The jump time in ticks
     */
    private static int calcJumpTime(double height, boolean jump, IPlayerContext ctx) {
        return calcJumpTime(height, jump, calcJumpTime(height, jump, 0));
    }

    /**
     * Gets the maximum jump height for a given jump boost lvl
     *
     * @param jump          If the jump is a jump and not a fall
     * @param jumpBoostLvl  What level of jump boost is active on the player
     * @return              The relative jump height
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
            return ctx.player().getActivePotionEffect(effect).getAmplifier();
        } else {
            return 0;
        }
    }

    /**
     * Move in the direction of moveDirYaw while facing towards playerYaw
     * (excluding MOVE_BACK)
     *
     * @param playerYaw     The yaw we are facing towards
     * @param moveDirYaw    The yaw (angle) to move towards
     * @return              The input required
     */
    private static Input sideMove(double playerYaw, double moveDirYaw) {
        double diff = moveDirYaw - playerYaw;
        if(diff > 180) {
            diff = 180 - diff;
        }
        if(diff < -180) {
            diff = -180 - diff;
        }
        if (diff >= 45) {
            return Input.MOVE_RIGHT;
        }
        if (diff <= -45) {
            return Input.MOVE_LEFT;
        }
        if (Math.abs(diff) < 45) {
            return Input.MOVE_FORWARD;
        }
        return null; // Not possible
    }

    /**
     * Move in the direction of moveDirYaw while facing towards playerLookDest
     * (excluding MOVE_BACK)
     *
     * @param src               The block we are at
     * @param playerLookDest    The block we are facing towards
     * @param moveDest          The block to move towards
     * @return                  The input required
     */
    private static Input sideMove(Vec3i src, Vec3i playerLookDest, Vec3i moveDest) {
        // Make vectors relative
        playerLookDest = VecUtils.subtract(playerLookDest, src);
        moveDest = VecUtils.subtract(moveDest, src);
        return sideMove(MathHelper.atan2(playerLookDest.getX(), -playerLookDest.getZ()) * RotationUtils.RAD_TO_DEG,
                MathHelper.atan2(moveDest.getX(), -moveDest.getZ()) * RotationUtils.RAD_TO_DEG);
    }

    double queuedAngleChange = 0;

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

        if(ticksFromStart > 40) { // Should generally have <= 12 (exceptions are descending jumps)
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

        final double jumpMod = 0.2; // Amount to shift the jump location by (towards the destination) (0.2 is max if block is present)
        double xJumpMod = jumpMod - Math.abs(simpleDirection.getXOffset()) * jumpMod; // perpendicular to offset
        double zJumpMod = jumpMod - Math.abs(simpleDirection.getZOffset()) * jumpMod;
        if((dest.getX() - src.getX()) < 0) {
            xJumpMod = -xJumpMod;
        }
        if((dest.getZ() - src.getZ()) < 0) {
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
        Vec3d futureLoc = MovementPrediction.getFutureLocation(ctx.player(), state, 1).getPosition(); // The predicted location 1 tick in the future

        Vec3d overshootVec = destVec.normalize().scale(2).subtract(motionVec.normalize()).normalize().add(ctx.playerFeetAsVec()); // The unit vector pointing in the ( direction that cancels out motion away from the destination ) + the current position
        double speedXZ = motionVec.distanceTo(new Vec3d(0, motionVec.y, 0));
        if(speedXZ < 0.15) {
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
                    MovementHelper.moveTowards(ctx, state, src.offset(simpleDirection, 2));
                    break;
                case MOMENTUM:
                    if (ticksFromStart == 0) {
                        logDebug("Momentum jump");
                        state.setInput(Input.JUMP, true);
                    } else if (ticksFromStart >= 1) {
                        MovementHelper.moveTowards(ctx, state, src.offset(simpleDirection, 2));
                        if (ticksFromStart <= 2) {
                            state.setInput(Input.SPRINT, false); // not sprinting for a few ticks
                        }
                    }
                    break;
                case EDGE:
                    logDebug(" AHHHHH ");
                    MovementHelper.moveTowards(ctx, state, dest);
                    break;
                case EDGE_NEO:
                    MovementHelper.moveTowards(ctx, state, new Vec3d(simpleDirection.getDirectionVec()).scale(0.3).add(VecUtils.getBlockPosCenter(dest)));
                    state.setInput(sideMove(src, dest, src.offset(simpleDirection)), true);
                    break;
            }
        } else if (curDist < 1 && ctx.player().onGround) {
            if (motionVec.length() < 0.3) {
                atDestTicks++;
                if(atDestTicks >= 3) {
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
            MovementPrediction.PredictionResult future = MovementPrediction.getFutureLocation(ctx.player(), state, Math.min(6, ticksRemaining)); // The predicted location a few ticks in the future
            double destAngle = MathHelper.atan2((ctx.player().posX - curDest.x), -(ctx.player().posZ - curDest.z)) * RotationUtils.RAD_TO_DEG;

            // adjust movement to attempt to dodge obstacles
                if (future.collidedHorizontally) {
                    double futureAngle = MathHelper.atan2(ctx.player().posX - future.posX, -(ctx.player().posZ - future.posZ)) * RotationUtils.RAD_TO_DEG;

                    double addAngle = (destAngle - futureAngle) * 4;
                    final double MAX_ANGLE = 50;
                    if (addAngle > MAX_ANGLE) { addAngle = MAX_ANGLE; }
                    if (addAngle < -MAX_ANGLE) { addAngle = -MAX_ANGLE; }

                    if (Math.abs(queuedAngleChange + addAngle) <= MAX_ANGLE * 2) {
                        logDebug("Adjusting movement to dodge an obstacle. Predicted collision location = " + future.getPosition());
                        queuedAngleChange += addAngle;
                    }
                }

            // Angle between destination and future position
            double angle = VecUtils.angle(future.getPosition(), curDest) * RotationUtils.RAD_TO_DEG;
            if (angle > 45) {
                state.setInput(sideMove(VecUtils.getYaw(futureLoc), destAngle), true);
            }

            state.setTarget(new MovementState.MovementTarget(
                    new Rotation((float) (destAngle + queuedAngleChange * 0.2), ctx.player().rotationPitch),
                    false
            )).setInput(Input.MOVE_FORWARD, true).setInput(sideMove(ctx.playerRotations().normalize().getYaw(), queuedAngleChange), true);
        }

        if (ctx.playerFeet().equals(dest)) {
            Block d = BlockStateInterface.getBlock(ctx, dest);
            if (d == Blocks.VINE || d == Blocks.LADDER) {
                return state.setStatus(MovementStatus.SUCCESS);
            }
        } else if (!ctx.playerFeet().equals(src) && // Don't jump on the src block (too early)
                (((Math.abs(futureLoc.x - (src.x + 0.5)) > 0.8 || Math.abs(futureLoc.z - (src.z + 0.5)) > 0.8) && distFromStart < 1.2) || // Centre 0.5 + Player hitbox 0.3 = 0.8, if we are this distance from src, jump
                    (type == JumpType.MOMENTUM && distToJumpXZ < 0.6) || // During a momentum jump the momentum jump will position us so just jump whenever possible (i.e. as soon as we land)
                    ((type == JumpType.EDGE || type == JumpType.EDGE_NEO) && distFromStart < 1))  // The prepLoc of an edge jump is on the edge of the block so just jump straight away
                    && ctx.player().onGround) { // To only log Jumping when we can actually jump
            state.setInput(Input.JUMP, true);
            logDebug("Jumping");
            ticksFromStart = 0; // Reset ticks from momentum/run-up phase
        }

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

        return state;
    }
}
