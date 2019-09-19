package baritone.utils.accessor;

public interface ITabCompleter {

    String getPrefix();

    void setPrefix(String prefix);

    boolean onGuiChatSetCompletions(String[] newCompl);
}
