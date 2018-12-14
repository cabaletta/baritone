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

import baritone.api.utils.IPlayerController;
import baritone.bot.IBaritoneUser;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;

/**
 * @author Brady
 * @since 11/14/2018
 */
public class BotPlayerController implements IPlayerController {

    private final IBaritoneUser user;
    private GameType gameType;

    public BotPlayerController(IBaritoneUser user) {
        this.user = user;
    }

    @Override
    public boolean clickBlock(BlockPos pos, EnumFacing side) {
        return false;
    }

    @Override
    public boolean onPlayerDamageBlock(BlockPos pos, EnumFacing side) {
        return false;
    }

    @Override
    public void resetBlockRemoving() {

    }

    @Override
    public void setGameType(GameType type) {
        this.gameType = type;
        this.gameType.configurePlayerCapabilities(this.user.getEntity().capabilities);
    }

    @Override
    public GameType getGameType() {
        return this.gameType;
    }
}
