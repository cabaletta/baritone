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

import baritone.api.utils.BetterBlockPos;

public class PlayerPhysics {

    public static int determinePlayerRealSupportLevel(BlockStateCachedData underneath, BlockStateCachedData within, VoxelResidency residency) {
        switch (residency) {
            case STANDARD_WITHIN_SUPPORT:
                return within.collisionHeightBlips();
            case UNDERNEATH_PROTRUDES_AT_OR_ABOVE_FULL_BLOCK:
                return underneath.collisionHeightBlips() - Blip.FULL_BLOCK;
            default:
                return -1;
        }
    }

    public static int determinePlayerRealSupportLevel(BlockStateCachedData underneath, BlockStateCachedData within) {
        return determinePlayerRealSupportLevel(underneath, within, canPlayerStand(underneath, within));
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
            if (!underneath.collidesWithPlayer) {
                return VoxelResidency.FLOATING;
            }
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

    public static Collision playerTravelCollides(Column within, Column into) {
        return playerTravelCollides(within.feetBlips,
                within.above,
                into.above,
                into.head,
                into.feet,
                into.underneath,
                within.underneath,
                within.feet,
                within.aboveAbove,
                into.aboveAbove);
    }

    /**
     * "Can the player walk forwards without needing to break anything?"
     * <p>
     * Takes into account things like the automatic +0.5 from walking into a slab.
     * <p>
     * Player is standing at X (feet) Y (head) on top of S, intends to walk forwards into this ABCD column
     * EF
     * UA
     * YB
     * XC
     * SD
     */
    public static Collision playerTravelCollides(int feetBlips,
                                                 BlockStateCachedData U,
                                                 BlockStateCachedData A,
                                                 BlockStateCachedData B,
                                                 BlockStateCachedData C,
                                                 BlockStateCachedData D,
                                                 BlockStateCachedData S,
                                                 BlockStateCachedData X,
                                                 BlockStateCachedData E,
                                                 BlockStateCachedData F) {
        if (Main.DEBUG && (feetBlips < 0 || feetBlips >= Blip.FULL_BLOCK)) {
            throw new IllegalStateException();
        }
        if (Main.DEBUG && (feetBlips != determinePlayerRealSupportLevel(S, X))) {
            throw new IllegalStateException();
        }
        boolean alreadyWithinU = protrudesIntoThirdBlock(feetBlips);
        if (Main.DEBUG && (alreadyWithinU && U.collidesWithPlayer)) {
            throw new IllegalStateException();
        }
        int couldJumpUpTo = feetBlips + Blip.JUMP;
        int couldStepUpTo = feetBlips + Blip.HALF_BLOCK;
        if (couldJumpUpTo >= Blip.TWO_BLOCKS && !E.collidesWithPlayer && !F.collidesWithPlayer) {
            // probably blocked, but maybe could we stand on A?
            // imagine X is soul sand, A is carpet. that jump is possible
            int jumpUpTwo = determinePlayerRealSupportLevel(B, A);
            if (jumpUpTwo >= 0 && jumpUpTwo <= couldJumpUpTo - Blip.TWO_BLOCKS) {
                if (Main.DEBUG && (!alreadyWithinU || protrudesIntoThirdBlock(jumpUpTwo))) {
                    throw new IllegalStateException(); // numeric impossibilities
                }
                return Collision.JUMP_TO_VOXEL_TWO_UP;
            }
        }
        if (alreadyWithinU && A.collidesWithPlayer) {
            return Collision.BLOCKED; // we are too tall. bonk!
        }
        // D cannot prevent us from doing anything because it cant be higher than 1.5. therefore, makes sense to check CB before DC.
        int voxelUp = determinePlayerRealSupportLevel(C, B);
        if (voxelUp >= 0) {
            // fundamentally a step upwards, from X to B instead of X to C
            // too high?
            if (protrudesIntoThirdBlock(voxelUp) && (E.collidesWithPlayer || F.collidesWithPlayer)) {
                return Collision.BLOCKED;
            }
            int heightRelativeToStartVoxel = voxelUp + Blip.FULL_BLOCK;
            if (heightRelativeToStartVoxel > couldJumpUpTo) {
                return Collision.BLOCKED;
            }
            if (heightRelativeToStartVoxel > couldStepUpTo) {
                return Collision.JUMP_TO_VOXEL_UP;
            } // else this is possible!
            if (Main.DEBUG && (U.collidesWithPlayer || A.collidesWithPlayer || !alreadyWithinU)) {
                // must already be colliding with U because in order for this step to be even possible, feet must be at least HALF_BLOCK
                throw new IllegalStateException();
            }
            // B can collide with player here, such as if X is soul sand, C is full block, and B is carpet
            return Collision.VOXEL_UP; // A is already checked, so that's it!
        }
        // voxelUp is impossible. pessimistically, this means B is colliding. optimistically, this means B and C are air.
        if (B.collidesWithPlayer) {
            // AB can no longer be possible since it was checked with E and F
            return Collision.BLOCKED; // we have ruled out stepping on top of B, so now if B is still colliding there is no way forward
        }
        int stayLevel = determinePlayerRealSupportLevel(D, C);
        if (stayLevel >= 0) {
            // fundamentally staying within the same vertical voxel, X -> C
            if (protrudesIntoThirdBlock(stayLevel) && !alreadyWithinU) { // step up, combined with our height, protrudes into U and A, AND we didn't already
                if (U.collidesWithPlayer) { // stayLevel could even be LESS than feet
                    return Collision.BLOCKED;
                }
                if (A.collidesWithPlayer) { // already checked (alreadyWithinU && A.collidesWithPlayer) earlier
                    return Collision.BLOCKED;
                }
            }
            if (stayLevel > couldStepUpTo) { // staying within the same voxel means that a jump will always succeed
                return Collision.JUMP_TO_VOXEL_LEVEL;
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
        if (D.collisionHeightBlips() < Blip.FULL_BLOCK + feetBlips) {
            return Collision.FALL;
        } else {
            return Collision.BLOCKED;
        }
    }

    public static boolean protrudesIntoThirdBlock(int feet) {
        return feet > Blip.TWO_BLOCKS - Blip.PLAYER_HEIGHT_SLIGHT_OVERESTIMATE; // > and not >= because the player height is a slight overestimate
    }

    static {
        if (Blip.PLAYER_HEIGHT_SLIGHT_OVERESTIMATE >= Blip.TWO_BLOCKS || Blip.PLAYER_HEIGHT_SLIGHT_OVERESTIMATE + Blip.HALF_BLOCK <= Blip.TWO_BLOCKS) {
            throw new IllegalStateException("Assumptions made in playerTravelCollides");
        }
        int maxFeet = Blip.FULL_BLOCK - 1;
        int couldJumpUpTo = maxFeet + Blip.JUMP;
        int maxWithinAB = couldJumpUpTo - Blip.TWO_BLOCKS;
        if (protrudesIntoThirdBlock(maxWithinAB)) {
            throw new IllegalStateException("Oh no, if this is true then playerTravelCollides needs to check another layer above EF");
        }
    }

    public enum Collision {
        // TODO maybe we need another option that is like "you could do it, but you shouldn't". like, "if you hit W, you would walk forward, but you wouldn't like the outcome" such as cactus or lava or something

        BLOCKED, // if you hit W, you would not travel (example: walking into wall)
        JUMP_TO_VOXEL_LEVEL, // blocked, BUT, if you jumped, you would end up at voxel level. this one is rare, it could only happen if you jump onto a block that is between 0.5 and 1.0 blocks high, such as 7-high snow layers
        JUMP_TO_VOXEL_UP, // blocked, BUT, if you jumped, you would end up at one voxel higher. this is the common case for jumping.
        JUMP_TO_VOXEL_TWO_UP, // blocked, BUT, if you jumped, you would end up two voxels higher. this can only happen for weird blocks like jumping out of soul sand and up one
        VOXEL_UP, // if you hit W, you will end up at a position that's a bit higher, such that you'd determineRealPlayerSupport up by one (example: walking from a partial block to a full block or higher, e.g. half slab to full block, or soul sand to full block, or soul sand to full block+carpet on top)
        VOXEL_LEVEL, // if you hit W, you will end up at a similar position, such that you'd determineRealPlayerSupport at the same integer grid location (example: walking forward on level ground)
        FALL; // if you hit W, you will not immediately collide with anything, at all, to the front or to the bottom (example: walking off a cliff)

        public int voxelVerticalOffset() {
            switch (this) {
                case VOXEL_LEVEL:
                case JUMP_TO_VOXEL_LEVEL:
                    return 0;
                case VOXEL_UP:
                case JUMP_TO_VOXEL_UP:
                    return 1;
                case JUMP_TO_VOXEL_TWO_UP:
                    return 2;
                default:
                    throw new IllegalStateException();
            }
        }

        public boolean requiresJump() {
            switch (this) {
                case VOXEL_LEVEL:
                case VOXEL_UP:
                    return false;
                case JUMP_TO_VOXEL_LEVEL:
                case JUMP_TO_VOXEL_UP:
                case JUMP_TO_VOXEL_TWO_UP:
                    return true;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    public static int playerFalls(long newPos, WorldState worldState, SolverEngineInput engineInput) {
        // this means that there is nothing preventing us from walking forward and falling
        // iterate downwards to see what we would hit
        for (int descent = 0; ; descent++) {
            // NOTE: you cannot do (descent*Face.DOWN.offset)&BetterBlockPos.POST_ADDITION_MASK because Y is serialized into the center of the long. but I suppose you could do it with X. hm maybe Y should be moved to the most significant bits purely to allow this :^)
            long support = BetterBlockPos.offsetBy(newPos, 0, -descent, 0);
            long under = Face.DOWN.offset(support);
            if (Main.DEBUG && !engineInput.bounds.inRangePos(under)) {
                throw new IllegalStateException(); // should be caught by PREVENTED_BY_UNDERNEATH
            }
            VoxelResidency res = canPlayerStand(engineInput.at(under, worldState), engineInput.at(support, worldState));
            if (Main.DEBUG && descent == 0 && res != VoxelResidency.FLOATING) {
                throw new IllegalStateException(); // CD shouldn't collide, it should be D and the one beneath...
            }
            switch (res) {
                case FLOATING:
                    continue; // as expected
                case PREVENTED_BY_UNDERNEATH:
                case PREVENTED_BY_WITHIN:
                    return -1; // no safe landing
                case UNDERNEATH_PROTRUDES_AT_OR_ABOVE_FULL_BLOCK:
                case STANDARD_WITHIN_SUPPORT:
                    // found our landing spot
                    if (Main.DEBUG && descent <= 0) {
                        throw new IllegalStateException();
                    }
                    return descent;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    /**
     * If the player feet is greater than the return from this function, the player can sneak out at that altitude without colliding with this column
     * <p>
     * If there is no collision possible this will return a negative number (which should fit in fine with the above ^ use case)
     */
    public static int highestCollision(BlockStateCachedData underneath, BlockStateCachedData within) {
        return Math.max(
                underneath.collidesWithPlayer ? underneath.collisionHeightBlips() - Blip.FULL_BLOCK : -1,
                within.collidesWithPlayer ? within.collisionHeightBlips() : -1
        );
    }
}
