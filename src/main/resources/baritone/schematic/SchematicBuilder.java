/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.schematic;

import baritone.Baritone;
import baritone.pathfinding.goals.GoalComposite;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;

/**
 *
 * @author leijurv
 */
public class SchematicBuilder {

    ArrayList<Tuple<BlockPos, Block>> plan = new ArrayList();
    BlockPos offset;
    Schematic schematic;

    public SchematicBuilder(Schematic schematic, BlockPos offset) {
        this.schematic = schematic;
        this.offset = offset;
        for (Entry<BlockPos, Block> entry : schematic.getEntries()) {
            plan.add(new Tuple(offset(entry.getKey()), entry.getValue()));
        }
    }

    public void tick() {
        HashSet<BlockPos> goal = getAllBlocksToPlaceShiftedUp();
        //Out.log("Ticking " + goal);
        if (goal != null) {
            Baritone.goal = new GoalComposite(goal);
            if (Baritone.currentPath == null && !Baritone.isThereAnythingInProgress) {
                Baritone.findPathInNewThread(false);
            }
        } else {
            //Out.gui("done building", Out.Mode.Standard);
        }
    }

    public HashSet<BlockPos> getAllBlocksToPlaceShiftedUp() {
        HashSet<BlockPos> toPlace = new HashSet<>();
        Block air = Blocks.AIR;
        for (int y = 0; y < schematic.getHeight(); y++) {
            for (int x = 0; x < schematic.getWidth(); x++) {
                for (int z = 0; z < schematic.getLength(); z++) {
                    BlockPos inSchematic = new BlockPos(x, y, z);
                    BlockPos inWorld = offset(inSchematic);
                    Block current = Baritone.get(inWorld).getBlock();
                    Block desired = schematic.getBlockFromBlockPos(inSchematic);
                    //Out.log(inSchematic + " " + current + " " + desired);
                    boolean currentlyAir = air.equals(current);
                    boolean shouldBeAir = desired == null || air.equals(desired);
                    if (currentlyAir && !shouldBeAir) {
                        toPlace.add(inWorld.up());
                    }
                }
            }
        }
        return toPlace.isEmpty() ? null : toPlace;
    }

    private BlockPos offset(BlockPos original) {
        return new BlockPos(original.getX() + offset.getX(), original.getY() + offset.getY(), original.getZ() + offset.getZ());
    }
}
