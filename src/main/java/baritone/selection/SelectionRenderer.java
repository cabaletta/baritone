package baritone.selection;

import baritone.api.event.events.RenderEvent;
import baritone.api.event.listener.AbstractGameEventListener;
import baritone.api.selection.ISelection;
import baritone.api.utils.IRenderer;
import net.minecraft.util.math.AxisAlignedBB;

public class SelectionRenderer implements IRenderer, AbstractGameEventListener {
    private final SelectionManager manager;

    SelectionRenderer(SelectionManager manager) {
        this.manager = manager;
        baritone.getGameEventHandler().registerEventListener(this);
    }

    public static void renderSelections(ISelection[] selections) {
        boolean ignoreDepth = settings.renderSelectionIgnoreDepth.value;
        float lineWidth = settings.selectionRenderLineWidthPixels.value;

        if (!settings.renderSelection.value) {
            return;
        }

        IRenderer.startLines(settings.colorSelection.value, lineWidth, ignoreDepth);

        for (ISelection selection : selections) {
            IRenderer.drawAABB(selection.aabb());
        }

        IRenderer.endLines(ignoreDepth);

        if (!settings.renderSelectionCorners.value) {
            return;
        }

        IRenderer.startLines(settings.colorSelectionPos1.value, lineWidth, ignoreDepth);

        for (ISelection selection : selections) {
            IRenderer.drawAABB(new AxisAlignedBB(selection.pos1(), selection.pos1().add(1, 1, 1)));
        }

        IRenderer.endLines(ignoreDepth);
        IRenderer.startLines(settings.colorSelectionPos2.value, lineWidth, ignoreDepth);

        for (ISelection selection : selections) {
            IRenderer.drawAABB(new AxisAlignedBB(selection.pos2(), selection.pos2().add(1, 1, 1)));
        }

        IRenderer.endLines(ignoreDepth);
    }

    @Override
    public void onRenderPass(RenderEvent event) {
        renderSelections(manager.getSelections());
    }
}
