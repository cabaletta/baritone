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

public class PlayerPhysics {

    /**
     * the player Y is within within. i.e. the player Y is greater than or equal within and less than within+1
     */
    public static int determinePlayerRealSupport(BlockStateCachedData underneath, BlockStateCachedData within) {
        if (within.collidesWithPlayer) {
            if (underneath.collisionHeightBlips != null && underneath.collisionHeightBlips - Blip.FULL_BLOCK > within.collisionHeightBlips) { // TODO > or >=
                if (!underneath.fullyWalkableTop) {
                    return -1;
                }
                return underneath.collisionHeightBlips - Blip.FULL_BLOCK; // this could happen if "underneath" is a fence and "within" is a carpet
            }
            if (!within.fullyWalkableTop || within.collisionHeightBlips >= Blip.FULL_BLOCK) {
                return -1;
            }
            return within.collisionHeightBlips;
        } else {
            if (!underneath.fullyWalkableTop || underneath.collisionHeightBlips < Blip.FULL_BLOCK) {
                return -1;
            }
            return underneath.collisionHeightBlips - Blip.FULL_BLOCK;
        }
    }

    /**
     * "Can the player walk forwards without needing to break anything?"
     * <p>
     * Takes into account things like the automatic +0.5 from walking into a slab. Does NOT take into account jumping.
     * <p>
     * Player is standing at X (feet) Y (head) on top of S, intends to walk forwards into this ABCD column
     * UA
     * YB
     * XC
     * SD
     *
     */
    public static Collision playerTravelCollides(int feet,
                                                 BlockStateCachedData U,
                                                 BlockStateCachedData A,
                                                 BlockStateCachedData B,
                                                 BlockStateCachedData C,
                                                 BlockStateCachedData D,
                                                 BlockStateCachedData S,
                                                 BlockStateCachedData X) {
        if (Main.DEBUG && (feet < 0 || feet >= Blip.FULL_BLOCK)) {
            throw new IllegalStateException();
        }
        if (Main.DEBUG && (feet != determinePlayerRealSupport(S, X))) {
            throw new IllegalStateException();
        }
        boolean alreadyWithinU = feet > Blip.TWO_BLOCKS - Blip.PLAYER_HEIGHT; // > and not >= because the player height is a slight overestimate
        if (Main.DEBUG && (alreadyWithinU && U.collidesWithPlayer)) {
            throw new IllegalStateException();
        }
        if (alreadyWithinU && A.collidesWithPlayer) {
            return Collision.BLOCKED; // we are too tall. bonk!
        }
        int couldStepUpTo = feet + Blip.HALF_BLOCK;
        // D cannot prevent us from doing anything because it cant be higher than 1.5. therefore, makes sense to check CB before DC.
        int stepUp = determinePlayerRealSupport(C, B);
        if (stepUp >= 0) {
            // fundamentally a step upwards, from X to B instead of X to C
            // too high?
            int heightRelativeToStartVoxel = stepUp + Blip.FULL_BLOCK;
            if (heightRelativeToStartVoxel > couldStepUpTo) {
                return Collision.BLOCKED;
            } // else this is possible!
            if (Main.DEBUG && (U.collidesWithPlayer || B.collidesWithPlayer || A.collidesWithPlayer || !alreadyWithinU)) {
                // must already be colliding with U because in order for this step to be even possible, feet must be at least HALF_BLOCK
                // TODO maybe it is possible for B.collidesWithPlayer here? like imagine X is soul sand and C is full block and B is carpet? grrrrrrrr
                throw new IllegalStateException();
            }
            return Collision.VOXEL_UP; // A is already checked, so that's it!
        }
        // betweenCandB is impossible. pessimistically, this means B is colliding. optimistically, this means B and C are air.
        int stayLevel = determinePlayerRealSupport(D, C);
        if (stayLevel >= 0) {
            // fundamentally staying within the same vertical voxel, X -> C
            if (stayLevel > couldStepUpTo) {
                return Collision.BLOCKED;
            }
            if (stayLevel > Blip.TWO_BLOCKS - Blip.PLAYER_HEIGHT && !alreadyWithinU) { // step up, combined with our height, protrudes into U and A, AND we didn't already
                if (U.collidesWithPlayer) { // stayLevel could even be LESS than feet
                    return Collision.BLOCKED;
                }
                if (A.collidesWithPlayer) {
                    return Collision.BLOCKED;
                }
            }
            if (B.collidesWithPlayer) {
                return Collision.BLOCKED; // obv, our head will go into B
            }
            return Collision.VOXEL_LEVEL;
        }
        if (B.collidesWithPlayer || C.collidesWithPlayer) {
            return Collision.BLOCKED;
        }
        if (!D.collidesWithPlayer) {
            return Collision.FALL;
        }
        if (Main.DEBUG && D.collisionHeightBlips == null) {
            throw new IllegalStateException();
        }
        if (Main.DEBUG && D.collisionHeightBlips >= Blip.FULL_BLOCK && D.fullyWalkableTop) {
            throw new IllegalStateException();
        }
        if (D.collisionHeightBlips < Blip.FULL_BLOCK + feet) {
            return Collision.FALL;
        } else {
            return Collision.BLOCKED;
        }
    }

    static {
        if (Blip.PLAYER_HEIGHT > Blip.TWO_BLOCKS || Blip.PLAYER_HEIGHT + Blip.HALF_BLOCK <= Blip.TWO_BLOCKS) {
            throw new IllegalStateException("Assumptions made in playerTravelCollides");
        }
    }

    public enum Collision {
        BLOCKED, // if you hit W, you would not travel (example: walking into wall)
        VOXEL_UP, // if you hit W, you will end up at a position that's a bit higher, such that you'd determineRealPlayerSupport up by one (example: walking from a partial block to a full block or higher, e.g. half slab to full block, or soul sand to full block, or soul sand to full block+carpet on top)
        VOXEL_LEVEL, // if you hit W, you will end up at a similar position, such that you'd determineRealPlayerSupport at the same integer grid location (example: walking forward on level ground)
        FALL // if you hit W, you will not immediately collide with anything, at all, to the front or to the bottom (example: walking off a cliff)
    }
}
