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

/**
 * @author Brady
 * @since 8/4/2018 11:06 AM
 */
public enum CachedWorldProvider implements Helper {

    INSTANCE;

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
