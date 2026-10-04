/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.pathing.movement.movements;

import java.util.concurrent.ConcurrentHashMap;

// a long fall, played out tick by tick, with a ladder or vine showing up in one cell of the column. pure math (no mc classes)
// so A* can ask it things off thread.
//
// vanilla 1.21.4 gives us two ways for a climbable to wipe the fall distance:
//   A. LivingEntity.handleOnClimbable: if the cell our feet START the tick in is climbable, vy is clamped to -0.15 and
//      fallDistance is zeroed, all before move. so the ladder has to exist by the time the tick starts, and a 1 tall cell
//      is skipped completely if no tick start lands inside it (this is why we "don't actually grab" ladders at high speed)
//   B. Entity.move: if we moved at least a block this tick, clip the movement segment against every fall damage resetting
//      block (as full cubes) and zero the fallDistance if it hits anything. no slowdown, but it does not care where the tick
//      starts. the landing tick counts too, and landing is what checks the fall distance, so a vine in the landing cell
//      means a 200 block fall does nothing. this is the one that makes a clutch work at terminal velocity
final class LadderClutch {

    // straight out of 1.21.4 LivingEntity.travelInAir: y += vy, then vy = (vy - 0.08) * 0.98f
    static final double GRAVITY = 0.08, DRAG = 0.9800000190734863;
    // what the first airborne tick starts with. on the ground vy gets zeroed by the floor and then the same formula runs
    static final double WALK_OFF_VY = (0 - GRAVITY) * DRAG;
    // handleOnClimbable
    static final double SLIDE = 0.15;
    static final double EYE = 1.62;
    // ladders are 3/16 thick on the wall side and the hitbox is 0.3 each way, so be at least this far from the wall plane
    // or the placement is refused (and if it wasn't, we'd land on top of the slab). vines have no collision, no limit
    static final double LADDER_CLEARANCE = 0.3 + 3 / 16.0;
    // where we try to hang out relative to the wall plane. 0.5 is only 0.0125 off the clearance and air control is not that good
    static final double WALL_GAP = 0.6;
    // where on the wall face we click, as a fraction up the cell. high, so the eye has less far to look, but not so high
    // that eye prediction being a bit off lands us on the block above
    static final double AIM_HEIGHT = 0.8;
    // tick starts closer than this to the top or bottom of a cell don't count as inside it, 0.05 is the float/server slop
    static final double SLOP = 0.05;
    // fall distance we're ok landing with. damage is ceil(fallDistance - 3)
    static final double SAFE_FALL = 3 - 0.05;
    // the cells above the landing floor we look at. a vine in any of these does it, higher than this and the fall
    // distance that's left after the reset is too much to land with anyway
    static final int CELLS = 5;
    static final int MAX_DROP = 256;
    private static final int MAX_TICKS = 400;
    // click needs a tick of aiming first, and the first couple of ticks are still walking off the edge
    static final int FIRST_PLACE = 2;
    private static final double REACH_SLOP = 0.25;

    private LadderClutch() {}

    // where we end up: the fall distance we land with, and how many ticks that took (sliding included)
    record Landing(double fall, double ticks) {}

    // put a climbable in the cell this far above the floor, clicking on this tick
    record Plan(int cell, int place, double ticks, double fall) {}

    // how high above the floor each tick starts, from a drop blocks fall. what MovementDescend works with, since the floor
    // we walk off of is the only height that's exactly known. the last entry is the last tick start before we land
    static double[] tickStarts(double drop) {
        double[] tmp = new double[MAX_TICKS];
        int n = 0;
        double y = drop, v = WALK_OFF_VY;
        while (y > 0 && n < MAX_TICKS) {
            tmp[n++] = y;
            y += v;
            v = (v - GRAVITY) * DRAG;
        }
        return java.util.Arrays.copyOf(tmp, n);
    }

    // does any tick start of a fall from startY land inside this cell. what a climbable that is already there needs,
    // this replaced "only grab when the fall is under 11 blocks", which was just this with the answer rounded off
    static boolean grabs(double startY, int cell) {
        double y = startY, v = WALK_OFF_VY;
        while (y >= cell) {
            if (y >= cell + 0.02 && y < cell + 1 - 0.02) {
                return true;
            }
            y += v;
            v = (v - GRAVITY) * DRAG;
        }
        return false;
    }

