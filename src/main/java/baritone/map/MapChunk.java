package baritone.map;

import baritone.Baritone;
import baritone.utils.BlockStateInterface;
import baritone.utils.pathing.BetterBlockPos;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MapChunk {

    private Chunk chunk;

    public MapChunk(Chunk chunk) {
        this.chunk = chunk;
    }

    public BufferedImage generateOverview() {
        BufferedImage bufferedImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                bufferedImage.setRGB(x, z, getColor(x, z));
            }
        }
        return bufferedImage;
    }

    public void writeImage() {
        Path image = getImagePath();
        if(!Files.exists(image.getParent())) {
            try {
                Files.createDirectory(image.getParent());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        BufferedImage bufferedImage = generateOverview();
        try {
            ImageIO.write(bufferedImage, "PNG", image.toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public Path getImagePath() {
        return new File(new File(Baritone.INSTANCE.getDir(), "map"), "chunk" + chunk.x + "-" + chunk.z + ".png").toPath();
    }

    public Chunk getChunk() {
        return chunk;
    }

    /**
     * getColor is a re-implementation of the Minecraft ItemMap's
     * surface mapping system. This is significantly less convoluted
     * than the poorly de-obfuscated code.
     *
     * We perform shading of the given coordinate relative to the
     * position one to the north of the coordinate, as can be seen
     * in Minecraft's maps.
     *
     * @param x relative x coordinate to the start of the chunk
     * @param z relative z coord.
     * @return Integer RGB value with contextual shading
     */
    private int getColor(int x, int z) {
        int chunkX = chunk.getPos().getXStart() + x;
        int chunkZ = chunk.getPos().getZStart() + z;
        BlockPos blockPos = new BetterBlockPos(chunkX, chunk.getHeight(new BetterBlockPos(chunkX, 0, chunkZ)), chunkZ);
        IBlockState blockState = BlockStateInterface.get(blockPos);
        MapColor mapColor = blockState.getMapColor(chunk.getWorld(), blockPos);

        // The chunk heightMap returns the first non-full block above the surface, including bushes.
        // We have to check this and shift our block down by one if the color is "air" (as in, there's no real block there)
        if(mapColor == MapColor.AIR) {
            blockPos = blockPos.down();
            blockState = BlockStateInterface.get(blockPos);
            mapColor = blockState.getMapColor(chunk.getWorld(), blockPos);
        }

        Color color = new Color(mapColor.colorValue);
        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();

        // Now we get the proper block for the position one to the north.
        BlockPos offset = blockPos.offset(EnumFacing.NORTH);
        // If we are at the north border of the chunk, we need to get the next chunk
        // to the north to ensure that we shade properly.
        offset = chunk.getWorld().getChunk(offset).isLoaded() ? offset : offset.south();
        // We adjust the height of the offset to the proper height value if the shading chunk is
        // loaded, or the same as our target block if the shading chunk is not.
        offset = new BlockPos(offset.getX(), chunk.getWorld().getChunk(offset).getHeight(offset), offset.getZ());
        // And once again, check to make sure we have an actual colored block an not "air"
        if(BlockStateInterface.get(offset).getMapColor(chunk.getWorld(), offset) == MapColor.AIR)
            offset = offset.down();

        float heightCoef;
        // Check if we need to handle water depth shading
        if(blockState.getMaterial().isLiquid()) {
            heightCoef = 1.0f;
            int i = 1;
            // Iterate over the 4 layers of water shading and color proportional to the depth
            for(; i < 5; i++) {
                if(BlockStateInterface.get(blockPos.down(i * 2)).getMaterial().isLiquid()) {
                    heightCoef -= 0.075f;
                } else {
                    break;
                }
            }
            // Add checkerboard shading to the odd layers to give texture
            if(i % 2 == 0)
                heightCoef += 0.075f * (((x + z) % 2 == 0) ? -1 : 1);
        } else {
            // We start the heightCoef at 0.8 to darken the colors similar to vanilla Minecraft's.
            heightCoef = 0.8f + (Math.signum(blockPos.getY() - offset.getY()) * 0.2f);
        }
        red = Math.min(255, (int) (red * heightCoef));
        green = Math.min(255, (int) (green * heightCoef));
        blue = Math.min(255, (int) (blue * heightCoef));

        return new Color(red, green, blue).getRGB();
    }

}
