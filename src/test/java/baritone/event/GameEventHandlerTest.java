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

package baritone.event;

import baritone.api.event.listener.AbstractGameEventListener;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class GameEventHandlerTest {

    @Test
    public void testListenerOrder() {
        GameEventHandler bus = new GameEventHandler(null /* baritone */);

        List<String> output = new ArrayList<>();

        bus.registerEventListener(0, new TestEventListener("0 0", output));
        bus.registerEventListener(0, new TestEventListener("0 1", output));
        bus.registerEventListener(1, new TestEventListener("1 2", output));
        bus.registerEventListener(new TestEventListener("_ 3", output));
        bus.registerEventListener(-1, new TestEventListener("-1 4", output));
        bus.registerEventListener(1, new TestEventListener("1 5", output));
        bus.registerEventListener(0, new TestEventListener("0 6", output));

        bus.onPlayerDeath();

        assertEquals(new ArrayList<>(Arrays.asList(
                "1 2", "1 5", "0 0", "0 1", "_ 3", "0 6", "-1 4"
                )),
                output
        );
    }

    private static class TestEventListener implements AbstractGameEventListener {
        private final String id;
        private final List<String> output;

        public TestEventListener(String id, List<String> output) {
            this.id = id;
            this.output = output;
        }

        @Override
        public void onPlayerDeath() { // the only event without an argument ☺
            output.add(id);
        }
    }
}
