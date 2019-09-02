package baritone.selection;

import baritone.api.selection.ISelection;
import baritone.api.selection.ISelectionManager;
import baritone.api.utils.BetterBlockPos;

import java.util.LinkedHashSet;
import java.util.Set;

public class SelectionManager implements ISelectionManager {
    private final Set<ISelection> selections = new LinkedHashSet<>();
    private ISelection[] selectionsArr = new ISelection[0];

    public SelectionManager() {
        new SelectionRenderer(this);
    }

    private void resetSelectionsArr() {
        selectionsArr = selections.toArray(new ISelection[0]);
    }

    @Override
    public synchronized ISelection addSelection(ISelection selection) {
        selections.add(selection);
        resetSelectionsArr();
        return selection;
    }

    @Override
    public ISelection addSelection(BetterBlockPos pos1, BetterBlockPos pos2) {
        return addSelection(new Selection(pos1, pos2));
    }

    @Override
    public synchronized ISelection removeSelection(ISelection selection) {
        selections.remove(selection);
        resetSelectionsArr();
        return selection;
    }

    @Override
    public synchronized ISelection[] removeAllSelections() {
        ISelection[] selectionsArr = getSelections();
        selections.clear();
        resetSelectionsArr();
        return selectionsArr;
    }

    @Override
    public ISelection[] getSelections() {
        return selectionsArr;
    }

    @Override
    public synchronized ISelection getOnlySelection() {
        if (selections.size() == 1) {
            return selections.iterator().next();
        }

        return null;
    }
}
