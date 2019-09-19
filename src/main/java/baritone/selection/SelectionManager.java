package baritone.selection;

import baritone.Baritone;
import baritone.api.selection.ISelection;
import baritone.api.selection.ISelectionManager;
import baritone.api.utils.BetterBlockPos;
import net.minecraft.util.EnumFacing;

import java.util.LinkedList;
import java.util.ListIterator;

public class SelectionManager implements ISelectionManager {

    private final LinkedList<ISelection> selections = new LinkedList<>();
    private ISelection[] selectionsArr = new ISelection[0];

    public SelectionManager(Baritone baritone) {
        new SelectionRenderer(baritone, this);
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
            return selections.peekFirst();
        }

        return null;
    }

    @Override
    public ISelection getLastSelection() {
        return selections.peekLast();
    }

    @Override
    public synchronized ISelection expand(ISelection selection, EnumFacing direction, int blocks) {
        for (ListIterator<ISelection> it = selections.listIterator(); it.hasNext(); ) {
            ISelection current = it.next();

            if (current == selection) {
                it.remove();
                it.add(current.expand(direction, blocks));
                resetSelectionsArr();
                return it.previous();
            }
        }

        return null;
    }

    @Override
    public synchronized ISelection contract(ISelection selection, EnumFacing direction, int blocks) {
        for (ListIterator<ISelection> it = selections.listIterator(); it.hasNext(); ) {
            ISelection current = it.next();

            if (current == selection) {
                it.remove();
                it.add(current.contract(direction, blocks));
                resetSelectionsArr();
                return it.previous();
            }
        }

        return null;
    }

    @Override
    public synchronized ISelection shift(ISelection selection, EnumFacing direction, int blocks) {
        for (ListIterator<ISelection> it = selections.listIterator(); it.hasNext(); ) {
            ISelection current = it.next();

            if (current == selection) {
                it.remove();
                it.add(current.shift(direction, blocks));
                resetSelectionsArr();
                return it.previous();
            }
        }

        return null;
    }
}
