/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package baritone.schematic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Tuple;

/**
 *
 * @author galdara
 */
public class Schematic {
    private final HashMap<BlockPos, Block> schematicBlocks;
    private final int width;
    private final int height;
    private final int length;
    public Schematic(HashMap<BlockPos, Block> blocks, int width, int height, int length) {
        schematicBlocks = blocks;
        this.width = width;
        this.height = height;
        this.length = length;
    }
    public Schematic(Block type, int desiredWidth) {
        if (type == null) {
            throw new IllegalArgumentException();
        }
        width = desiredWidth;
        height = desiredWidth;
        length = 1;
        schematicBlocks = new HashMap();
        for (int i = 0; i < desiredWidth; i++) {
            schematicBlocks.put(new BlockPos(i, 0, 0), type);
            schematicBlocks.put(new BlockPos(i, desiredWidth - 1, 0), type);
            schematicBlocks.put(new BlockPos(desiredWidth / 2, i, 0), type);
        }
    }
    public Schematic(Block type, int desiredWidth, boolean rightFacing, boolean dots) {
        if (type == null) {
            throw new IllegalArgumentException();
        }
        int size = (desiredWidth - 1) / 2;
        width = size * 2 + 1;
        height = 1;
        length = width;
        schematicBlocks = new HashMap();
        for (int x = 0; x < width; x++) {
            schematicBlocks.put(new BlockPos(x, 0, size), type);
            schematicBlocks.put(new BlockPos(size, 0, x), type);
        }
        for (int x = 0; x < size; x++) {
            schematicBlocks.put(new BlockPos(rightFacing ? x + size + 1 : x, 0, 0), type);
            schematicBlocks.put(new BlockPos(0, 0, rightFacing ? x : x + size + 1), type);
            schematicBlocks.put(new BlockPos(width - 1, 0, rightFacing ? x + size + 1 : x), type);
            schematicBlocks.put(new BlockPos(rightFacing ? x : x + size + 1, 0, width - 1), type);
        }
        if (dots) {
            schematicBlocks.put(new BlockPos(size / 2, 0, size / 2), type);
            schematicBlocks.put(new BlockPos(size / 2 + size + 1, 0, size / 2), type);
            schematicBlocks.put(new BlockPos(size / 2 + size + 1, 0, size / 2 + size + 1), type);
            schematicBlocks.put(new BlockPos(size / 2, 0, size / 2 + size + 1), type);
        }
    }
    /**
     * Tuple links the BlockPos and Block to one another.
     *
     * @param blockPos
     * @return Tuple of BlockPos and Block
     */
    public Tuple<BlockPos, Block> getTupleFromBlockPos(BlockPos blockPos) {
        if (schematicBlocks.containsKey(blockPos)) {
            return new Tuple<BlockPos, Block>(blockPos, schematicBlocks.get(blockPos));
        }
        return null;
    }
    /**
     * Gets given block type in schematic from a BlockPos
     *
     * @param blockPos
     * @return
     */
    public Block getBlockFromBlockPos(BlockPos blockPos) {
        return schematicBlocks.get(blockPos);
    }
    /**
     * Gives the length along the X axis
     *
     * @return Schematic width
     */
    public int getWidth() {
        return width;
    }
    /**
     * Gives the height along the y axis
     *
     * @return Schematic height
     */
    public int getHeight() {
        return height;
    }
    /**
     * Gives the length along the z axis
     *
     * @return Schematic length
     */
    public int getLength() {
        return length;
    }
    public ArrayList<Entry<BlockPos, Block>> getEntries() {
        return new ArrayList(schematicBlocks.entrySet());
    }
}
