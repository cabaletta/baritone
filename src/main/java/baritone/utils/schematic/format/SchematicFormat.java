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

package baritone.utils.schematic.format;

import baritone.utils.schematic.parse.ISchematicParser;
import baritone.utils.schematic.parse.MCEditParser;
import baritone.utils.schematic.parse.SpongeParser;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Brady
 * @since 12/13/2019
 */
public enum SchematicFormat {

    /**
     * The MCEdit schematic specification. Commonly denoted by the ".schematic" file extension.
     */
    MCEDIT("schematic", MCEditParser.INSTANCE),

    /**
     * The SpongePowered Schematic Specification. Commonly denoted by the ".schem" file extension.
     *
     * @see <a href="https://github.com/SpongePowered/Schematic-Specification">Sponge Schematic Specification</a>
     */
    SPONGE("schem", SpongeParser.INSTANCE);

    private final String extension;
    private final ISchematicParser parser;

    SchematicFormat(String extension, ISchematicParser parser) {
        this.extension = extension;
        this.parser = parser;
    }

    public final ISchematicParser getParser() {
        return this.parser;
    }

    public static Optional<SchematicFormat> getByFile(File schematic) {
        // TODO: Better identification
        // Maybe peek file contents and make a safe determination?
        return getByExtension(FilenameUtils.getExtension(schematic.getAbsolutePath()));
    }

    public static Optional<SchematicFormat> getByExtension(String extension) {
        return extension == null || extension.isEmpty()
            ? Optional.empty()
            : Stream.of(values()).filter(format -> format.extension.equalsIgnoreCase(extension)).findFirst();
    }
}
