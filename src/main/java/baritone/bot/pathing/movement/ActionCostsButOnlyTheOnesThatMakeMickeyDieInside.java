package baritone.bot.pathing.movement;

public interface ActionCostsButOnlyTheOnesThatMakeMickeyDieInside {
    double[] FALL_N_BLOCKS_COST = generateFallNBlocksCost();

    static double[] generateFallNBlocksCost() {
        double[] costs = new double[257];
        for (int i = 0; i < 257; i++) {
            costs[i] = distanceToTicks(i);
        }
        return costs;
    }

    static double velocity(int ticks) {
        return (Math.pow(0.98, ticks) - 1) * -3.92;
    }

    static double oldFormula(double ticks) {
        return -3.92 * (99 - 49.5 * (Math.pow(0.98, ticks) + 1) - ticks);
    }

    static double distanceToTicks(double distance) {
        if (distance == 0) {
            return 0; // Avoid 0/0 NaN
        }
        int tickCount = 0;
        while (true) {
            double fallDistance = velocity(tickCount);
            if (distance <= fallDistance) {
                return tickCount + distance / fallDistance;
            }
            distance -= fallDistance;
            tickCount++;
        }
    }


}
