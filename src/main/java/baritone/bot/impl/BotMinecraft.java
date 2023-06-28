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

package baritone.bot.impl;

import baritone.api.bot.IBaritoneUser;
import baritone.utils.ObjectAllocator;
import baritone.utils.accessor.IGameSettings;
import baritone.utils.accessor.IMinecraft;
import com.google.common.util.concurrent.ListenableFuture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.toasts.GuiToast;
import net.minecraft.client.main.GameConfiguration;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.Session;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.concurrent.Callable;

/**
 * "Implementation" of {@link Minecraft} which gets allocated without receiving a constructor call.
 * This allows us to avoid the game's setup process (moreso in versions after 1.12 than 1.12 itself).
 *
 * @author Brady
 * @since 3/3/2020
 */
public final class BotMinecraft extends Minecraft {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private IBaritoneUser user;
    private Tutorial tutorial;
    private GuiToast toastGui;

    private BotMinecraft(GameConfiguration gameConfig) {
        super(gameConfig);
    }

    @Nullable
    @Override
    public Entity getRenderViewEntity() {
        return mc.getRenderViewEntity();
    }

    @Nonnull
    @Override
    public Session getSession() {
        return this.user.getSession();
    }

    @Override
    public @Nonnull MinecraftSessionService getSessionService() {
        return mc.getSessionService();
    }

    @Override
    public <V> @Nonnull ListenableFuture<V> addScheduledTask(@Nonnull Callable<V> callableToSchedule) {
        return mc.addScheduledTask(callableToSchedule);
    }

    @Override
    public boolean isCallingFromMinecraftThread() {
        return mc.isCallingFromMinecraftThread();
    }

    @Nonnull
    @Override
    public Tutorial getTutorial() {
        return this.tutorial;
    }

    @Nonnull
    @Override
    public SoundHandler getSoundHandler() {
        return BotSoundHandler.INSTANCE;
    }

    @Nonnull
    @Override
    public GuiToast getToastGui() {
        return this.toastGui;
    }

    @Override
    public void displayGuiScreen(@Nullable GuiScreen guiScreenIn) {
        // do nothing
        if (guiScreenIn == null) {
            if (mc.currentScreen instanceof BotGuiInventory) {
                mc.displayGuiScreen(null);
            }
        }
    }

    @Nonnull
    @Override
    public TextureManager getTextureManager() {
        return mc.getTextureManager();
    }

    @Nonnull
    @Override
    public RenderItem getRenderItem() {
        return mc.getRenderItem();
    }

    @Nonnull
    @Override
    public TextureMap getTextureMapBlocks() {
        return mc.getTextureMapBlocks();
    }

    @Override
    public void dispatchKeypresses() {
        // Do nothing
    }

    public static BotMinecraft allocate(IBaritoneUser user) {
        BotMinecraft bm = ObjectAllocator.allocate(BotMinecraft.class);
        ((IMinecraft) (Object) bm).setGameDir(mc.gameDir);

        // Gui Compatibility
        bm.fontRenderer = mc.fontRenderer;

        bm.user = user;
        bm.tutorial = new Tutorial(bm);
        bm.gameSettings = createGameSettings(bm);
        bm.effectRenderer = BotParticleManager.INSTANCE;
        bm.toastGui = new GuiToast(bm);
        return bm;
    }

    private static GameSettings createGameSettings(BotMinecraft bm) {
        GameSettings settings = ObjectAllocator.allocate(GameSettings.class);

        // Settings that get accessed on entity tick
        settings.keyBindSprint = ObjectAllocator.allocate(KeyBinding.class);
        settings.autoJump = false;

        // Settings that are sent to the server
        settings.language = "en_us";
        settings.renderDistanceChunks = 8;
        settings.chatVisibility = EntityPlayer.EnumChatVisibility.FULL;
        settings.chatColours = true;
        settings.mainHand = EnumHandSide.RIGHT;

        // Gui Compatibility
        settings.keyBindPickBlock = mc.gameSettings.keyBindPickBlock;
        settings.keyBindsHotbar = mc.gameSettings.keyBindsHotbar;
        settings.keyBindInventory = mc.gameSettings.keyBindInventory;
        settings.keyBindDrop = mc.gameSettings.keyBindDrop;

        // Private fields that must be initialized
        IGameSettings accessor = (IGameSettings) settings;
        accessor.setMc(bm);
        accessor.setSetModelParts(new HashSet<>());

        return settings;
    }
}
