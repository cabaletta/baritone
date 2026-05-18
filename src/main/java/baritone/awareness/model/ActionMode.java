package baritone.awareness.model;

/** Action modes the decision engine can commit to each tick. */
public enum ActionMode {
    IDLE,
    ATTACK,
    DEFEND,
    RETREAT,
    REPOSITION,
    HEAL,
    SHIELD,
    ESCAPE
}
