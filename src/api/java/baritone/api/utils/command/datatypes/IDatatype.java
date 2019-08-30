package baritone.api.utils.command.datatypes;

import baritone.api.utils.command.exception.CommandInvalidArgumentException;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;

import java.util.stream.Stream;

/**
 * Since interfaces cannot enforce the presence of a constructor, it's on you to make sure there is a constructor that
 * accepts a single {@link ArgConsumer} argument. The constructor will perform all needed validation, and
 * {@link ArgConsumer#getDatatype(Class)} will handle RuntimeExceptions and translate them into
 * {@link CommandInvalidArgumentException}s. There must always be a constructor with no arguments so that
 * {@link ArgConsumer} can create an instance for tab completion.
 */
public interface IDatatype {
    Stream<String> tabComplete(ArgConsumer consumer);
}
