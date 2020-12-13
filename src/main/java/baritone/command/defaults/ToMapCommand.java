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

package baritone.command.defaults;

import baritone.Baritone;
import baritone.api.IBaritone;
import baritone.api.cache.ICachedRegion;
import baritone.api.cache.ICachedWorld;
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.exception.CommandException;
import baritone.cache.CachedWorld;
import baritone.cache.WorldScanner;
import net.minecraft.block.AirBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ToMapCommand extends Command {

    private HashMap<String, Color> cachedColors;
    private HashMap<String, BufferedImage> cachedTextures;
    private static String DEFAULT_BLOCK_ID = "ice";
    private ExecutorService executor;

    public ToMapCommand(IBaritone baritone) {
        super(baritone, "tomap");
        cachedColors = new HashMap<>();
        cachedTextures = new HashMap<>();
        executor = Executors.newFixedThreadPool(5);
    }

    private List<int[]> getRegionCoordsOnDisk() {
        return Arrays.asList(
                new int[]{1, 1},
                new int[]{1, 0},
                new int[]{0, 1},
                new int[]{0, 0},
                new int[]{-1, 0},
                new int[]{0, -1},
                new int[]{-1, -1}
        );
//        System.out.println("Testing region coords on disk");
//        String[] regionCoordsOnDisk = new File(
//                Baritone.getDir(),
//                mc.getCurrentServerData().serverIP + "/DIM0/cache").list();
//        List<int[]> fileNamesForRegions = new ArrayList<>();
//        for (String regionFileName : regionCoordsOnDisk) {
//            Matcher m = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.bcr").matcher(regionFileName);
//            if (m.find()) {
//                int regionX = Integer.parseInt(m.group(1));
//                int regionZ = Integer.parseInt(m.group(2));
//                fileNamesForRegions.add(new int[]{regionX, regionZ});
//            }
//        }
//        return fileNamesForRegions;
    }

    private boolean isEveryChunkedLoaded() {
        ctx.worldData().getCachedWorld().reloadAllFromDisk();
        boolean everyChunkWasLoaded = true;
        for (int[] regionCoords : getRegionCoordsOnDisk()) {
            int rX = regionCoords[0];
            int rZ = regionCoords[1];
            CachedWorld cw = (CachedWorld) ctx.worldData().getCachedWorld(); // is this cast a fair assumption?
            cw.tryLoadFromDisk(rX, rZ);
            ICachedRegion cr = cw.getRegion(rX, rZ);
            System.out.println("Region " + rX + " " + rZ + " was loaded: " + (cr != null));
            everyChunkWasLoaded &= cr != null;
        }
        return everyChunkWasLoaded;
    }


    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        WorldScanner.INSTANCE.repack(ctx);
        ctx.worldData().getCachedWorld().save();
//        ctx.worldData().getCachedWorld().reloadAllFromDisk();
        System.out.println("Loading cached chunks");
//        baritone.getWorldProvider().getCurrentWorld().getCachedWorld().reloadAllFromDisk();
//        if(!isEveryChunkedLoaded()){
//            return;
//        }
//        System.out.println("All cached chunks loaded succesfully");
        for (int[] regionCoords : getRegionCoordsOnDisk()) {
            int rX = regionCoords[0];
            int rZ = regionCoords[1];
            executor.execute(saveToImage(rX, rZ));
//            saveToImage(rX, rZ).start();
        }
        executor.shutdown();
        while(!executor.isTerminated());
    }

    private Runnable saveToImage(int rX, int rZ) {
        return new Runnable() {
            @Override
            public void run() {
                CachedWorld cw = (CachedWorld) ctx.worldData().getCachedWorld(); // is this cast a fair assumption?
                cw.tryLoadFromDisk(rX, rZ);
                ICachedRegion cr = cw.getRegion(rX, rZ);
//            System.out.println("cr is null: " + (cr == null));
//            if (cr == null)
//                continue;
                Map<String, int[]> cornerBlockCoordinatesForZeroZero = getBlockCoordsOfRegionCorners(rX, rZ);
                int[] nw = cornerBlockCoordinatesForZeroZero.get("NW");
                Map<Integer, Map<Integer, String>> topView = new LinkedHashMap<>(); // in block coordinates
                Pattern p = Pattern.compile("Block\\{minecraft:(.*)\\}");
                logDirect("Getting top blocks for region " + rX + " " + rZ);
                for (int x = nw[0]; x < nw[0] + 512; x++) {
                    if (!topView.containsKey(x)) {
                        HashMap<Integer, String> zId = new LinkedHashMap<>();
                        topView.put(x, zId);
                    }
                    for (int z = nw[1]; z < nw[1] + 512; z++) {
                        for (int y = 256; y >= 0; y--) {
                            try {
                                BlockState bs = cr.getBlock(x, y, z);
                                String id;
                                if (bs == null) {
                                    continue;
//                                id = DEFAULT_BLOCK_ID; // does anyone have a better idea?
                                } else {
                                    try {
                                        Matcher m = p.matcher(bs.toString());
                                        if (m.find())
                                            id = m.group(1);
                                        else {
                                            System.out.println("Failed to find block id for " + bs.toString());
                                            return;
                                        }
                                    } catch (Exception e) {
                                        System.out.println("No match found for " + bs.toString());
                                        logDirect("No match found for " + bs.toString());
                                        return;
                                    }
                                }
                                if (bs == null || !(bs.getBlock() instanceof AirBlock)) { // we must show this block
                                    topView.get(x).put(z, id);
                                    break;
                                }
                            } catch (Exception e) {
//                            System.out.println("Couldn't get BlockState for " + x + " " + y + " " + z);
                            }
                        }
                        if (!topView.containsKey(x)) {
                            if (!topView.get(x).containsKey(z)) {
                                topView.get(x).put(z, DEFAULT_BLOCK_ID); // this is definitely not accurate
                            }
                        }
                    }
                }
                logDirect("Finished scanning");
                logDirect("Asserting size");
                if (topView.isEmpty()) {
                    System.out.println("Topview is empty: " + topView.isEmpty() + " for " + rX + " " + rZ);
                    return;
                }
                logDirect("Calculate coordinate offsets");
                int minX = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE;
                int minZ = Integer.MAX_VALUE;
                int maxZ = Integer.MIN_VALUE;
                Integer offsetX = null;
                Integer offsetZ = null;

                minX = Collections.min(topView.keySet());
                maxX = Collections.max(topView.keySet());
                for (Map<Integer, String> zId : topView.values()) {
                    if (!zId.keySet().isEmpty()) {
                        int minZForThisXAxis = Collections.min(zId.keySet());
                        if (minZForThisXAxis < minZ)
                            minZ = minZForThisXAxis;

                        int maxZForThisXAxis = Collections.max(zId.keySet());
                        if (maxZForThisXAxis > maxZ)
                            maxZ = maxZForThisXAxis;
                    }
                }

                offsetX = -minX;
                offsetZ = -minZ;
                BufferedImage result = new BufferedImage(
                        512,
                        512,
                        BufferedImage.TYPE_INT_ARGB);
                logDirect("Starting coloring");
                for (int x = minX; x < maxX + offsetX; x++) {
                    for (int z = minZ; z < maxZ + offsetZ; z++) {
                        String blockId = null;
                        try {
                            blockId = topView.get(x).getOrDefault(z, DEFAULT_BLOCK_ID);
                            if (!blockId.equals(DEFAULT_BLOCK_ID)) {
                                BufferedImage textureAtCoord = getTextureForBlockId(blockId);
                                Color c = averageColor(textureAtCoord, blockId);
                                result.setRGB(x + offsetX, z + offsetZ, c.getRGB());
                            } else {
                                result.setRGB(x + offsetX, z + offsetZ, Color.TRANSLUCENT);
                            }
                        } catch (IOException e) {
                            System.out.println("Error setting color for " + blockId + " at " + x + " " + z);
                            e.printStackTrace();
                        }
                    }
                }
                logDirect("Finished coloring");
                try {
                    File outputFile = Minecraft.getInstance().gameDir.toPath()
                            .resolve("screenshots")
                            .resolve(mc.getCurrentServerData().serverIP + "." + rX + "." + rZ + ".png").toFile();
                    ImageIO.write(result, "png", outputFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                cw.uncacheRegion(rX, rZ);
            }
        };
    }

    public Color averageColor(BufferedImage bi, String blockId) {
        if (!cachedColors.containsKey(blockId)) {
            System.out.println("Getting average color for " + blockId);
            int x1 = 16;
            int z1 = 16;
            long sumr = 0, sumg = 0, sumb = 0;
            for (int x = 0; x < x1; x++) {
                for (int z = 0; z < z1; z++) {
                    Color pixel = new Color(bi.getRGB(x, z));
                    sumr += pixel.getRed();
                    sumg += pixel.getGreen();
                    sumb += pixel.getBlue();
                }
            }
            float num = 16 * 16 * 256;
            try {
                cachedColors.put(blockId, new Color(sumr / num, sumg / num, sumb / num));
            } catch (Exception e) {
                cachedColors.put(blockId, new Color(Color.TRANSLUCENT));
            }
        }
        return cachedColors.get(blockId);
    }

    private BufferedImage getTextureForBlockId(String blockId) throws IOException {
        String base = "/home/nexor/.minecraft/versions/1.15.2-OptiFine_HD_U_G5_pre1/" +
                "1.15.2-OptiFine_HD_U_G5_pre1/assets/minecraft/textures/block/";
        if (cachedTextures.containsKey(blockId))
            return cachedTextures.get(blockId);
        System.out.println("Getting texture for " + blockId);
        Path withTopPath = Paths.get(base + blockId + "_top.png");
        Path withoutTopPath = Paths.get(base + blockId + ".png");
        Path debug = Paths.get(base + "debug.png");
        BufferedImage texture;
        if (Files.exists(withTopPath))
            texture = ImageIO.read(Files.newInputStream(withTopPath));
        else if (Files.exists(withoutTopPath))
            texture = ImageIO.read(Files.newInputStream(withoutTopPath));
        else
            texture = ImageIO.read(Files.newInputStream(debug));
        cachedTextures.put(blockId, texture);
        return texture;
    }

    public static Map<String, int[]> getBlockCoordsOfRegionCorners(int x, int z) {
        return new HashMap<String, int[]>() {
            {
                put("NW", new int[]{
                        x * 512,
                        z * 512,
                });
                put("NE", new int[]{
                        (x + 1) * 512 - 1,
                        z * 512,
                });
                put("SW", new int[]{
                        x * 512,
                        (z + 1) * 512 - 1,
                });
                put("SE", new int[]{
                        (x + 1) * 512 - 1,
                        (z + 1) * 512 - 1,
                });
            }
        };
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Write chunk view of current world to image";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Loads all cached chunks to create an image file",
                "stored under minecraft screenshots folder as <world name>.png"
        );
    }
}
