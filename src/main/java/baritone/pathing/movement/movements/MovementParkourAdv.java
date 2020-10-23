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

public class MovementParkourAdv extends Movement {

    private static final BetterBlockPos[] EMPTY = new BetterBlockPos[]{};

    private static final HashMap<EnumFacing, HashSet<Vec3i>> ALL_VALID_DIR = new HashMap<EnumFacing, HashSet<Vec3i>>();
    private static final HashSet<Vec3i> ALL_VALID = new HashSet<Vec3i>();
    private static final HashSet<Vec3i> SOUTH_VALID = new HashSet<Vec3i>();
    private static final HashSet<Vec3i> WEST_VALID = new HashSet<Vec3i>();
    private static final HashSet<Vec3i> NORTH_VALID = new HashSet<Vec3i>();
    private static final HashSet<Vec3i> EAST_VALID = new HashSet<Vec3i>();

    private static final HashMap<Vec3i, Double> DISTANCE_CACHE = new HashMap<Vec3i, Double>();

    static {
        int[][] validQuadrant = {{2, 1}, {3, 1}, {4, 1}, {5, 1},
                                 {1, 2}, {2, 2}, {3, 2}, {4, 2},
                                 {1, 3}, {2, 3}, {3, 3}};
        for (int i = 0; i < validQuadrant.length; i++) {
            int z = validQuadrant[i][1];
            for (int neg = -1; neg < 2; neg += 2) { // -1 and 1
                int x = neg * validQuadrant[i][0];
                Vec3i southVec = new Vec3i(x, 0, z);
                SOUTH_VALID.add(southVec);
                WEST_VALID.add(rotateAroundY(southVec, Math.toRadians(90)));
                NORTH_VALID.add(rotateAroundY(southVec, Math.toRadians(180)));
                EAST_VALID.add(rotateAroundY(southVec, Math.toRadians(270)));
            }
        }

        ALL_VALID_DIR.put(EnumFacing.SOUTH, SOUTH_VALID);
        ALL_VALID_DIR.put(EnumFacing.WEST, WEST_VALID);
        ALL_VALID_DIR.put(EnumFacing.NORTH, NORTH_VALID);
        ALL_VALID_DIR.put(EnumFacing.EAST, EAST_VALID);

        for (HashSet<Vec3i> posbJumpsForDir : ALL_VALID_DIR.values()) {
            for (Vec3i vec : posbJumpsForDir) {
                ALL_VALID.add(vec);
                DISTANCE_CACHE.put(vec, vec.getDistance(0, 0, 0));
            }
        }
    }

    /**
     * Rotates the vector clockwise around the y axis, by an angle radians.
     * The result is the block that the vector lies in. (resultant xyz coords floored)
     * Works best with angles that are multiples of pi/2 (90 degrees)
     *
     * @param input the input vector to rotate
     * @param angle amount to rotate clockwise by, in radians
     * @return
     */
    private static Vec3i rotateAroundY(Vec3i input, double angle) {
        double angleCos = Math.cos(angle);
        double angleSin = Math.sin(angle);

        double x = angleCos * input.getX() + angleSin * input.getZ();
        double z = -angleSin * input.getX() + angleCos * input.getZ();
        return new Vec3i(x, input.getY(), z);
    }

    private static double dist;
    private static boolean ascend;
    private static Vec3i direction;
    private EnumFacing simpleDirection = EnumFacing.SOUTH;
    private final HashSet<BetterBlockPos> adjJumpBlocks = new HashSet<BetterBlockPos>();

    private MovementParkourAdv(IBaritone baritone, BetterBlockPos src, BetterBlockPos dest, boolean ascend) {
        super(baritone, src, dest, EMPTY);
        dist = src.getDistance(dest.x, dest.y, dest.z);
        direction = new Vec3i(dest.x - src.x, dest.y - src.y, dest.z - src.z);
        this.ascend = ascend;
        Vec3d norDir = normalize(direction);
        System.out.println("DIRECTION: " + norDir);
        for (Vec3i vec : approxBlock(norDir)) {
            adjJumpBlocks.add(new BetterBlockPos(src.add(vec)));
        }
    }

    @Override
    public double calculateCost(CalculationContext context) {
        return 0;
    }

    /**
     * Normalizes an integer vector to a double vector.
     *
     * @param vec
     * @return
     */
    private static Vec3d normalize(Vec3i vec) {
        double length = DISTANCE_CACHE.get(vec);
        double x = vec.getX() / length;
        double y = vec.getY() / length;
        double z = vec.getZ() / length;
        return new Vec3d(x, y, z);
    }

