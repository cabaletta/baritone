package adris.altoclef.commandsystem;

import java.util.ArrayList;
import java.util.List;

public class ArgParser {
    private final ArgBase[] _args;
    int argCounter;
    int unitCounter;
    String[] argUnits;

    public ArgParser(ArgBase... args) {
        this._args = args;
        argCounter = 0;
        unitCounter = 0;
    }

    // Given a single line as a String, parse it into a list of keywords
    public static List<String> splitLineIntoKeywords(String line) {
        List<String> result = new ArrayList<String>();
        // By default, it's just spaces. But sometimes we want to count quotes. So do it manually.
        String last_kword = "";
        boolean open_quote = false;
        char prev_char = '\0';
        for (char c : line.toCharArray()) {
            // We found a quote, update our "quote" state.
            if (c == '\"') {
                open_quote = !open_quote;
            }
            if (prev_char == '\\') {
                if (c == '#' || c == '"') {
                    // We escaped this pound sign, so ignore the escaping backslash
                    last_kword = last_kword.substring(0, last_kword.length() - 1);
                }
            } else {
                if (c == '#') {
                    // Bail! Everything beyond this is part of a comment, so ignore.
                    break;
                }
            }
            if (c == ' ' && !open_quote) {
                // If it's empty, just ignore.
                if (last_kword.length() != 0) {
                    // Remove trailing whitespace
                    result.add(last_kword.trim());
                }
                last_kword = "";
            } else {
                // We don't care about speed here.
                //noinspection StringConcatenationInLoop
                last_kword += c;
            }
            prev_char = c;
        }
        // Add the remainder
        if (last_kword.length() != 0) {
            result.add(last_kword.trim());
        }
        return result;
    }

    public void loadArgs(String line, boolean removeFirst) {
        List<String> units = splitLineIntoKeywords(line);
        // Discard the first element since, well, it will always be the name of the command.
        if (removeFirst && units.size() != 0) {
            units.remove(0);
        }
        argUnits = new String[units.size()];
        units.toArray(argUnits);
        argCounter = 0;
        unitCounter = 0;
    }

    // Get the next argument.
    public <T> T get(Class<T> type) throws CommandException {

        if (argCounter >= _args.length) {
            throw new CommandException("You tried grabbing more arguments than you had... Bad move.");
        }
        ArgBase arg = _args[argCounter];
        if (!arg.isArbitrarilyLong() && argUnits.length > _args.length) {
            throw new CommandException(String.format("Too many arguments provided %d. The maximum is %d.", argUnits.length, _args.length));
        }

        // Current values from arrays
        ++argCounter;
        if (arg.isArray()) {
            argCounter = _args.length;
        }

        // If this can be default and we don't have enough (unit) args provided to use this arg, use the default value instead of reading from our arg list.
        int givenArgs = argUnits.length;
        if (arg.hasDefault() && arg.getMinArgCountToUseDefault() >= givenArgs) {
            return arg.getDefault(type);
        }

        if (unitCounter >= argUnits.length) {
            throw new CommandException(String.format("Not enough arguments supplied: You supplied %d.", argUnits.length));
        }

        String unit = argUnits[unitCounter];
        String[] unitPlusRemaining = new String[argUnits.length - unitCounter];
        System.arraycopy(argUnits, unitCounter, unitPlusRemaining, 0, unitPlusRemaining.length);
        //Array.Copy(argUnits, unitCounter, unitPlusRemaining, 0, unitPlusRemaining.Length);
        //argUnits.CopyTo(unitPlusRemaining, unitCounter);

        ++unitCounter;

        // If our type is not valid, try um handling the defaults.

        return arg.parseUnit(unit, unitPlusRemaining);
    }

    public ArgBase[] getArgs() {
        return _args;
    }

    // Dear god kill this system already
    public String[] getArgUnits() {
        return argUnits;
    }
}
