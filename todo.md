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
- [x] Implement Citizens NPC integration

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
- [ ] **Goal Queue System**: Implement a system where only one goal can be active at a time
  - [ ] Add goal queue/waiting list functionality
  - [ ] Auto-activate next goal when current one completes
  - [ ] Commands to manage goal queue (add to queue, reorder, remove from queue)
  - [ ] Config option to enable/disable queue mode vs multiple active goals
  - [ ] Queue status display showing upcoming goals

## Testing
- [x] Local testing and validation
- [x] Item turn-in testing
- [x] Border expansion and persistence testing