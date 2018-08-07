package baritone.bot.pathing.movement;

import baritone.bot.utils.ToolSet;

/**
 * @author Brady
 * @since 8/7/2018 4:30 PM
 */
public class CalculationContext {

    private final ToolSet toolSet;

    public CalculationContext() {
        this(new ToolSet());
    }

    public CalculationContext(ToolSet toolSet) {
        this.toolSet = toolSet;
    }

    public ToolSet getToolSet() {
        return this.toolSet;
    }
}
