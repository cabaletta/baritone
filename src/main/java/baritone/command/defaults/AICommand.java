package baritone.command.defaults;

import baritone.api.IBaritone;
import baritone.api.command.Command;
import baritone.api.command.argument.IArgParserManager;
import baritone.api.command.exception.CommandException;

import java.util.List;
import java.util.stream.Stream;

public class AICommand extends Command {

    public AICommand(IBaritone baritone) {
        super(baritone, "ai");
    }

    @Override
    public void execute(String label, IArgParserManager parserManager, baritone.api.command.argument.IArgConsumer args) throws CommandException {
        if (!args.hasAny()) {
            throw new CommandException("Usage: #ai <prompt|stop|load|save>");
        }
        String prompt = args.getString();
        while (args.hasAny()) {
            prompt += " " + args.getString();
        }

        if (prompt.equalsIgnoreCase("stop")) {
            baritone.getAIProcess().stop();
        } else if (prompt.equalsIgnoreCase("load")) {
            baritone.getAIProcess().loadHistory();
        } else if (prompt.equalsIgnoreCase("save")) {
            baritone.getAIProcess().saveHistory();
        } else {
            baritone.getAIProcess().prompt(prompt);
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgParserManager parserManager, baritone.api.command.argument.IArgConsumer args) throws CommandException {
        return Stream.of("stop", "load", "save");
    }

    @Override
    public String getShortDesc() {
        return "Controls the AI agent";
    }

    @Override
    public List<String> getLongDesc() {
        return List.of("Start an AI agent with a prompt, or stop/load/save its history.");
    }
}