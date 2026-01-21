# Community Goals Plugin

A comprehensive Paper/Spigot plugin that brings your server community together through shared objectives. Create engaging community goals that automatically expand your world border when completed, giving players a sense of collective achievement and progression.

---

## 🌟 Features

### 🎯 **Community Goals System**
- **Multiple Active Goals**: Run several community goals simultaneously
- **Flexible Progress Tracking**: Set any target number (items, blocks, kills, etc.)
- **Goal States**: Active, Paused, Completed, and Cancelled states for full control
- **Persistent Storage**: All goals survive server restarts and are automatically saved
- **Real-time Updates**: Progress updates instantly across all players

### 🌍 **Automatic World Border Expansion**
- **Seamless Integration**: Border expands automatically when goals are completed
- **Smooth Animation**: 30-second expansion animation for dramatic effect
- **Configurable Amounts**: Set custom expansion amounts per goal completion
- **Multi-World Support**: Configure separate borders and rewards per world
- **Persistent Settings**: Border settings are saved and restored on server restart
- **Manual Control**: Admin commands for manual border management

### 📢 **Player Engagement & Announcements**
- **Server-wide Broadcasts**: Completion messages announce achievements to all players
- **Progress Visibility**: Players can check goal progress anytime
- **Visual Feedback**: Color-coded progress indicators and status messages
- **Achievement Celebration**: Dramatic announcements when goals are completed

### ⚙️ **Comprehensive Admin Tools**
- **Easy Goal Creation**: Simple commands to create goals with custom names and descriptions
- **Progress Management**: Set, adjust, or complete goals manually
- **Border Control**: Full control over world border size, center, and expansion settings
- **Data Management**: Manual save commands and configuration options

---

---

## Getting Started (Server Owners)

Follow these steps to set up separate Overworld/Nether goals and borders.

### 1) Install
1. Drop the plugin JAR into `plugins/`.
2. Start the server once to generate `plugins/CommunityGoals/config.yml`.
3. Stop the server.

### 2) Configure Borders per World
Edit `plugins/CommunityGoals/config.yml`:

```yaml
world-borders:
  default-world: "world"
  worlds:
    world:
      enabled: true
      center-x: 0
      center-z: 0
      initial-size: 1000
      expansion-amount: 200
    world_nether:
      enabled: true
      center-x: 0
      center-z: 0
      initial-size: 200
      expansion-amount: 50
```

Notes:
- World names must match exactly (e.g., `world`, `world_nether`).
- `default-world` is used if you don?t specify a world when creating a goal.

### 3) Create Goals per Dimension
Overworld example:
```
/goal-admin create wood_collection "Lumber Drive" 5000 200 world "Collect 5000 wood logs"
```

Nether example:
```
/goal-admin create blaze_rods "Blaze Hunt" 250 100 world_nether "Collect 250 blaze rods"
```

### 4) Add NPCs for Each Dimension (Optional)
Install FancyNpcs, then:
```
/goal-npc link OverworldGuide wood_collection
/goal-npc link NetherGuide blaze_rods
```

### 5) Verify Borders
```
/goal-admin border world info
/goal-admin border world_nether info
```

### 6) (Optional) Enable Queue Mode per World
In config:
```yaml
goals:
  queue-enabled: true
```

Manage per-world queues:
```
/goal-admin queue world list
/goal-admin queue world_nether list
```

## 📋 Commands Reference

### 👥 **Player Commands** (`/goal`)

| Command | Description | Example |
|---------|-------------|---------|
| `/goal` | Display help and available commands | `/goal` |
| `/goal list` | Show all active community goals with progress bars | `/goal list` |
| `/goal info <id>` | View detailed information about a specific goal | `/goal info diamonds` |

*Note: Players contribute to goals through gameplay or admin commands. The plugin is designed for server-wide community participation.*

### 🛠️ **Admin Commands** (`/goal-admin`)

#### **Goal Management**
| Command | Description | Example |
|---------|-------------|---------|
| `/goal-admin create <id> <name> <target> [reward] [world] [description]` | Create a new community goal | `/goal-admin create diamonds "Diamond Collection" 1000 200 world "Collect 1000 diamonds to expand the border"` |
| `/goal-admin delete <id>` | Permanently delete a goal | `/goal-admin delete diamonds` |
| `/goal-admin info <id>` | View detailed goal information including creation date | `/goal-admin info diamonds` |
| `/goal-admin list` | Display all goals with progress bars and completion status | `/goal-admin list` |

#### **Progress Control**
| Command | Description | Example |
|---------|-------------|---------|
| `/goal-admin setprogress <id> <amount>` | Set exact progress amount for a goal | `/goal-admin setprogress diamonds 750` |
| `/goal-admin complete <id>` | Instantly complete a goal (triggers border expansion) | `/goal-admin complete diamonds` |
| `/goal-admin setstate <id> <state>` | Change goal state (ACTIVE, PAUSED, COMPLETED, CANCELLED) | `/goal-admin setstate diamonds PAUSED` |
| `/goal-admin setreward <id> <amount>` | Set per-goal border expansion reward | `/goal-admin setreward diamonds 200` |

