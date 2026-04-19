package baritone.process;

import baritone.api.IBaritone;
import baritone.api.process.IAIProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.event.events.PathEvent;
import baritone.api.event.events.ChatEvent;
import baritone.api.event.listener.AbstractGameEventListener;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AIProcess extends AbstractGameEventListener implements IAIProcess {
    private final IBaritone baritone;
    private final JsonArray history = new JsonArray();
    private boolean active = false;
    private boolean thinking = false;

    public AIProcess(IBaritone baritone) {
        this.baritone = baritone;
        baritone.getGameEventHandler().registerEventListener(this);
        initSystemPrompt();
    }

    private void initSystemPrompt() {
        JsonObject msg = new JsonObject();
        msg.addProperty("role", "system");
        msg.addProperty("content", "You are an AI playing Minecraft via Baritone. Respond ONLY in JSON. Actions: {\"action\":\"goto\",\"x\":0,\"y\":64,\"z\":0}, {\"action\":\"mine\",\"block\":\"diamond_ore\",\"count\":1}, {\"action\":\"say\",\"message\":\"text\"}, {\"action\":\"ask\",\"question\":\"text\"}, {\"action\":\"stop\"}. Wait for events after action.");
        history.add(msg);
    }

    @Override
    public void prompt(String goal) {
        active = true;
        JsonObject msg = new JsonObject();
        msg.addProperty("role", "user");
        msg.addProperty("content", goal);
        history.add(msg);
        log("AI activated: " + goal);
        think();
    }

    @Override
    public void stop() {
        active = false;
        thinking = false;
        log("AI stopped.");
    }

    @Override
    public void loadHistory() {
        log("History loaded (mock).");
    }

    @Override
    public void saveHistory() {
        log("History saved (mock).");
    }

    private void think() {
        if (!active || thinking) return;
        thinking = true;
        new Thread(() -> {
            try {
                String key = baritone.getSettings().aiApiKey.value;
                if (key == null || key.isEmpty()) {
                    log("Error: AI API Key not set.");
                    stop();
                    return;
                }

                URL url = new URL(baritone.getSettings().aiBaseUrl.value);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + key);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JsonObject req = new JsonObject();
                req.addProperty("model", baritone.getSettings().aiModel.value);
                req.add("messages", history);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(req.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                if (code == 200) {
                    try (Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                        JsonObject res = JsonParser.parseReader(reader).getAsJsonObject();
                        String reply = res.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString();
                        
                        JsonObject msg = new JsonObject();
                        msg.addProperty("role", "assistant");
                        msg.addProperty("content", reply);
                        history.add(msg);
                        
                        handleAction(reply);
                    }
                } else {
                    log("API Error: HTTP " + code);
                    stop();
                }
            } catch (Exception e) {
                log("API Exception: " + e.getMessage());
                stop();
            } finally {
                thinking = false;
            }
        }).start();
    }

    private void handleAction(String reply) {
        try {
            JsonObject action = JsonParser.parseString(reply).getAsJsonObject();
            String type = action.get("action").getAsString();
            
            if ("goto".equals(type)) {
                int x = action.get("x").getAsInt();
                int y = action.get("y").getAsInt();
                int z = action.get("z").getAsInt();
                baritone.getCommandManager().execute("goto " + x + " " + y + " " + z);
            } else if ("mine".equals(type)) {
                String block = action.get("block").getAsString();
                int count = action.has("count") ? action.get("count").getAsInt() : 1;
                baritone.getCommandManager().execute("mine " + count + " " + block);
            } else if ("say".equals(type) || "ask".equals(type)) {
                String msg = action.has("message") ? action.get("message").getAsString() : action.get("question").getAsString();
                log("AI: " + msg);
            } else if ("stop".equals(type)) {
                stop();
            }
        } catch (Exception e) {
            log("Invalid action JSON: " + reply);
            feedEvent("System", "Invalid JSON format. Please output strictly JSON.");
        }
    }

    private void feedEvent(String role, String text) {
        if (!active) return;
        JsonObject msg = new JsonObject();
        msg.addProperty("role", role);
        msg.addProperty("content", text);
        history.add(msg);
        think();
    }

    @Override
    public void onPathEvent(PathEvent event) {
        if (!active) return;
        if (event == PathEvent.AT_GOAL) {
            feedEvent("system", "PathEvent: Arrived at goal.");
        } else if (event == PathEvent.CALC_FAILED) {
            feedEvent("system", "PathEvent: Calculation failed.");
        }
    }

    @Override
    public void onSendChatMessage(ChatEvent event) {
        if (!active || event.getMessage().startsWith("#")) return;
        feedEvent("user", "Player says: " + event.getMessage());
    }

    private void log(String msg) {
        baritone.getPlayerContext().player().displayClientMessage(Component.literal("[Baritone AI] " + msg), false);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        return new PathingCommand(null, PathingCommandType.DEFER);
    }

    @Override
    public void onLostControl() {}

    @Override
    public String displayName0() {
        return "AI Process";
    }
}