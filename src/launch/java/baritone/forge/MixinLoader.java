package baritone.forge;

import java.util.Map;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

public class MixinLoader {

	public MixinLoader() {
		MixinBootstrap.init();
		Mixins.addConfiguration("mixins.client.json");
		MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
	}

	public String[] getASMTransformerClass() {
		return new String[0];
	}

	public String getModContainerClass() {
		return null;
	}

	public String getSetupClass() {
		return null;
	}

	public String getAccessTransformerClass() {
		return null;
	}

	public void injectData(Map<String, Object> data) {

	}

}
