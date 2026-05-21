package baritone.combat.watchdog;

/**
 * Priority-ordered CPvP anomaly states detected by the Watchdog.
 *
 * Evaluation order in WatchdogEngine.evaluate(): highest-threat first.
 *   1  AIRBORNE_COMBO_LOCK   — explosion-launched + enemy within 5 m
 *   2  BOXED_CAGE_TRAP       — fully surrounded by obsidian, stuck
 *   3  CEILING_MACE_DROP     — enemy airborne with Mace, falling
 *   4  LOW_GROUND_EXPOSURE   — enemy lower, deploying obsidian/crystals
 *   5  CRYSTAL_CROSS_FIRE    — live end crystal <6 m, enemy nearby
 *   6  PROGRESSIVE_SURROUND  — obsidian cage forming (escape before sealed)
 */
public enum MaceCpvpSituation {
    NONE,
    AIRBORNE_COMBO_LOCK,
    BOXED_CAGE_TRAP,
    CEILING_MACE_DROP,
    LOW_GROUND_EXPOSURE,
    CRYSTAL_CROSS_FIRE,
    PROGRESSIVE_SURROUND
}
