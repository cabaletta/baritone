package baritone.api.process;

public interface IAIProcess extends IBaritoneProcess {
    void prompt(String goal);
    void stop();
    void loadHistory();
    void saveHistory();
}