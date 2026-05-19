package adris.altoclef.commandsystem;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;

public abstract class Command {

    private final ArgParser parser;
    private final String _name;
    private final String _description;
    private AltoClef _mod;
    private Runnable _onFinish = null;

    public Command(String name, String description, ArgBase... args) {
        _name = name;
        _description = description;
        parser = new ArgParser(args);
    }

    public void run(AltoClef mod, String line, Runnable onFinish) throws CommandException {
        _onFinish = onFinish;
        _mod = mod;
        parser.loadArgs(line, true);
        call(mod, parser);
    }

    protected void finish() {
        if (_onFinish != null)
            //noinspection unchecked
            _onFinish.run();
    }

    public String getHelpRepresentation() {
        StringBuilder sb = new StringBuilder(_name);
        for (ArgBase arg : parser.getArgs()) {
            sb.append(" ");
            sb.append(arg.getHelpRepresentation());
        }
        return sb.toString();
    }

    protected void log(Object message) {
        Debug.logMessage(message.toString());
    }

    protected void logError(Object message) {
        Debug.logError(message.toString());
    }

    protected abstract void call(AltoClef mod, ArgParser parser) throws CommandException;

    public String getName() {
        return _name;
    }

    public String getDescription() {
        return _description;
    }
}
