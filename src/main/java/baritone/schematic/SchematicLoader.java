/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.schematic;

import baritone.util.Out;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

/**
 *
 * @author galdara
 */
public class SchematicLoader {

    public static File schematicDir;
    private static final HashMap<File, Schematic> cachedSchematics = new HashMap<>();

    private SchematicLoader() {
        schematicDir = new File(Minecraft.getMinecraft().mcDataDir, "schematics");
        schematicDir.mkdir();
        for (File file : schematicDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".schematic");
            }
        })) {
            try {
                cachedSchematics.put(file, loadFromFile(file));
            } catch (IOException ex) {
                Logger.getLogger(SchematicLoader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static SchematicLoader getLoader() {
        return new SchematicLoader();
    }

    public final Schematic loadFromFile(File nbtFile) throws FileNotFoundException, IOException {
        if (cachedSchematics.containsKey(nbtFile)) {
            return cachedSchematics.get(nbtFile);
        }
        FileInputStream fileInputStream = new FileInputStream(nbtFile);
        NBTTagCompound compound = CompressedStreamTools.readCompressed(fileInputStream);
        System.out.print(compound);
        int height, width, length;
        height = compound.getInteger("Height");
        width = compound.getInteger("Width");
        length = compound.getInteger("Length");
        byte[][][] blocks = new byte[width][height][length], data = new byte[width][height][length];
        byte[] rawBlocks = compound.getByteArray("Blocks");
        HashMap<BlockPos, Block> blocksMap = new HashMap<>();
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    int index = y * width * length + z * width + x;
                    blocks[x][y][z] = rawBlocks[index];
                    blocksMap.put(new BlockPos(x, y, z), Block.getBlockById(rawBlocks[index]));
                }
            }
        }
        Out.log(blocksMap);
        Schematic schematic = new Schematic(blocksMap, width, height, length);
        cachedSchematics.put(nbtFile, schematic);
        return schematic;
    }
}
