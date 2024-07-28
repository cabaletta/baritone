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

package fi.dy.masa.litematica.schematic.placement;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class SchematicPlacementManager {
    private final List<SchematicPlacement> schematicPlacements = new ArrayList<>();

    @Nullable
    private SchematicPlacement selectedPlacement;

    //in case of a java.lang.NoSuchMethodError try change the name of this method to getAllSchematicPlacements()
    //there are inconsistencies in the litematica mod about the naming of this method
    public List<SchematicPlacement> getAllSchematicsPlacements() {
        return schematicPlacements;
    }

    public SchematicPlacement getSelectedSchematicPlacement() {
        return selectedPlacement;
    }
}