    private static HashSet<Vec3i> getLineApprox(Vec3i vec) {
        return approxBlocks(getLine(vec));
    }

    //Maybe cache
    public static ArrayList<Vec3d> getLine(Vec3i vector) {
        int length = (int) Math.ceil(DISTANCE_CACHE.get(vector));
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
     * @return
     */
    public static HashSet<Vec3i> approxBlocks(Collection<Vec3d> vectors) {
        HashSet<Vec3i> output = new HashSet<Vec3i>();
        for(Vec3d vector : vectors) {
            output.addAll(approxBlock(vector));
        }
        return output;
    }

    /**
     * When the vector is pointing to a location close to the edge of a block also returns the block next to that edge.
     *
     * @param vector
     * @return
     */
    public static HashSet<Vec3i> approxBlock(Vec3d vector) {
        HashSet<Vec3i> output = new HashSet<Vec3i>();

        final double overlapAmount = 0.3;

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

        if(localX > 1 - overlapAmount) {
            output.add(new Vec3i(blockX + modX, blockY, blockZ));
        }

        if(localY > 1 - overlapAmount) {
            output.add(new Vec3i(blockX, blockY + modY, blockZ));
        }

        if(localZ > 1 - overlapAmount) {
            output.add(new Vec3i(blockX, blockY, blockZ + modZ));
        }

        modX *= -1;
        modY *= -1;
        modZ *= -1;

        if(localX < overlapAmount) {
            output.add(new Vec3i(blockX + modX, blockY, blockZ));
        }

        if(localY < overlapAmount) {
            output.add(new Vec3i(blockX, blockY + modY, blockZ));
        }

        if(localZ < overlapAmount) {
            output.add(new Vec3i(blockX, blockY, blockZ + modZ));
        }

        return output;
    }

    @Override
    protected Set<BetterBlockPos> calculateValidPositions() {
        HashSet<BetterBlockPos> out = new HashSet<BetterBlockPos>();
        for (Vec3i vec : getLineApprox(direction)) {
            out.add(new BetterBlockPos(src.add(vec)));
        }
        return out;
    }

    public static MovementParkourAdv cost(CalculationContext context, BetterBlockPos src) {
        MutableMoveResult res = new MutableMoveResult();
        cost(context, src.x, src.y, src.z, res);
        int dist = Math.abs(res.x - src.x) + Math.abs(res.z - src.z);
        return new MovementParkourAdv(context.getBaritone(), src, new BetterBlockPos(res.x, res.y, res.z), res.y > src.y);
    }

    public static void cost(CalculationContext context, int srcX, int srcY, int srcZ, MutableMoveResult res) {

        if(srcX == 244 && srcY == 80 && srcZ == 67) {
            res.x = 243;
            res.y = srcY;
            res.z = 69;
            res.cost = 1;
        }


        if (!context.allowParkour) {
            System.out.println("TEST -4");
            return;
        }
        if (srcY == 256 && !context.allowJumpAt256) {
            System.out.println("TEST -3");
            return;
        }

        ArrayList<EnumFacing> safeDirs = new ArrayList<EnumFacing>(3);

        for (EnumFacing dir : HORIZONTALS) {
            int xDiff = dir.getXOffset();
            int zDiff = dir.getZOffset();
            if (MovementHelper.fullyPassable(context, srcX + xDiff, srcY, srcZ + zDiff)) { //block in foot in directly adjacent block
                continue;
            }
            IBlockState adj = context.get(srcX + xDiff, srcY - 1, srcZ + zDiff);
            if (MovementHelper.canWalkOn(context.bsi, srcX + xDiff, srcY - 1, srcZ + zDiff, adj)) { // don't parkour if we could just traverse (for now)
                // second most common case -- we could just traverse not parkour
                continue;
            }
            if (MovementHelper.avoidWalkingInto(adj.getBlock()) && adj.getBlock() != Blocks.WATER && adj.getBlock() != Blocks.FLOWING_WATER) { // magma sucks
                continue;
            }
            if (!MovementHelper.fullyPassable(context, srcX + xDiff, srcY + 1, srcZ + zDiff)) { //block in head in directly adjacent block
                continue;
            }
            if (!MovementHelper.fullyPassable(context, srcX + xDiff, srcY + 2, srcZ + zDiff)) { //Block above head in directly adjacent block
                continue;
            }
            if (!MovementHelper.fullyPassable(context, srcX, srcY + 2, srcZ)) { //Block above head
                System.out.println("TEST -2");
                return;
            }
            safeDirs.add(dir);
        }

        IBlockState standingOn = context.get(srcX, srcY - 1, srcZ);
        if (standingOn.getBlock() == Blocks.VINE || standingOn.getBlock() == Blocks.LADDER || standingOn.getBlock() instanceof BlockStairs || MovementHelper.isBottomSlab(standingOn) || standingOn.getBlock() instanceof BlockLiquid) {
            System.out.println("TEST -1, " + srcX + ", " + (srcY - 1) + ", " + srcZ + ", " + " = " + standingOn.getBlock());
            return;
        }
        int maxJump;
        if (standingOn.getBlock() == Blocks.SOUL_SAND) {
            maxJump = 2; // 1 block gap
        } else {
            if (context.canSprint) {
                maxJump = 4;
            } else {
                maxJump = 3;
            }
        }

        for (EnumFacing safeDir : safeDirs) {
            Iterator<Vec3i> posbJumps = ALL_VALID_DIR.get(safeDir).iterator();
            for (int i = 0; i < ALL_VALID_DIR.get(safeDir).size(); i++) {
                Vec3i posbJump = posbJumps.next();
                int xDiff = safeDir.getXOffset();
                int zDiff = safeDir.getZOffset();
                int destX = srcX + posbJump.getX();
                int destZ = srcZ + posbJump.getZ();

                if (!MovementHelper.fullyPassable(context, destX, srcY + 1, destZ)) {
                    System.out.println("TEST 1");
                    return;
                }
                if (!MovementHelper.fullyPassable(context, destX, srcY + 2, destZ)) {
                    System.out.println("TEST 2");
                    return;
                }
                IBlockState destInto = context.bsi.get0(destX, srcY, destZ);
                if (!MovementHelper.fullyPassable(context.bsi.access, context.bsi.isPassableBlockPos.setPos(destX, srcY, destZ), destInto)) {
                    System.out.println("TEST 3");
                    if (DISTANCE_CACHE.get(posbJump) <= 3 && context.allowParkourAscend && context.canSprint && MovementHelper.canWalkOn(context.bsi, destX, srcY, destZ, destInto) /* && MovementParkour.checkOvershootSafety(context.bsi, destX + xDiff, srcY + 1, destZ + zDiff) */) {
                        res.x = destX;
                        res.y = srcY + 1;
                        res.z = destZ;
                        res.cost = DISTANCE_CACHE.get(posbJump) * SPRINT_ONE_BLOCK_COST + context.jumpPenalty;
                    }
                    return;
                }
                IBlockState landingOn = context.bsi.get0(destX, srcY - 1, destZ);
                // farmland needs to be canwalkon otherwise farm can never work at all, but we want to specifically disallow ending a jumy on farmland haha
                if (landingOn.getBlock() != Blocks.FARMLAND && MovementHelper.canWalkOn(context.bsi, destX, srcY - 1, destZ, landingOn)) {
                    System.out.println("TEST 4");
                    if (MovementParkour.checkOvershootSafety(context.bsi, destX + xDiff, srcY, destZ + zDiff)) {
                        System.out.println("TEST 5");
                        res.x = destX;
                        res.y = srcY;
                        res.z = destZ;
                        res.cost = MovementParkour.costFromJumpDistance((int) Math.round(DISTANCE_CACHE.get(posbJump))) + context.jumpPenalty;
                    }
                    return;
                }
                System.out.println("TEST 6");
                if (!MovementHelper.fullyPassable(context, destX, srcY + 3, destZ)) {
                    return;
                }

                if (maxJump != 4) {
                    return;
                }
                if (!context.allowParkourPlace) {
                    return;
                }
                // time 2 pop off with that dank skynet parkour place
                IBlockState toReplace = context.get(destX, srcY - 1, destZ);
                double placeCost = context.costOfPlacingAt(destX, srcY - 1, destZ, toReplace);
                if (placeCost >= COST_INF) {
                    return;
                }
                if (!MovementHelper.isReplaceable(destX, srcY - 1, destZ, toReplace, context.bsi)) {
                    return;
                }
                if (!MovementParkour.checkOvershootSafety(context.bsi, destX + xDiff, srcY, destZ + zDiff)) {
                    //return;
                }

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
                        res.cost = MovementParkour.costFromJumpDistance(4) + placeCost + context.jumpPenalty;
                        return;
                    }
                }
            }
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

        if (dist >= 4 || ascend) {
            state.setInput(Input.SPRINT, true);
        }
        
        if(ctx.playerFeet().equals(src)) {
            MovementHelper.moveTowards(ctx, state, src.offset(simpleDirection, 2));
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
