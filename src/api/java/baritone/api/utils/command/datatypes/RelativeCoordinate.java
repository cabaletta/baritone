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

import baritone.api.utils.command.exception.CommandException;
import baritone.api.utils.command.helpers.arguments.IArgConsumer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public enum RelativeCoordinate implements IDatatypePost<Double, Double> {
    INSTANCE;

    private static Pattern PATTERN = Pattern.compile("^(~?)([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)|)$");

    @Override
    public Double apply(IDatatypeContext ctx, Double origin) throws CommandException {
        if (origin == null) {
            origin = 0.0D;
        }

        Matcher matcher = PATTERN.matcher(ctx.getConsumer().getString());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("pattern doesn't match");
        }

        boolean isRelative = !matcher.group(1).isEmpty();
        double offset = matcher.group(2).isEmpty() ? 0 : Double.parseDouble(matcher.group(2));

        if (isRelative) {
            return origin + offset;
        }
        return offset;
    }

    @Override
    public Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException {
        final IArgConsumer consumer = ctx.getConsumer();
        if (!consumer.has(2) && consumer.getString().matches("^(~|$)")) {
            return Stream.of("~");
        }
        return Stream.empty();
    }
}
