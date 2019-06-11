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

package baritone.utils.player;

import baritone.api.utils.Helper;
import baritone.api.utils.IPlayerController;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.GameType;
import net.minecraft.world.World;

/**
 * Implementation of {@link IPlayerController} that chains to the primary player controller's methods
 *
 * @author Brady
 * @since 12/14/2018
 */
public enum PrimaryPlayerController implements IPlayerController, Helper {

    INSTANCE;

    @Override
    public boolean onPlayerDamageBlock(BlockPos pos, Direction side) {

        return mc.field_71442_b.onPlayerDamageBlock(pos, side);
    }

    @Override
    public void resetBlockRemoving() {
        mc.field_71442_b.resetBlockRemoving();
    }

    @Override
    public ItemStack windowClick(int windowId, int slotId, int mouseButton, ClickType type, PlayerEntity player) {
        return mc.field_71442_b.windowClick(windowId, slotId, mouseButton, type, player);
    }

    @Override
    public void setGameType(GameType type) {
        mc.field_71442_b.setGameType(type);
    }

    @Override
    public GameType getGameType() {
        return mc.field_71442_b.getCurrentGameType();
    }

    @Override
    public ActionResultType processRightClickBlock(ClientPlayerEntity player, World world, Hand hand, BlockRayTraceResult result) {
        // primaryplayercontroller is always in a ClientWorld so this is ok
        return mc.field_71442_b.func_217292_a(player, (ClientWorld) world, hand, result);
    }

    @Override
    public ActionResultType processRightClick(ClientPlayerEntity player, World world, Hand hand) {
        return mc.field_71442_b.processRightClick(player, world, hand);
    }
}
