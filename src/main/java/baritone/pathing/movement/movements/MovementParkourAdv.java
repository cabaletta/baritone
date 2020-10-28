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
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.VecUtils;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.utils.BlockStateInterface;
import baritone.utils.pathing.MutableMoveResult;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
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

    private static final HashMap<EnumFacing, HashSet<Vec3i>> ALL_VALID_DIR = new HashMap<EnumFacing, HashSet<Vec3i>>();
    private static final HashSet<Vec3i> ALL_VALID = new HashSet<Vec3i>();
    private static final HashSet<Vec3i> SOUTH_VALID = new HashSet<Vec3i>();
    private static final HashSet<Vec3i> WEST_VALID = new HashSet<Vec3i>();
    private static final HashSet<Vec3i> NORTH_VALID = new HashSet<Vec3i>();
    private static final HashSet<Vec3i> EAST_VALID = new HashSet<Vec3i>();

    private static final HashMap<Vec3i, Double> DISTANCE_CACHE = new HashMap<Vec3i, Double>();

    //cost is similar to an equivalent straight flat jump in blocks
    private static final double ASCEND_COST_PER_BLOCK = 0.8;
    private static final double DESCEND_COST_PER_BLOCK = -0.3;
    private static final double TURN_COST = 0.3; //per radian

    private static final double MAX_JUMP_SPRINT = 4.6 + TURN_COST * Math.toRadians(30); //0.157
    private static final double MAX_JUMP_WALK = 3.25 + TURN_COST * Math.toRadians(30);
    private static final double MAX_JUMP_SLOWED = 2; //soulsand

    //private static final double SPRINT_THRESHOLD = 3.25 + TURN_COST * Math.toRadians(30); // Distance required for a sprint jump

    static {
        int[][] validQuadrant = {{2, 1}, {3, 1}, {4, 1},
                {0, 2}, {1, 2}, {2, 2}, {3, 2}, {4, 2},
                {2, 3}};
        for (int i = 0; i < validQuadrant.length; i++) {
            int z = validQuadrant[i][0];
            for (int neg = -1; neg < 2; neg += 2) { // -1 and 1
                int x = neg * validQuadrant[i][1];
                Vec3i southVec = new Vec3i(x, 0, z);
                SOUTH_VALID.add(southVec);
                WEST_VALID.add(rotateAroundY(southVec, Math.toRadians(-90)));
                NORTH_VALID.add(rotateAroundY(southVec, Math.toRadians(-180)));
                EAST_VALID.add(rotateAroundY(southVec, Math.toRadians(-270)));
            }
        }

        ALL_VALID_DIR.put(EnumFacing.SOUTH, SOUTH_VALID);
        ALL_VALID_DIR.put(EnumFacing.WEST, WEST_VALID);
        ALL_VALID_DIR.put(EnumFacing.NORTH, NORTH_VALID);
        ALL_VALID_DIR.put(EnumFacing.EAST, EAST_VALID);

        System.out.println(ALL_VALID_DIR);

        for (HashSet<Vec3i> posbJumpsForDir : ALL_VALID_DIR.values()) {
            for (Vec3i vec : posbJumpsForDir) {
                ALL_VALID.add(vec);
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
     * @return
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
    private boolean inStartingPosition = false;
    private int atDestTicks = 0;

    private MovementParkourAdv(CalculationContext context, BetterBlockPos src, BetterBlockPos dest, EnumFacing simpleDirection) {
        super(context.baritone, src, dest, EMPTY);
        direction = VecUtils.subtract(dest, src);
        moveDist = calcMoveDist(context, src.getX(), src.getY(), src.getZ(), MovementHelper.isBottomSlab(context.get(src.down())) ? -0.5 : 0, direction, simpleDirection);
        this.ascendAmount = dest.y - src.y;
        this.simpleDirection = simpleDirection;
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
     * @param vec
     * @return
     */
    private static Vec3d normalize(Vec3i vec) {
        double length = getDistance(vec);
        double x = vec.getX() / length;
        double y = vec.getY() / length;
        double z = vec.getZ() / length;
        return new Vec3d(x, y, z);
    }

    private static HashSet<Vec3i> getLineApprox(Vec3i vec, double overlap, double accPerBlock) {
        return approxBlocks(getLine(vec, accPerBlock), overlap);
    }

    //Maybe cache
    public static ArrayList<Vec3d> getLine(Vec3i vector, double accPerBlock) {
        double length = Math.ceil(getDistance(vector));
        ArrayList<Vec3d> line = new ArrayList<Vec3d>();
        for (double i = 0; i <= length; i += (1 / accPerBlock)) {
            line.add(normalize(vector).scale(i).add(0.5, 0.5, 0.5));
        }
        return line;
    }

    // gravity = -0.0784 m/t^2 or -31.36 m/s^2 (under the assumption that baritone runs at 20 ticks a second)

    /**
     * Checks if each vector is pointing to a location close to the edge of a block. If so also returns the block next to that edge.
     *
     * @param vectors
     * @param overlap   The size of the edge
     * @return
     */
    public static HashSet<Vec3i> approxBlocks(Collection<Vec3d> vectors, double overlap) {
        HashSet<Vec3i> output = new HashSet<Vec3i>();
        for (Vec3d vector : vectors) {
            output.addAll(approxBlock(vector, overlap));
        }
        return output;
    }

    private static void approxQuadrant(HashSet<Vec3i> output, Vec3d vector, double overlap, boolean posX, boolean posY, boolean posZ) {
        for(double x = vector.x /* + (posX ? 1 : -1) */; (posX ? (x <= Math.floor(vector.x) + overlap) : (x >= Math.ceil(vector.x) - overlap)); x += posX ? 1 : -1) {
            for(double y = vector.y /* + (posY ? 1 : -1) */; (posY ? (y <= Math.floor(vector.y) + overlap) : (y >= Math.ceil(vector.y) - overlap)); y += posY ? 1 : -1) {
                for(double z = vector.z /* + (posZ ? 1 : -1) */; (posZ ? (z <= Math.floor(vector.z) + overlap) : (z >= Math.ceil(vector.z) - overlap)); z += posZ ? 1 : -1) {
                    output.add(new Vec3i(-x, -y, -z));
                }
            }
        }
    }

    private static final boolean[][] allBooleans3 = {{false, false, false}, {false, false, true}, {false, true, false}, {false, true, true}, {true, false, false}, {true, false, true}, {true, true, false}, {true, true, true}};

    /**
     * When the vector is pointing to a location close to the edge of a block also returns the block next to that edge.
     *
     * @param vector    The vector
     * @param overlap   The size of the edge
     * @return
     */
    public static HashSet<Vec3i> approxBlock(Vec3d vector, double overlap) {
        HashSet<Vec3i> output = new HashSet<Vec3i>();

        for (boolean[] boolCom : allBooleans3) {
            approxQuadrant(output, vector, overlap, boolCom[0], boolCom[1], boolCom[2]);
        }

        /*
        double x = vector.x;
        double y = vector.y;
        double z = vector.z;

        int blockX = (int) Math.floor(x);
        int blockY = (int) Math.floor(y);
        int blockZ = (int) Math.floor(z);

        double localX = x - blockX;
        double localY = y - blockY;
        double localZ = z - blockZ;

        int modX = 1;
        int modY = 1;
        int modZ = 1;

        if(x < 0) {
            localX -= blockX * 2;
            modX = -1;
        }

        if(y < 0) {
            localY -= blockY * 2;
            modY = -1;
        }

        if(z < 0) {
            localZ -= blockZ * 2;
            modZ = -1;
        }

        output.add(new Vec3i(blockX, blockY, blockZ));

        if(localX >= 1 - overlap) {
            output.add(new Vec3i(blockX + modX, blockY, blockZ));
        }

        if(localY >= 1 - overlap) {
            output.add(new Vec3i(blockX, blockY + modY, blockZ));
        }

        if(localZ >= 1 - overlap) {
            output.add(new Vec3i(blockX, blockY, blockZ + modZ));
        }



        if(localX <= overlap) {
            output.add(new Vec3i(blockX - modX, blockY, blockZ));
        }

        if(localY <= overlap) {
            output.add(new Vec3i(blockX, blockY - modY, blockZ));
        }

        if(localZ <= overlap) {
            output.add(new Vec3i(blockX, blockY, blockZ - modZ));
        } //*/

        return output;
    }

    @Override
    protected Set<BetterBlockPos> calculateValidPositions() {
        HashSet<BetterBlockPos> out = new HashSet<BetterBlockPos>();
        for (Vec3i vec : getLineApprox(direction, 1, 3)) {
            BetterBlockPos pos = new BetterBlockPos(src.add(vec));
            out.add(pos);       //Jumping from blocks
            out.add(pos.up());  //Jumping into blocks
        }
        out.add(dest);
        out.add(src);
        return out;
    }

    private static boolean checkBlocksInWay(CalculationContext context, int srcX, int srcY, int srcZ, Vec3i jump, EnumFacing jumpDirection, int ascendAmount) {
        Vec3i endPoint = VecUtils.add(VecUtils.subtract(jump, jumpDirection.getDirectionVec()), 0, ascendAmount, 0);
        HashSet<Vec3i> jumpLine = getLineApprox(endPoint, 0, 1);
        //jumpLine.remove(endPoint); //Depending on the angle of the line the endpoint can be found in the line
        //jumpLine.remove(VecUtils.add(endPoint, 0, 1, 0));
        jumpLine.remove(VecUtils.add(endPoint, 0, -1, 0)); //Block standing on
        for (Vec3i jumpVec : jumpLine) {
            for (int i = 0; i <= 2; i++) { //Checks feet, head, above head, for each block. (can double check some blocks on ascends/descends)
                Vec3i vec = VecUtils.add(VecUtils.add(jumpVec, jumpDirection.getDirectionVec()), srcX, srcY + i, srcZ);
                if (!MovementHelper.fullyPassable(context, vec.getX(), vec.getY(), vec.getZ())) {
                    System.out.println("Blocks in the way, block = " + vec);
                    return true;
                }
            }
        }
        return false;
    }

    public static MovementParkourAdv cost(CalculationContext context, BetterBlockPos src, EnumFacing simpleDirection) {
        MutableMoveResult res = new MutableMoveResult();
        cost(context, src.x, src.y, src.z, res, simpleDirection);
        int dist = Math.abs(res.x - src.x) + Math.abs(res.z - src.z);
        return new MovementParkourAdv(context, src, new BetterBlockPos(res.x, res.y, res.z), simpleDirection);
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

        if (!MovementHelper.fullyPassable(context, srcX + xDiff, srcY, srcZ + zDiff)) { //block in foot in directly adjacent block
            return;
        }
        IBlockState adj = context.get(srcX + xDiff, srcY - 1, srcZ + zDiff);
        if (MovementHelper.canWalkOn(context.bsi, srcX + xDiff, srcY - 1, srcZ + zDiff, adj)) { // don't parkour if we could just traverse (for now)
            // second most common case -- we could just traverse not parkour
            return;
        }
        if (MovementHelper.avoidWalkingInto(adj.getBlock()) && adj.getBlock() != Blocks.WATER && adj.getBlock() != Blocks.FLOWING_WATER) { // magma sucks
            return;
        }
        if (!MovementHelper.fullyPassable(context, srcX + xDiff, srcY + 1, srcZ + zDiff)) { //block in head in directly adjacent block
            return;
        }
        if (!MovementHelper.fullyPassable(context, srcX + xDiff, srcY + 2, srcZ + zDiff)) { //Block above head in directly adjacent block
            return;
        }
        if (!MovementHelper.fullyPassable(context, srcX, srcY + 2, srcZ)) { //Block above head
            return;
        }

        IBlockState standingOn = context.get(srcX, srcY - 1, srcZ);
        if (standingOn.getBlock() == Blocks.VINE || standingOn.getBlock() == Blocks.LADDER || standingOn.getBlock() instanceof BlockStairs || standingOn.getBlock() instanceof BlockLiquid) {
            System.out.println("TEST -1, " + srcX + ", " + (srcY - 1) + ", " + srcZ + ", " + " = " + standingOn.getBlock());
            return;
        }

        if(MovementHelper.isBottomSlab(standingOn)) {
            if (!Baritone.settings().allowWalkOnBottomSlab.value) {
                return;
            }
            extraAscend += 0.5;
        }

        double maxJump;
        if (standingOn.getBlock() == Blocks.SOUL_SAND) {
            maxJump = MAX_JUMP_SLOWED; // 1 block gap
        } else {
            if (context.canSprint) {
                maxJump = MAX_JUMP_SPRINT;
            } else {
                maxJump = MAX_JUMP_WALK;
            }
        }

        MutableMoveResult lowestCost = res;

        for (Vec3i posbJump : ALL_VALID_DIR.get(simpleDirection)) {
            int destX = srcX + posbJump.getX();
            int destZ = srcZ + posbJump.getZ();

            if (!MovementHelper.fullyPassable(context, destX, srcY + 1, destZ)) { //block in head at destination
                continue;
            }

            if (calcMoveDist(context, srcX, srcY, srcZ, extraAscend, posbJump, simpleDirection) > maxJump) {
                continue;
            }

            IBlockState destInto = context.bsi.get0(destX, srcY, destZ);
            //Must ascend here as foot has block, && no block in head at destination (if ascend)
            if (!MovementHelper.fullyPassable(context.bsi.access, context.bsi.isPassableBlockPos.setPos(destX, srcY, destZ), destInto) && MovementHelper.fullyPassable(context, destX, srcY + 2, destZ)) {
                //ascendAmount += 1;
                posbJump = VecUtils.add(posbJump, 0, 1, 0);

                if (calcMoveDist(context, srcX, srcY, srcZ, extraAscend, posbJump, simpleDirection) > maxJump) {
                    System.out.println("Can't ascend, moveDistance = " + calcMoveDist(context, srcX, srcY, srcZ, extraAscend, posbJump, simpleDirection));
                    continue;
                }

                System.out.println("TEST 2, ASCENDING, moveDistance = " + calcMoveDist(context, srcX, srcY, srcZ, extraAscend, posbJump, simpleDirection));
                if (context.allowParkourAscend && MovementHelper.canWalkOn(context.bsi, destX, srcY, destZ, destInto) /* && MovementParkour.checkOvershootSafety(context.bsi, destX + xDiff, srcY + 1, destZ + zDiff) */) {
                    if (checkBlocksInWay(context, srcX, srcY, srcZ, posbJump, simpleDirection, 1)) {
                        continue; // Blocks are in the way
                    }

                    lowestCost = getMoveResult(context, destX, srcY + 1, destZ, extraAscend, VecUtils.add(posbJump, 0, 1, 0), simpleDirection, 0, lowestCost, res);
                    continue;
                }
                continue;
            }

            if (checkBlocksInWay(context, srcX, srcY, srcZ, posbJump, simpleDirection, 0)) {
                continue; // Blocks are in the way for a flat jump , Descend still possible?
            }

            for (int decendAmount = 0; decendAmount < context.maxFallHeightNoWater; decendAmount++) {
                IBlockState landingOn = context.bsi.get0(destX, srcY - decendAmount - 1, destZ);

                // farmland needs to be canWalkOn otherwise farm can never work at all, but we want to specifically disallow ending a jump on farmland haha
                if (landingOn.getBlock() != Blocks.FARMLAND && MovementHelper.canWalkOn(context.bsi, destX, srcY - decendAmount - 1, destZ, landingOn)) {
                    //if (checkOvershootSafety(context.bsi, destX + xDiff, srcY, destZ + zDiff)) {
                    lowestCost = getMoveResult(context, destX, srcY - decendAmount, destZ, extraAscend, VecUtils.add(posbJump, 0, -decendAmount, 0), simpleDirection, 0, lowestCost, res);
                    continue;
                    //}
                    //System.out.println("TEST 4, Flat Failed");
                    //continue;
                }
            }

            //No block to land on, we now test for a parkour place

            if (!MovementHelper.fullyPassable(context, destX, srcY + 3, destZ)) {
                continue; //If block above head at destination
            }

            if (!context.allowParkourPlace) {
                continue; // Settings don't allow a parkour place
            }

            System.out.println("TEST 6, Parkour Place, in dir " + simpleDirection);

            // time 2 pop off with that dank skynet parkour place
            IBlockState toReplace = context.get(destX, srcY - 1, destZ);
            double placeCost = context.costOfPlacingAt(destX, srcY - 1, destZ, toReplace);
            if (placeCost >= COST_INF) {
                continue; // Not allowed to place here
            }
            if (!MovementHelper.isReplaceable(destX, srcY - 1, destZ, toReplace, context.bsi)) {
                continue; // Not allowed to place here
            }
            //TODO
            //if (!checkOvershootSafety(context.bsi, destX + xDiff, srcY, destZ + zDiff)) {
                //continue; // Can place but will overshoot into a bad location
            //}

            //Check if a block side is available/visible to place on
            for (int j = 0; j < 5; j++) {
                int againstX = destX + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[j].getXOffset();
                int againstY = srcY - 1 + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[j].getYOffset();
                int againstZ = destZ + HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP[j].getZOffset();
                if (againstX == srcX + xDiff * 3 && againstZ == srcZ + zDiff * 3) { // we can't turn around that fast
                    continue;
                }
                if (MovementHelper.canPlaceAgainst(context.bsi, againstX, againstY, againstZ)) {
                    lowestCost = getMoveResult(context, destX, srcY, destZ, extraAscend, posbJump, simpleDirection, placeCost, lowestCost, res);
                    continue;
                }
            }
        }
        res = lowestCost;
        if (res.cost < 1000) {
            Vec3i jumpVec = new Vec3i(res.x - srcX, res.y - srcY, res.z - srcZ);
            System.out.println("Dir = " + simpleDirection + ", Cost: " + res.cost + ", Distance: " + getDistance(jumpVec, simpleDirection) + ", MoveDis: " + calcMoveDist(context, srcX, srcY, srcZ, extraAscend, jumpVec, simpleDirection));
        }
    }

    private static MutableMoveResult getMoveResult(CalculationContext context, int x, int y, int z, double extraAscend, Vec3i jump, EnumFacing jumpDirection, double costModifiers, MutableMoveResult curLowestCost, MutableMoveResult res) {
        res.x = x;
        res.y = y;
        res.z = z;
        res.cost = costFromJump(context, x - jump.getX(), y - jump.getY(), z - jump.getZ(), extraAscend, jump, jumpDirection) + costModifiers;
        if(res.cost < curLowestCost.cost) {
            return res;
        }
        return curLowestCost;
    }

    private static double calcMoveDist(CalculationContext context, int srcX, int srcY, int srcZ, double extraAscend, Vec3i jump, EnumFacing jumpDirection) {
        double ascendAmount = jump.getY() + extraAscend;

        //Accounting for slab height
        IBlockState destBlock = context.get(jump.getX() + srcX, jump.getY() + srcY - 1, + jump.getZ() + srcZ);
        if(MovementHelper.isBottomSlab(destBlock)) {
            ascendAmount -= 0.5;
        }

        jump = new Vec3i(jump.getX(), 0, jump.getZ());
        double distance = getDistance(jump, jumpDirection);

        //Modifying distance so that ascends have larger distances while descends have smaller
        if(ascendAmount > 0) {
            distance += ascendAmount * ASCEND_COST_PER_BLOCK;
        } else {
            distance += ascendAmount * -DESCEND_COST_PER_BLOCK; // ascendAmount is negative
        }

        //Calculating angle between vectors
        Vec3d jumpVec = new Vec3d(jump.getX() - jumpDirection.getXOffset(), 0, jump.getZ() - jumpDirection.getZOffset()).normalize();
        double angle = Math.acos(jumpVec.dotProduct(new Vec3d(jumpDirection.getDirectionVec()))); //in radians

        //This distance is unitless as it contains: modifiers on ascends/descends, and the slowdowns in changing directions midair (angle)
        return distance + TURN_COST * angle;
    }

    @Override
    public boolean safeToCancel(MovementState state) {
        // since we don't really know anything about momentum, it can only be canceled during prep phase (i.e. before the jump)
        // e.g. can't cancel while cancelling momentum or we may fall off the block.
        return state.getStatus() != MovementStatus.RUNNING;
    }

    private static double costFromJump(CalculationContext context, int srcX, int srcY, int srcZ, double extraAscend, Vec3i jump, EnumFacing jumpDirection) {
        double distance = calcMoveDist(context, srcX, srcY, srcZ, extraAscend, jump, jumpDirection);
        if (distance >= MAX_JUMP_WALK) {
            return SPRINT_ONE_BLOCK_COST * distance + context.jumpPenalty;
        }
        return WALK_ONE_BLOCK_COST * distance + context.jumpPenalty;
    }

    @Override
    protected boolean prepared(MovementState state) {
        if (inStartingPosition || state.getStatus() == MovementStatus.WAITING) {
            return true;
        }
        Vec3d offset = new Vec3d(simpleDirection.getOpposite().getDirectionVec()).scale(0.3);
        Vec3d preJumpPos = offset.add(src.x, src.y, src.z);
        double distance = preJumpPos.distanceTo(ctx.playerFeetAsVec());
        System.out.println("Distance to prejump = " + distance);
        boolean prepLocPassable = MovementHelper.fullyPassable(ctx, src.offset(simpleDirection.getOpposite()));
        if ((distance > 0.75 && prepLocPassable) || (distance > 0.80 && !prepLocPassable)) {
            MovementHelper.moveBackwardsTowards(ctx, state, src, offset);
            return false;
        } else {
            inStartingPosition = true;
            return true;
        }
    }

    //V0 = 0
    //V1 = 0.42 (if jumping) OR -0.0784 (if falling)
    //VN+1 = (VN - 0.08) * 0.98
    //If |VN+1| < 0.01, it is 0 instead

    //Jumping
    //Per tick
    //0.41999
    //0.75319
    //1.00133
    //1.1661
    //1.24918 highest
    //1.17078
    //1.01555
    //0.78502
    //0.48071
    //0.10408

    /**
     * Calculates the fall velocity for a regular jump. (no glitches)
     *
     * @param ticksFromStart    Ticks past since the jump began
     * @param jump              If jump is a jump and not a fall.
     * @return      The (y-direction) velocity in blocks per tick
     */
    private static double getFallVelocity(int ticksFromStart, boolean jump) {
        if(ticksFromStart <= 0) {
            return 0;
        }
        if(ticksFromStart == 1) {
            if(jump) {
                return 0.42;
            } else {
                return -0.0784;
            }
        }
        double vel = (getFallVelocity(ticksFromStart - 1, jump) - 0.08) * 0.98;
        if(vel < 0.01) {
            return 0;
        } else {
            return vel;
        }
    }

    /**
     * Gets the relative y position of the player at the specified tick in a jump.
     *
     * @param ticksFromStart    Ticks past since the jump began
     * @param jump              If jump is a jump and not a fall.
     * @return      The y position in blocks
     */
    private static double getFallPosition(int ticksFromStart, boolean jump) {
        int yPos = 0;
        for (int i = 1; i <= ticksFromStart; i++) {
            yPos += getFallVelocity(i, jump);
        }
        return yPos;
    }

    @Override
    public MovementState updateState(MovementState state) {

        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        if (ctx.playerFeet().y < (src.y + ((ascendAmount > 0) ? 0 : ascendAmount))) {
            // we have fallen
            logDebug("sorry");
            return state.setStatus(MovementStatus.UNREACHABLE);
        }

        if (moveDist > MAX_JUMP_WALK) {
            state.setInput(Input.SPRINT, true);
        }

        double curDistSq = ctx.playerFeetAsVec().squareDistanceTo(dest.getX() + 0.5, dest.getY() + 0.5, dest.getZ() + 0.5);
        double distToJump = ctx.playerFeetAsVec().squareDistanceTo(src.getX() + (simpleDirection.getXOffset() * 0.8) + 0.5, src.getY() + 0.5, src.getZ() + (simpleDirection.getZOffset() * 0.8) + 0.5);
        
        if(ctx.playerFeet().equals(src)) {
            MovementHelper.moveTowards(ctx, state, src.offset(simpleDirection));
        } else if (curDistSq < 1) {
            atDestTicks++;
            if(atDestTicks >= 3 && curDistSq < 0.6) {
                logDebug("Canceled momentum for " + atDestTicks + " ticks");
                return state.setStatus(MovementStatus.SUCCESS);
            }
            MovementHelper.moveBackwardsTowards(ctx, state, src);
            logDebug("Cancelling momentum, dis = " + curDistSq);
        } else {
            MovementHelper.moveTowards(ctx, state, dest);
            logDebug("Moving to dest, dis = " + curDistSq);
            atDestTicks = 0;
        }

        if (ctx.playerFeet().equals(dest)) {
            Block d = BlockStateInterface.getBlock(ctx, dest);
            if (d == Blocks.VINE || d == Blocks.LADDER) {
                // it physically hurt me to add support for parkour jumping onto a vine
                // but i did it anyway
                logDebug("UPDATE 1");
                return state.setStatus(MovementStatus.SUCCESS);
            }
            if (ctx.player().posY - ctx.playerFeet().getY() < 0.094) { // lilypads
                logDebug("FINISH");
                state.setStatus(MovementStatus.SUCCESS);
            }
        } else if (!ctx.playerFeet().equals(src)) {
            if (ctx.playerFeet().equals(src.offset(simpleDirection)) || ctx.player().posY - src.y > 0.0001) {
                if (!MovementHelper.canWalkOn(ctx, dest.down()) && !ctx.player().onGround && MovementHelper.attemptToPlaceABlock(state, baritone, dest.down(), true, false) == PlaceResult.READY_TO_PLACE) {
                    //Attempt to catch fall??

                    // go in the opposite order to check DOWN before all horizontals -- down is preferable because you don't have to look to the side while in midair, which could mess up the trajectory
                    state.setInput(Input.CLICK_RIGHT, true);
                }
                // prevent jumping too late by checking for ascend
                /*
                if (moveDist >= 3) { // this is a 2 block gap, dest = src + direction * 3
                    double distFromStart = ctx.playerFeetAsVec().squareDistanceTo(dest.x, dest.y, dest.z);
                    if (distFromStart < Math.pow(0.7, 2)) {
                        logDebug("UPDATE 2");
                        return state;
                    }
                } //*/

                logDebug("Jumping");

                state.setInput(Input.JUMP, true);
            }
        }
        return state;
    }
}
