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

package baritone.launch.mixins;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.event.events.BlockInteractEvent;
import baritone.utils.accessor.IPlayerControllerMP;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinPlayerController implements IPlayerControllerMP {

    @Accessor("isDestroying")
    @Override
    public abstract void setIsHittingBlock(boolean isHittingBlock);

    @Accessor("isDestroying")
    @Override
    public abstract boolean isHittingBlock();

    @Accessor("destroyBlockPos")
    @Override
    public abstract BlockPos getCurrentBlock();

    @Invoker("ensureHasSentCarriedItem")
    @Override
    public abstract void callSyncCurrentPlayItem();

    @Accessor("destroyDelay")
    @Override
    public abstract void setDestroyDelay(int destroyDelay);

    @Redirect(
            method = "performUseItemOn",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/world/level/block/state/BlockState.useItemOn(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"
            )
    )
    private InteractionResult onBlockUseItemOn(BlockState state, ItemStack stack, Level level, Player player, InteractionHand hand, BlockHitResult hitResult) {
        InteractionResult result = state.useItemOn(stack, level, player, hand, hitResult);
        onBlockUsed(player, hitResult, result);
        return result;
    }

    @Redirect(
            method = "performUseItemOn",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/world/level/block/state/BlockState.useWithoutItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"
            )
    )
    private InteractionResult onBlockUseWithoutItem(BlockState state, Level level, Player player, BlockHitResult hitResult) {
        InteractionResult result = state.useWithoutItem(level, player, hitResult);
        onBlockUsed(player, hitResult, result);
        return result;
    }

    // Only the block's own use counts; item use (e.g. placing a block against a bed) happens after these calls.
    private static void onBlockUsed(Player player, BlockHitResult hitResult, InteractionResult result) {
        if (!result.consumesAction() || !(player instanceof LocalPlayer)) {
            return;
        }
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer((LocalPlayer) player);
        if (baritone != null) {
            baritone.getGameEventHandler().onBlockInteract(new BlockInteractEvent(hitResult.getBlockPos(), BlockInteractEvent.Type.USE));
        }
    }
}
