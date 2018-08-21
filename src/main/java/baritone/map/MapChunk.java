package baritone.map;

import baritone.bot.Baritone;
import baritone.bot.utils.BlockStateInterface;
import baritone.bot.utils.pathing.BetterBlockPos;
import net.minecraft.block.material.MapColor;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.chunk.Chunk;
import org.lwjgl.util.Color;

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

    public void writeImage() {
        Path image = getImagePath();
        if(!Files.exists(image.getParent())) {
            try {
                Files.createDirectory(image.getParent());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        BufferedImage bufferedImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                bufferedImage.setRGB(x, z, getColor(x, z));
            }
        }
        try {
            ImageIO.write(bufferedImage, "PNG", image.toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public Path getImagePath() {
        return new File(new File(Baritone.INSTANCE.getDir(), "map"), "chunk" + chunk.x + "-" + chunk.z + ".png").toPath();
    }

    public int getColor(int x, int z) {
        int chunkX = chunk.getPos().getXStart() + x;
        int chunkZ = chunk.getPos().getZStart() + z;
        MutableBlockPos blockPos = new MutableBlockPos(chunkX, chunk.getHeight(new BetterBlockPos(chunkX, 0, chunkZ)), chunkZ);
        MapColor color = BlockStateInterface.get(blockPos).getMapColor(chunk.getWorld(), blockPos);
        int colorValue = color != MapColor.AIR ? color.colorValue : BlockStateInterface.get(blockPos.move(EnumFacing.DOWN)).getMapColor(chunk.getWorld(), blockPos).colorValue;
        int red = (colorValue >> 16) & 0xFF;
        int green = (colorValue >> 8) & 0xFF;
        int blue = colorValue & 0xFF;

        float heightCoef = (((float) blockPos.getY()) / 255) + 0.5f;
        colorValue = (int) (red * heightCoef);
        colorValue = (colorValue << 8) + (int) (green * heightCoef);
        colorValue = (colorValue << 8) + (int) (blue * heightCoef);
        return colorValue;
    }

}
