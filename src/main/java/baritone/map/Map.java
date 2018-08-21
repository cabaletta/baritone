package baritone.map;

import baritone.bot.Baritone;
import baritone.bot.behavior.Behavior;
import baritone.bot.event.events.ChunkEvent;
import net.minecraft.util.math.ChunkPos;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Map extends Behavior {

    public static final Map INSTANCE = new Map();

    private Map() {}

    private BufferedImage fullImage = new BufferedImage(4080, 4080, BufferedImage.TYPE_INT_RGB);

    @Override
    public void onChunkEvent(ChunkEvent event) {
        if(event.getType() != ChunkEvent.Type.POPULATE)
            return;

        MapChunk map = new MapChunk(world().getChunk(event.getX(), event.getZ()));
        ChunkPos pos = map.getChunk().getPos();
        int startX;
        int startZ;
        if(pos.x == 0 && pos.z == 0) {
            startX = (fullImage.getWidth() / 2) - 9;
            startZ = (fullImage.getHeight() / 2) - 9;
        } else {
            int widthOffset = (((fullImage.getWidth() / 2) - 1) + (int) Math.signum(pos.x) * -8);
            int heightOffset = (((fullImage.getHeight() / 2) - 1) + (int) Math.signum(pos.z) * -8);
            startX = widthOffset + (16 * (pos.x + (pos.x > 0 ? -1 : 0)));
            startZ = heightOffset + (16 * (pos.z + (pos.z > 0 ? -1 : 0)));
        }

        Graphics graphics = fullImage.getGraphics();
        graphics.drawImage(map.generateOverview(), startX, startZ, null);
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
        try {
            ImageIO.write(fullImage, "PNG", image.toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public Path getImagePath() {
        return new File(new File(Baritone.INSTANCE.getDir(), "map"), "full-map.png").toPath();
    }

}
