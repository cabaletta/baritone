package baritone.bot.pathing.util;

/**
 * @author Brady
 * @since 8/4/2018 1:11 AM
 */
public enum PathingBlockType {

    AIR  (0b00),
    WATER(0b01),
    AVOID(0b10),
    SOLID(0b11);

    private final boolean[] bits;

    PathingBlockType(int bits) {
        this.bits = new boolean[] {
                (bits & 0b10) != 0,
                (bits & 0b01) != 0
        };
    }

    public final boolean[] getBits() {
        return this.bits;
    }

    public static PathingBlockType fromBits(boolean b1, boolean b2) {
        for (PathingBlockType type : values())
            if (type.bits[0] == b1 && type.bits[1] == b2)
                return type;

        // This will never happen, but if it does, assume it's just AIR
        return PathingBlockType.AIR;
    }
}
