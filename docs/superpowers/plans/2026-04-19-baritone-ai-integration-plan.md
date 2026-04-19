# Baritone AI Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Integrate an LLM native agent into Baritone that can understand natural language chat commands, autonomously execute Baritone actions, and interact via chat.

**Architecture:** A new `AIProcess` state machine runs alongside other Baritone processes. It intercepts chat and path events to maintain a continuous context window, which it sends to the LLM (via HTTP/Gson) to determine the next JSON-formatted action.

**Tech Stack:** Java 17+, `java.net.HttpURLConnection`, `com.google.gson`.

---

### Task 1: Add AI Settings

**Files:**
- Modify: `src/api/java/baritone/api/Settings.java`

- [ ] **Step 1: Add setting fields**

Insert these fields into `src/api/java/baritone/api/Settings.java` (around line 60, after the logger):

```java
    /**
     * API Key for the LLM (e.g. OpenAI)
     */
    public final Setting<String> aiApiKey = new Setting<>("");

    /**
     * Base URL for the LLM API
     */
    public final Setting<String> aiBaseUrl = new Setting<>("https://api.openai.com/v1/chat/completions");

    /**
     * Model to use for the LLM
     */
    public final Setting<String> aiModel = new Setting<>("gpt-4o-mini");
```

- [ ] **Step 2: Commit**

```bash
git add src/api/java/baritone/api/Settings.java
git commit -m "feat: add AI settings to Baritone Settings"
```

### Task 2: Define IAIProcess Interface

**Files:**
- Create: `src/api/java/baritone/api/process/IAIProcess.java`
- Modify: `src/api/java/baritone/api/IBaritone.java`

- [ ] **Step 1: Create IAIProcess interface**

Create `src/api/java/baritone/api/process/IAIProcess.java`:

```java
package baritone.api.process;

public interface IAIProcess extends IBaritoneProcess {
    void prompt(String goal);
    void stop();
    void loadHistory();
    void saveHistory();
}
```

- [ ] **Step 2: Add to IBaritone interface**

Modify `src/api/java/baritone/api/IBaritone.java` to add the getter:

```java
// Find: IBuilderProcess getBuilderProcess();
// Add below it:
    IAIProcess getAIProcess();
```

- [ ] **Step 3: Commit**

```bash
git add src/api/java/baritone/api/process/IAIProcess.java src/api/java/baritone/api/IBaritone.java
git commit -m "feat: define IAIProcess interface"
```

### Task 3: Implement AIProcess

**Files:**
- Create: `src/main/java/baritone/process/AIProcess.java`
- Modify: `src/main/java/baritone/Baritone.java`

- [ ] **Step 1: Create AIProcess class**

Create `src/main/java/baritone/process/AIProcess.java`:

```java
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
        // Simple mock for plan compilation, full impl later
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
```

- [ ] **Step 2: Instantiate AIProcess in Baritone**

Modify `src/main/java/baritone/Baritone.java`:

```java
// Find imports and add:
import baritone.process.AIProcess;
import baritone.api.process.IAIProcess;

// In class Baritone:
// Find: private final CustomGoalProcess customGoalProcess;
// Add:
private final AIProcess aiProcess;

// In constructor:
// Find: customGoalProcess = new CustomGoalProcess(this);
// Add:
this.aiProcess = new AIProcess(this);
this.gameEventHandler.registerEventListener(this.aiProcess);

// Add getter method:
@Override
public IAIProcess getAIProcess() {
    return aiProcess;
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/baritone/process/AIProcess.java src/main/java/baritone/Baritone.java
git commit -m "feat: implement AIProcess"
```

### Task 4: Register AICommand

**Files:**
- Create: `src/main/java/baritone/command/defaults/AICommand.java`
- Modify: `src/main/java/baritone/command/defaults/DefaultCommands.java`

- [ ] **Step 1: Create AICommand**

Create `src/main/java/baritone/command/defaults/AICommand.java`:

```java
package baritone.command.defaults;

import baritone.api.IBaritone;
import baritone.api.command.Command;
import baritone.api.command.argument.IArgParserManager;
import baritone.api.command.exception.CommandException;

import java.util.List;
import java.util.stream.Stream;

public class AICommand extends Command {

    public AICommand(IBaritone baritone) {
        super(baritone, "ai");
    }

    @Override
    public void execute(String label, IArgParserManager parserManager, baritone.api.command.argument.IArgConsumer args) throws CommandException {
        if (!args.hasAny()) {
            throw new CommandException("Usage: #ai <prompt|stop|load|save>");
        }
        String prompt = args.getString();
        while (args.hasAny()) {
            prompt += " " + args.getString();
        }

        if (prompt.equalsIgnoreCase("stop")) {
            baritone.getAIProcess().stop();
        } else if (prompt.equalsIgnoreCase("load")) {
            baritone.getAIProcess().loadHistory();
        } else if (prompt.equalsIgnoreCase("save")) {
            baritone.getAIProcess().saveHistory();
        } else {
            baritone.getAIProcess().prompt(prompt);
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgParserManager parserManager, baritone.api.command.argument.IArgConsumer args) throws CommandException {
        return Stream.of("stop", "load", "save");
    }

    @Override
    public String getShortDesc() {
        return "Controls the AI agent";
    }

    @Override
    public List<String> getLongDesc() {
        return List.of("Start an AI agent with a prompt, or stop/load/save its history.");
    }
}
```

- [ ] **Step 2: Register in DefaultCommands**

Modify `src/main/java/baritone/command/defaults/DefaultCommands.java`:

```java
// Find: new WaypointsCommand(baritone),
// Add below it:
            new AICommand(baritone),
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/baritone/command/defaults/AICommand.java src/main/java/baritone/command/defaults/DefaultCommands.java
git commit -m "feat: add AICommand"
```
