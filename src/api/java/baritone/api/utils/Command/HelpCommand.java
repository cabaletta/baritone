package baritone.api.utils.Command;

import baritone.api.utils.Helper;

import java.util.ArrayList;


public class HelpCommand implements ICommand, Helper {
    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDesc() {
        return "Lists all commands and their description.";
    }

    @Override
    public void onCommand(boolean hasArgs, String[] args) {
        if(hasArgs)
            logDirect("you dont need any args fyi"); // TODO change this to ony send one descritpion



        ArrayList<ICommand> cmds = CommandHandler.INSTANCE.getCommands();
        logDirect("there are " + cmds.size() + " commands loaded");

        for (ICommand cmd : cmds) {
            logDirect(CommandHandler.INSTANCE.getPrefix() + cmd.getName() + ": " + cmd.getDesc());
        }


    }


}
