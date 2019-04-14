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
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import baritone.cache.WorldScanner;
import baritone.utils.BaritoneProcessHelper;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class FarmProcess extends BaritoneProcessHelper {

    private boolean active;

    public FarmProcess(Baritone baritone) {
        super(baritone);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void doit() {
        active = true;
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        List<BlockPos> memes = WorldScanner.INSTANCE.scanChunkRadius(ctx, Arrays.asList(Blocks.FARMLAND, Blocks.WHEAT), 256, 10, 4);

        List<BlockPos> toBreak = new ArrayList<>();
        List<BlockPos> toRightClickOnTop = new ArrayList<>();
        for (BlockPos pos : memes) {
            IBlockState state = ctx.world().getBlockState(pos);
            if (state.getBlock() == Blocks.FARMLAND) {
                if (ctx.world().getBlockState(pos.up()).getBlock() instanceof BlockAir) {
                    toRightClickOnTop.add(pos);
                }
            } else {
                if (state.getValue(BlockCrops.AGE) == 7) {
                    toBreak.add(pos);
                }
            }
        }

        baritone.getInputOverrideHandler().clearAllKeys();
        for (BlockPos pos : toBreak) {
            Optional<Rotation> rot = RotationUtils.reachable(ctx, pos);
            if (rot.isPresent()) {
                baritone.getLookBehavior().updateTarget(rot.get(), true);
                if (ctx.objectMouseOver() != null && ctx.objectMouseOver().typeOfHit == RayTraceResult.Type.BLOCK && ctx.objectMouseOver().getBlockPos().equals(pos)) {
                    baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_LEFT, true);
                }
                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
            }
        }
        for (BlockPos pos : toRightClickOnTop) {
            Optional<Rotation> rot =  RotationUtils.reachableOffset(ctx.player(), pos, new Vec3d(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5), ctx.playerController().getBlockReachDistance());
            if (rot.isPresent()) {
                baritone.getLookBehavior().updateTarget(rot.get(), true);
                if (ctx.objectMouseOver() != null && ctx.objectMouseOver().typeOfHit == RayTraceResult.Type.BLOCK && ctx.objectMouseOver().getBlockPos().equals(pos)) {
                    baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
                }
                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
            }
        }

        List<Goal> goalz = new ArrayList<>();
        for (BlockPos pos : toBreak) {
            goalz.add(new GoalBlock(pos));
        }
        for (BlockPos pos : toRightClickOnTop) {
            goalz.add(new GoalBlock(pos.up()));
        }
        return new PathingCommand(new GoalComposite(goalz.toArray(new Goal[0])), PathingCommandType.SET_GOAL_AND_PATH);
    }

    @Override
    public void onLostControl() {
        active = false;
    }

    @Override
    public String displayName0() {
        return "Farming";
    }
}
