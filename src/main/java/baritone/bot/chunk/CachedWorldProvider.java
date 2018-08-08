package baritone.bot.chunk;

import baritone.bot.utils.Helper;
import baritone.launch.mixins.accessor.IAnvilChunkLoader;
import baritone.launch.mixins.accessor.IChunkProviderServer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.WorldServer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Brady
 * @since 8/4/2018 11:06 AM
 */
public enum CachedWorldProvider implements Helper {

    INSTANCE;

    private static final Pattern REGION_REGEX = Pattern.compile("r\\.(-?[0-9]+)\\.(-?[0-9]+)\\.bcr");

    private final Map<String, CachedWorld> singlePlayerWorldCache = new HashMap<>();

    private CachedWorld currentWorld;

    public final CachedWorld getCurrentWorld() {
        return this.currentWorld;
    }

    public final void initWorld(WorldClient world) {
        IntegratedServer integratedServer;
        if ((integratedServer = mc.getIntegratedServer()) != null) {

            WorldServer localServerWorld = integratedServer.getWorld(world.provider.getDimensionType().getId());
            IChunkProviderServer provider = (IChunkProviderServer) localServerWorld.getChunkProvider();
            IAnvilChunkLoader loader = (IAnvilChunkLoader) provider.getChunkLoader();

            Path dir = new File(new File(loader.getChunkSaveLocation(), "region"), "cache").toPath();
            if (!Files.exists(dir)) {
                try {
                    Files.createDirectories(dir);
                } catch (IOException ignored) {}
            }

            this.currentWorld = this.singlePlayerWorldCache.computeIfAbsent(dir.toString(), CachedWorld::new);

            try {
                Files.list(dir).forEach(path -> {
                    String file = path.getFileName().toString();
                    Matcher matcher = REGION_REGEX.matcher(file);
                    if (matcher.matches()) {
                        int rx = Integer.parseInt(matcher.group(1));
                        int ry = Integer.parseInt(matcher.group(2));
                        // Recognize the region for when we load from file
                        this.currentWorld.getOrCreateRegion(rx, ry);
                    }
                });
            } catch (Exception ignored) {}

            this.currentWorld.load();
        }
        // TODO: Store server worlds
    }

    public final void closeWorld() {
        this.currentWorld = null;
    }

    public final void ifWorldLoaded(Consumer<CachedWorld> currentWorldConsumer) {
        if (this.currentWorld != null)
            currentWorldConsumer.accept(this.currentWorld);
    }
}
