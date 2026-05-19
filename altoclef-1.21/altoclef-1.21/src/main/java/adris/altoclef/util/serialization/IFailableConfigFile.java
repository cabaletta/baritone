package adris.altoclef.util.serialization;

public interface IFailableConfigFile {
    void onFailLoad();

    boolean failedToLoad();
}
