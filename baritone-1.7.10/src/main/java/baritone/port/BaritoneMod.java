package baritone.port;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(modid = "@MODID@", name = "@NAME@", version = "@VERSION@")
public class BaritoneMod {

    @Instance("@MODID@")
    public static BaritoneMod instance;

    @SidedProxy(clientSide = "baritone.port.ClientProxy", serverSide = "baritone.port.CommonProxy")
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        // no-op
    }
}
