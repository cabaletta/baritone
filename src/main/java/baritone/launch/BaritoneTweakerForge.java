package baritone.launch;

import net.minecraft.launchwrapper.LaunchClassLoader;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.tools.obfuscation.mcp.ObfuscationServiceMCP;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Brady
 * @since 7/31/2018 10:09 PM
 */
public class BaritoneTweakerForge extends BaritoneTweaker {

    @Override
    public final void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        this.args = new ArrayList<>();
    }

    @Override
    public final void injectIntoClassLoader(LaunchClassLoader classLoader) {
        super.injectIntoClassLoader(classLoader);
        MixinEnvironment.getDefaultEnvironment().setObfuscationContext(ObfuscationServiceMCP.SEARGE);
    }
}
