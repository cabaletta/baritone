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

import baritone.utils.BaritoneProcessHelper;

import baritone.api.BaritoneAPI;

public class AIProcess extends BaritoneProcessHelper implements IAIProcess, AbstractGameEventListener {
    private final JsonArray history = new JsonArray();
    private boolean active = false;
    private boolean thinking = false;

    public AIProcess(baritone.Baritone baritone) {
        super(baritone);
        baritone.getGameEventHandler().registerEventListener(this);
        initSystemPrompt();
    }

    private void initSystemPrompt() {
        JsonObject msg = new JsonObject();
        msg.addProperty("role", "system");
        msg.addProperty("content", "You are an autonomous AI Agent playing Minecraft via the Baritone API. You perceive the world through events and can interact by outputting strictly JSON formatted actions.\n" +
                "\n" +
                "Available actions (tools):\n" +
                "1. Movement: {\"action\":\"goto\", \"x\": <int>, \"y\": <int>, \"z\": <int>} - Move to specific coordinates.\n" +
                "2. Mining: {\"action\":\"mine\", \"block\": \"<string>\", \"count\": <int>} - Mine specific blocks.\n" +
                "3. Communication: {\"action\":\"say\", \"message\": \"<string>\"} - Send a message in chat.\n" +
                "4. Inquiry: {\"action\":\"ask\", \"question\": \"<string>\"} - Ask the user a question.\n" +
                "5. Get Player Info: {\"action\":\"get_player_info\"} - Retrieve your current position, health, hunger, and other stats.\n" +
                "6. Get Environment Info: {\"action\":\"get_env_info\"} - Retrieve current time, weather, and world info.\n" +
                "7. Execute Baritone Command: {\"action\":\"execute_command\", \"command\": \"<string>\"} - Execute any valid Baritone command (e.g., 'explore', 'farm', 'follow').\n" +
                "8. Stop: {\"action\":\"stop\"} - Terminate the AI session when the goal is achieved or impossible.\n" +
                "\n" +
                "Rules:\n" +
                "- ALWAYS output exactly one JSON object per response. No extra text outside the JSON.\n" +
                "- Wait for the system to provide event feedback after every action. Do not assume an action succeeded until confirmed.\n" +
                "- If you lack information, use the appropriate action to gather it or ask the user.\n" +
                "- Think step-by-step and use tools to explore the environment, gather information, and achieve the user's goals.");
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
                String key = BaritoneAPI.getSettings().aiApiKey.value;
                if (key == null || key.isEmpty()) {
                    log("Error: AI API Key not set.");
                    stop();
                    return;
                }

                URL url = new URL(BaritoneAPI.getSettings().aiBaseUrl.value);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + key);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JsonObject req = new JsonObject();
                req.addProperty("model", BaritoneAPI.getSettings().aiModel.value);
                req.add("messages", history);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(req.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                if (code == 200) {
                    try (Reader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                        JsonObject res = JsonParser.parseReader(reader).getAsJsonObject();
                        JsonObject messageObj = res.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message");
                        
                        String reasoning = "";
                        if (messageObj.has("reasoning_content") && !messageObj.get("reasoning_content").isJsonNull()) {
                            reasoning = messageObj.get("reasoning_content").getAsString();
                        }
                        if (reasoning != null && !reasoning.trim().isEmpty()) {
                            log("Thinking: " + reasoning.trim());
                        }

                        String reply = "";
                        if (messageObj.has("content") && !messageObj.get("content").isJsonNull()) {
                            reply = messageObj.get("content").getAsString();
                        }

                        if (reply == null || reply.trim().isEmpty()) {
                            log("Error: AI returned empty content.");
                            stop();
                            return;
                        }

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
            reply = reply.trim();
            if (reply.startsWith("```json")) {
                reply = reply.substring(7);
            } else if (reply.startsWith("```")) {
                reply = reply.substring(3);
            }
            if (reply.endsWith("```")) {
                reply = reply.substring(0, reply.length() - 3);
            }
            reply = reply.trim();

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
                baritone.getPlayerContext().player().connection.sendChat(msg);
            } else if ("get_player_info".equals(type)) {
                net.minecraft.client.player.LocalPlayer player = baritone.getPlayerContext().player();
                String info = String.format("Pos: [%.1f, %.1f, %.1f], Health: %.1f, Food: %d", 
                        player.getX(), player.getY(), player.getZ(), player.getHealth(), player.getFoodData().getFoodLevel());
                feedEvent("System", "Player Info: " + info);
            } else if ("get_env_info".equals(type)) {
                net.minecraft.client.multiplayer.ClientLevel level = baritone.getPlayerContext().level();
                String info = String.format("Time: %d, Weather: %s", 
                        level.getDayTime() % 24000, level.isRaining() ? "Raining" : "Clear");
                feedEvent("System", "Environment Info: " + info);
            } else if ("execute_command".equals(type)) {
                String cmd = action.get("command").getAsString();
                baritone.getCommandManager().execute(cmd);
                feedEvent("System", "Executed Baritone Command: " + cmd);
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
        logDirect("[Baritone AI] " + msg);
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