package baritone.selection;

import baritone.Baritone;
import baritone.api.event.events.RenderEvent;
import baritone.api.event.listener.AbstractGameEventListener;
import baritone.api.selection.ISelection;
import baritone.utils.IRenderer;
import net.minecraft.util.math.AxisAlignedBB;

import java.awt.*;

public class SelectionRenderer implements IRenderer, AbstractGameEventListener {

    public static final double SELECTION_BOX_EXPANSION = .005D;

    private final SelectionManager manager;

    SelectionRenderer(Baritone baritone, SelectionManager manager) {
        this.manager = manager;
        baritone.getGameEventHandler().registerEventListener(this);
    }

    public static void renderSelections(ISelection[] selections, float opacity, boolean ignoreDepth, float lineWidth, boolean renderSelection, Color colorSelection, boolean renderSelectionCorners, Color colorSelectionPos1, Color colorSelectionPos2) {
        if(!renderSelection)
            return;

        IRenderer.startLines(colorSelection, opacity, lineWidth, ignoreDepth);

        for (ISelection selection : selections) {
            IRenderer.drawAABB(selection.aabb(), SELECTION_BOX_EXPANSION);
        }

        if (renderSelectionCorners) {
            IRenderer.glColor(colorSelectionPos1, opacity);

            for (ISelection selection : selections) {
                IRenderer.drawAABB(new AxisAlignedBB(selection.pos1(), selection.pos1().add(1, 1, 1)));
            }

            IRenderer.glColor(colorSelectionPos2, opacity);

            for (ISelection selection : selections) {
                IRenderer.drawAABB(new AxisAlignedBB(selection.pos2(), selection.pos2().add(1, 1, 1)));
            }
        }

        IRenderer.endLines(ignoreDepth);
    }

    @Override
    public void onRenderPass(RenderEvent event) {
        if(!manager.isHomeAreaManager())
            renderSelections(manager.getSelections(),
                    settings.selectionOpacity.value,
                    settings.renderSelectionIgnoreDepth.value,
                    settings.selectionLineWidth.value,
                    settings.renderSelection.value,
                    settings.colorSelection.value,
                    settings.renderSelectionCorners.value,
                    settings.colorSelectionPos1.value,
                    settings.colorSelectionPos2.value);
        else
            renderSelections(manager.getSelections(),
                    settings.selectionHomeAreaOpacity.value,
                    settings.renderHomeAreaSelection.value,
                    settings.selectionHomeAreaLineWidth.value,
                    settings.renderHomeAreaSelection.value,
                    settings.colorHomeAreaSelection.value,
                    settings.renderHomeAreaSelectionCorners.value,
                    settings.colorHomeAreaSelectionPos1.value,
                    settings.colorHomeAreaSelectionPos2.value);

    }
}
