## Describe your suggestion
Setting Baritone to mine valuables like diamonds while AFK can stall unexpectedly once inventory fills up.

After enough time, the bot may spend long periods idle because it has no free inventory space. A block-compression option for mined resources would let mining continue longer without user intervention.

### Proposed behavior
- Only craft into storage blocks for resources relevant to the current `#mine` target list.
- When inventory is full:
  - Move to or identify a safe place to pause.
  - Check whether a crafting table is available.
  - If no crafting table is present, check whether one can be crafted from inventory materials.
- Place a crafting table, craft compressible resources into blocks, then break and recover the table (configurable).
- Resume mining.
- Repeat the process until no additional target resources can be compressed.

## Settings
- Toggle: allow consuming wood/planks in inventory to craft a crafting table when missing.
- Toggle: if missing table + crafting ingredients, search nearby for an existing crafting table first, then optionally gather wood/planks.
- Toggle: compress only `#mine` target resources vs all compressible inventory resources.
- Toggle: require searching for a safe pause position vs immediate-place craft attempt.
- Toggle: collect placed crafting table after crafting vs leave it behind.

## Context
This would improve long AFK mining sessions by reducing downtime caused by full inventory while still giving users control over resource usage and risk tolerance.

## Final checklist
- [x] I know how to properly use check boxes
- [x] I have not used any OwO's or UwU's in this issue.
