package baritone.api.utils.Command;


import java.util.ArrayList;

public class CommandHandler {

    public static CommandHandler INSTANCE;

    protected String prefix;
    protected ArrayList<ICommand> commands;

    public CommandHandler(String prefix){
        CommandHandler.INSTANCE = this;
        this.prefix = prefix;
        this.commands = new java.util.ArrayList<>();

        this.commands.add(new HelpCommand());
    }


    public void registerCommand(ICommand command){
        commands.add(command);
    }

    public boolean handleCommand(String message){
        if(!message.startsWith(prefix))
            return false;
        String[] args = message.split(" ");
        String command = args[0].substring(1);
        for(ICommand c : commands)
            if(c.getName().equals(command)) {
                c.onCommand(args.length > 1, remFirstElement(args));
                return true;
            }

        return false;
    }



    public ArrayList<ICommand> getCommands(){
        return commands;
    }

    public String getPrefix(){
        return prefix;
    }



    private String[] remFirstElement(String[] array){
        String[] rV = new String[array.length - 1];
        for (int i = 0; i < rV.length; i++) {
            rV[i] = array[i + 1];
        }
        return rV;
    }

}
