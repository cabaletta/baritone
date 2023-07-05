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
import net.minecraft.world.DimensionType;
import baritone.api.utils.IPlayerContext;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.Tuple;
import net.minecraft.world.World;
import net.minecraft.world.storage.FolderName;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Brady
 * @since 8/4/2018
 */
public class WorldProvider implements IWorldProvider {

    private static final Map<Path, WorldData> worldCache = new HashMap<>();

    private final Baritone baritone;
    private final IPlayerContext ctx;
    private WorldData currentWorld;

    /**
     * This lets us detect a broken load/unload hook.
     * @see #detectAndHandleBrokenLoading()
     */
    private World mcWorld;

    public WorldProvider(Baritone baritone) {
        this.baritone = baritone;
        this.ctx = baritone.getPlayerContext();
    }

    @Override
    public final WorldData getCurrentWorld() {
        this.detectAndHandleBrokenLoading();
        return this.currentWorld;
    }

    /**
     * Called when a new world is initialized to discover the
     *
     * @param world The new world
     */
    public final void initWorld(World world) {
        this.getSaveDirectories(world).ifPresent(dirs -> {
            final Path worldDir = dirs.getA();
            final Path readmeDir = dirs.getB();

            try {
                // lol wtf is this baritone folder in my minecraft save?
                // good thing we have a readme
                Files.createDirectories(readmeDir);
                Files.write(
                        readmeDir.resolve("readme.txt"),
                        "https://github.com/cabaletta/baritone\n".getBytes(StandardCharsets.US_ASCII)
                );
            } catch (IOException ignored) {}

            // We will actually store the world data in a subfolder: "DIM<id>"
            final Path worldDataDir = this.getWorldDataDirectory(worldDir, world);
            try {
                Files.createDirectories(worldDataDir);
            } catch (IOException ignored) {}

            System.out.println("Baritone world data dir: " + worldDataDir);
            synchronized (worldCache) {
                this.currentWorld = worldCache.computeIfAbsent(worldDataDir, d -> new WorldData(d, world.getDimensionKey()));
            }
            this.mcWorld = ctx.world();
        });
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

    private Path getWorldDataDirectory(Path parent, World world) {
        return DimensionType.getDimensionFolder(world.getDimensionKey(), parent.toFile()).toPath();
    }

    /**
     * @param world The world
     * @return An {@link Optional} containing the world's baritone dir and readme dir, or {@link Optional#empty()} if
     *         the world isn't valid for caching.
     */
    private Optional<Tuple<Path, Path>> getSaveDirectories(World world) {
        Path worldDir;
        Path readmeDir;

        // If there is an integrated server running (Aka Singleplayer) then do magic to find the world save file
        if (ctx.minecraft().isSingleplayer()) {
            worldDir = ctx.minecraft().getIntegratedServer().func_240776_a_(FolderName.DOT);

            // Gets the "depth" of this directory relative to the game's run directory, 2 is the location of the world
            if (worldDir.relativize(ctx.minecraft().gameDir.toPath()).getNameCount() != 2) {
                // subdirectory of the main save directory for this world
                worldDir = worldDir.getParent();
            }

            worldDir = worldDir.resolve("baritone");
            readmeDir = worldDir;
        } else { // Otherwise, the server must be remote...
            String folderName;
            final ServerData serverData = ctx.minecraft().getCurrentServerData();
            if (serverData != null) {
                folderName = ctx.minecraft().isConnectedToRealms() ? "realms" : serverData.serverIP;
            } else {
                //replaymod causes null currentServerData and false singleplayer.
                System.out.println("World seems to be a replay. Not loading Baritone cache.");
                currentWorld = null;
                mcWorld = ctx.world();
                return Optional.empty();
            }
            if (SystemUtils.IS_OS_WINDOWS) {
                folderName = folderName.replace(":", "_");
            }
            // TODO: This should probably be in "baritone/servers"
            worldDir = baritone.getDirectory().resolve(folderName);
            // Just write the readme to the baritone directory instead of each server save in it
            readmeDir = baritone.getDirectory();
        }

        return Optional.of(new Tuple<>(worldDir, readmeDir));
    }

    /**
     * Why does this exist instead of fixing the event? Some mods break the event. Lol.
     */
    private void detectAndHandleBrokenLoading() {
        if (this.mcWorld != ctx.world()) {
            if (this.currentWorld != null) {
                System.out.println("mc.world unloaded unnoticed! Unloading Baritone cache now.");
                closeWorld();
            }
            if (ctx.world() != null) {
                System.out.println("mc.world loaded unnoticed! Loading Baritone cache now.");
                initWorld(ctx.world());
            }
        } else if (this.currentWorld == null && ctx.world() != null && (ctx.minecraft().isSingleplayer() || ctx.minecraft().getCurrentServerData() != null)) {
            System.out.println("Retrying to load Baritone cache");
            initWorld(ctx.world());
        }
    }
}
