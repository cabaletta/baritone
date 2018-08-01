package baritone.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Brady
 * @since 7/31/2018 10:10 PM
 */
public class BaritoneTweakerOptifine extends BaritoneTweaker {

    @Override
    public final void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        this.args = new ArrayList<>();
    }
}