#### **System Management**
| Command | Description | Example |
|---------|-------------|---------|
| `/goal-admin save` | Force save all goals to disk | `/goal-admin save` |
| `/goal-admin queue [world] list` | List queued goals and active goal | `/goal-admin queue world list` |
| `/goal-admin queue [world] add <id>` | Add a goal to the queue | `/goal-admin queue world add diamonds` |
| `/goal-admin queue [world] remove <id>` | Remove a goal from the queue | `/goal-admin queue world remove diamonds` |
| `/goal-admin queue [world] move <id> <pos>` | Move a goal in the queue | `/goal-admin queue world move diamonds 2` |
| `/goal-admin queue [world] next` | Activate the next queued goal | `/goal-admin queue world next` |

#### **World Border Management**
| Command | Description | Example |
|---------|-------------|---------|
| `/goal-admin border [world] info` | Display current border size, center, and expansion settings | `/goal-admin border world info` |
| `/goal-admin border [world] set <size>` | Set border to specific size in blocks | `/goal-admin border world set 1000` |
| `/goal-admin border [world] expand [amount]` | Expand border by amount (uses config default if omitted) | `/goal-admin border world expand 200` |
| `/goal-admin border [world] center <x> <z>` | Move border center to coordinates | `/goal-admin border world center 0 0` |

---

## ⚙️ Configuration

### 📁 **Main Config** (`plugins/CommunityGoals/config.yml`)

The plugin automatically generates a configuration file on first startup. Here are the key settings:

```yaml
# World border configuration (multi-world)
world-borders:
  default-world: "world"            # Default world for new goals
  worlds:
    world:
      enabled: true                 # Enable/disable automatic border expansion
      center-x: 0                   # Border center X coordinate
      center-z: 0                   # Border center Z coordinate  
      initial-size: 50              # Starting border size in blocks
      expansion-amount: 100         # Blocks to expand when goals complete
    world_nether:
      enabled: true
      center-x: 0
      center-z: 0
      initial-size: 50
      expansion-amount: 50

# Goals system
goals:
  announcements-enabled: true       # Enable server-wide completion announcements
  auto-save-interval: 5             # Minutes between automatic saves
  queue-enabled: false              # Enable goal queue mode (one active goal at a time)

# Persistence settings
persistence:
  type: "yaml"                      # Storage format (yaml recommended)
  data-folder: "data"               # Folder name for goal data
```

### 🔧 **Important Configuration Notes**

- **World Names**: Ensure each entry under `world-borders.worlds` matches your world names exactly
- **Default World**: Set `world-borders.default-world` to the world you want new goals to use by default
- **Initial Size**: Set `initial-size` to your current border size to avoid resets
- **Expansion Amount**: Adjust `expansion-amount` based on your server's progression pace
- **Per-Goal Rewards**: Use `/goal-admin setreward` or the create reward field to override the default expansion
- **Auto-save**: Lower intervals provide better data safety but may impact performance

---

## 🚀 Getting Started

### **Step 1: Installation**
1. Download the plugin JAR file
2. Place it in your server's `plugins/` folder
3. Start or restart your server
4. The plugin will create default configuration files

### **Step 2: Configure Your World Border**
1. Check your current border size: `/goal-admin border <world> info`
2. Update the config file with your current settings:
   ```yaml
   world-borders:
     default-world: "world"
     worlds:
       world:
         center-x: 0             # Current center X
         center-z: 0             # Current center Z
         initial-size: 1000      # Current border size
         expansion-amount: 200   # How much to expand per goal
       world_nether:
         center-x: 0
         center-z: 0
         initial-size: 200
         expansion-amount: 50
   ```
3. Restart the server to apply changes

### **Step 3: Create Your First Goal**
```bash
/goal-admin create wood_collection "Lumber Drive" 5000 world "Collect 5000 wood logs to expand our territory!"
```
Add a world name (e.g., `world_nether`) to create Nether-specific goals.

### **Step 4: Test the System**
```bash
# Check the goal was created
/goal-admin list

# Add some progress
/goal-admin setprogress wood_collection 2500

# Complete the goal to test border expansion
/goal-admin setprogress wood_collection 5000
```

You should see server-wide messages and the border expanding!

### **Step 5 (Optional): Add FancyNpcs Goal NPCs**
To use NPCs, install the FancyNpcs plugin on your server and restart. Then stand where you want the NPC and run:
```bash
/goal-npc link <npc_name> <goal_id>
```

Example:
```bash
/goal-npc link GoalGuide wood_collection
```

The NPC will spawn at your location. When players right-click it, they will see the goal info.

