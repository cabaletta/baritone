package adris.altoclef.commandsystem;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;

import java.util.Collection;
import java.util.HashMap;
import java.util.function.Consumer;

public class CommandExecutor {

    private final HashMap<String, Command> _commandSheet = new HashMap<>();
    private final AltoClef _mod;

    public CommandExecutor(AltoClef mod) {
        _mod = mod;
    }

    public void registerNewCommand(Command... commands) {
        for (Command command : commands) {
            if (_commandSheet.containsKey(command.getName())) {
                Debug.logInternal("Command with name " + command.getName() + " already exists! Can't register that name twice.");
                continue;
            }
            _commandSheet.put(command.getName(), command);
        }
    }

    private String getCommandPrefix() {
        return _mod.getModSettings().getCommandPrefix();
    }

    public boolean isClientCommand(String line) {
        return line.startsWith(getCommandPrefix());
    }

    // This is how we "nest" command finishes so we can complete them in order.
    private void executeRecursive(Command[] commands, String[] parts, int index, Runnable onFinish, Consumer<CommandException> getException) {
        if (index >= commands.length) {
            onFinish.run();
            return;
        }
        Command command = commands[index];
        String part = parts[index];
        try {
            if (command == null) {
                getException.accept(new CommandException("Invalid command:" + part));
                executeRecursive(commands, parts, index + 1, onFinish, getException);
            } else {
                command.run(_mod, part, () -> executeRecursive(commands, parts, index + 1, onFinish, getException));
            }
        } catch (CommandException ae) {
            getException.accept(new CommandException(ae.getMessage() + "\nUsage: " + command.getHelpRepresentation(), ae));
        }
    }

    public void execute(String line, Runnable onFinish, Consumer<CommandException> getException) {
        if (!isClientCommand(line)) return;
        line = line.substring(getCommandPrefix().length());
        // Run commands separated by ;
        String[] parts = line.split(";");
        Command[] commands = new Command[parts.length];
        try {
            for (int i = 0; i < parts.length; ++i) {
                commands[i] = getCommand(parts[i]);
            }
        } catch (CommandException e) {
            getException.accept(e);
        }
        executeRecursive(commands, parts, 0, onFinish, getException);
    }

    public void execute(String line, Consumer<CommandException> getException) {
        execute(line, () -> {
        }, getException);
    }

    public void execute(String line) {
        execute(line, ex -> Debug.logWarning(ex.getMessage()));
    }

    public void executeWithPrefix(String line) {
        if (!line.startsWith(getCommandPrefix())) {
            line = getCommandPrefix() + line;
        }
        execute(line);
    }

    private Command getCommand(String line) throws CommandException {
        line = line.trim();
        if (line.length() != 0) {
            String command = line;
            int firstSpace = line.indexOf(' ');
            if (firstSpace != -1) {
                command = line.substring(0, firstSpace);
            }

            if (!_commandSheet.containsKey(command)) {
                throw new CommandException("Command " + command + " does not exist.");
            }

            return _commandSheet.get(command);
        }
        return null;

    }

    public Collection<Command> allCommands() {
        return _commandSheet.values();
    }

    public Command get(String name) {
        return (_commandSheet.getOrDefault(name, null));
    }
}
