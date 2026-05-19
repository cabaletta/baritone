package adris.altoclef.commandsystem;

public class CommandException extends Exception {

    public CommandException(String message) {
        super(message);
    }

    public CommandException(String message, Exception child) {
        super(message, child);
    }
}
