package adris.altoclef.butler;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.ChatMessageEvent;
import adris.altoclef.eventbus.events.TaskFinishedEvent;
import adris.altoclef.ui.MessagePriority;
import net.minecraft.network.message.MessageType;

import java.util.Objects;

/**
 * The butler system lets authorized players send commands to the bot to execute.
 * <p>
 * This effectively makes the bot function as a servant, or butler.
 * <p>
 * Authorization is defined in "altoclef_butler_whitelist.txt" and "altoclef_butler_blacklist.txt"
 * and depends on the "useButlerWhitelist" and "useButlerBlacklist" settings in "altoclef_settings.json"
 */
public class Butler {

    private static final String BUTLER_MESSAGE_START = "` ";

    private final AltoClef _mod;

    private final WhisperChecker _whisperChecker = new WhisperChecker();

    private final UserAuth _userAuth;

    private String _currentUser = null;

    // Utility variables for command logic
    private boolean _commandInstantRan = false;
    private boolean _commandFinished = false;

    public Butler(AltoClef mod) {
        _mod = mod;
        _userAuth = new UserAuth(mod);

        // Revoke our current user whenever a task finishes.
        EventBus.subscribe(TaskFinishedEvent.class, evt -> {
            if (_currentUser != null) {
                _currentUser = null;
            }
        });

        // Receive system events
        EventBus.subscribe(ChatMessageEvent.class, evt -> {
            boolean debug = ButlerConfig.getInstance().whisperFormatDebug;
            String message = evt.messageContent();
            String sender = evt.senderName();
            MessageType messageType = evt.messageType();
            String receiver = mod.getPlayer().getName().getString();
            if (sender != null && !Objects.equals(sender, receiver) && messageType.chat().style().isItalic()
                    && messageType.chat().style().getColor() != null
                    && Objects.equals(messageType.chat().style().getColor().getName(), "gray")) {
                String wholeMessage = sender + " " + receiver + " " + message;
                if (debug) {
                    Debug.logMessage("RECEIVED WHISPER: \"" + wholeMessage + "\".");
                }
                _mod.getButler().receiveMessage(wholeMessage, receiver);
            }
        });
    }

    private void receiveMessage(String msg, String receiver) {
        // Format: <USER> whispers to you: <MESSAGE>
        // Format: <USER> whispers: <MESSAGE>
        WhisperChecker.MessageResult result = this._whisperChecker.receiveMessage(_mod, receiver, msg);
        if (result != null) {
            this.receiveWhisper(result.from, result.message);
        } else if (ButlerConfig.getInstance().whisperFormatDebug) {
            Debug.logMessage("    Not Parsing: MSG format not found.");
        }
    }

    private void receiveWhisper(String username, String message) {

        boolean debug = ButlerConfig.getInstance().whisperFormatDebug;
        // Ignore messages from other bots.
        if (message.startsWith(BUTLER_MESSAGE_START)) {
            if (debug) {
                Debug.logMessage("    Rejecting: MSG is detected to be sent from another bot.");
            }
            return;
        }

        if (_userAuth.isUserAuthorized(username)) {
            executeWhisper(username, message);
        } else {
            if (debug) {
                Debug.logMessage("    Rejecting: User \"" + username + "\" is not authorized.");
            }
            if (ButlerConfig.getInstance().sendAuthorizationResponse) {
                sendWhisper(username, ButlerConfig.getInstance().failedAuthorizationResposne.replace("{from}", username), MessagePriority.UNAUTHORIZED);
            }
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isUserAuthorized(String username) {
        return _userAuth.isUserAuthorized(username);
    }

    public void onLog(String message, MessagePriority priority) {
        if (_currentUser != null) {
            sendWhisper(message, priority);
        }
    }

    public void onLogWarning(String message, MessagePriority priority) {
        if (_currentUser != null) {
            sendWhisper("[WARNING:] " + message, priority);
        }
    }

    public void tick() {
        // Nothing for now.
    }

    public String getCurrentUser() {
        return _currentUser;
    }

    public boolean hasCurrentUser() {
        return _currentUser != null;
    }

    private void executeWhisper(String username, String message) {
        String prevUser = _currentUser;
        _commandInstantRan = true;
        _commandFinished = false;
        _currentUser = username;
        sendWhisper("Command Executing: " + message, MessagePriority.TIMELY);
        String prefix = ButlerConfig.getInstance().requirePrefixMsg ? _mod.getModSettings().getCommandPrefix() : "";
        AltoClef.getCommandExecutor().execute(prefix + message, () -> {
            // On finish
            sendWhisper("Command Finished: " + message, MessagePriority.TIMELY);
            if (!_commandInstantRan) {
                _currentUser = null;
            }
            _commandFinished = true;
        }, e -> {
            sendWhisper("TASK FAILED: " + e.getMessage(), MessagePriority.ASAP);
            e.printStackTrace();
            _currentUser = null;
            _commandInstantRan = false;
        });
        _commandInstantRan = false;
        // Only set the current user if we're still running.
        if (_commandFinished) {
            _currentUser = prevUser;
        }
    }

    private void sendWhisper(String message, MessagePriority priority) {
        if (_currentUser != null) {
            sendWhisper(_currentUser, message, priority);
        } else {
            Debug.logWarning("Failed to send butler message as there are no users present: " + message);
        }
    }

    private void sendWhisper(String username, String message, MessagePriority priority) {
        _mod.getMessageSender().enqueueWhisper(username, BUTLER_MESSAGE_START + message, priority);
    }
}
