/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

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
