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

public interface IBlockStateDataProvider {

    int numStates();

    BlockStateCachedData getNullable(int i);

    default BlockStateCachedData[] allNullable() {
        BlockStateCachedData[] ret = new BlockStateCachedData[numStates()];
        RuntimeException ex = null;
        for (int i = 0; i < ret.length; i++) {
            try {
                ret[i] = getNullable(i);
            } catch (RuntimeException e) {
                if (ex != null) {
                    ex.printStackTrace(); // printstacktrace all but the one that we throw
                }
                ex = e;
            }
        }
        if (ex != null) {
            throw ex; // throw the last one
        }
        return ret;
    }
}
