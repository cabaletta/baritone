package adris.altoclef.mixins;

import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.BlockBrokenEvent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class BlockModifiedByPlayerMixin {

    @Inject(
            method = "onBreak",
            at = @At("HEAD")
    )
    public void onBlockBroken(World world, BlockPos pos, BlockState state, PlayerEntity player, CallbackInfoReturnable<BlockState> ci) {
        if (player.getWorld() == world) {
            BlockBrokenEvent evt = new BlockBrokenEvent();
            evt.blockPos = pos;
            evt.blockState = state;
            evt.player = player;
            EventBus.publish(evt);
        }
    }

    @Inject(
            method = "onPlaced",
            at = @At("HEAD")
    )
    public void onBlockPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack, CallbackInfo ci) {
        // This one is weirdly unreliable.
        //Debug.logInternal("[TEMP] global place");
        //StaticMixinHookups.onBlockPlaced(world, pos, state, placer, itemStack);
    }

}
