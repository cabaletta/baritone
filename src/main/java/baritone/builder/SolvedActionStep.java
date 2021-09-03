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

import java.util.OptionalLong;

public class SolvedActionStep {

    private final long placePosition;
    private final long playerEndPosition;
    private final Face towardsSneakSupport;

    public SolvedActionStep(long posAndSneak) {
        this(posAndSneak, -1);
    }

    public SolvedActionStep(long posAndSneak, long blockPlacedAt) {
        this.playerEndPosition = posAndSneak & BetterBlockPos.POST_ADDITION_MASK;
        this.placePosition = blockPlacedAt;
        this.towardsSneakSupport = SneakPosition.sneakDirectionFromPlayerToSupportingBlock(posAndSneak);
        if (Main.DEBUG && blockPlacedAt < -1) {
            throw new IllegalStateException();
        }
    }

    public OptionalLong placeAt() {
        return placePosition == -1 ? OptionalLong.empty() : OptionalLong.of(placePosition);
    }

    public long playerMovesTo() {
        return playerEndPosition;
    }

    public Face towardsSupport() {
        return towardsSneakSupport;
    }

    public boolean sneaking() {
        return towardsSneakSupport != null;
    }
}
