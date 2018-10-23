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

package baritone.bot;

import baritone.bot.net.BotNetHandlerPlayClient;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * @author Brady
 * @since 10/23/2018
 */
public interface IBaritoneUser {

    @Nullable BotNetHandlerPlayClient getConnection();

    @Nullable EntityBot getLocalEntity();

    @Nullable EntityOtherPlayerMP getRemoteEntity();

    @Nullable World getWorld();
}
