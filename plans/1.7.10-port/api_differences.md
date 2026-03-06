# Key API Differences for Porting Baritone to Minecraft 1.7.10 Forge

## Overview
Minecraft 1.7.10 uses Forge 10.13.4.x and MCP mappings. The codebase is obfuscated in production, requiring deobfuscation for development. Modern Baritone (1.12+) uses Yarn or Mojang mappings with BlockStates. 1.7.10 lacks BlockStates; blocks use IDs and metadata.

LOTR Mod adds custom blocks (e.g., mithril, custom woods) and entities (orcs, etc.), so pathing must handle modded Block IDs beyond vanilla.

## World and Chunk Access
- **Modern**: `World.getBlockState(pos)` returns BlockState. Chunks use `PalettedContainer` for block storage.
- **1.7.10**: `World.getBlock(x, y, z)` returns Block. Use `Block.getIdFromBlock(block)` for ID, `world.getBlockMetadata(x,y,z)` for meta. Chunks are `Chunk` with `ExtendedBlockStorage[]` arrays for sections.
- **Adaptation**: Rewrite `BlockStateInterface` to use ID/meta pairs. Cache must store int[ID][meta] or similar instead of BlockState.
- **Cache**: `CachedChunk` needs to handle old chunk format. Use `Chunk.getBlockStorageArray()` for sections.

## Events
- **Modern**: Uses Forge's `EventBus` with modern events like `TickEvent`, `PlayerTickEvent`.
- **1.7.10**: FML's `MinecraftForge.EVENT_BUS` and `FMLCommonHandler`. Events like `TickEvent.PlayerTickEvent`, `WorldEvent.Load`.
- **Adaptation**: Register Baritone's `GameEventHandler` to 1.7.10 events. Chat events via `ClientChatReceivedEvent`.

## Player and Input
- **Modern**: `Minecraft.getInstance().player`, input via `InputEvent` or direct field access.
- **1.7.10**: `Minecraft.getMinecraft().thePlayer`. Movement input via `EntityClientPlayerMP.movementInput`. Override via reflection or mixins (but 1.7.10 has no Mixins; use Forge hooks or direct access).
- **Adaptation**: `LookBehavior` must set `thePlayer.rotationYaw/Pitch`. Pathing uses `PlayerControllerMP` for actions like block breaking.

## Blocks and Interactions
- **Vanilla Blocks**: IDs 0-255, meta 0-15. Custom mods extend this.
- **Pathing Movements**: `MovementHelper` checks `canWalkOn` via `block.isPassable` or custom logic. For LOTR, add checks for mod blocks (e.g., treat LOTR stone as solid).
- **Mining/Placing**: Use `PlayerControllerMP.clickBlock` for mining, `onPlayerRightClick` for placing.
- **LOTR Compatibility**: Dynamically detect LOTR blocks via `Block.blockRegistry` iteration. Adjust `Settings` for custom costs (e.g., mine mithril slower).

## Commands
- **Modern**: Chat via `ClientPlayerEntity.sendChatMessage`.
- **1.7.10**: Intercept `ClientChatReceivedEvent` for commands, send via `thePlayer.sendChatMessage`.
- **Adaptation**: `CommandManager` registers to chat event, parses args similarly but handles old string utils.

## Pathing Core
- Algorithms (A*, nodes, costs) are version-agnostic; port directly.
- Movements (ascend, traverse) need updated block checks using 1.7.10 `IBlockAccess`.

## Rendering
- **Modern**: `RenderWorldLastEvent`, Tessellator.
- **1.7.10**: `RenderWorldLastEvent`, old Tessellator with `addVertex`.
- **Adaptation**: `PathRenderer` uses 1.7.10 OpenGL calls.

## Inventory and Tools
- **Modern**: `PlayerInventory`.
- **1.7.10**: `InventoryPlayer`. Slot indices different.
- **Tool Selection**: Iterate `thePlayer.inventory.mainInventory` for best tool.

## Networking and Multiplayer
- Packets via `SimpleNetworkWrapper` in modern; 1.7.10 uses custom channels or direct.
- Baritone mostly client-side, but sync settings if needed.

## Build and Obfuscation
- Use MCP 9.18 for 1.7.10 mappings.
- Gradle setup with Forge MDK; apply MCP mappings in dev.

## LOTR Specifics
- LOTR registers blocks like `Blocks.lotr_stone`, entities like `EntityOrc`.
- In cache, treat unknown blocks as air or solid based on `isOpaqueCube()`.
- Add settings for LOTR biomes/structures in pathing goals.

## References
- Forge 1.7.10 Docs: http://mcforge.readthedocs.io/en/1.7.10/
- MCP Mappings: Search for MCP 9.18.
- LOTR Source: If available, analyze block classes for pathing properties.

This document will be updated as more details are discovered during porting.