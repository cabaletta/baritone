Regular old baritone along with some personal additions. I do not care about this anymore. java is probably the worst that has happended to coding since javascript.

# Player radar
### Via Chat Commands

Pauses automation when other players are detected based on spectator state or distance.

Enable spectator detection (pause if any player is in spectator mode):
```
#set pauseOnSpectator true
```

Enable nearby player detection (pause if a player is within your configured radius):
```
#set pauseOnPlayerNearby true
```

Change the detection radius (in blocks for nearby-player pause):
```
#set pauseOnPlayerNearbyRadius 100
```

Disable the features (resume normal behavior without player checks):
```
#set pauseOnSpectator false
#set pauseOnPlayerNearby false
```

# Block Radar
### Via Chat Commands

Searches an area for target blocks and reports matching coordinates.

Scan for one block type (quick search using default radius):
```
#scan spawner
```

Scan for multiple blocks (find any of the listed targets):
```
#scan spawner ancient_debris diamond_ore
```

Scan with explicit radius (override default search distance):
```
#scan diamond_ore 128
```

Stop on first match (fastest locate mode):
```
#scan -first ancient_debris 200
```

Scan a fixed region (bounded box from point A to point B):
```
#scan ancient_debris 0 0 0 200 120 200
```

Check progress and results (view live status and found positions):
```
#scan progress
#scan list
```

Stop or clear scan state (cancel active scan or reset stored results):
```
#scan stop
#scan clear
```

Tune scan behavior (default radius, max radius, logging verbosity):
```
#set blockScanDefaultRadius 64
#set blockScanMaxRadius 500
#set blockScanLogEachFind false
```

# Agentic builder
### Via Chat Commands

Builds schematics autonomously by gathering, crafting, and smelting required materials.

Start from schematic in your schematics folder (uses current player position as origin):
```
#agenticbuild start mybuild.litematic
```

Start from schematic at explicit origin (set exact placement coordinates):
```
#agenticbuild start mybuild.litematic 100 64 100
```

Start from currently open Litematica schematic (optionally by open index):
```
#agenticbuild litematic
```

Check state and required materials (progress, phase, and missing resources):
```
#agenticbuild status
#agenticbuild materials
```

Control execution (pause, resume, skip current task, or stop):
```
#agenticbuild pause
#agenticbuild resume
#agenticbuild skip
#agenticbuild stop
```

Mark a material as unavailable (skip attempts to gather that resource):
```
#agenticbuild unavailable quartz_block
```

Tune agentic behavior (enable modules and gather thresholds):
```
#set agenticBuilderEnabled true
#set agenticGatherMaterials true
#set agenticCraftMaterials true
#set agenticSmeltMaterials true
#set agenticGatherRadius 256
#set agenticMinGatherBatch 64
#set agenticAutoResumeBuild true
```