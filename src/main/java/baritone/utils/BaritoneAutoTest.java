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

package baritone.utils;

import baritone.api.BaritoneAPI;
import baritone.api.event.events.TickEvent;
import baritone.api.event.listener.AbstractGameEventListener;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.utils.Helper;
import baritone.api.utils.IPlayerContext;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.settings.AmbientOcclusionStatus;
import net.minecraft.client.settings.CloudOption;
import net.minecraft.client.settings.GraphicsFanciness;
import net.minecraft.client.settings.ParticleStatus;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.server.IDynamicRegistries;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.HTTPUtil;
import net.minecraft.util.datafix.codec.DatapackCodec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.gen.settings.DimensionGeneratorSettings;
import net.minecraft.world.server.ServerWorld;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.OptionalLong;

/**
 * Responsible for automatically testing Baritone's pathing algorithm by automatically creating a world with a specific
 * seed, setting a specified goal, and only allowing a certain amount of ticks to pass before the pathing test is
 * considered a failure. In order to test locally, docker may be used, or through an IDE: Create a run config which runs
 * in a separate directory from the primary one (./run), and set the enrivonmental variable {@code BARITONE_AUTO_TEST}
 * to {@code true}.
 *
 * @author leijurv, Brady
 */
public class BaritoneAutoTest implements AbstractGameEventListener, Helper {

    public static final BaritoneAutoTest INSTANCE = new BaritoneAutoTest();

    public static final boolean ENABLE_AUTO_TEST = "true".equals(System.getenv("BARITONE_AUTO_TEST"));
    private static final long TEST_SEED = -928872506371745L;
    private static final BlockPos STARTING_POSITION = new BlockPos(0, 65, 0);
    private static final Goal GOAL = new GoalBlock(69, 69, 420);
    private static final int MAX_TICKS = 3300;

    /**
     * Called right after the {@link GameSettings} object is created in the {@link Minecraft} instance.
     */
    public void onPreInit() {
        if (!BaritoneAutoTest.ENABLE_AUTO_TEST) {
            return;
        }
        System.out.println("Optimizing Game Settings");

        GameSettings s = mc.gameSettings;
        s.framerateLimit = 20;
        s.mipmapLevels = 0;
        s.particles = ParticleStatus.MINIMAL;
        s.overrideWidth = 128;
        s.overrideHeight = 128;
        s.heldItemTooltips = false;
        s.entityShadows = false;
        s.chatScale = 0.0F;
        s.ambientOcclusionStatus = AmbientOcclusionStatus.OFF;
        s.cloudOption = CloudOption.OFF;
        s.field_238330_f_ = GraphicsFanciness.FAST;
        s.tutorialStep = TutorialSteps.NONE;
        s.hideGUI = true;
        s.fov = 30.0F;
    }

    @Override
    public void onTick(TickEvent event) {
        IPlayerContext ctx = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext();
        // If we're on the main menu then create the test world and launch the integrated server
        if (mc.currentScreen instanceof MainMenuScreen) {
            System.out.println("Beginning Baritone automatic test routine");
            mc.displayGuiScreen(null);
            WorldSettings worldsettings = new WorldSettings("BaritoneAutoTest", GameType.SURVIVAL, false, Difficulty.NORMAL, true, new GameRules(), DatapackCodec.field_234880_a_);
            mc.func_238192_a_("BaritoneAutoTest", worldsettings, IDynamicRegistries.func_239770_b_(), DimensionGeneratorSettings.field_236202_b_.create(false, OptionalLong.of(TEST_SEED)));
        }

        IntegratedServer server = mc.getIntegratedServer();

        // If the integrated server is launched and the world has initialized, set the spawn point
        // to our defined starting position
        if (server != null && server.getWorld(World.field_234918_g_) != null) {
            server.setDifficultyForAllWorlds(Difficulty.PEACEFUL, true);
            if (mc.player == null) {
                server.execute(() -> {
                    server.getWorld(World.field_234918_g_).func_241124_a__(STARTING_POSITION);
                    server.getCommandManager().handleCommand(server.getCommandSource(), "/difficulty peaceful");
                    int result = server.getCommandManager().handleCommand(server.getCommandSource(), "/gamerule spawnRadius 0");
                    if (result != 0) {
                        throw new IllegalStateException(result + "");
                    }
                });
                for (final ServerWorld world : mc.getIntegratedServer().getWorlds()) {
                    // If the world has initialized, set the spawn point to our defined starting position
                    if (world != null) {
                        world.getGameRules().get(GameRules.SPAWN_RADIUS).func_234909_b_("0");
                        world.func_241124_a__(STARTING_POSITION);
                    }
                }
            }
        }

        if (event.getType() == TickEvent.Type.IN) { // If we're in-game

            // Force the integrated server to share the world to LAN so that
            // the ingame pause menu gui doesn't actually pause our game
            if (mc.isSingleplayer() && !mc.getIntegratedServer().getPublic()) {
                mc.getIntegratedServer().shareToLAN(GameType.SURVIVAL, false, HTTPUtil.getSuitableLanPort());
            }

            // For the first 200 ticks, wait for the world to generate
            if (event.getCount() < 200) {
                System.out.println("Waiting for world to generate... " + event.getCount());
                return;
            }

            // Print out an update of our position every 5 seconds
            if (event.getCount() % 100 == 0) {
                System.out.println(ctx.playerFeet() + " " + event.getCount());
            }

            // Setup Baritone's pathing goal and (if needed) begin pathing
            if (!BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().isActive()) {
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(GOAL);
            }

            // If we have reached our goal, print a message and safely close the game
            if (GOAL.isInGoal(ctx.playerFeet())) {
                System.out.println("Successfully pathed to " + ctx.playerFeet() + " in " + event.getCount() + " ticks");
                try {
                    File file = new File("success");
                    System.out.println("Writing success to " + file.getAbsolutePath());
                    Files.write(file.getAbsoluteFile().toPath(), "Success!".getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mc.shutdown();
                mc.shutdownMinecraftApplet();
                System.exit(0);
            }

            // If we have exceeded the expected number of ticks to complete the pathing
            // task, then throw an IllegalStateException to cause the build to fail
            if (event.getCount() > MAX_TICKS) {
                throw new IllegalStateException("took too long");
            }
        }
    }

    private BaritoneAutoTest() {
    }
}
