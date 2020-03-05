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

package baritone.bot.spec;

import baritone.api.bot.IBaritoneUser;
import baritone.api.utils.Helper;
import baritone.utils.ObjectAllocator;
import com.google.common.util.concurrent.ListenableFuture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfiguration;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.util.Session;

import javax.annotation.Nonnull;
import java.util.concurrent.Callable;

/**
 * "Implementation" of {@link Minecraft} which gets allocated without receiving a constructor call.
 * This allows us to avoid the game's setup process (moreso in versions after 1.12 than 1.12 itself).
 *
 * @author Brady
 * @since 3/3/2020
 */
public final class BotMinecraft extends Minecraft implements Helper {

    private IBaritoneUser user;
    private BotTutorial tutorial;

    private BotMinecraft(GameConfiguration gameConfig) {
        super(gameConfig);
    }

    @Nonnull
    @Override
    public Session getSession() {
        return this.user.getSession();
    }

    @Override
    public MinecraftSessionService getSessionService() {
        return mc.getSessionService();
    }

    @Override
    public <V> ListenableFuture<V> addScheduledTask(Callable<V> callableToSchedule) {
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

    public static BotMinecraft allocate(IBaritoneUser user) {
        BotMinecraft bm = ObjectAllocator.allocate(BotMinecraft.class);
        bm.user = user;
        bm.tutorial = new BotTutorial(bm);
        bm.gameSettings = new GameSettings();
        bm.gameSettings.autoJump = false;
        return bm;
    }
}
