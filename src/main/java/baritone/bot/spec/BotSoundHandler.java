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

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.settings.GameSettings;

import javax.annotation.Nonnull;

/**
 * @author Brady
 * @since 3/7/2020
 */
public final class BotSoundHandler extends SoundHandler {

    public static final BotSoundHandler INSTANCE = new BotSoundHandler(null, null);

    public BotSoundHandler(IResourceManager manager, GameSettings gameSettingsIn) {
        super(manager, gameSettingsIn);
    }

    @Override
    public void playSound(@Nonnull ISound sound) {}
}
