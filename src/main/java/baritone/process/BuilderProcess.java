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
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.MovementHelper;
import baritone.utils.BaritoneProcessHelper;
import baritone.utils.ISchematic;
import baritone.utils.PathingCommandContext;
import baritone.utils.Schematic;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static baritone.api.pathing.movement.ActionCosts.COST_INF;

public class BuilderProcess extends BaritoneProcessHelper {
    public BuilderProcess(Baritone baritone) {
        super(baritone);
    }

    private HashSet<BetterBlockPos> incorrectPositions;
    private String name;
    private ISchematic schematic;
    private Vec3i origin;

    public boolean build(String schematicFile) {
        File file = new File(new File(Minecraft.getMinecraft().gameDir, "schematics"), schematicFile);
        System.out.println(file + " " + file.exists());

        NBTTagCompound tag;
        try (FileInputStream fileIn = new FileInputStream(file)) {
            tag = CompressedStreamTools.readCompressed(fileIn);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if (tag == null) {
            return false;
        }
        name = schematicFile;
        schematic = parse(tag);
        origin = ctx.playerFeet();
        return true;
    }

    private static ISchematic parse(NBTTagCompound schematic) {
        return new Schematic(schematic);
    }

    @Override
    public boolean isActive() {
        return schematic != null;
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
                    if (curr.getBlock() != Blocks.AIR && !valid(curr, desired)) {
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

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        // TODO somehow tell inventorybehavior what we'd like to have on the hotbar
        // perhaps take the 16 closest positions in incorrectPositions to ctx.playerFeet that aren't desired to be air, and then snag the top 4 most common block states, then request those on the hotbar


        // this will work as is, but it'll be trashy
        // need to iterate over incorrectPositions and see which ones we can "correct" from our current standing position


        BuilderCalculationContext bcc = new BuilderCalculationContext(schematic, origin);
        if (!recalc(bcc)) {
            logDirect("Done building");
            onLostControl();
            return null;
        }
        Optional<Tuple<BetterBlockPos, Rotation>> toBreak = toBreakNearPlayer(bcc);
        baritone.getInputOverrideHandler().clearAllKeys();
        if (toBreak.isPresent() && isSafeToCancel && ctx.player().onGround) {
            // we'd like to pause to break this block
            // only change look direction if it's safe (don't want to fuck up an in progress parkour for example
            Rotation rot = toBreak.get().getSecond();
            BetterBlockPos pos = toBreak.get().getFirst();
            baritone.getLookBehavior().updateTarget(rot, true);
            MovementHelper.switchToBestToolFor(ctx, bcc.get(pos));
            if (Objects.equals(ctx.objectMouseOver().getBlockPos(), rot) || ctx.playerRotations().isReallyCloseTo(rot)) {
                baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_LEFT, true);
            }
            return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
        }

        Goal[] goals = assemble(bcc);
        if (goals.length == 0) {
            logDirect("Unable to do it =(");
            onLostControl();
            return null;
        }
        return new PathingCommandContext(new GoalComposite(goals), PathingCommandType.FORCE_REVALIDATE_GOAL_AND_PATH, bcc);
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
                    if (schematic.inSchematic(x, y, z)) {
                        if (!valid(bcc.bsi.get0(x + origin.getX(), y + origin.getY(), z + origin.getZ()), schematic.desiredState(x, y, z))) {
                            incorrectPositions.add(new BetterBlockPos(x + origin.getX(), y + origin.getY(), z + origin.getZ()));
                        }
                    }
                }
            }
        }
    }

    private Goal[] assemble(BuilderCalculationContext bcc) {
        List<IBlockState> approxPlacable = placable();
        List<BetterBlockPos> placable = incorrectPositions.stream().filter(pos -> bcc.bsi.get0(pos).getBlock() == Blocks.AIR && approxPlacable.contains(bcc.getSchematic(pos.x, pos.y, pos.z))).collect(Collectors.toList());
        if (!placable.isEmpty()) {
            return placable.stream().filter(pos -> !placable.contains(pos.down()) && !placable.contains(pos.down(2))).map(BetterBlockPos::up).map(GoalBlock::new).toArray(Goal[]::new);
        }
        return incorrectPositions.stream().filter(pos -> bcc.bsi.get0(pos).getBlock() != Blocks.AIR).map(GoalBreak::new).toArray(Goal[]::new);
    }

    public static class GoalBreak extends GoalGetToBlock {

        public GoalBreak(BlockPos pos) {
            super(pos);
        }

        @Override
        public boolean isInGoal(int x, int y, int z) {
            // can't stand right on top of a block, that might not work (what if it's unsupported, can't break then)
            if (x == this.x && y == this.y + 1 && z == this.z) {
                return false;
            }
            // but any other adjacent works for breaking, including inside or below
            return super.isInGoal(x, y, z);
        }
    }

    @Override
    public void onLostControl() {
        incorrectPositions = null;
        name = null;
        schematic = null;
    }

    @Override
    public String displayName() {
        return "Building " + name;
    }

    /**
     * Hotbar contents, if they were placed
     * <p>
     * Always length nine, empty slots become Blocks.AIR.getDefaultState()
     *
     * @return
     */
    public List<IBlockState> placable() {
        List<IBlockState> result = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = ctx.player().inventory.mainInventory.get(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof ItemBlock)) {
                result.add(Blocks.AIR.getDefaultState());
                continue;
            }
            // <toxic cloud>
            result.add(((ItemBlock) stack.getItem()).getBlock().getStateForPlacement(ctx.world(), ctx.playerFeet(), EnumFacing.UP, (float) ctx.player().posX, (float) ctx.player().posY, (float) ctx.player().posZ, stack.getItem().getMetadata(stack.getMetadata()), ctx.player()));
            // </toxic cloud>
        }
        return result;
    }

    public boolean valid(IBlockState current, IBlockState desired) {
        // TODO more complicated comparison logic I guess
        return desired == null || current.equals(desired);
    }

    public class BuilderCalculationContext extends CalculationContext {
        private final List<IBlockState> placable;
        private final ISchematic schematic;
        private final int originX;
        private final int originY;
        private final int originZ;

        public BuilderCalculationContext(ISchematic schematic, Vec3i schematicOrigin) {
            super(BuilderProcess.this.baritone, true); // wew lad
            this.placable = placable();
            this.schematic = schematic;
            this.originX = schematicOrigin.getX();
            this.originY = schematicOrigin.getY();
            this.originZ = schematicOrigin.getZ();
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
                if (sch.getBlock() == Blocks.AIR) {
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
                if (sch.getBlock() == Blocks.AIR) {
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
