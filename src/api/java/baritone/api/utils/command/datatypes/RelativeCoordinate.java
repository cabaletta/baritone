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

package baritone.api.utils.command.datatypes;

import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import net.minecraft.util.math.MathHelper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class RelativeCoordinate implements IDatatypePost<Double, Double> {
    public static Pattern PATTERN = Pattern.compile("^(~?)([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)|)$");

    final boolean isRelative;
    final double offset;

    public RelativeCoordinate() {
        isRelative = true;
        offset = 0;
    }

    public RelativeCoordinate(ArgConsumer consumer) {
        if (!consumer.has()) {
            throw new RuntimeException("relative coordinate requires an argument");
        }

        Matcher matcher = PATTERN.matcher(consumer.getString());

        if (!matcher.matches()) {
            throw new RuntimeException("pattern doesn't match");
        }

        isRelative = !matcher.group(1).isEmpty();
        offset = matcher.group(2).isEmpty() ? 0 : Double.parseDouble(matcher.group(2));
    }

    @Override
    public Double apply(Double origin) {
        if (isRelative) {
            return origin + offset;
        }

        return offset;
    }

    public int applyFloor(double origin) {
        return MathHelper.floor(apply(origin));
    }

    @Override
    public Stream<String> tabComplete(ArgConsumer consumer) {
        if (!consumer.has(2) && consumer.getString().matches("^(~|$)")) {
            return Stream.of("~");
        }

        return Stream.empty();
    }
}
