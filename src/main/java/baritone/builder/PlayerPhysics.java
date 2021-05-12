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

package baritone.builder;

import static baritone.builder.BlockStateCachedData.get;

public class PlayerPhysics {

    /**
     * previously, we assumed that playerY = Y meant that player feet would be at literally exactly Y.00
     * but really, obviously, if we are standing on soul sand, we are at Y-0.125. if we are standing on carpet, we are at Y+0.0625
     * <p>
     * the crossover point is 0.7
     * <p>
     * examples:
     * <p>
     * lower = stone,     upper = air,         returns 0 (obv)
     * lower = stone,     upper = carpet,      returns 0.0625 (thats how high carpet is)
     * lower = farmland,  upper = air,         returns -0.0625 (because farmland is 0.9375 and this is one minus that, i.e. the player is 0.0625 "below ground level")
     * lower = soul sand, upper = air,         returns -0.125 (because soul sand is 0.875)
     * lower = soul sand, upper = carpet,      returns 0.0625 (because its impossible to be standing on the soul sand with the carpet there, they've got to be on the carpet)
     * lower = stone,     upper = bottom slab, returns 0.5 (obv)
     * lower = fence,     upper = air,         returns 0.5
     * lower = fence,     upper = carpet,      returns 0.5 (because the fence collision box pokes through the carpet)
     * <p>
     * lower = anything, upper = anything taller than 0.5, returns NaN (shift up by one and try again)
     * lower = anything shorter than 0.8, upper = air, returns NaN (shift down by one and try again
     */
    public static double determinePlayerRealSupport(BlockStateCachedData lower, BlockStateCachedData upper) {
        double realPlayerFeetY;
        if (upper.collidesWithPlayer) {
            if (!upper.fullyWalkableTop) {
                throw new IllegalStateException();
            }
            // we MUST be standing on the upper, at a minimum
            // at this point, we know that realPlayerFeetY >= upper.supportedPlayerY
            // what's the exception?
            // something like a carpet on top of a fence, sadly :(
            if (lower.supportedPlayerY != null && lower.supportedPlayerY - 1 > upper.supportedPlayerY) {
                if (!lower.fullyWalkableTop) {
                    return Double.NaN; // something like a fence :(
                }
                realPlayerFeetY = lower.supportedPlayerY - 1;
            } else {
                realPlayerFeetY = upper.supportedPlayerY; // if upper is full, it will properly trigger >0.6 dont worry
            }
        } else {
            // we MUST be truly supported by only the lower
            if (!lower.fullyWalkableTop) {
                return Double.NaN;
            }
            realPlayerFeetY = lower.supportedPlayerY - 1; // can go negative (e.g. soul sand gives -0.125)
        }
        if (realPlayerFeetY < -0.126 || realPlayerFeetY > 0.501) {
            return Double.NaN;
        }
        return realPlayerFeetY;
    }

    /**
     * "Can the player walk forwards without needing to break anything?"
     * <p>
     * Takes into account things like the automatic +0.5 from walking into a slab. Does NOT take into account jumping.
     * <p>
     * Player is standing at X Y on top of S
     * UA
     * YB
     * XC
     * SD
     * <p>
     * Why do we need D? Sadly, imagine D is a fence and S is soul sand. That is a gap of 0.625 so we must detect it and return false.
     * Why do we need A? uh idk get back to me, i was super sure about it 5 minutes ago now i've forgotten
     *
     * @param feet must ALWAYS be equal to {@link #determinePlayerRealSupport(BlockStateCachedData, BlockStateCachedData)}  determinePlayerRealSupport(S, X)}
     */
    public static Collision playerTravelCollides(double feet, int U, int A, int B, int C, int D) {
        double lowerCollision = feet + 0.501; // player can autowalk up a half slab without having to jump
        double upperCollision = feet + 1.799; // height of player
        boolean alreadyMustBeCollidingWithU = upperCollision > 2; // if our head is already protruding into U, there is no need to check it
        if (Main.DEBUG && (alreadyMustBeCollidingWithU && get(U).collidesWithPlayer)) {
            throw new IllegalStateException();
        }
        if (get(D).supportedPlayerY != null && get(D).supportedPlayerY - 1 > lowerCollision) {
            // example: D is fence, S is soul sand, feet is -0.125, lowerCollision is 0.376, get(D).supportedPlayerY is 1.5
            return Collision.BLOCKED; // D
        }  // D does not prevent walking forward. C still could, however.
        double betweenCandB = determinePlayerRealSupport(get(C), get(B));
        if (!Double.isNaN(betweenCandB)) {
            // fundamentally a step upwards, from SX to CB instead of SX to DC
            if (lowerCollision < 1 + betweenCandB) { // can we, uh, actually make that step upwards?
                return Collision.BLOCKED; // we would hit C or B
            }
            if (Main.DEBUG && (get(U).collidesWithPlayer || get(B).collidesWithPlayer || !alreadyMustBeCollidingWithU)) {
                throw new IllegalStateException();
            }
            // imagine: X is a bottom slab, C is a full block, B is air. feet is 0.5, lowerCollision is 1.001 (meaning just barely out of C and into the bottom of B), betweenCandB is zero
            if (get(A).collidesWithPlayer) {
                return Collision.BLOCKED;
            }
            return Collision.VOXEL_UP;
        }
        // betweenCandB is NaN. pessimistically, this means B is colliding. optimistically, this means B and C are air.
        double betweenDandC = determinePlayerRealSupport(get(D), get(C));


        // if feet=0.5 then we want real support between C and B, not D and C
        //double resultingFeet = determinePlayerRealSupport(D, C);
    }

    public enum Collision {
        BLOCKED, // if you hit W, you would not travel (example: walking into wall)
        VOXEL_UP, // if you hit W, you will end up at a position that's a bit higher, such that you'd determineRealPlayerSupport up by one (this is pretty much JUST for walking from bottom slab to something taller such as a full block, or soul sand or something higher than 0.5 I guess)
        VOXEL_LEVEL, // if you hit W, you will end up at a similar position, such that you'd determineRealPlayerSupport at the same integer grid location (example: walking forward on level ground)
        FALL // if you hit W, you will not immediately collide with anything, at all, to the front or to the bottom (example: walking off a cliff)
    }
}
