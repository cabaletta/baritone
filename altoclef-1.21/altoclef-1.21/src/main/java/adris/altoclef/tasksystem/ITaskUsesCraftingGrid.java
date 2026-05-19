package adris.altoclef.tasksystem;

/**
 * Some tasks (mainly tasks that open containers) will break if they don't require
 * that the 2x2 crafting grid is empty. Tasks which implement this interface
 * require implementers to maintain an empty crafting grid.
 * <p>
 * This interface let's a task declare that it MUST have certain slots clear before it can execute.
 */
public interface ITaskUsesCraftingGrid {

}
