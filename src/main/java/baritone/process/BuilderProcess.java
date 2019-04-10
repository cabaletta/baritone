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

package baritone.process;

import baritone.Baritone;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalComposite;
import baritone.api.pathing.goals.GoalGetToBlock;
import baritone.api.process.IBuilderProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.*;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.utils.BaritoneProcessHelper;
import baritone.utils.BlockStateInterface;
import baritone.utils.PathingCommandContext;
import baritone.utils.schematic.AirSchematic;
import baritone.utils.schematic.Schematic;
import net.minecraft.block.BlockAir;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.VoxelShape;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static baritone.api.pathing.movement.ActionCosts.COST_INF;

public class BuilderProcess extends BaritoneProcessHelper implements IBuilderProcess {

    private HashSet<BetterBlockPos> incorrectPositions;
    private String name;
    private ISchematic schematic;
    private Vec3i origin;
    private int ticks;
    private boolean paused;

    public BuilderProcess(Baritone baritone) {
        super(baritone);
    }

    public boolean build(String schematicFile, BlockPos origin) {
        File file = new File(new File(Minecraft.getInstance().gameDir, "schematics"), schematicFile);
        System.out.println(file + " " + file.exists());
        return build(schematicFile, file, origin);
    }

    @Override
    public void build(String name, ISchematic schematic, Vec3i origin) {
        this.name = name;
        this.schematic = schematic;
        this.origin = origin;
        this.paused = false;
    }

    public void resume() {
        paused = false;
    }

