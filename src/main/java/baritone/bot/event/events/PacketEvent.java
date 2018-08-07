package baritone.bot.event.events;

import net.minecraft.network.Packet;

/**
 * @author Brady
 * @since 8/6/2018 9:31 PM
 */
public final class PacketEvent {

    private final Packet<?> packet;

    public PacketEvent(Packet<?> packet) {
        this.packet = packet;
    }

    public final Packet<?> getPacket() {
        return this.packet;
    }
}
