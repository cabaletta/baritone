/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import baritone.Baritone;
import static baritone.Baritone.findPathInNewThread;
import static baritone.Baritone.goal;
import baritone.pathfinding.goals.GoalBlock;
import baritone.pathfinding.goals.GoalTwoBlocks;
import baritone.util.Manager;
import baritone.util.Out;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.EmptyChunk;

/**
 *
 * @author leijurv
 */
public class Memory extends Manager {
    public static HashMap<Block, BlockMemory> blockMemory = new HashMap();
    public static HashMap<String, BlockPos> playerLocationMemory = new HashMap();
    public static HashMap<String, BlockPos> goalMemory = new HashMap();
    public static ArrayList<String> playersCurrentlyInRange = new ArrayList();
    public static Thread scanThread = null;
    public static Block air = null;
    public Memory() {
    }
    @Override
    protected void onCancel() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    @Override
    protected void onStart() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    @Override
    protected boolean onEnabled(boolean enabled) {
        return true;
    }

    public static class BlockMemory {
        final Block block;
        final ArrayList<BlockPos> knownPositions;//idk whether to use hashset or arraylist here...
        private volatile double furthest;
        public BlockMemory(Block block) {
            this.block = block;
            this.knownPositions = new ArrayList();
        }
        public void checkForChange() {
            for (BlockPos pos : new ArrayList<BlockPos>(knownPositions)) {//make duplicate to prevent concurrent modification exceptions
                boolean loaded = blockLoaded(pos);
                if (!loaded) {
                    //Out.gui("Too far away from " + pos + " to remember that it's " + block, true);
                }
                Block current = Minecraft.getMinecraft().world.getBlockState(pos).getBlock();
                if (!current.equals(block) || !loaded) {
                    //Out.gui("Block at " + pos + " has changed from " + block + " to " + current + ". Removing from memory.", true);
                    knownPositions.remove(pos);
                    if (loaded) {
                        scanBlock(pos);//rescan to put in proper memory
                    }
                }
            }
        }
        public void put(BlockPos pos) {
            if (knownPositions.size() < 100) {
                knownPositions.add(pos);
                double dist = distSq(pos);
                if (dist > furthest) {
                    furthest = dist;
                    return;
                }
            }
            double dist = distSq(pos);
            if (dist < furthest) {
                knownPositions.add(pos);
                knownPositions.remove(furthest());
                recalcFurthest();
            }
        }
        public BlockPos getOne() {
            for (BlockPos pos : knownPositions) {
                return pos;
            }
            return null;
        }
        public BlockPos closest() {
            BlockPos best = null;
            double dist = Double.MAX_VALUE;
            for (BlockPos pos : knownPositions) {
                if (!blockLoaded(pos)) {
                    continue;
                }
                if (!block.equals(Minecraft.getMinecraft().world.getBlockState(pos).getBlock())) {
                    continue;
                }
                double d = distSq(pos);
                if (best == null || d < dist) {
                    dist = d;
                    best = pos;
                }
            }
            return best;
        }
        public void recalcFurthest() {
            double dist = Double.MIN_VALUE;
            for (BlockPos pos : knownPositions) {
                double d = distSq(pos);
                if (d > dist) {
                    dist = d;
                }
            }
            furthest = dist;
        }
        public BlockPos furthest() {
            BlockPos best = null;
            double dist = Double.MIN_VALUE;
            for (BlockPos pos : knownPositions) {
                double d = distSq(pos);
                if (best == null || d > dist) {
                    dist = d;
                    best = pos;
                }
            }
            return best;
        }
    }
    public static double distSq(BlockPos pos) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        double diffX = player.posX - (pos.getX() + 0.5D);
        double diffY = (player.posY + 1.62) - (pos.getY() + 0.5D);
        double diffZ = player.posZ - (pos.getZ() + 0.5D);
        return diffX * diffX + diffY * diffY + diffZ * diffZ;
    }
    @Override
    public void onTick() {
        if (air == null) {
            air = Block.getBlockById(0);
        }
        playersCurrentlyInRange.clear();
        for (EntityPlayer pl : Minecraft.getMinecraft().world.playerEntities) {
            String blah = pl.getName().trim().toLowerCase();
            playerLocationMemory.put(blah, new BlockPos(pl.posX, pl.posY, pl.posZ));
            playersCurrentlyInRange.add(blah);
        }
        if (scanThread == null) {
            scanThread = new Thread() {
                @Override
                public void run() {
                    try {
                        run1();
                    } catch (Exception ex) {
                        Logger.getLogger(Memory.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    scanThread = null;
                }
                public void run1() {
                    Out.gui("Starting passive block scan thread", Out.Mode.Debug);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Memory.class.getName()).log(Level.SEVERE, null, ex);
                        return;
                    }
                    while (true) {
                        if (Minecraft.getMinecraft() != null && Minecraft.getMinecraft().player != null && Minecraft.getMinecraft().world != null) {
                            //Out.gui("Beginning passive block scan", true);
                            long start = System.currentTimeMillis();
                            scan();
                            long end = System.currentTimeMillis();
                            //Out.gui("Passive block scan over after " + (end - start) + "ms", true);
                        }
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Memory.class.getName()).log(Level.SEVERE, null, ex);
                            return;
                        }
                    }
                }
            };
            scanThread.start();
        }
    }
    public static String findCommand(String block) {
        String lower = block.toLowerCase();
        Out.log(lower);
        BlockPos best = null;
        double d = Double.MAX_VALUE;
        for (Block type : blockMemory.keySet()) {
            //Out.log("Considering " + type);
            if (type.toString().toLowerCase().contains(lower)) {
                BlockPos pos = blockMemory.get(type).closest();
                Out.log("find" + type + " " + pos);
                if (pos != null) {
                    double dist = distSq(pos);
                    if (best == null || dist < d) {
                        d = dist;
                        best = pos;
                    }
                }
            }
        }
        if (best != null) {
            return block + " at " + best;
        }
        return "none";
    }
    public static BlockPos closestOne(String... block) {
        /*BlockPos best = null;
         double d = Double.MAX_VALUE;
         for (Block type : blockMemory.keySet()) {
         //Out.log("Considering " + type);
         for (String b : block) {
         String lower = "block{minecraft:" + b.toLowerCase() + "}";
         if (type.toString().toLowerCase().equals(lower)) {
         BlockPos pos = blockMemory.get(type).closest();
         Out.log("closest" + type + " " + pos);
         if (pos != null) {
         double dist = distSq(pos);
         if (best == null || dist < d) {
         d = dist;
         best = pos;
         }
         }
         }
         }
         }
         return best;*/
        ArrayList<BlockPos> u = closest(1, block);
        if (u.isEmpty()) {
            return null;
        }
        return u.get(0);
    }
    public static ArrayList<BlockPos> closest(int num, String... block) {
        ArrayList<BlockPos> result = new ArrayList();
        for (Block type : blockMemory.keySet()) {
            //Out.log("Considering " + type);
            for (String b : block) {
                String lower = "block{minecraft:" + b.toLowerCase() + "}";
                if (type.toString().toLowerCase().equals(lower)) {
                    for (BlockPos pos : blockMemory.get(type).knownPositions) {
                        if (type.equals(Minecraft.getMinecraft().world.getBlockState(pos).getBlock()) && !result.contains(pos)) {
                            if (b.equals("stone")) {
                                if (!type.getItemDropped(Minecraft.getMinecraft().world.getBlockState(pos), new Random(), 0).equals(Item.getByNameOrId("cobblestone"))) {
                                    continue;
                                }
                            }
                            result.add(pos);
                        }
                    }
                }
            }
        }
        result.sort(new Comparator<BlockPos>() {
            @Override
            public int compare(BlockPos o1, BlockPos o2) {
                return Double.compare(distSq(o1), distSq(o2));
            }
        });
        return new ArrayList(result.subList(0, Math.min(num, result.size())));
    }
    public static String findGoCommand(String block) {
        String lower = block.toLowerCase();
        Out.log(lower);
        BlockPos best = null;
        double d = Double.MAX_VALUE;
        for (Block type : blockMemory.keySet()) {
            //Out.log("Considering " + type);
            if (type.toString().toLowerCase().contains(lower)) {
                BlockPos pos = blockMemory.get(type).closest();
                Out.log("findgo" + type + " " + pos);
                if (pos != null) {
                    double dist = distSq(pos);
                    if (best == null || dist < d) {
                        d = dist;
                        best = pos;
                    }
                }
            }
        }
        if (best != null) {
            goal = new GoalTwoBlocks(best);
            Baritone.findPathInNewThread(true);
            return "Pathing to " + goal + " " + block + " at " + best;
        }
        return "none";
    }
    public static void scan() {
        for (Block block : blockMemory.keySet()) {
            blockMemory.get(block).checkForChange();
        }
        for (Block block : blockMemory.keySet()) {
            blockMemory.get(block).recalcFurthest();
        }
        BlockPos playerFeet = Minecraft.getMinecraft().player.getPosition0();
        int X = playerFeet.getX();
        int Y = playerFeet.getY();
        int Z = playerFeet.getZ();
        int ymin = Math.max(0, Y - 10);
        int ymax = Math.min(Y + 10, 256);
        for (int x = X; x >= X - SCAN_DIST && blockLoaded(new BlockPos(x, Y, Z)); x--) {
            for (int z = Z; z >= Z - SCAN_DIST && blockLoaded(new BlockPos(x, Y, z)); z--) {
                for (int y = ymin; y <= ymax; y++) {
                    scanBlock(new BlockPos(x, y, z));
                }
            }
            for (int z = Z; z <= Z + SCAN_DIST && blockLoaded(new BlockPos(x, Y, z)); z++) {
                for (int y = ymin; y <= ymax; y++) {
                    scanBlock(new BlockPos(x, y, z));
                }
            }
        }
        for (int x = X; x <= X + SCAN_DIST && blockLoaded(new BlockPos(x, Y, Z)); x++) {
            for (int z = Z; z >= Z - SCAN_DIST && blockLoaded(new BlockPos(x, Y, z)); z--) {
                for (int y = ymin; y <= ymax; y++) {
                    scanBlock(new BlockPos(x, y, z));
                }
            }
            for (int z = Z; z <= Z + SCAN_DIST && blockLoaded(new BlockPos(x, Y, z)); z++) {
                for (int y = ymin; y <= ymax; y++) {
                    scanBlock(new BlockPos(x, y, z));
                }
            }
        }
    }
    public static final int SCAN_DIST = 50;
    public static void scanBlock(BlockPos pos) {
        Block block = Minecraft.getMinecraft().world.getBlockState(pos).getBlock();
        if (air.equals(block)) {
            return;
        }
        BlockMemory memory = getMemory(block);
        memory.put(pos);
    }
    public static BlockMemory getMemory(Block block) {
        BlockMemory cached = blockMemory.get(block);
        if (cached != null) {
            return cached;
        }
        BlockMemory n = new BlockMemory(block);
        blockMemory.put(block, n);
        return n;
    }
    public static boolean blockLoaded(BlockPos pos) {
        return !(Minecraft.getMinecraft().world.getChunkFromBlockCoords(pos) instanceof EmptyChunk);
    }
    public static String gotoCommand(String targetName) {
        for (String name : playerLocationMemory.keySet()) {
            if (name.contains(targetName) || targetName.contains(name)) {
                Baritone.goal = new GoalBlock(playerLocationMemory.get(name));
                findPathInNewThread(Minecraft.getMinecraft().player.getPosition0(), true);
                return "Pathing to " + name + " at " + goal;
            }
        }
        /*
         for (EntityPlayer pl : Minecraft.getMinecraft().world.playerEntities) {
         String blah = pl.getName().trim().toLowerCase();
         if (blah.contains(name) || name.contains(blah)) {
         BlockPos pos = new BlockPos(pl.posX, pl.posY, pl.posZ);
         goal = new GoalBlock(pos);
         findPathInNewThread(Minecraft.getMinecraft().player.getPosition0(), true);
         return "Pathing to " + pl.getName() + " at " + goal;
         }
         }*/
        return "Couldn't find " + targetName;
    }
    public static String playerCommand(String targetName) {
        String resp = "";
        for (EntityPlayer pl : Minecraft.getMinecraft().world.playerEntities) {
            resp += "(" + pl.getName() + "," + pl.posX + "," + pl.posY + "," + pl.posZ + ")\n";
            if (pl.getName().equals(targetName)) {
                BlockPos pos = new BlockPos(pl.posX, pl.posY, pl.posZ);
                goal = new GoalBlock(pos);
                return "Set goal to " + goal;
            }
        }
        for (String x : resp.split("\n")) {
            Out.gui(x, Out.Mode.Minimal);
        }
        if (targetName.equals("")) {
            return "";
        }
        return "Couldn't find " + targetName;
    }
}
