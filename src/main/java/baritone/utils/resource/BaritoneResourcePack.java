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

package baritone.utils.resource;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

/**
 * @author Brady
 * @since 12/15/2018
 */
public class BaritoneResourcePack implements IResourcePack {

    @Override
    public InputStream getInputStream(@Nonnull ResourceLocation location) {
        return getResourceStream(location);
    }

    @Override
    public boolean resourceExists(@Nonnull ResourceLocation location) {
        return getResourceStream(location) != null;
    }

    @Nonnull
    @Override
    public Set<String> getResourceDomains() {
        return Collections.singleton("baritone");
    }

    @Nullable
    @Override
    public <T extends IMetadataSection> T getPackMetadata(@Nonnull MetadataSerializer metadataSerializer, @Nonnull String metadataSectionName) throws IOException {
        return null;
    }

    @Override
    public BufferedImage getPackImage() {
        return null;
    }

    @Nonnull
    @Override
    public String getPackName() {
        return "baritone";
    }

    @Nullable
    private InputStream getResourceStream(ResourceLocation location) {
        return BaritoneResourcePack.class.getResourceAsStream("/assets/" + location.getNamespace() + "/" + location.getPath());
    }
}
