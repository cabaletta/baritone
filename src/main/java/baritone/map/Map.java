package baritone.map;

import baritone.behavior.Behavior;
import baritone.chunk.WorldProvider;
import baritone.event.events.ChunkEvent;
import net.minecraft.util.math.ChunkPos;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Map extends Behavior {

    public static final Map INSTANCE = new Map();

    private BufferedImage fullImage = new BufferedImage(4080, 4080, BufferedImage.TYPE_INT_RGB);

    private Map() {
        try {
            fullImage = ImageIO.read(getImagePath().toFile());
        } catch (IOException ignored) { }
    }

    @Override
    public void onChunkEvent(ChunkEvent event) {
        if (event.getType() != ChunkEvent.Type.POPULATE)
            return;

        MapChunk map = new MapChunk(world().getChunk(event.getX(), event.getZ()));
        stitchMapChunk(map);
        if (world().getChunkProvider().isChunkGeneratedAt(event.getX(), event.getZ() - 1)) {
            stitchMapChunk(new MapChunk(world().getChunk(event.getX(), event.getZ() - 1)));
        }
    }

    private void stitchMapChunk(MapChunk map) {
        ChunkPos pos = map.getChunk().getPos();
        int startX = pos.x * 16 + (fullImage.getWidth() / 2) - 8;
        int startZ = pos.z * 16 + (fullImage.getHeight() / 2) - 8;
        Graphics graphics = fullImage.getGraphics();
        graphics.drawImage(map.generateOverview(), startX, startZ, null);
    }

    public void writeImage() {
        Path image = getImagePath();
        if (!Files.exists(image.getParent())) {
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
        return WorldProvider.INSTANCE.getCurrentWorld().getMapDirectory().resolve("full-map.png");
    }

}
