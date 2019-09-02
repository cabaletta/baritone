package baritone.api.selection;

import baritone.api.utils.BetterBlockPos;

/**
 * The selection manager handles setting Baritone's selections. You can set the selection here, as well as retrieving
 * the current selection.
 */
public interface ISelectionManager {
    /**
     * Adds a new selection. The added selection is returned.
     *
     * @param selection The new selection to add.
     */
    ISelection addSelection(ISelection selection);

    /**
     * Adds a new {@link ISelection} constructed from the given block positions. The new selection is returned.
     *
     * @param pos1 One corner of the selection
     * @param pos2 The new corner of the selection
     */
    ISelection addSelection(BetterBlockPos pos1, BetterBlockPos pos2);

    /**
     * Removes the selection from the current selections.
     *
     * @param selection The selection to remove.
     * @return The removed selection.
     */
    ISelection removeSelection(ISelection selection);

    /**
     * Removes all selections.
     *
     * @return The selections that were removed, sorted from oldest to newest..
     */
    ISelection[] removeAllSelections();

    /**
     * @return The current selections, sorted from oldest to newest.
     */
    ISelection[] getSelections();

    /**
     * For anything expecting only one selection, this method is provided. However, to enforce multi-selection support,
     * this method will only return a selection if there is ONLY one.
     *
     * @return The only selection.
     */
    ISelection getOnlySelection();
}
