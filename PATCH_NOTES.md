# CommunityGoals Patch Notes

## v1.0.0 (current build)

### New Features
- FancyNpcs integration for goal NPCs and a central "Goals Hub" NPC.
- Central GUI with 3 goals displayed at a time (1 active + locked ones).
- Turn-in chest flow: players submit items, invalid/excess items are returned automatically.
- Goal Queue system (optional): only one active goal at a time with queued/locked goals.
- Per-goal border expansion rewards (overrides global expansion amount).
- Auto-delete completed goals and refresh GUI after completion.

### Quality / Maintenance
- Logging now uses the plugin logger instead of System.out/err.
- FancyNpcs 2.9.1 compatibility added (Paper 1.21.1).

---

## How to Use

### 1) Enable Queue Mode (optional)
Edit `plugins/CommunityGoals/config.yml` and add queue-enabled:

```yaml
goals: {announcements-enabled: true, announcement-interval: 10, default-target: 100, queue-enabled: true}
```

Restart the server.

### 2) Create Goals (IDs must be single words)
```bash
/goal-admin create diamonds "Diamond Collection" 10 50 "Collect 10 diamonds"
```
- `diamonds` = goal ID (no spaces)
- `10` = target progress
- `50` = reward (border expansion)
- Description is optional

### 3) Set/Adjust Rewards
```bash
/goal-admin setreward diamonds 200
```

### 4) Goal Queue Commands (only if enabled)
```bash
/goal-admin queue list
/goal-admin queue add <id>
/goal-admin queue remove <id>
/goal-admin queue move <id> <pos>
/goal-admin queue next
```

### 5) Create a Central Goals NPC
Stand where you want the NPC:
```bash
/goal-npc central set GoalsHub
/goal-npc list
/goal-npc central remove
```
Right-click the central NPC to open the goals GUI.

### 6) Link a Goal NPC (optional)
```bash
/goal-npc link <npc_name> <goal_id>
/goal-npc unlink <npc_name>
```
Right-clicking that NPC shows goal info.

### 7) Turn-in Flow
- Click an active goal in the GUI -> empty chest opens.
- Place items -> close chest.
- Valid items are accepted; invalid/excess items return automatically.
