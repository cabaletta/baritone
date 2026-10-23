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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone. If not, see <https://www.gnu.org/licenses/>.
 */
package baritone.api.command.helpers;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TabCompleteHelperTest {
    private static final String[] IDS = {
            "minecraft:diamond_ore", "minecraft:stone",
            "example:diamond_ore", "example:ore/cobalt",
            "another_mod:liveroot_block", "another_mod:mining_log_core"
    };

    private List<String> complete(String prefix) {
        return new TabCompleteHelper(IDS)
                .filterPrefixNamespaced(prefix)
                .sortAlphabetically()
                .stream()
                .collect(Collectors.toList());
    }

    @Test
    public void emptyPrefixIncludesEveryNamespace() {
        List<String> expected = Arrays.stream(IDS).sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
        assertEquals(expected, complete(""));
    }

    @Test
    public void barePathMatchesModdedAndVanillaIds() {
        assertEquals(Arrays.asList("another_mod:liveroot_block"), complete("live"));
        assertEquals(Arrays.asList("example:diamond_ore", "minecraft:diamond_ore"), complete("diamond"));
    }

    @Test
    public void partialNamespaceDoesNotNeedAColon() {
        assertEquals(Arrays.asList("another_mod:liveroot_block", "another_mod:mining_log_core"), complete("another"));
        assertEquals(Arrays.asList("example:diamond_ore", "example:ore/cobalt"), complete("examp"));
    }

    @Test
    public void explicitNamespaceNeverBroadens() {
        assertEquals(Arrays.asList("minecraft:diamond_ore"), complete("minecraft:diamond"));
        assertEquals(Arrays.asList("example:diamond_ore"), complete("example:diamond"));
        assertEquals(Arrays.asList("example:diamond_ore", "example:ore/cobalt"), complete("example:"));
    }

    @Test
    public void explicitEmptyNamespaceKeepsMinecraftDefault() {
        assertEquals(Arrays.asList("minecraft:stone"), complete(":stone"));
    }

    @Test
    public void supportsSlashAndUnderscorePaths() {
        assertEquals(Arrays.asList("example:ore/cobalt"), complete("ore/"));
        assertEquals(Arrays.asList("example:ore/cobalt"), complete("example:ore/"));
        assertEquals(Arrays.asList("another_mod:mining_log_core"), complete("mining_"));
    }

    @Test
    public void invalidAndMissingPrefixesAreEmpty() {
        for (String prefix : Arrays.asList("bad prefix", "bad@id", "example:path:bad", "UpperCase", "absent:", "no_match")) {
            assertTrue(prefix, complete(prefix).isEmpty());
        }
    }

    @Test
    public void fullyQualifiedCandidatesRemainUnambiguous() {
        assertEquals(1, complete("example:diamond_ore").size());
        assertTrue(complete("").stream().allMatch(id -> id.contains(":")));
    }

    @Test
    public void matchingIsIndependentOfDefaultLocale() {
        Locale saved = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals(Arrays.asList("another_mod:mining_log_core"), complete("mining"));
        } finally {
            Locale.setDefault(saved);
        }
    }

    @Test
    public void ordinaryCommandPrefixFilterIsUnchanged() {
        assertEquals(Arrays.asList("MiNe"), new TabCompleteHelper(new String[]{"MiNe", "pickup"})
                .filterPrefix("mi").stream().collect(Collectors.toList()));
    }
}
