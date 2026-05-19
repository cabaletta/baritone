package adris.altoclef.mixins;

// ActionResult ClientPlayerInteractionManager.interactBlock(ClientPlayerEntity player, ClientWorld world, Hand hand, BlockHitResult hitResult);

import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.BlockInteractEvent;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public final class ClientInteractWithBlockMixin {
    @Inject(
            method = "interactBlock",
            at = @At("HEAD")
    )
    private void onClientBlockInteract(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> ci) {
        //Debug.logMessage("(client) INTERACTED WITH: " + (hitResult != null? hitResult.getBlockPos() : "(nothing)"));
        if (hitResult != null) {
            EventBus.publish(new BlockInteractEvent(hitResult));
        }

    }
}
