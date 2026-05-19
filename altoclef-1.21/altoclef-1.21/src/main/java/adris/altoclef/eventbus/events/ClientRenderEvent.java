package adris.altoclef.eventbus.events;

import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;

public class ClientRenderEvent {
    public MatrixStack stack;
    public RenderTickCounter tickDelta;

    public ClientRenderEvent(MatrixStack stack, RenderTickCounter tickDelta) {
        this.stack = stack;
        this.tickDelta = tickDelta;
    }
}