To set the **central goals NPC** for a world (opens that world's goals GUI):
```bash
/goal-npc central set <npc_name>
```

Example:
```bash
/goal-npc central set GoalsHub
```

Right-clicking this NPC opens the goals menu (3 goals at a time). Locked goals are shown but not clickable.

Other useful commands:
```bash
/goal-npc list
/goal-npc unlink <npc_name>
/goal-npc central remove [world]
```

---

## 🎮 Gameplay Examples

### **Example Goal Ideas**

#### **Resource Collection Goals**
```bash
/goal-admin create iron_age "Iron Age" 2000 "Mine 2000 iron ore to advance our civilization"
/goal-admin create food_drive "Community Feast" 1500 "Gather 1500 food items for the community"
/goal-admin create stone_quarry "Great Quarry" 10000 "Collect 10000 stone blocks for construction"
```

#### **Building & Construction Goals**
```bash
/goal-admin create mega_build "Mega Structure" 50000 "Place 50000 blocks in community builds"
/goal-admin create road_network "Road Network" 5000 "Build 5000 blocks of roads connecting settlements"
```

#### **Exploration Goals**
```bash
/goal-admin create exploration "Territory Expansion" 100 "Discover 100 new chunks to expand our map"
/goal-admin create treasure_hunt "Treasure Hunters" 50 "Find 50 buried treasures"
```

### **Managing Multiple Goals**

You can run several goals simultaneously:
```bash
# Create multiple active goals
/goal-admin create diamonds "Diamond Rush" 500 "Collect diamonds for tools"
/goal-admin create emeralds "Trade Empire" 200 "Gather emeralds for trading"
/goal-admin create netherite "Ultimate Gear" 50 "Find netherite for the best equipment"

# Check all active goals
/goal-admin list

# Pause a goal temporarily
/goal-admin setstate emeralds PAUSED

# Resume it later
/goal-admin setstate emeralds ACTIVE
```

---

## 🔒 Permissions

| Permission | Description | Default |
|-----------|-------------|---------|
| `communitygoals.player` | Use basic player commands (`/goal`) | Everyone |
| `communitygoals.admin` | Use all admin commands (`/goal-admin`) | OP only |


---

## 📊 Data Storage & Backup

### **Data Location**
All goal data is stored in: `plugins/CommunityGoals/data/goals.yml`

### **Backup Recommendations**
- **Automatic**: The plugin auto-saves every 5 minutes (configurable)
- **Manual**: Use `/goal-admin save` before server maintenance
- **File Backup**: Include the entire `plugins/CommunityGoals/` folder in your backup routine

### **Example Goal Data Structure**
```yaml
goals:
  diamonds:
    id: "diamonds"
    name: "Diamond Collection"
    description: "Collect diamonds for the community"
    world: "world"
    currentProgress: 750
    targetProgress: 1000
    state: "ACTIVE"
    createdAt: 1705600000000
    completedAt: 0
```

---

## 🔧 Troubleshooting

### **Common Issues**

#### **"Goal not found" Error**
- **Cause**: Typo in goal ID or goal was deleted
- **Solution**: Use `/goal-admin list` to see all goal IDs

#### **Border Not Expanding**
- **Cause**: World name mismatch or border at maximum size
- **Solutions**: 
  - Check `/goal-admin border <world> info` for current settings
  - Verify `world-borders.worlds` entries in config match your world names exactly
  - Ensure border isn't at Minecraft's maximum size (30,000,000 blocks)

#### **Goals Not Saving**
- **Cause**: File permission issues or disk space
- **Solutions**:
  - Check server logs for error messages
  - Verify `plugins/CommunityGoals/data/` folder has write permissions
  - Use `/goal-admin save` to force a manual save

#### **Border Resets on Restart**
- **Cause**: Config not saving border changes
- **Solution**: The latest version automatically saves border changes to config

### **Getting Help**

1. **Check Server Logs**: Look in `logs/latest.log` for error messages
2. **Verify Installation**: Ensure the plugin appears in `/plugins` command
3. **Test Permissions**: Make sure you have `communitygoals.admin` permission
4. **Check Config**: Verify your `config.yml` has correct world names and settings

---

## 🎉 Tips for Server Owners

### **Creating Engaging Goals**
- **Start Small**: Begin with achievable goals to build momentum
- **Vary Types**: Mix resource collection, building, and exploration goals
- **Seasonal Events**: Create special goals for holidays or server events
- **Community Input**: Ask players what goals they'd like to work toward

### **Border Expansion Strategy**
- **Gradual Growth**: Use smaller expansion amounts (50-200 blocks) for steady progression
- **Major Milestones**: Larger expansions (500+ blocks) for significant achievements
- **Resource Balance**: Consider how border expansion affects resource availability

### **Goal Management Best Practices**
- **Clear Descriptions**: Write descriptive goal names and explanations
- **Reasonable Targets**: Set targets that require community effort but aren't impossible
- **Regular Updates**: Check progress and adjust goals based on player engagement
- **Celebrate Success**: Use the completion announcements to celebrate community achievements

### **Server Integration Ideas**
- **Economy Integration**: Reward goal completion with server currency
- **Rank Progression**: Tie border expansions to server rank unlocks
- **Event Coordination**: Use goals to drive server-wide events and competitions
- **Community Building**: Goals that require cooperation between different player groups

---

This plugin transforms individual player actions into community achievements, creating a shared sense of progress and accomplishment that keeps players engaged and working together toward common goals!
