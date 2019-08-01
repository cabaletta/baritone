package baritone.api.utils.Command;

public interface ICommand {

    public String getName();


    public String getDesc();

    public void onCommand(boolean hasArgs, String[] args);
}
