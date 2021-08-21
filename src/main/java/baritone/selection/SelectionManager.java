package baritone.selection;

import baritone.Baritone;
import baritone.api.IBaritone;
import baritone.api.selection.ISelection;
import baritone.api.selection.ISelectionManager;
import baritone.api.utils.BetterBlockPos;
import baritone.cache.WorldData;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.LinkedList;
import java.util.ListIterator;

public class SelectionManager implements ISelectionManager {

    private final LinkedList<ISelection> selections = new LinkedList<>();
    private ISelection[] selectionsArr = new ISelection[0];
    private boolean isHomeArea;
    IBaritone baritone;

    public SelectionManager(Baritone baritone, boolean homeAreaManager) {
        new SelectionRenderer(baritone, this);
        isHomeArea = homeAreaManager;
        this.baritone = baritone;
    }

    private void resetSelectionsArr() {
        selectionsArr = selections.toArray(new ISelection[0]);
        if(isHomeArea)
            baritone.getWorldProvider().getCurrentWorld().getCachedHomeAreas().save(this);
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

    @Override
    public boolean isHomeAreaManager()
    {
        return this.isHomeArea;
    }

    @Override
    public boolean selectionContainsPoint(BlockPos vector) {
        for (int i = 0; selectionsArr.length != i; ++i)
            if(selectionsArr[i].aabb().intersects(new AxisAlignedBB(vector)))
                return true;

        return false;
    }
}
