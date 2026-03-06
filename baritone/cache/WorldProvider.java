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
import baritone.api.utils.IPlayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraft.client.multiplayer.ServerData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

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
        Path worldDir;
        Path readmeDir;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.isSingleplayer()) {
            String worldName = mc.getIntegratedServer().getFolderName();
            Path savesDir = Paths.get(mc.mcDataDir.getAbsolutePath(), "saves", worldName);
            worldDir = savesDir.resolve("baritone");
            readmeDir = worldDir;
        } else {
            ServerData serverData = mc.getCurrentServerData();
            if (serverData == null) {
                System.out.println("No server data, not loading cache.");
                return;
            }
            String folderName = serverData.serverIP;
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("win")) {
                folderName = folderName.replace(":", "_");
            }
            worldDir = baritone.getDirectory().resolve(folderName);
            readmeDir = baritone.getDirectory();
        }

        try {
            Files.createDirectories(readmeDir);
            Files.write(
                    readmeDir.resolve("readme.txt"),
                    "https://github.com/cabaletta/baritone\n".getBytes(StandardCharsets.US_ASCII)
            );
        } catch (IOException ignored) {}

        // We will actually store the world data in a subfolder: "DIM<id>"
        final Path worldDataDir = worldDir.resolve("DIM" + world.provider.dimensionId);
        try {
            Files.createDirectories(worldDataDir);
        } catch (IOException ignored) {}

        System.out.println("Baritone world data dir: " + worldDataDir);
        synchronized (worldCache) {
            this.currentWorld = worldCache.computeIfAbsent(worldDataDir, d -> new WorldData(d, world.provider.dimensionId));
        }
        this.mcWorld = ctx.world();
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
        } else if (this.currentWorld == null && ctx.world() != null && (Minecraft.getMinecraft().isSingleplayer() || Minecraft.getMinecraft().getCurrentServerData() != null)) {
            System.out.println("Retrying to load Baritone cache");
            initWorld(ctx.world());
        }
    }
}