# Community Goals Plugin - Development Todo

## Setup Phase
- [x] Set up Gradle project structure
- [x] Create core data models (Goal, State, Border)
- [x] Implement config loading (config.yml)

## Persistence Layer
- [x] Implement YAML persistence layer

## Core Logic
- [x] Implement goal progress tracking logic
- [x] Implement command turn-in handler
- [x] Implement FancyNpcs integration

## Features
- [x] Implement world border expansion
- [x] Implement progress announcements (BossBar/ActionBar)
- [x] Implement item turn-in with inventory validation and removal
- [x] Fix border persistence across server restarts

## Admin & Security
- [x] Implement admin commands (/goal admin)
- [x] Add command to create/add community goals
- [x] Add commands to configure border expansion amount
- [x] Add permissions and security checks

## Future Features
- [x] **Goal Queue System**: Implement a system where only one goal can be active at a time
  - [x] Add goal queue/waiting list functionality
  - [x] Auto-activate next goal when current one completes
  - [x] Commands to manage goal queue (add to queue, reorder, remove from queue)
  - [x] Config option to enable/disable queue mode vs multiple active goals
  - [x] Queue status display showing upcoming goals

- [x] **Per-Goal Reward System**: Allow different border expansion amounts per goal
  - [x] Add reward/expansion amount field to Goal model
  - [x] Update create command to accept custom reward: `/goal-admin create <id> <name> <target> <reward> [description]`
  - [x] Modify goal completion to use individual goal rewards instead of global config
  - [x] Add command to modify existing goal rewards: `/goal-admin setreward <id> <amount>`
  - [x] Display reward amount in goal info and list commands
  - [x] Backward compatibility with global expansion amount as default

- [ ] **Custom Ore + One Tool (Vanilla-Focused, Region-Gated)**: Add a new ore that spawns only in specific biomes, drops a raw item, and fuels both community goals and one special tool (no full armor set, no netherite replacement)
  - [ ] Define biome/region gating and vein size (rarer than diamond, higher concentration in target regions)
  - [ ] Define processing (smelt/blast to ingot) and limit Fortune scaling if needed
  - [x] Implement ore generation via Terra config (biome-gated feature, small veins, Y range)
  - [x] Implement drops via datapack loot table (ore block → raw item; vanilla Fortune behavior)
  - [x] Add furnace + blast recipes in datapack (raw → ingot)
  - [ ] Implement the one special tool with a situational, vanilla-friendly bonus: Ore Echo System (when mining a natural ore, nearby same-type natural ores glow briefly 1–2s; no auto-break)
  - [ ] Add community goals tied to the ore/tool (collection milestones, unlocks, announcements)

## Testing
- [x] Local testing and validation
- [x] Item turn-in testing
- [x] Border expansion and persistence testing
