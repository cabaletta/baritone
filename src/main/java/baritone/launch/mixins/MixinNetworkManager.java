package baritone.launch.mixins;

import baritone.bot.Baritone;
import baritone.bot.event.events.PacketEvent;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Brady
 * @since 8/6/2018 9:30 PM
 */
@Mixin(NetworkManager.class)
public class MixinNetworkManager {

    @Inject(
            method = "dispatchPacket",
            at = @At("HEAD")
    )
    private void dispatchPacket(Packet<?> inPacket, final GenericFutureListener<? extends Future<? super Void >>[] futureListeners, CallbackInfo ci) {
        Baritone.INSTANCE.getGameEventHandler().onSendPacket(new PacketEvent(inPacket));
    }

    @Inject(
            method = "channelRead0",
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/network/Packet.processPacket(Lnet/minecraft/network/INetHandler;)V"
            )
    )
    private void preProcessPacket(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        Baritone.INSTANCE.getGameEventHandler().onReceivePacket(new PacketEvent(packet));
    }
}
