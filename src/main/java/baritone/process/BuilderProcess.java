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
import baritone.pathing.movement.CalculationContext;
import baritone.utils.BaritoneProcessHelper;
import baritone.utils.BlockStateInterface;
import baritone.utils.ISchematic;
import baritone.utils.PathingCommandContext;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3i;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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
        throw new UnsupportedOperationException("would rather die than parse " + schematic);
    }

    @Override
    public boolean isActive() {
        return schematic != null;
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        // TODO somehow tell inventorybehavior what we'd like to have on the hotbar
        // perhaps take the 16 closest positions in incorrectPositions to ctx.playerFeet that aren't desired to be air, and then snag the top 4 most common block states, then request those on the hotbar


        // this will work as is, but it'll be trashy
        // need to iterate over incorrectPositions and see which ones we can "correct" from our current standing position

        // considerations:
        //  shouldn't break blocks that are supporting our current path segment, maybe?
        //
        return new PathingCommandContext(new GoalComposite(assemble()), PathingCommandType.FORCE_REVALIDATE_GOAL_AND_PATH, new BuilderCalculationContext(schematic, origin));
    }

    private Goal[] assemble() {
        BlockStateInterface bsi = new CalculationContext(baritone).bsi;
        return incorrectPositions.stream().map(pos ->
                bsi.get0(pos).getBlock() == Blocks.AIR ?
                        // it's air and it shouldn't be
                        new GoalBlock(pos.up())
                        // it's a block and it shouldn't be
                        // todo disallow right above
                        : new GoalGetToBlock(pos) // replace with GoalTwoBlocks to mine using pathfinding system only
        ).toArray(Goal[]::new);
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
                if (placable.contains(sch)) {
                    return 0; // thats right we gonna make it FREE to place a block where it should go in a structure
                    // no place block penalty at all 😎
                    // i'm such an idiot that i just tried to copy and paste the epic gamer moment emoji too
                    // get added to unicode when?
                }
                if (!hasThrowaway) {
                    return COST_INF;
                }
                if (sch.getBlock() == Blocks.AIR) {
                    // we want this to be air, but they're asking if they can place here
                    // this won't be a schematic block, this will be a throwaway
                    return placeBlockCost * 2; // we're going to have to break it eventually
                } else {
                    // we want it to be something that we don't have
                    // even more of a pain to place something wrong
                    return placeBlockCost * 3;
                }
            } else {
                if (hasThrowaway) {
                    return placeBlockCost;
                } else {
                    return COST_INF;
                }
            }
        }

        @Override
        public boolean canBreakAt(int x, int y, int z) {
            if (!allowBreak || isPossiblyProtected(x, y, z)) {
                return false;
            }
            IBlockState sch = getSchematic(x, y, z);
            if (sch != null) {
                if (sch.getBlock() == Blocks.AIR) {
                    // it should be air
                    // regardless of current contents, we can break it
                    return true;
                }
                // it should be a real block
                // is it already that block?
                return !bsi.get0(x, y, z).equals(sch); // can break if it's wrong
                // TODO do blocks in render distace only?
                // TODO allow breaking blocks that we have a tool to harvest and immediately place back?
            } else {
                return true; // why not lol
            }
        }
    }
}
