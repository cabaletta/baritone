package adris.altoclef.util.helpers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

public class InputHelper {

    public static boolean isKeyPressed(int code) {
        return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), code);
    }
}
