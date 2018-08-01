/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.pathfinding.goals;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;

/**
 *
 * @author avecowa
 */
public class GoalGetToBlock extends GoalComposite{
    public static BlockPos goalPos;
    public GoalGetToBlock(BlockPos pos) {
        super(ajacentBlocks(goalPos = pos));
    }
    public GoalGetToBlock(){
        this(Baritone.playerFeet);
    }
    public static BlockPos[] ajacentBlocks(BlockPos pos){
        BlockPos[] sides = new BlockPos[6];
        for(int i = 0; i < 6; i++){
            sides[i] = pos.offset(EnumFacing.values()[i]);
        }
        return sides;
    }
}
