package adris.altoclef.butler;

import adris.altoclef.util.helpers.ConfigHelper;

public class ButlerConfig {

    private static ButlerConfig _instance = new ButlerConfig();

    static {
        ConfigHelper.loadConfig("configs/butler.json", ButlerConfig::new, ButlerConfig.class, newConfig -> _instance = newConfig);
    }

    /**
     * If true, will use blacklist for rejecting users from using your player as a butler
     */
    public boolean useButlerBlacklist = true;
    /**
     * If true, will use whitelist to only accept users from said whitelist.
     */
    public boolean useButlerWhitelist = false;
    /**
     * Servers have different messaging plugins that change the way messages are displayed.
     * Rather than attempt to implement all of them and introduce a big security risk,
     * you may define custom whisper formats that the butler will watch out for.
     * <p>
     * Within curly brackets are three special parts:
     * <p>
     * {from}: Who the message was sent from
     * {to}: Who the message was sent to, butler will ignore if this is not your username.
     * {message}: The message.
     * <p>
     * <p>
     * WARNING: The butler will only accept non-chat messages as commands, but don't make this too lenient,
     * else you may risk unauthorized control to the bot. Basically, make sure that only whispers can
     * create the following messages.
     */
    public String[] whisperFormats = new String[]{
            "{from} {to} {message}"
    };
    /**
     * If set to true, will print information about whispers that are parsed and those
     * that have failed parsing.
     * <p>
     * Enable this if you need help setting up the whisper format.
     */
    public boolean whisperFormatDebug = true;
    /**
     * Determines if failure messages should be sent to a non-authorized entity attempting to use butler
     * <p>
     * Disable this if you need to stay undercover.
     */
    public boolean sendAuthorizationResponse = true;
    /**
     * The response sent in a failed execution due to non-authorization
     * {from}: the username of the player who triggered the failed authorization response
     */
    public String failedAuthorizationResposne = "Sorry {from} but you are not authorized!";
    /**
     * Use this to choose if the prefix should be required in messages
     * <p>
     * Disable this if you want to be able to send normal messages and not butler commands.
     */
    public boolean requirePrefixMsg = false;

    public static ButlerConfig getInstance() {
        return _instance;
    }
}
