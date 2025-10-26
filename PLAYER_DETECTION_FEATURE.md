# Player Detection Pause Feature

This document describes the new player detection pause feature added to Baritone.

## Overview

Baritone now includes two new settings that allow it to automatically pause when:
1. Any player on the server enters spectator mode
2. Any player comes within a specified visual range

This is useful for avoiding detection or maintaining a low profile on servers.

## Settings

### `pauseOnSpectator`
- **Type:** Boolean
- **Default:** `false`
- **Description:** When enabled, Baritone will pause whenever any player on the server enters spectator mode. This is useful for detecting when server moderators or admins (who often use spectator mode) are online and potentially watching.

### `pauseOnPlayerNearby`
- **Type:** Boolean
- **Default:** `false`
- **Description:** When enabled, Baritone will pause whenever any player comes within the specified radius of your position.

### `pauseOnPlayerNearbyRadius`
- **Type:** Integer
- **Default:** `64` (blocks)
- **Description:** Defines the detection radius in blocks for the `pauseOnPlayerNearby` feature. Players within this distance will trigger a pause.

## Usage

### Via Chat Commands

Enable spectator detection:
```
#set pauseOnSpectator true
```

Enable nearby player detection:
```
#set pauseOnPlayerNearby true
```

Change the detection radius:
```
#set pauseOnPlayerNearbyRadius 100
```

Disable the features:
```
#set pauseOnSpectator false
#set pauseOnPlayerNearby false
```

### Via Settings File

You can also configure these settings in your Baritone settings file:

```json
{
  "pauseOnSpectator": true,
  "pauseOnPlayerNearby": true,
  "pauseOnPlayerNearbyRadius": 64
}
```

## How It Works

The feature is implemented as a `PlayerDetectionPauserProcess` that:
- Runs with high priority (5.2) to ensure it pauses before other processes
- Checks all entities on the server each tick when active
- Only activates when at least one of the pause settings is enabled
- Automatically resumes when the triggering condition is no longer present

### Detection Logic

**Spectator Mode Detection:**
- Iterates through all entities on the server
- Filters for PlayerEntity instances
- Checks if the player is in spectator mode using `player.isSpectator()`
- Excludes the local player from checks
- Logs the name of the spectator player when detected

**Nearby Player Detection:**
- Iterates through all entities on the server
- Filters for PlayerEntity instances
- Calculates distance squared for performance
- Checks if any player is within the configured radius
- Excludes the local player from checks
- Logs the name and distance of nearby players when detected

## Resuming

Once the triggering condition is removed (spectator leaves or players move away), Baritone will automatically resume its previous task. You don't need to manually resume unless you want to override the automatic behavior.

## Notes

- Both features can be enabled simultaneously
- The process has higher priority than the inventory pauser to ensure detection happens first
- Error handling is in place to prevent crashes from entity iteration issues
- The local player is always excluded from detection checks
- Detection messages are logged to chat when a player is detected

## Performance

The feature is designed to be lightweight:
- Only active when at least one setting is enabled
- Uses distance squared calculations to avoid expensive square root operations
- Includes exception handling to gracefully handle edge cases
- Marked as a temporary process to allow proper cleanup

## Example Use Cases

1. **Server with moderators:** Enable `pauseOnSpectator` to detect when staff members are watching
2. **PvP servers:** Enable `pauseOnPlayerNearby` with a radius of 50-100 blocks to avoid being caught by other players
3. **Combined protection:** Enable both features for maximum stealth on busy servers
