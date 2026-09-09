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

package baritone.utils.locate;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.core.registries.Registries;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.minecraft.world.level.levelgen.structure.BuiltinStructures.*;

/** Accepts registry identifiers, vanilla structure tags and readable aliases. */
public record StructureTarget(String name, int radius) {
    public StructureTarget {
        if (radius < BiomeSearch.STEP || radius > BiomeSearch.MAX_RADIUS) {
            throw new IllegalArgumentException("Radius must be between 32 and 12800 blocks.");
        }
        String id = name == null ? "" : name.startsWith("#") ? name.substring(1) : name;
        if (id.isEmpty() || Identifier.tryParse(id) == null) {
            throw new IllegalArgumentException("Expected a structure identifier or tag.");
        }
    }

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("endcity", END_CITY.identifier().getPath()), Map.entry("woodland_mansion", WOODLAND_MANSION.identifier().getPath()),
            Map.entry("ocean_monument", OCEAN_MONUMENT.identifier().getPath()), Map.entry("nether_fortress", "fortress"),
            Map.entry("jungle_temple", JUNGLE_TEMPLE.identifier().getPath()),
            Map.entry("bastion", "bastion_remnant"), Map.entry("desert_temple", "desert_pyramid"),
            Map.entry("witch_hut", "swamp_hut"), Map.entry("village", "#minecraft:village"),
            Map.entry("mineshaft", "#minecraft:mineshaft"), Map.entry("shipwreck", "#minecraft:shipwreck"),
            Map.entry("ruined_portal", "#minecraft:ruined_portal"), Map.entry("ocean_ruin", "#minecraft:ocean_ruin"));

    public static StructureTarget parse(String text) {
        String[] parts = text.trim().split("\\s+");
        int radius = BiomeSearch.DEFAULT_RADIUS;
        int count = parts.length;
        if (count > 1 && parts[count - 1].matches("[+-]?\\d+")) {
            try {
                radius = Integer.parseInt(parts[--count]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Radius must be between 32 and 12800 blocks.");
            }
        }
        if (radius < BiomeSearch.STEP || radius > BiomeSearch.MAX_RADIUS) {
            throw new IllegalArgumentException("Radius must be between 32 and 12800 blocks.");
        }
        String name = String.join("_", Arrays.copyOf(parts, count)).toLowerCase(Locale.ROOT);
        name = ALIASES.getOrDefault(name, name);
        String id = name.startsWith("#") ? name.substring(1) : name;
        if (id.isEmpty() || Identifier.tryParse(id) == null) {
            throw new IllegalArgumentException("Expected a structure name, for example end_city or village.");
        }
        return new StructureTarget(name, radius);
    }

    public Set<ResourceKey<Structure>> resolve(Registry<Structure> registry) {
        Identifier id = Identifier.parse(name.startsWith("#") ? name.substring(1) : name);
        if (name.startsWith("#")) {
            return registry.get(TagKey.create(Registries.STRUCTURE, id))
                    .orElseThrow(() -> new IllegalArgumentException("Unknown structure tag: " + name))
                    .stream().map(holder -> holder.unwrapKey().orElseThrow()).collect(Collectors.toSet());
        }
        return Set.of(registry.get(id).map(Holder.Reference::key)
                .orElseThrow(() -> new IllegalArgumentException("Unknown structure: " + name)));
    }

    public static Stream<String> suggestions() {
        return Stream.concat(builtinNames(), ALIASES.keySet().stream()).distinct();
    }

    private static Stream<String> builtinNames() {
        List<ResourceKey<Structure>> builtin = List.of(PILLAGER_OUTPOST, MINESHAFT, MINESHAFT_MESA,
                WOODLAND_MANSION, JUNGLE_TEMPLE, DESERT_PYRAMID, IGLOO, SHIPWRECK, SHIPWRECK_BEACHED,
                SWAMP_HUT, STRONGHOLD, OCEAN_MONUMENT, OCEAN_RUIN_COLD, OCEAN_RUIN_WARM, FORTRESS,
                NETHER_FOSSIL, END_CITY, BURIED_TREASURE, BASTION_REMNANT, VILLAGE_PLAINS, VILLAGE_DESERT,
                VILLAGE_SAVANNA, VILLAGE_SNOWY, VILLAGE_TAIGA, RUINED_PORTAL_STANDARD, RUINED_PORTAL_DESERT,
                RUINED_PORTAL_JUNGLE, RUINED_PORTAL_SWAMP, RUINED_PORTAL_MOUNTAIN, RUINED_PORTAL_OCEAN,
                RUINED_PORTAL_NETHER, ANCIENT_CITY, TRAIL_RUINS, TRIAL_CHAMBERS);
        return builtin.stream().map(key -> key.identifier().getPath());
    }

    public static Stream<String> complete(String raw) {
        String prefix = raw.toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
        int lastSpace = raw.lastIndexOf(' ');
        int consumed = lastSpace < 0 ? 0 : raw.substring(0, lastSpace + 1).replaceAll("\\s+", "_").length();
        Stream<String> names = suggestions();
        if (prefix.startsWith("minecraft:")) {
            names = builtinNames().map(name -> "minecraft:" + name);
        } else if (prefix.startsWith("#")) {
            names = ALIASES.values().stream().filter(name -> name.startsWith("#")).distinct();
        }
        return names.filter(name -> name.startsWith(prefix)).map(name -> name.substring(consumed)).sorted();
    }
}
