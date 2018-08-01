package baritone.bot.utils;

import java.util.function.Supplier;

/**
 * @author Brady
 * @since 8/1/2018 12:56 AM
 */
public final class Utils {

    public static void ifConditionThen(Supplier<Boolean> condition, Runnable runnable) {
        if (condition.get())
            runnable.run();
    }
}
