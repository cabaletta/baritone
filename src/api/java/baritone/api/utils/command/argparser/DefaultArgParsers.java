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

package baritone.api.utils.command.argparser;

import baritone.api.utils.command.argument.CommandArgument;

import java.util.List;
import java.util.Locale;

import static java.util.Arrays.asList;

public class DefaultArgParsers {
    public static class IntArgumentParser extends ArgParser<Integer> implements IArgParser.Stateless<Integer> {
        public static final IntArgumentParser INSTANCE = new IntArgumentParser();

        public IntArgumentParser() {
            super(Integer.class);
        }

        @Override
        public Integer parseArg(CommandArgument arg) throws RuntimeException {
            return Integer.parseInt(arg.value);
        }
    }

    public static class LongArgumentParser extends ArgParser<Long> implements IArgParser.Stateless<Long> {
        public static final LongArgumentParser INSTANCE = new LongArgumentParser();

        public LongArgumentParser() {
            super(Long.class);
        }

        @Override
        public Long parseArg(CommandArgument arg) throws RuntimeException {
            return Long.parseLong(arg.value);
        }
    }

    public static class FloatArgumentParser extends ArgParser<Float> implements IArgParser.Stateless<Float> {
        public static final FloatArgumentParser INSTANCE = new FloatArgumentParser();

        public FloatArgumentParser() {
            super(Float.class);
        }

        @Override
        public Float parseArg(CommandArgument arg) throws RuntimeException {
            String value = arg.value;

            if (!value.matches("^([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)|)$")) {
                throw new RuntimeException("failed float format check");
            }

            return Float.parseFloat(value);
        }
    }

    public static class DoubleArgumentParser extends ArgParser<Double> implements IArgParser.Stateless<Double> {
        public static final DoubleArgumentParser INSTANCE = new DoubleArgumentParser();

        public DoubleArgumentParser() {
            super(Double.class);
        }

        @Override
        public Double parseArg(CommandArgument arg) throws RuntimeException {
            String value = arg.value;

            if (!value.matches("^([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)|)$")) {
                throw new RuntimeException("failed double format check");
            }

            return Double.parseDouble(value);
        }
    }

    public static class BooleanArgumentParser extends ArgParser<Boolean> implements IArgParser.Stateless<Boolean> {
        public static final BooleanArgumentParser INSTANCE = new BooleanArgumentParser();

        public static final List<String> TRUTHY_VALUES = asList("1", "true", "yes", "t", "y", "on", "enable");
        public static final List<String> FALSY_VALUES = asList("0", "false", "no", "f", "n", "off", "disable");

        public BooleanArgumentParser() {
            super(Boolean.class);
        }

        @Override
        public Boolean parseArg(CommandArgument arg) throws RuntimeException {
            String value = arg.value;

            if (TRUTHY_VALUES.contains(value.toLowerCase(Locale.US))) {
                return true;
            } else if (FALSY_VALUES.contains(value.toLowerCase(Locale.US))) {
                return false;
            } else {
                throw new RuntimeException("invalid boolean");
            }
        }
    }

    public static final List<ArgParser<?>> all = asList(
        IntArgumentParser.INSTANCE,
        LongArgumentParser.INSTANCE,
        FloatArgumentParser.INSTANCE,
        DoubleArgumentParser.INSTANCE,
        BooleanArgumentParser.INSTANCE
    );
}