    // slop moves the edges of everything (tick start inside the cell, move long enough). positive is stingy, negative is
    // generous, and a plan has to be safe either way so a tick start sitting right on an edge never decides anything
    static Landing play(double y, double v, double fall, int floor, int cell, int place, double slop) {
        for (int t = 0; t < MAX_TICKS; t++) {
            boolean there = t >= place;
            if (there && y >= cell + slop && y < cell + 1 - slop) {
                fall = 0;
                v = Math.max(v, -SLIDE);
            }
            double end = y + v;
            boolean landed = end <= floor;
            if (landed) {
                end = floor;
            }
            double moved = y - end;
            if (there && fall != 0 && moved >= 1 + slop * 0.4 && end <= cell + 1 - slop && y >= cell + slop) {
                fall = 0;
            }
            if (landed) {
                // the landing move itself isn't added, Entity.checkFallDamage only counts moves where we stayed in the air
                return new Landing(fall, t + (v < 0 ? moved / -v : 0));
            }
            fall += moved;
            y = end;
            v = (v - GRAVITY) * DRAG;
        }
        return new Landing(Double.MAX_VALUE, MAX_TICKS);
    }

    // can we click the wall next to this cell from here. the cell has to be at or below our feet cell (above us does
    // nothing), and the eye to the point we aim at has to be within reach. the ray stays in our own column until the wall
    // plane so there's nothing else to hit on the way
    static boolean canReach(double feetY, int cell, double reach) {
        if (cell > Math.floor(feetY)) {
            return false;
        }
        double eye = feetY + EYE;
        return Math.hypot(WALL_GAP, eye - (cell + AIM_HEIGHT)) <= reach - REACH_SLOP;
    }

    // the best way to clutch from this exact state. mask has bit k set if the cell k above the floor is air with something
    // to hang a climbable on. earliest click wins (it's the most room to retry), then the lowest cell
    static Plan pick(double y0, double v0, double fall0, int floor, int mask, int firstPlace, double reach) {
        double[] ys = new double[MAX_TICKS];
        double[] vs = new double[MAX_TICKS];
        int n = 0;
        double y = y0, v = v0;
        while (y > floor && n < MAX_TICKS) {
            ys[n] = y;
            vs[n] = v;
            n++;
            y += v;
            v = (v - GRAVITY) * DRAG;
        }
        for (int p = firstPlace; p < n; p++) {
            for (int k = 0; k < CELLS; k++) {
                if ((mask & (1 << k)) == 0 || !canReach(ys[p] - floor, k, reach)) {
                    continue;
                }
                Landing stingy = play(y0, v0, fall0, floor, floor + k, p, SLOP);
                if (stingy.fall() > SAFE_FALL) {
                    continue;
                }
                Landing generous = play(y0, v0, fall0, floor, floor + k, p, -SLOP);
                if (generous.fall() <= SAFE_FALL) {
                    return new Plan(k, p, Math.max(stingy.ticks(), generous.ticks()), Math.max(stingy.fall(), generous.fall()));
                }
            }
        }
        return null;
    }

    private static final Plan NONE = new Plan(-1, -1, 0, 0);
    private static final ConcurrentHashMap<Long, Plan> MEMO = new ConcurrentHashMap<>();

    // pick from a fall off a ledge. A* asks this for every column it falls down, and the answer is only the drop, which
    // cells have something to hang on, and your reach, so remember it
    static Plan plan(int drop, int mask, double reach) {
        if (drop > MAX_DROP || drop < 1) {
            return null;
        }
        long key = ((long) drop << 24) | ((long) mask << 16) | Math.round(reach * 10);
        Plan p = MEMO.get(key);
        if (p == null) {
            p = pick(drop, WALK_OFF_VY, 0, 0, mask, FIRST_PLACE, reach);
            MEMO.put(key, p == null ? NONE : p);
        }
        return p == NONE || p == null ? null : p;
    }
}
