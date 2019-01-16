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

package baritone.api.process;

/**
 * Very simple and limited API exposure of BuilderProcess.
 */
public interface IBuilderProcess extends IBaritoneProcess {

    /**
     * Build from a schematic file inside of .minecraft/schematics
     *
     * @param schematicFile The name of the schematic file
     * @return Success or failure
     */
    boolean build(String schematicFile);

    /**
     * Clear a specified XYZ area
     */
    void clearArea(int clearX, int clearY, int clearZ);

    default void cancel() {
        this.onLostControl();
    }
}