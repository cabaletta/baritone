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

package baritone.cache;

import baritone.Baritone;
import baritone.api.cache.IWorldProvider;
import baritone.utils.Helper;
import baritone.utils.accessor.IAnvilChunkLoader;
import baritone.utils.accessor.IChunkProviderServer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.WorldServer;

import java.io.File;
import java.io.FileOutputStream;
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
public enum WorldProvider implements IWorldProvider, Helper {

    INSTANCE;

    private final Map<Path, WorldData> worldCache = new HashMap<>();

    private WorldData currentWorld;

    @Override
    public final WorldData getCurrentWorld() {
        return this.currentWorld;
    }

    public final void initWorld(WorldClient world) {
        int dimensionID = world.provider.getDimensionType().getId();
        File directory;
        File readme;
        IntegratedServer integratedServer = mc.getIntegratedServer();
        if (integratedServer != null) {
            WorldServer localServerWorld = integratedServer.getWorld(dimensionID);
            IChunkProviderServer provider = (IChunkProviderServer) localServerWorld.getChunkProvider();
            IAnvilChunkLoader loader = (IAnvilChunkLoader) provider.getChunkLoader();
            directory = loader.getChunkSaveLocation();

            // Gets the "depth" of this directory relative the the game's run directory, 2 is the location of the world
            if (directory.toPath().relativize(mc.gameDir.toPath()).getNameCount() != 2) {
                // subdirectory of the main save directory for this world
                directory = directory.getParentFile();
            }

            directory = new File(directory, "baritone");
            readme = directory;

        } else {
            //remote
            directory = new File(Baritone.getDir(), mc.getCurrentServerData().serverIP);
            readme = Baritone.getDir();
        }
        // lol wtf is this baritone folder in my minecraft save?
        try (FileOutputStream out = new FileOutputStream(new File(readme, "readme.txt"))) {
            // good thing we have a readme
            out.write("https://github.com/cabaletta/baritone\n".getBytes());
        } catch (IOException ignored) {}

        directory = new File(directory, "DIM" + dimensionID);
        Path dir = directory.toPath();
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException ignored) {}
        }
        System.out.println("Baritone world data dir: " + dir);
        this.currentWorld = this.worldCache.computeIfAbsent(dir, WorldData::new);
    }

    public final void closeWorld() {
        WorldData world = this.currentWorld;
        this.currentWorld = null;
        if (world == null) {
            return;
        }
        world.onClose();
    }

    public final void ifWorldLoaded(Consumer<WorldData> currentWorldConsumer) {
        if (this.currentWorld != null) {
            currentWorldConsumer.accept(this.currentWorld);
        }
    }
}