    @Override
    public boolean build(String name, File schematic, Vec3i origin) {
        NBTTagCompound tag;
        try (FileInputStream fileIn = new FileInputStream(schematic)) {
            tag = CompressedStreamTools.readCompressed(fileIn);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if (tag == null) {
            return false;
        }
        build(name, parse(tag), origin);
        return true;
    }

    public void clearArea(BlockPos corner1, BlockPos corner2) {
        BlockPos origin = new BlockPos(Math.min(corner1.getX(), corner2.getX()), Math.min(corner1.getY(), corner2.getY()), Math.min(corner1.getZ(), corner2.getZ()));
        int widthX = Math.abs(corner1.getX() - corner2.getX()) + 1;
        int heightY = Math.abs(corner1.getY() - corner2.getY()) + 1;
        int lengthZ = Math.abs(corner1.getZ() - corner2.getZ()) + 1;
        build("clear area", new AirSchematic(widthX, heightY, lengthZ), origin);
    }

    private static ISchematic parse(NBTTagCompound schematic) {
        return new Schematic(schematic);
    }

    @Override
    public boolean isActive() {
        return schematic != null;
    }

    public IBlockState placeAt(int x, int y, int z) {
        if (!isActive()) {
            return null;
        }
        if (!schematic.inSchematic(x - origin.getX(), y - origin.getY(), z - origin.getZ())) {
            return null;
        }
        IBlockState state = schematic.desiredState(x - origin.getX(), y - origin.getY(), z - origin.getZ());
        if (state.getBlock() instanceof BlockAir) {
            return null;
        }
        return state;
    }


    public Optional<Tuple<BetterBlockPos, Rotation>> toBreakNearPlayer(BuilderCalculationContext bcc) {
        BetterBlockPos center = ctx.playerFeet();
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = 0; dy <= 5; dy++) {
                for (int dz = -5; dz <= 5; dz++) {
                    int x = center.x + dx;
                    int y = center.y + dy;
                    int z = center.z + dz;
                    IBlockState desired = bcc.getSchematic(x, y, z);
                    if (desired == null) {
                        continue; // irrelevant
                    }
                    IBlockState curr = bcc.bsi.get0(x, y, z);
                    if (!(curr.getBlock() instanceof BlockAir) && !valid(curr, desired)) {
                        BetterBlockPos pos = new BetterBlockPos(x, y, z);
                        Optional<Rotation> rot = RotationUtils.reachable(ctx.player(), pos, ctx.playerController().getBlockReachDistance());
                        if (rot.isPresent()) {
                            return Optional.of(new Tuple<>(pos, rot.get()));
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    public class Placement {
        private final int hotbarSelection;
        private final BlockPos placeAgainst;
        private final EnumFacing side;
        private final Rotation rot;

        public Placement(int hotbarSelection, BlockPos placeAgainst, EnumFacing side, Rotation rot) {
            this.hotbarSelection = hotbarSelection;
            this.placeAgainst = placeAgainst;
            this.side = side;
            this.rot = rot;
        }
    }

    public Optional<Placement> searchForPlacables(BuilderCalculationContext bcc, List<IBlockState> desirableOnHotbar) {
        BetterBlockPos center = ctx.playerFeet();
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = -5; dy <= 1; dy++) {
                for (int dz = -5; dz <= 5; dz++) {
                    int x = center.x + dx;
                    int y = center.y + dy;
                    int z = center.z + dz;
                    IBlockState desired = bcc.getSchematic(x, y, z);
                    if (desired == null) {
                        continue; // irrelevant
                    }
                    IBlockState curr = bcc.bsi.get0(x, y, z);
                    if (MovementHelper.isReplacable(x, y, z, curr, bcc.bsi) && !valid(curr, desired)) {
                        if (dy == 1 && bcc.bsi.get0(x, y + 1, z).getBlock() instanceof BlockAir) {
                            continue;
                        }
                        desirableOnHotbar.add(desired);
                        Optional<Placement> opt = possibleToPlace(desired, x, y, z, bcc.bsi);
                        if (opt.isPresent()) {
                            return opt;
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    public boolean placementPlausible(BlockPos pos, IBlockState state) {
        VoxelShape voxelshape = state.getCollisionShape(ctx.world(), pos);
        return voxelshape.isEmpty() || ctx.world().checkNoEntityCollision(null, voxelshape.withOffset(pos.getX(), pos.getY(), pos.getZ()));
    }

    public Optional<Placement> possibleToPlace(IBlockState toPlace, int x, int y, int z, BlockStateInterface bsi) {
        for (EnumFacing against : EnumFacing.values()) {
            BetterBlockPos placeAgainstPos = new BetterBlockPos(x, y, z).offset(against);
            IBlockState placeAgainstState = bsi.get0(placeAgainstPos);
            if (MovementHelper.isReplacable(placeAgainstPos.x, placeAgainstPos.y, placeAgainstPos.z, placeAgainstState, bsi)) {
                continue;
            }
            if (!toPlace.isValidPosition(ctx.world(), new BetterBlockPos(x, y, z))) {
                continue;
            }
            if (!placementPlausible(new BetterBlockPos(x, y, z), toPlace)) {
                continue;
            }
            AxisAlignedBB aabb = placeAgainstState.getShape(ctx.world(), placeAgainstPos).getBoundingBox();
            for (Vec3d placementMultiplier : aabbSideMultipliers(against)) {
                double placeX = placeAgainstPos.x + aabb.minX * placementMultiplier.x + aabb.maxX * (1 - placementMultiplier.x);
                double placeY = placeAgainstPos.y + aabb.minY * placementMultiplier.y + aabb.maxY * (1 - placementMultiplier.y);
                double placeZ = placeAgainstPos.z + aabb.minZ * placementMultiplier.z + aabb.maxZ * (1 - placementMultiplier.z);
                Rotation rot = RotationUtils.calcRotationFromVec3d(ctx.playerHead(), new Vec3d(placeX, placeY, placeZ), ctx.playerRotations());
                RayTraceResult result = RayTraceUtils.rayTraceTowards(ctx.player(), rot, ctx.playerController().getBlockReachDistance());
                if (result != null && result.type == RayTraceResult.Type.BLOCK && result.getBlockPos().equals(placeAgainstPos) && result.sideHit == against.getOpposite()) {
                    OptionalInt hotbar = hasAnyItemThatWouldPlace(toPlace, result, rot);
                    if (hotbar.isPresent()) {
                        return Optional.of(new Placement(hotbar.getAsInt(), placeAgainstPos, against.getOpposite(), rot));
                    }
                }
            }
        }
        return Optional.empty();
    }


    public OptionalInt hasAnyItemThatWouldPlace(IBlockState desired, RayTraceResult result, Rotation rot) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = ctx.player().inventory.mainInventory.get(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof ItemBlock)) {
                continue;
            }
            float originalYaw = ctx.player().rotationYaw;
            float originalPitch = ctx.player().rotationPitch;
            // the state depends on the facing of the player sometimes
            ctx.player().rotationYaw = rot.getYaw();
            ctx.player().rotationPitch = rot.getPitch();
            BlockItemUseContext meme = new BlockItemUseContext(new ItemUseContext(
                    ctx.player(),
                    stack,
                    result.getBlockPos().offset(result.sideHit),
                    result.sideHit,
                    (float) result.hitVec.x - result.getBlockPos().getX(),
                    (float) result.hitVec.y - result.getBlockPos().getY(),
                    (float) result.hitVec.z - result.getBlockPos().getZ()
            ));
            IBlockState wouldBePlaced = ((ItemBlock) stack.getItem()).getBlock().getStateForPlacement(meme);
            ctx.player().rotationYaw = originalYaw;
            ctx.player().rotationPitch = originalPitch;
            if (wouldBePlaced == null) {
                continue;
            }
            if (!meme.canPlace()) {
                continue;
            }
            if (valid(wouldBePlaced, desired)) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }

    private static Vec3d[] aabbSideMultipliers(EnumFacing side) {
        switch (side) {
            case UP:
                return new Vec3d[]{new Vec3d(0.5, 1, 0.5), new Vec3d(0.1, 1, 0.5), new Vec3d(0.9, 1, 0.5), new Vec3d(0.5, 1, 0.1), new Vec3d(0.5, 1, 0.9)};
            case DOWN:
                return new Vec3d[]{new Vec3d(0.5, 0, 0.5), new Vec3d(0.1, 0, 0.5), new Vec3d(0.9, 0, 0.5), new Vec3d(0.5, 0, 0.1), new Vec3d(0.5, 0, 0.9)};
            case NORTH:
            case SOUTH:
            case EAST:
            case WEST:
                double x = side.getXOffset() == 0 ? 0.5 : (1 + side.getXOffset()) / 2D;
                double z = side.getZOffset() == 0 ? 0.5 : (1 + side.getZOffset()) / 2D;
                return new Vec3d[]{new Vec3d(x, 0.25, z), new Vec3d(x, 0.75, z)};
            default: // null
                throw new IllegalStateException();
        }
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        baritone.getInputOverrideHandler().clearAllKeys();
        if (paused) {
            return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
        }
        BuilderCalculationContext bcc = new BuilderCalculationContext();
        if (!recalc(bcc)) {
            logDirect("Done building");
            onLostControl();
            return null;
        }
        trim(bcc);
        if (baritone.getInputOverrideHandler().isInputForcedDown(Input.CLICK_LEFT)) {
            ticks = 5;
        } else {
            ticks--;
        }
        Optional<Tuple<BetterBlockPos, Rotation>> toBreak = toBreakNearPlayer(bcc);
        baritone.getInputOverrideHandler().clearAllKeys();
        if (toBreak.isPresent() && isSafeToCancel && ctx.player().onGround) {
            // we'd like to pause to break this block
            // only change look direction if it's safe (don't want to fuck up an in progress parkour for example
            Rotation rot = toBreak.get().getB();
            BetterBlockPos pos = toBreak.get().getA();
            baritone.getLookBehavior().updateTarget(rot, true);
            MovementHelper.switchToBestToolFor(ctx, bcc.get(pos));
            if (ctx.player().isSneaking()) {
                // really horrible bug where a block is visible for breaking while sneaking but not otherwise
                // so you can't see it, it goes to place something else, sneaks, then the next tick it tries to break
                // and is unable since it's unsneaked in the intermediary tick
                baritone.getInputOverrideHandler().setInputForceState(Input.SNEAK, true);
            }
            if (Objects.equals(ctx.objectMouseOver().getBlockPos(), pos) || ctx.playerRotations().isReallyCloseTo(rot)) {
                baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_LEFT, true);
            }
            return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
        }
        List<IBlockState> desirableOnHotbar = new ArrayList<>();
        Optional<Placement> toPlace = searchForPlacables(bcc, desirableOnHotbar);
        if (toPlace.isPresent() && isSafeToCancel && ctx.player().onGround && ticks <= 0) {
            Rotation rot = toPlace.get().rot;
            baritone.getLookBehavior().updateTarget(rot, true);
            ctx.player().inventory.currentItem = toPlace.get().hotbarSelection;
            baritone.getInputOverrideHandler().setInputForceState(Input.SNEAK, true);
            if ((Objects.equals(ctx.objectMouseOver().getBlockPos(), toPlace.get().placeAgainst) && ctx.objectMouseOver().sideHit.equals(toPlace.get().side)) || ctx.playerRotations().isReallyCloseTo(rot)) {
                baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
            }
            return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
        }

        List<IBlockState> approxPlacable = placable(36);
        if (Baritone.settings().allowInventory.value) {
            ArrayList<Integer> usefulSlots = new ArrayList<>();
            List<IBlockState> noValidHotbarOption = new ArrayList<>();
            outer:
            for (IBlockState desired : desirableOnHotbar) {
                for (int i = 0; i < 9; i++) {
                    if (valid(approxPlacable.get(i), desired)) {
                        usefulSlots.add(i);
                        continue outer;
                    }
                }
                noValidHotbarOption.add(desired);
            }

            outer:
            for (int i = 9; i < 36; i++) {
                for (IBlockState desired : noValidHotbarOption) {
                    if (valid(approxPlacable.get(i), desired)) {
                        baritone.getInventoryBehavior().attemptToPutOnHotbar(i, usefulSlots::contains);
                        break outer;
                    }
                }
            }
        }


        Goal goal = assemble(bcc, approxPlacable.subList(0, 9));
        if (goal == null) {
            goal = assemble(bcc, approxPlacable); // we're far away, so assume that we have our whole inventory to recalculate placable properly
            if (goal == null) {
                logDirect("Unable to do it. Pausing. resume to resume, cancel to cancel");
                paused = true;
                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
            }
        }
        return new PathingCommandContext(goal, PathingCommandType.FORCE_REVALIDATE_GOAL_AND_PATH, bcc);
    }

    public boolean recalc(BuilderCalculationContext bcc) {
        if (incorrectPositions == null) {
            incorrectPositions = new HashSet<>();
            fullRecalc(bcc);
            if (incorrectPositions.isEmpty()) {
                return false;
            }
        }
        recalcNearby(bcc);
        if (incorrectPositions.isEmpty()) {
            fullRecalc(bcc);
        }
        return !incorrectPositions.isEmpty();
    }

    public void trim(BuilderCalculationContext bcc) {
        HashSet<BetterBlockPos> copy = new HashSet<>(incorrectPositions);
        copy.removeIf(pos -> pos.distanceSq(ctx.player().posX, ctx.player().posY, ctx.player().posZ) > 200);
        if (!copy.isEmpty()) {
            incorrectPositions = copy;
        }
    }

    public void recalcNearby(BuilderCalculationContext bcc) {
        BetterBlockPos center = ctx.playerFeet();
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = -5; dy <= 5; dy++) {
                for (int dz = -5; dz <= 5; dz++) {
                    int x = center.x + dx;
                    int y = center.y + dy;
                    int z = center.z + dz;
                    IBlockState desired = bcc.getSchematic(x, y, z);
                    if (desired != null) {
                        // we care about this position
                        if (valid(bcc.bsi.get0(x, y, z), desired)) {
                            incorrectPositions.remove(new BetterBlockPos(x, y, z));
                        } else {
                            incorrectPositions.add(new BetterBlockPos(x, y, z));
                        }
                    }
                }
            }
        }
    }

    public void fullRecalc(BuilderCalculationContext bcc) {
        incorrectPositions = new HashSet<>();
        for (int y = 0; y < schematic.heightY(); y++) {
            for (int z = 0; z < schematic.lengthZ(); z++) {
                for (int x = 0; x < schematic.widthX(); x++) {
                    if (schematic.inSchematic(x, y, z) && !valid(bcc.bsi.get0(x + origin.getX(), y + origin.getY(), z + origin.getZ()), schematic.desiredState(x, y, z))) {
                        incorrectPositions.add(new BetterBlockPos(x + origin.getX(), y + origin.getY(), z + origin.getZ()));
                    }
                }
            }
        }
    }

    private Goal assemble(BuilderCalculationContext bcc, List<IBlockState> approxPlacable) {
        List<BetterBlockPos> placable = incorrectPositions.stream().filter(pos -> bcc.bsi.get0(pos).getBlock() instanceof BlockAir && approxPlacable.contains(bcc.getSchematic(pos.x, pos.y, pos.z))).collect(Collectors.toList());
        Goal[] toBreak = incorrectPositions.stream().filter(pos -> !(bcc.bsi.get0(pos).getBlock() instanceof BlockAir)).map(GoalBreak::new).toArray(Goal[]::new);
        Goal[] toPlace = placable.stream().filter(pos -> !placable.contains(pos.down()) && !placable.contains(pos.down(2))).map(pos -> placementgoal(pos, bcc)).toArray(Goal[]::new);

        if (toPlace.length != 0) {
            return new JankyGoalComposite(new GoalComposite(toPlace), new GoalComposite(toBreak));
        }
        if (toBreak.length == 0) {
            return null;
        }
        return new GoalComposite(toBreak);
    }

    public static class JankyGoalComposite implements Goal {
        private final Goal primary;
        private final Goal fallback;

        public JankyGoalComposite(Goal primary, Goal fallback) {
            this.primary = primary;
            this.fallback = fallback;
        }


        @Override
        public boolean isInGoal(int x, int y, int z) {
            return primary.isInGoal(x, y, z) || fallback.isInGoal(x, y, z);
        }

        @Override
        public double heuristic(int x, int y, int z) {
            return primary.heuristic(x, y, z);
        }

        @Override
        public String toString() {
            return "JankyComposite Primary: " + primary + " Fallback: " + fallback;
        }
    }

    public static class GoalBreak extends GoalGetToBlock {

        public GoalBreak(BlockPos pos) {
            super(pos);
        }

        @Override
        public boolean isInGoal(int x, int y, int z) {
            // can't stand right on top of a block, that might not work (what if it's unsupported, can't break then)
            if (y > this.y) {
                return false;
            }
            // but any other adjacent works for breaking, including inside or below
            return super.isInGoal(x, y, z);
        }
    }

    public Goal placementgoal(BlockPos pos, BuilderCalculationContext bcc) {
        if (!(ctx.world().getBlockState(pos).getBlock() instanceof BlockAir)) {
            return new GoalPlace(pos);
        }
        boolean allowSameLevel = !(ctx.world().getBlockState(pos.up()).getBlock() instanceof BlockAir);
        for (EnumFacing facing : Movement.HORIZONTALS_BUT_ALSO_DOWN____SO_EVERY_DIRECTION_EXCEPT_UP) {
            if (MovementHelper.canPlaceAgainst(ctx, pos.offset(facing)) && placementPlausible(pos, bcc.getSchematic(pos.getX(), pos.getY(), pos.getZ()))) {
                return new GoalAdjacent(pos, allowSameLevel);
            }
        }
        return new GoalPlace(pos);
    }

    public static class GoalAdjacent extends GoalGetToBlock {
        private boolean allowSameLevel;

        public GoalAdjacent(BlockPos pos, boolean allowSameLevel) {
            super(pos);
            this.allowSameLevel = allowSameLevel;
        }

        public boolean isInGoal(int x, int y, int z) {
            if (x == this.x && y == this.y && z == this.z) {
                return false;
            }
            if (!allowSameLevel && y == this.y - 1) {
                return false;
            }
            if (y < this.y - 1) {
                return false;
            }
            return super.isInGoal(x, y, z);
        }

        public double heuristic(int x, int y, int z) {
            // prioritize lower y coordinates
            return this.y * 100 + super.heuristic(x, y, z);
        }
    }

    public static class GoalPlace extends GoalBlock {
        public GoalPlace(BlockPos placeAt) {
            super(placeAt.up());
        }

        public double heuristic(int x, int y, int z) {
            // prioritize lower y coordinates
            return this.y * 100 + super.heuristic(x, y, z);
        }
    }

    @Override
    public void onLostControl() {
        incorrectPositions = null;
        name = null;
        schematic = null;
        paused = false;
    }

    @Override
    public String displayName0() {
        return paused ? "Builder Paused" : "Building " + name;
    }

    public List<IBlockState> placable(int size) {
        List<IBlockState> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            ItemStack stack = ctx.player().inventory.mainInventory.get(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof ItemBlock)) {
                result.add(Blocks.AIR.getDefaultState());
                continue;
            }
            // <toxic cloud>
            result.add(((ItemBlock) stack.getItem()).getBlock().getStateForPlacement(new BlockItemUseContext(new ItemUseContext(ctx.player(), stack, ctx.playerFeet(), EnumFacing.UP, (float) ctx.player().posX, (float) ctx.player().posY, (float) ctx.player().posZ))));
            // </toxic cloud>
        }
        return result;
    }

    public boolean valid(IBlockState current, IBlockState desired) {
        if (desired == null) {
            return true;
        }
        if (current.getBlock() instanceof BlockAir && desired.getBlock() instanceof BlockAir) {
            return true;
        }
        // TODO more complicated comparison logic I guess
        return current.equals(desired);
    }

    public class BuilderCalculationContext extends CalculationContext {
        private final List<IBlockState> placable;
        private final ISchematic schematic;
        private final int originX;
        private final int originY;
        private final int originZ;

        public BuilderCalculationContext() {
            super(BuilderProcess.this.baritone, true); // wew lad
            this.placable = placable(9);
            this.schematic = BuilderProcess.this.schematic;
            this.originX = origin.getX();
            this.originY = origin.getY();
            this.originZ = origin.getZ();

            this.jumpPenalty += 10;
            this.backtrackCostFavoringCoefficient = 1;
        }

        private IBlockState getSchematic(int x, int y, int z) {
            if (schematic.inSchematic(x - originX, y - originY, z - originZ)) {
                return schematic.desiredState(x - originX, y - originY, z - originZ);
            } else {
                return null;
            }
        }

        @Override
        public double costOfPlacingAt(int x, int y, int z) {
            if (isPossiblyProtected(x, y, z) || !worldBorder.canPlaceAt(x, z)) { // make calculation fail properly if we can't build
                return COST_INF;
            }
            IBlockState sch = getSchematic(x, y, z);
            if (sch != null) {
                // TODO this can return true even when allowPlace is off.... is that an issue?
                if (sch.getBlock() instanceof BlockAir) {
                    // we want this to be air, but they're asking if they can place here
                    // this won't be a schematic block, this will be a throwaway
                    return placeBlockCost * 2; // we're going to have to break it eventually
                }
                if (placable.contains(sch)) {
                    return 0; // thats right we gonna make it FREE to place a block where it should go in a structure
                    // no place block penalty at all 😎
                    // i'm such an idiot that i just tried to copy and paste the epic gamer moment emoji too
                    // get added to unicode when?
                }
                if (!hasThrowaway) {
                    return COST_INF;
                }
                // we want it to be something that we don't have
                // even more of a pain to place something wrong
                return placeBlockCost * 3;
            } else {
                if (hasThrowaway) {
                    return placeBlockCost;
                } else {
                    return COST_INF;
                }
            }
        }

        @Override
        public double breakCostMultiplierAt(int x, int y, int z) {
            if (!allowBreak || isPossiblyProtected(x, y, z)) {
                return COST_INF;
            }
            IBlockState sch = getSchematic(x, y, z);
            if (sch != null) {
                if (sch.getBlock() instanceof BlockAir) {
                    // it should be air
                    // regardless of current contents, we can break it
                    return 1;
                }
                // it should be a real block
                // is it already that block?
                if (valid(bsi.get0(x, y, z), sch)) {
                    return 3;
                } else {
                    // can break if it's wrong
                    // would be great to return less than 1 here, but that would actually make the cost calculation messed up
                    // since we're breaking a block, if we underestimate the cost, then it'll fail when it really takes the correct amount of time
                    return 1;

                }
                // TODO do blocks in render distace only?
                // TODO allow breaking blocks that we have a tool to harvest and immediately place back?
            } else {
                return 1; // why not lol
            }
        }
    }
}
