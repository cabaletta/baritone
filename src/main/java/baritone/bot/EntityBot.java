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

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.StatisticsManager;
import net.minecraft.world.World;

/**
 * @author Brady
 * @since 10/23/2018
 */
@SuppressWarnings("EntityConstructor")
public class EntityBot extends EntityPlayerSP {

    public EntityBot(Minecraft mc, World world, NetHandlerPlayClient netHandlerPlayClient, StatisticsManager statisticsManager, RecipeBook recipeBook) {
        super(mc, world, netHandlerPlayClient, statisticsManager, recipeBook);
    }
}
