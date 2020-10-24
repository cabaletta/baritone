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

import baritone.api.IBaritone;
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
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
import org.lwjgl.Sys;

import java.util.*;

public class MovementParkourAdv extends Movement {

    private static final BetterBlockPos[] EMPTY = new BetterBlockPos[]{};

    private static final HashMap<EnumFacing, HashSet<Vec3i>> ALL_VALID_DIR = new HashMap<EnumFacing, HashSet<Vec3i>>();
    private static final HashSet<Vec3i> ALL_VALID = new HashSet<Vec3i>();
    private static final HashSet<Vec3i> SOUTH_VALID = new HashSet<Vec3i>();
    private static final HashSet<Vec3i> WEST_VALID = new HashSet<Vec3i>();
    private static final HashSet<Vec3i> NORTH_VALID = new HashSet<Vec3i>();
    private static final HashSet<Vec3i> EAST_VALID = new HashSet<Vec3i>();

    private static final HashMap<Vec3i, Double> DISTANCE_CACHE = new HashMap<Vec3i, Double>();

    private static final double SPRINT_THRESHOLD = 2.8;

    static {
        int[][] validQuadrant = {{2, 1}, {3, 1}, {4, 1},
                                 {1, 2}, {2, 2}, {3, 2}, {4, 2},
                                 {1, 3}, {2, 3}};
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
        return getDistance(new Vec3i(vec.getX() + offset.getXOffset(), vec.getY() + offset.getYOffset(), vec.getZ() + offset.getZOffset()));
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

    private final double dist;
    private final boolean ascend;
    private final Vec3i direction;
    private final EnumFacing simpleDirection;
    private final Vec3i jumpDirection;
    private final HashSet<BetterBlockPos> adjJumpBlocks = new HashSet<BetterBlockPos>();
    private boolean inStartingPosition = false;

    private MovementParkourAdv(IBaritone baritone, BetterBlockPos src, BetterBlockPos dest, EnumFacing simpleDirection, boolean ascend) {
        super(baritone, src, dest, EMPTY);
        dist = src.getDistance(dest.x, dest.y, dest.z);
        direction = VecUtils.subtract(dest, src);
        this.ascend = ascend;
        this.simpleDirection = simpleDirection;
        jumpDirection = VecUtils.subtract(direction, simpleDirection.getDirectionVec());
        Vec3d norDir = normalize(direction);
        System.out.println("DIRECTION: " + norDir);
        for (Vec3i vec : approxBlock(norDir, 0.3)) {
            adjJumpBlocks.add(new BetterBlockPos(src.add(vec)));
        }
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

    private static HashSet<Vec3i> getLineApprox(Vec3i vec, double overlap) {
        return approxBlocks(getLine(vec), overlap);
    }

    //Maybe cache
    public static ArrayList<Vec3d> getLine(Vec3i vector) {
        int length = (int) Math.ceil(getDistance(vector));
        ArrayList<Vec3d> line = new ArrayList<Vec3d>();
        for(int i = 0; i <= length; i++) {
            line.add(normalize(vector).scale(i));
        }
        return line;
    }

    /**
     * Checks if each vector is pointing to a location close to the edge of a block. If so also returns the block next to that edge.
     *
     * @param vectors
     * @param overlap   The size of the edge
     * @return
     */
    public static HashSet<Vec3i> approxBlocks(Collection<Vec3d> vectors, double overlap) {
        HashSet<Vec3i> output = new HashSet<Vec3i>();
        for(Vec3d vector : vectors) {
            output.addAll(approxBlock(vector, overlap));
        }
        return output;
    }

    /**
     * When the vector is pointing to a location close to the edge of a block also returns the block next to that edge.
     *
     * @param vector    The vector
     * @param overlap   The size of the edge
     * @return
     */
    public static HashSet<Vec3i> approxBlock(Vec3d vector, double overlap) {
        HashSet<Vec3i> output = new HashSet<Vec3i>();

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

        if(localX > 1 - overlap) {
            output.add(new Vec3i(blockX + modX, blockY, blockZ));
        }

        if(localY > 1 - overlap) {
            output.add(new Vec3i(blockX, blockY + modY, blockZ));
        }

        if(localZ > 1 - overlap) {
            output.add(new Vec3i(blockX, blockY, blockZ + modZ));
        }

        modX *= -1;
        modY *= -1;
        modZ *= -1;

        if(localX < overlap) {
            output.add(new Vec3i(blockX + modX, blockY, blockZ));
        }

        if(localY < overlap) {
            output.add(new Vec3i(blockX, blockY + modY, blockZ));
        }

        if(localZ < overlap) {
            output.add(new Vec3i(blockX, blockY, blockZ + modZ));
        }

        return output;
    }

    @Override
    protected Set<BetterBlockPos> calculateValidPositions() {
        HashSet<BetterBlockPos> out = new HashSet<BetterBlockPos>();
        for (Vec3i vec : getLineApprox(direction, 0.3)) {
            out.add(new BetterBlockPos(src.add(vec)).up());
        }
        return out;
    }

    private static boolean checkBlocksInWay(CalculationContext context, int srcX, int srcY, int srcZ, Vec3i jump, EnumFacing jumpDirection, int ascendAmount) {
        Vec3i endPoint = VecUtils.add(VecUtils.subtract(jump, jumpDirection.getDirectionVec()), 0, ascendAmount, 0);
        HashSet<Vec3i> jumpLine = getLineApprox(endPoint, 0.1);

        //TODO: Endpoint changes? (By approximately (positive) jumpDirection)
        jumpLine.remove(endPoint);
        jumpLine.remove(VecUtils.add(endPoint, 0, 1, 0));
        jumpLine.remove(VecUtils.add(endPoint, 0, -1, 0));

        String test = ("InWay endPoint = " + VecUtils.add(endPoint, srcX, srcY + 1, srcZ));

        for (Vec3i jumpVec : jumpLine) {
            Vec3i vec = jumpVec; // VecUtils.add(jumpVec, jumpDirection.getDirectionVec());
            if (!MovementHelper.fullyPassable(context, srcX + vec.getX(), srcY + vec.getY() + 1, srcZ + vec.getZ())) {
                System.out.println(test);
                System.out.println("TEST 1.1, Blocks in the way, Line = " + (srcX + vec.getX()) + ", " + (srcY + vec.getY() + 1) + ", " + (srcZ + vec.getZ()));
                return true;
            }
        }
        return false;
    }

    public static MovementParkourAdv cost(CalculationContext context, BetterBlockPos src, EnumFacing simpleDirection) {
        MutableMoveResult res = new MutableMoveResult();
        cost(context, src.x, src.y, src.z, res, simpleDirection);
        int dist = Math.abs(res.x - src.x) + Math.abs(res.z - src.z);
        return new MovementParkourAdv(context.getBaritone(), src, new BetterBlockPos(res.x, res.y, res.z), simpleDirection,res.y > src.y);
    }

    public static void cost(CalculationContext context, int srcX, int srcY, int srcZ, MutableMoveResult res, EnumFacing simpleDirection) {

        System.out.println("START COST CALC");

        if (!context.allowParkour) {
            System.out.println("TEST -4");
            return;
        }
        if (srcY == 256 && !context.allowJumpAt256) {
            System.out.println("TEST -3");
            return;
        }

        int xDiff = simpleDirection.getXOffset();
        int zDiff = simpleDirection.getZOffset();
        int yDiff = 0; // Used for ascends and descends

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
        if (standingOn.getBlock() == Blocks.VINE || standingOn.getBlock() == Blocks.LADDER || standingOn.getBlock() instanceof BlockStairs || MovementHelper.isBottomSlab(standingOn) || standingOn.getBlock() instanceof BlockLiquid) {
            System.out.println("TEST -1, " + srcX + ", " + (srcY - 1) + ", " + srcZ + ", " + " = " + standingOn.getBlock());
            return;
        }
        double maxJump;
        if (standingOn.getBlock() == Blocks.SOUL_SAND) {
            maxJump = 2; // 1 block gap
        } else {
            if (context.canSprint) {
                maxJump = 4;
            } else {
                maxJump = 3;
            }
        }

        System.out.println(".. IN DIR " + simpleDirection);

        MutableMoveResult lowestCost = res;

        for (Vec3i posbJump : ALL_VALID_DIR.get(simpleDirection)) {
            int destX = srcX + posbJump.getX();
            int destZ = srcZ + posbJump.getZ();

            if (!MovementHelper.fullyPassable(context, destX, srcY + 1, destZ)) { //block in head at destination
                System.out.println("TEST 1");
                continue;
            }

            IBlockState destInto = context.bsi.get0(destX, srcY, destZ);
            //Must ascend here as foot has block, && no block in head at destination (if ascend)
            if (!MovementHelper.fullyPassable(context.bsi.access, context.bsi.isPassableBlockPos.setPos(destX, srcY, destZ), destInto) && MovementHelper.fullyPassable(context, destX, srcY + 2, destZ)) {
                System.out.println("TEST 2, ASCENDING, Distance = " + getDistance(posbJump, simpleDirection));
                if (getDistance(posbJump, simpleDirection) <= 4 &&  context.allowParkourAscend && context.canSprint && MovementHelper.canWalkOn(context.bsi, destX, srcY, destZ, destInto) /* && MovementParkour.checkOvershootSafety(context.bsi, destX + xDiff, srcY + 1, destZ + zDiff) */) {
                    if(checkBlocksInWay(context, srcX, srcY, srcZ, posbJump, simpleDirection, 1)) {
                        continue; // Blocks are in the way
                    }

                    res.x = destX;
                    res.y = srcY + 1;
                    res.z = destZ;
                    res.cost = costFromJumpDistance(context, getDistance(new Vec3i(posbJump.getX(), posbJump.getY() + 1, posbJump.getZ()), simpleDirection));
                    if(res.cost < lowestCost.cost) {
                        lowestCost = res;
                    }
                    continue;
                }
                continue;
            }
            IBlockState landingOn = context.bsi.get0(destX, srcY - 1, destZ);

            if(checkBlocksInWay(context, srcX, srcY, srcZ, posbJump, simpleDirection, 0)) {
                continue; // Blocks are in the way for a flat jump , Descend still possible?
            }

            // farmland needs to be canWalkOn otherwise farm can never work at all, but we want to specifically disallow ending a jump on farmland haha
            if (landingOn.getBlock() != Blocks.FARMLAND && MovementHelper.canWalkOn(context.bsi, destX, srcY - 1, destZ, landingOn)) {
                //if (checkOvershootSafety(context.bsi, destX + xDiff, srcY, destZ + zDiff)) {
                    System.out.println("TEST 5, Flat Confirmed, " + destX + ", " + destZ);
                    res.x = destX;
                    res.y = srcY;
                    res.z = destZ;
                    res.cost = costFromJumpDistance(context, getDistance(posbJump, simpleDirection));
                if(res.cost < lowestCost.cost) {
                    lowestCost = res;
                }
                continue;
                //}
                //System.out.println("TEST 4, Flat Failed");
                //continue;
            }
            //No block to land on, we now test for a parkour place

            if (!MovementHelper.fullyPassable(context, destX, srcY + 3, destZ)) {
                continue; //If block above head at destination
            }

            if (maxJump >= 4) {
                continue; //Can't parkour place a 3 block jump?
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
                //continue; // Can place but will overshoot the jump
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
                    res.x = destX;
                    res.y = srcY;
                    res.z = destZ;
                    res.cost = costFromJumpDistance(context, getDistance(posbJump, simpleDirection)) + placeCost;
                    if(res.cost < lowestCost.cost) {
                        lowestCost = res;
                    }
                    continue;
                }
            }
        }
        res = lowestCost;
        if (res.cost < 1000) {
            System.out.println("Dir = " + simpleDirection + ", Calc: " + res.x + " - " + srcX + ", Distance: " + getDistance(new Vec3i(res.x - srcX, res.y - srcY, res.z - srcZ), simpleDirection));
        }
    }

    private static double costFromJumpDistance(CalculationContext context, double distance) {
        if (distance >= SPRINT_THRESHOLD) {
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

    @Override
    public MovementState updateState(MovementState state) {
        System.out.println("PRE UPDATE STATE");
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }
        System.out.println("UPDATE STATE, dist: " + dist);
        if (ctx.playerFeet().y < src.y) {
            // we have fallen
            logDebug("sorry");
            return state.setStatus(MovementStatus.UNREACHABLE);
        }

        if (dist >= SPRINT_THRESHOLD || ascend) {
            state.setInput(Input.SPRINT, true);
        }
        
        if(ctx.playerFeet().equals(src)) {
            MovementHelper.moveTowards(ctx, state, src.offset(simpleDirection, 2));
        } else if (ctx.playerFeet().equals(dest)) {
            MovementHelper.moveTowards(ctx, state, dest.offset(simpleDirection.getOpposite()));
        } else {
            MovementHelper.moveTowards(ctx, state, dest);
        }

        if (ctx.playerFeet().equals(dest)) {
            Block d = BlockStateInterface.getBlock(ctx, dest);
            if (d == Blocks.VINE || d == Blocks.LADDER) {
                // it physically hurt me to add support for parkour jumping onto a vine
                // but i did it anyway
                System.out.println("UPDATE 1");
                return state.setStatus(MovementStatus.SUCCESS);
            }
            if (ctx.player().posY - ctx.playerFeet().getY() < 0.094) { // lilypads
                System.out.println("FINISH");
                state.setStatus(MovementStatus.SUCCESS);
            }
        } else if (!ctx.playerFeet().equals(src)) {
            System.out.println("UPDATE 2.1");
            if (ctx.playerFeet().equals(src.offset(simpleDirection)) || ctx.player().posY - src.y > 0.0001) {
                System.out.println("UPDATE 2.2");
                if (!MovementHelper.canWalkOn(ctx, dest.down()) && !ctx.player().onGround && MovementHelper.attemptToPlaceABlock(state, baritone, dest.down(), true, false) == PlaceResult.READY_TO_PLACE) {
                    // go in the opposite order to check DOWN before all horizontals -- down is preferable because you don't have to look to the side while in midair, which could mess up the trajectory
                    state.setInput(Input.CLICK_RIGHT, true);
                }
                // prevent jumping too late by checking for ascend
                if (dist == 3 && !ascend) { // this is a 2 block gap, dest = src + direction * 3
                    double distFromStart = ctx.playerFeet().getDistance(dest.x, dest.y, dest.z);
                    if (distFromStart < 0.7) {
                        System.out.println("UPDATE 2");
                        return state;
                    }
                }

                state.setInput(Input.JUMP, true);
            } else if (ctx.playerFeet().distanceSq(direction) > 1)  {
                state.setInput(Input.SPRINT, false);
                if (false) {
                    MovementHelper.moveTowards(ctx, state, src);
                } else {
                    MovementHelper.moveTowards(ctx, state, dest);
                }
            }
        }
        System.out.println("UPDATE 3");
        return state;
    }
}
