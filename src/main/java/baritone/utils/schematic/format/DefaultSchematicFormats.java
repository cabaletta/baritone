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

import baritone.api.schematic.IStaticSchematic;
import baritone.api.schematic.format.ISchematicFormat;
import baritone.utils.schematic.format.defaults.LitematicaSchematic;
import baritone.utils.schematic.format.defaults.MCEditSchematic;
import baritone.utils.schematic.format.defaults.SpongeSchematic;
import net.minecraft.nbt.NbtAccounter;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

/**
 * Default implementations of {@link ISchematicFormat}
 *
 * @author Brady
 * @since 12/13/2019
 */
public enum DefaultSchematicFormats implements ISchematicFormat {

    /**
     * The MCEdit schematic specification. Commonly denoted by the ".schematic" file extension.
     */
    MCEDIT("schematic") {
        @Override
        public IStaticSchematic parse(InputStream input) throws IOException {
            return new MCEditSchematic(NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap()));
        }
    },

    /**
     * The SpongePowered Schematic Specification. Commonly denoted by the ".schem" file extension.
     *
     * @see <a href="https://github.com/SpongePowered/Schematic-Specification">Sponge Schematic Specification</a>
     */
    SPONGE("schem") {
        @Override
        public IStaticSchematic parse(InputStream input) throws IOException {
            CompoundTag nbt = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
            int version = nbt.getInt("Version");
            switch (version) {
                case 1:
                case 2:
                    return new SpongeSchematic(nbt);
                default:
                    throw new UnsupportedOperationException("Unsupported Version of a Sponge Schematic");
            }
        }
    },

    /**
     * The Litematica schematic specification. Commonly denoted by the ".litematic" file extension.
     */
    LITEMATICA("litematic") {
        @Override
        public IStaticSchematic parse(InputStream input) throws IOException {
            CompoundTag nbt = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
            int version = nbt.getInt("Version");
            switch (version) {
                case 4: //1.12
                case 5: //1.13-1.17
                    throw new UnsupportedOperationException("This litematic Version is too old.");
                case 6: //1.18+
                    return new LitematicaSchematic(nbt, false);
                default:
                    throw new UnsupportedOperationException("Unsuported Version of a Litematica Schematic");
            }
        }
    };

    private final String extension;

    DefaultSchematicFormats(String extension) {
        this.extension = extension;
    }

    @Override
    public boolean isFileType(File file) {
        return this.extension.equalsIgnoreCase(FilenameUtils.getExtension(file.getAbsolutePath()));
    }
}
