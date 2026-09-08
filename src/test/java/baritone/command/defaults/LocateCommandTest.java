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

package baritone.command.defaults;

import baritone.api.command.exception.CommandException;
import baritone.api.IBaritone;
import baritone.api.utils.IPlayerContext;
import baritone.api.pathing.calc.IPathingControlManager;
import baritone.command.argument.ArgConsumer;
import baritone.command.argument.CommandArguments;
import org.junit.Test;
import org.mockito.MockMakers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LocateCommandTest {
    @Test
    public void structuresRequireExplicitSubcommand() throws CommandException {
        IBaritone baritone = mock(IBaritone.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        when(baritone.getPlayerContext()).thenReturn(mock(IPlayerContext.class, withSettings().mockMaker(MockMakers.SUBCLASS)));
        when(baritone.getPathingControlManager()).thenReturn(mock(IPathingControlManager.class, withSettings().mockMaker(MockMakers.SUBCLASS)));
        LocateCommand command = new LocateCommand(baritone);
        assertEquals(java.util.List.of("biome", "structure", "seed", "preset", "cancel"), command.tabComplete("locate", new ArgConsumer(null, CommandArguments.from("", true))).toList());
        assertTrue(command.tabComplete("locate", new ArgConsumer(null, CommandArguments.from("end c", true))).toList().isEmpty());
        assertEquals(java.util.List.of("city"), command.tabComplete("locate", new ArgConsumer(null, CommandArguments.from("structure end c", true))).toList());
        assertEquals(java.util.List.of("end_city"), command.tabComplete("locate", new ArgConsumer(null, CommandArguments.from("structure end_", true))).toList());
        for (String shorthand : java.util.List.of("end city", "village", "stronghold")) {
            CommandException error = assertThrows(CommandException.class,
                    () -> command.execute("locate", new ArgConsumer(null, CommandArguments.from(shorthand))));
            assertTrue(error.getMessage().contains("#locate structure <name>"));
        }
    }

    private long parse(String text) throws CommandException {
        ArgConsumer args = new ArgConsumer(null, CommandArguments.from(text));
        long seed = LocateCommand.parseSeed(args);
        assertFalse(args.hasAny());
        return seed;
    }

    @Test
    public void textSeedsPreserveCaseAndInternalWhitespace() throws CommandException {
        assertEquals(1208984482L, parse("oogabooga"));
        assertEquals((long) "North  Carolina".hashCode(), parse("  North  Carolina  "));
        assertNotEquals(parse("oogabooga"), parse("Oogabooga"));
        assertNotEquals(parse("North Carolina"), parse("North  Carolina"));
    }

    @Test
    public void numericSeedsKeepAll64Bits() throws CommandException {
        assertEquals(0L, parse("0"));
        assertEquals(Long.MAX_VALUE, parse(Long.toString(Long.MAX_VALUE)));
        assertEquals(Long.MIN_VALUE, parse(Long.toString(Long.MIN_VALUE)));
        // Like Minecraft's world-creation screen, an overflowing number is a text seed.
        assertEquals((long) "9223372036854775808".hashCode(), parse("9223372036854775808"));
        assertThrows(CommandException.class, () -> parse("   "));
    }
}
