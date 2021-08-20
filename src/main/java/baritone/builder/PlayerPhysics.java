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

    public static int determinePlayerRealSupportLevel(BlockStateCachedData underneath, BlockStateCachedData within) {
        switch (canPlayerStand(underneath, within)) {
            case STANDARD_WITHIN_SUPPORT:
                return within.collisionHeightBlips();
            case UNDERNEATH_PROTRUDES_AT_OR_ABOVE_FULL_BLOCK:
                return underneath.collisionHeightBlips() - Blip.FULL_BLOCK;
            default:
                return -1;
        }
    }

    /**
     * the player Y is within within. i.e. the player Y is greater than or equal within and less than within+1
     * <p>
     * underneath is the block underneath that, which we annoyingly also have to check due to fences and other blocks that are taller than a block
     */
    public static VoxelResidency canPlayerStand(BlockStateCachedData underneath, BlockStateCachedData within) {
        if (within.collidesWithPlayer) {
            if (underneath.collidesWithPlayer && underneath.collisionHeightBlips() - Blip.FULL_BLOCK > within.collisionHeightBlips()) { // > because imagine something like slab on top of fence, we can walk on the slab even though the fence is equivalent height
                if (!underneath.fullyWalkableTop) {
                    return VoxelResidency.PREVENTED_BY_UNDERNEATH;
                }
                return VoxelResidency.UNDERNEATH_PROTRUDES_AT_OR_ABOVE_FULL_BLOCK; // this could happen if "underneath" is a fence and "within" is a carpet
            }
            if (!within.fullyWalkableTop) {
                return VoxelResidency.PREVENTED_BY_WITHIN;
            }
            if (within.collisionHeightBlips() >= Blip.FULL_BLOCK) {
                return VoxelResidency.IMPOSSIBLE_WITHOUT_SUFFOCATING;
            }
            return VoxelResidency.STANDARD_WITHIN_SUPPORT;
        } else {
            if (!underneath.fullyWalkableTop) {
                return VoxelResidency.PREVENTED_BY_UNDERNEATH;
            }
            if (underneath.collisionHeightBlips() < Blip.FULL_BLOCK) { // short circuit only calls collisionHeightBlips when fullyWalkableTop is true, so this is safe
                return VoxelResidency.FLOATING;
            }
            return VoxelResidency.UNDERNEATH_PROTRUDES_AT_OR_ABOVE_FULL_BLOCK;
        }
    }

    public enum VoxelResidency {
        UNDERNEATH_PROTRUDES_AT_OR_ABOVE_FULL_BLOCK, // i fucking hate notch for adding fences to the game. anyway this means that the height is underneath.collisionHeightBlips minus a full block.
        STANDARD_WITHIN_SUPPORT, // :innocent: emoji, the height is simply the collisionHeightBlips of the within

        IMPOSSIBLE_WITHOUT_SUFFOCATING, // aka: um we are literally underground
        FLOATING, // aka: um we are literally floating in midair

        PREVENTED_BY_UNDERNEATH, // fences :woozy_face:
        PREVENTED_BY_WITHIN, // what are you even thinking?
    }

    public static boolean valid(VoxelResidency res) {
        // FWIW this is equivalent to "return determinePlayerRealSupportLevel > 0"
        switch (res) {
            case UNDERNEATH_PROTRUDES_AT_OR_ABOVE_FULL_BLOCK:
            case STANDARD_WITHIN_SUPPORT:
                return true;
            default:
                return false;
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
        if (Main.DEBUG && (feet != determinePlayerRealSupportLevel(S, X))) {
            throw new IllegalStateException();
        }
        boolean alreadyWithinU = feet > Blip.TWO_BLOCKS - Blip.PLAYER_HEIGHT_SLIGHT_OVERESTIMATE; // > and not >= because the player height is a slight overestimate
        if (Main.DEBUG && (alreadyWithinU && U.collidesWithPlayer)) {
            throw new IllegalStateException();
        }
        if (alreadyWithinU && A.collidesWithPlayer) {
            return Collision.BLOCKED; // we are too tall. bonk!
        }
        int couldStepUpTo = feet + Blip.HALF_BLOCK;
        // D cannot prevent us from doing anything because it cant be higher than 1.5. therefore, makes sense to check CB before DC.
        int stepUp = determinePlayerRealSupportLevel(C, B);
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
        // stepUp is impossible. pessimistically, this means B is colliding. optimistically, this means B and C are air.
        if (B.collidesWithPlayer) { // we have ruled out stepping on top of B, so now if B is still colliding there is no way forward
            return Collision.BLOCKED;
        }
        int stayLevel = determinePlayerRealSupportLevel(D, C);
        if (stayLevel >= 0) {
            // fundamentally staying within the same vertical voxel, X -> C
            if (stayLevel > couldStepUpTo) {
                return Collision.BLOCKED;
            }
            if (stayLevel > Blip.TWO_BLOCKS - Blip.PLAYER_HEIGHT_SLIGHT_OVERESTIMATE && !alreadyWithinU) { // step up, combined with our height, protrudes into U and A, AND we didn't already
                if (U.collidesWithPlayer) { // stayLevel could even be LESS than feet
                    return Collision.BLOCKED;
                }
                if (A.collidesWithPlayer) { // already checked (alreadyWithinU && A.collidesWithPlayer) earlier
                    return Collision.BLOCKED;
                }
            }
            return Collision.VOXEL_LEVEL;
        }
        if (C.collidesWithPlayer) {
            return Collision.BLOCKED;
        }
        if (!D.collidesWithPlayer) {
            return Collision.FALL;
        }
        if (Main.DEBUG && D.collisionHeightBlips() >= Blip.FULL_BLOCK && D.fullyWalkableTop) {
            throw new IllegalStateException();
        }
        if (D.collisionHeightBlips() < Blip.FULL_BLOCK + feet) {
            return Collision.FALL;
        } else {
            return Collision.BLOCKED;
        }
    }

    static {
        if (Blip.PLAYER_HEIGHT_SLIGHT_OVERESTIMATE >= Blip.TWO_BLOCKS || Blip.PLAYER_HEIGHT_SLIGHT_OVERESTIMATE + Blip.HALF_BLOCK <= Blip.TWO_BLOCKS) {
            throw new IllegalStateException("Assumptions made in playerTravelCollides");
        }
    }

    public enum Collision {
        BLOCKED, // if you hit W, you would not travel (example: walking into wall)
        VOXEL_UP, // if you hit W, you will end up at a position that's a bit higher, such that you'd determineRealPlayerSupport up by one (example: walking from a partial block to a full block or higher, e.g. half slab to full block, or soul sand to full block, or soul sand to full block+carpet on top)
        VOXEL_LEVEL, // if you hit W, you will end up at a similar position, such that you'd determineRealPlayerSupport at the same integer grid location (example: walking forward on level ground)
        FALL // if you hit W, you will not immediately collide with anything, at all, to the front or to the bottom (example: walking off a cliff)
        // TODO maybe we need another option that is like "you could do it, but you shouldn't". like, "if you hit W, you would walk forward, but you wouldn't like the outcome" such as cactus or lava or something
    }
}
