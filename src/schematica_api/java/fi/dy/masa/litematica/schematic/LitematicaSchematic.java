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

package fi.dy.masa.litematica.schematic;

import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import net.minecraft.core.BlockPos;

public class LitematicaSchematic {

    public LitematicaBlockStateContainer getSubRegionContainer(String name) {
        throw new LinkageError();
    }

    public BlockPos getAreaSize(String name) {
        throw new LinkageError();
    }
}