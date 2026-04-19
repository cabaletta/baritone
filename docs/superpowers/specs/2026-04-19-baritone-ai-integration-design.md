# Baritone AI Integration Design

## 1. Overview
Integrate an LLM (Large Language Model) natively into the Baritone Minecraft mod to allow players to control the bot via natural language. The AI agent will have a continuous loop to monitor task progress, react to events, and ask the player for clarifications.

## 2. Architecture & Components

### 2.1 Configuration
Add the following settings to `baritone.api.Settings`:
- `aiApiKey` (String, default: `""`) - API key for the LLM provider.
- `aiBaseUrl` (String, default: `"https://api.openai.com/v1"`) - Base URL to support OpenAI or custom endpoints (e.g., local models, proxies).
- `aiModel` (String, default: `"gpt-4o-mini"`) - The model to use.

### 2.2 Commands
Create `baritone.command.defaults.AICommand`:
- `#ai <prompt>`: Initializes the AI with a natural language goal.
- `#ai stop`: Halts the AI loop and clears context.

### 2.3 AI Process (`baritone.process.AIProcess`)
Implement `baritone.api.process.IAIProcess` (extends `IBaritoneProcess`).
- **State Management**: Maintains the current state (`IDLE`, `THINKING`, `EXECUTING`, `WAITING_FOR_USER`).
- **Context Window**: Stores a `List<ChatMessage>` to maintain conversation history.
- **Event Interception**: Subscribes to `PathEvent` (to detect task completion/failure) and `ChatEvent` (to capture player responses when the AI asks a question).

### 2.4 API Client
- **Transport**: Use `java.net.HttpURLConnection` for network requests to maintain zero external dependencies beyond what Minecraft provides.
- **JSON**: Use Minecraft's bundled `com.google.gson` library for parsing requests and responses.

## 3. State Machine & Data Flow

### 3.1 System Prompt & Actions
The System Prompt instructs the LLM to act as a Minecraft bot and respond *only* in a specific JSON format.
Supported actions include:
- `{"action": "goto", "x": 10, "y": 64, "z": 20}`
- `{"action": "mine", "block": "diamond_ore", "count": 5}`
- `{"action": "say", "message": "I found the diamonds!"}`
- `{"action": "ask", "question": "Which way should I explore?"}`
- `{"action": "stop"}`

### 3.2 Feedback Loop
1. **User Input**: Player types `#ai find a village`.
2. **Thinking**: `AIProcess` enters `THINKING`, spins up a background thread, and POSTs the context to the LLM API.
3. **Execution**: The LLM returns a JSON action (e.g., `goto`). `AIProcess` parses it, delegates the task to the appropriate Baritone process (e.g., `CustomGoalProcess`), and enters `EXECUTING`.
4. **Monitoring**: `AIProcess` monitors `PathEvent`. When `PathEvent.AT_GOAL` is fired, it injects a System message: `"Action 'goto' completed."`
5. **Re-Evaluation**: The completion message triggers another `THINKING` phase so the LLM can decide the next step, creating a continuous autonomous loop.

## 4. Error Handling
- **API Errors**: If the API request fails (network error, invalid key, rate limit), `AIProcess` will print a localized error message to the player's chat and reset to `IDLE`.
- **Parsing Errors**: If the LLM returns invalid JSON, `AIProcess` will inject a System message `"Invalid format, please reply in JSON"` and retry (max 3 retries).
- **Execution Errors**: If a Baritone task fails (e.g., `PathEvent.CALC_FAILED`), it injects a System message `"Path failed: No route found"` and lets the LLM decide an alternative approach.

## 5. Scope & Implementation Plan
This design focuses purely on core pathing, mining, and basic chat interaction. Advanced features like complex inventory management or building schematics via AI are deferred to future iterations to keep the initial implementation scoped and robust.