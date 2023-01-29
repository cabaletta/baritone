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
import baritone.api.utils.Helper;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;

/**
 * @author Brady
 * @since 8/4/2018
 */
public class WorldProvider implements IWorldProvider, Helper {

    private static final Map<Path, WorldData> worldCache = new HashMap<>(); // this is how the bots have the same cached world

    private WorldData currentWorld;
    private Level mcWorld; // this let's us detect a broken load/unload hook

    @Override
    public final WorldData getCurrentWorld() {
        detectAndHandleBrokenLoading();
        return this.currentWorld;
    }

    /**
     * Called when a new world is initialized to discover the
     *
     * @param world The world's Registry Data
     */
    public final void initWorld(ResourceKey<Level> worldKey, DimensionType world) {
        File directory;
        File readme;

        IntegratedServer integratedServer = mc.getSingleplayerServer();

        // If there is an integrated server running (Aka Singleplayer) then do magic to find the world save file
        if (mc.hasSingleplayerServer()) {
            directory = DimensionType.getStorageFolder(worldKey, integratedServer.getWorldPath(LevelResource.ROOT).toFile());

            // Gets the "depth" of this directory relative the the game's run directory, 2 is the location of the world
            if (directory.toPath().relativize(mc.gameDirectory.toPath()).getNameCount() != 2) {
                // subdirectory of the main save directory for this world
                directory = directory.getParentFile();
            }

            directory = new File(directory, "baritone");
            readme = directory;
        } else { // Otherwise, the server must be remote...
            String folderName;
            if (mc.getCurrentServer() != null) {
                folderName = mc.isConnectedToRealms() ? "realms" : mc.getCurrentServer().ip;
            } else {
                //replaymod causes null currentServer and false singleplayer.
                System.out.println("World seems to be a replay. Not loading Baritone cache.");
                currentWorld = null;
                mcWorld = mc.level;
                return;
            }
            if (SystemUtils.IS_OS_WINDOWS) {
                folderName = folderName.replace(":", "_");
            }
            directory = new File(Baritone.getDir(), folderName);
            readme = Baritone.getDir();
        }

        // lol wtf is this baritone folder in my minecraft save?
        try (FileOutputStream out = new FileOutputStream(new File(readme, "readme.txt"))) {
            // good thing we have a readme
            out.write("https://github.com/cabaletta/baritone\n".getBytes());
        } catch (IOException ignored) {}

        // We will actually store the world data in a subfolder: "DIM<id>"
        Path dir = getDimDir(worldKey, world.logicalHeight(), directory);
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException ignored) {}
        }

        System.out.println("Baritone world data dir: " + dir);
        synchronized (worldCache) {
            this.currentWorld = worldCache.computeIfAbsent(dir, d -> new WorldData(d, world));
        }
        this.mcWorld = mc.level;
    }

    public final Path getDimDir(ResourceKey<Level> level, int height, File directory) {
        return directory.toPath().resolve(level.location().getNamespace()).resolve(level.location().getPath() + "_" + height);
    }

    public final void closeWorld() {
        WorldData world = this.currentWorld;
        this.currentWorld = null;
        this.mcWorld = null;
        if (world == null) {
            return;
        }
        world.onClose();
    }

    public final void ifWorldLoaded(Consumer<WorldData> currentWorldConsumer) {
        detectAndHandleBrokenLoading();
        if (this.currentWorld != null) {
            currentWorldConsumer.accept(this.currentWorld);
        }
    }

    private final void detectAndHandleBrokenLoading() {
        if (this.mcWorld != mc.level) {
            if (this.currentWorld != null) {
                System.out.println("mc.world unloaded unnoticed! Unloading Baritone cache now.");
                closeWorld();
            }
            if (mc.level != null) {
                System.out.println("mc.world loaded unnoticed! Loading Baritone cache now.");
                initWorld(mc.level.dimension(), mc.level.dimensionType());
            }
        } else if (currentWorld == null && mc.level != null && (mc.hasSingleplayerServer() || mc.getCurrentServer() != null)) {
            System.out.println("Retrying to load Baritone cache");
            initWorld(mc.level.dimension(), mc.level.dimensionType());
        }
    }
}
