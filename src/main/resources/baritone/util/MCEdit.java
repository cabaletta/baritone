package baritone.util;

import baritone.Baritone;
import baritone.pathfinding.goals.GoalComposite;
import java.util.HashSet;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

/**
 *
 * @author leijurv
 */
public class MCEdit extends Manager {

    static BlockPos pos1 = null;
    static BlockPos pos2 = null;

    public static String pos1(String s) {
        return "Pos 1: " + (pos1 = Baritone.playerFeet);
    }

    public static String pos2(String s) {
        return "Pos 2: " + (pos2 = Baritone.playerFeet);
    }

    public static String delete(String s) {
        Manager.getManager(MCEdit.class).toggle();
        return "k";
    }

    private static HashSet<BlockPos> toBreak() {
        HashSet<BlockPos> toBreak = new HashSet<>();
        for (int x = Math.min(pos1.getX(), pos2.getX()); x <= Math.max(pos1.getX(), pos2.getX()); x++) {
            for (int y = Math.max(pos1.getY(), pos2.getY()); y >= Math.min(pos1.getY(), pos2.getY()); y--) {
                for (int z = Math.min(pos1.getZ(), pos2.getZ()); z <= Math.max(pos1.getZ(), pos2.getZ()); z++) {
                    BlockPos po = new BlockPos(x, y, z);
                    Block b = Baritone.get(po).getBlock();
                    if (!Blocks.AIR.equals(b)) {
                        toBreak.add(po);
                        if (toBreak.size() > 20) {
                            return toBreak;
                        }
                    }
                }
            }
        }
        return toBreak;
    }

    @Override
    protected void onTick() {
        HashSet<BlockPos> toBreak = toBreak();
        if (!toBreak.isEmpty()) {
            Baritone.goal = new GoalComposite(toBreak);
            if (Baritone.currentPath == null && !Baritone.isThereAnythingInProgress) {
                Baritone.findPathInNewThread(false);
            }
        }
    }

    @Override
    protected void onCancel() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void onStart() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
