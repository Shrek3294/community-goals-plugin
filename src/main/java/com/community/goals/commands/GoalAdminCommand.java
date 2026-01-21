package com.community.goals.commands;

import com.community.goals.Goal;
import com.community.goals.State;
import com.community.goals.features.BorderExpansionManager;
import com.community.goals.features.BorderManagerRegistry;
import com.community.goals.logic.GoalProgressTracker;
import com.community.goals.logic.GoalQueueManager;
import com.community.goals.persistence.ConfigManager;
import com.community.goals.persistence.PersistenceManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * Handles /goal admin commands
 */
public class GoalAdminCommand extends BaseCommand {
    private final BorderManagerRegistry borderRegistry;
    private final ConfigManager configManager;
    private final GoalQueueManager queueManager;
    private final Logger logger;
    
    public GoalAdminCommand(GoalProgressTracker tracker, PersistenceManager persistence, BorderManagerRegistry borderRegistry, ConfigManager configManager, GoalQueueManager queueManager, Logger logger) {
        super(tracker, persistence);
        this.borderRegistry = borderRegistry;
        this.configManager = configManager;
        this.queueManager = queueManager;
        this.logger = logger;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!hasPermission(sender, PermissionManager.GOAL_ADMIN)) {
            sendError(sender, "You don't have permission to use admin commands");
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "create":
                return handleCreate(sender, args);
            case "delete":
                return handleDelete(sender, args);
            case "info":
                return handleInfo(sender, args);
            case "list":
                return handleList(sender, args);
            case "setprogress":
                return handleSetProgress(sender, args);
            case "complete":
                return handleComplete(sender, args);
            case "setstate":
                return handleSetState(sender, args);
            case "setreward":
                return handleSetReward(sender, args);
            case "save":
                return handleSave(sender);
            case "border":
                return handleBorder(sender, args);
            case "queue":
                return handleQueue(sender, args);
            default:
                showHelp(sender);
                return true;
        }
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sendError(sender, "Usage: /goal admin create <id> <name> <target> [reward] [world] [description]");
            sendInfo(sender, "Example: /goal admin create diamonds \"Diamond Collection\" 10 200 world \"Collect diamonds\"");
            return true;
        }

        // Parse arguments handling quoted strings
        String[] parsedArgs = parseQuotedArgs(args);
        
        if (parsedArgs.length < 4) {
            sendError(sender, "Usage: /goal admin create <id> <name> <target> [reward] [world] [description]");
            sendInfo(sender, "Example: /goal admin create diamonds \"Diamond Collection\" 10 200 world \"Collect diamonds\"");
            return true;
        }

        String id = parsedArgs[1];
        String name = parsedArgs[2];
        long target;
        double rewardExpansion = 0;
        
        try {
            target = Long.parseLong(parsedArgs[3]);
        } catch (NumberFormatException e) {
            sendError(sender, "Target must be a number");
            return true;
        }

        int descriptionIndex = 4;
        if (parsedArgs.length > 4) {
            try {
                rewardExpansion = Double.parseDouble(parsedArgs[4]);
                if (rewardExpansion < 0) {
                    sendError(sender, "Reward must be zero or positive");
                    return true;
                }
                descriptionIndex = 5;
            } catch (NumberFormatException ignored) {
                rewardExpansion = 0;
            }
        }

        String worldName = resolveDefaultWorld(sender);
        if (parsedArgs.length > descriptionIndex) {
            String worldCandidate = resolveWorldName(parsedArgs[descriptionIndex]);
            if (worldCandidate != null) {
                worldName = worldCandidate;
                descriptionIndex++;
            }
        }

        String description = parsedArgs.length > descriptionIndex ? parsedArgs[descriptionIndex] : "No description";

        try {
            Goal goal = tracker.createGoal(id, name, description, target, worldName);
            if (rewardExpansion > 0) {
                goal.setRewardExpansion(rewardExpansion);
                persistence.saveGoal(goal);
            }
            if (queueManager != null && queueManager.isEnabled()) {
                queueManager.handleGoalCreated(goal);
            }
            sendSuccess(sender, "Goal created: " + goal.getName() + " (ID: " + goal.getId() + ")");
            return true;
        } catch (IllegalArgumentException e) {
            sendError(sender, e.getMessage());
            return true;
        }
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendError(sender, "Usage: /goal admin delete <id>");
            return true;
        }

        String goalId = args[1];
        Goal goal = getGoalOrError(sender, goalId);
        if (goal == null) return true;

        tracker.deleteGoal(goalId);
        if (queueManager != null && queueManager.isEnabled()) {
            queueManager.handleGoalDeleted(goal);
        }
        sendSuccess(sender, "Goal deleted: " + goal.getName());
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendError(sender, "Usage: /goal admin info <id>");
            return true;
        }

        String goalId = args[1];
        Goal goal = getGoalOrError(sender, goalId);
        if (goal == null) return true;

        sender.sendMessage("");
        sender.sendMessage("§6§l=== Goal Information ===");
        sender.sendMessage("§7ID: §f" + goal.getId());
        sender.sendMessage("§7Name: §f" + goal.getName());
        sender.sendMessage("§7Description: §f" + goal.getDescription());
        sender.sendMessage("§7World: §f" + goal.getWorldName());
        sender.sendMessage("§7Progress: §f" + goal.getCurrentProgress() + " / " + goal.getTargetProgress());
        sender.sendMessage(String.format("§7Percentage: §f%.2f%%", goal.getProgressPercentage()));
        if (goal.getRewardExpansion() > 0) {
            sender.sendMessage("§7Reward Expansion: §f" + goal.getRewardExpansion() + " blocks");
        }
        sender.sendMessage("§7State: " + goal.getState().getColoredName());
        sender.sendMessage("§7Created: §f" + new java.util.Date(goal.getCreatedAt()));
        if (goal.isCompleted()) {
            sender.sendMessage("§7Completed: §f" + new java.util.Date(goal.getCompletedAt()));
        }
        sender.sendMessage("");
        return true;
    }

    private boolean handleList(CommandSender sender, String[] args) {
        Collection<Goal> goals = tracker.getAllGoals();
        
        sender.sendMessage("");
        sender.sendMessage("§6§l=== Community Goals ===");
        sender.sendMessage("§7Total: " + goals.size());
        sender.sendMessage("");
        
        for (Goal goal : goals) {
            String status = goal.isCompleted() ? "§a✓" : "§7○";
            String progressBar = createProgressBar(goal.getProgressPercentage());
            String rewardLabel = goal.getRewardExpansion() > 0 ? String.format(" §7(+%.0f)", goal.getRewardExpansion()) : "";
            String worldLabel = " §7(" + goal.getWorldName() + ")";
            sender.sendMessage(String.format(
                "%s §f%s%s%s §7[%s] %.1f%%",
                status,
                goal.getName(),
                rewardLabel,
                worldLabel,
                progressBar,
                goal.getProgressPercentage()
            ));
        }
        
        sender.sendMessage("");
        return true;
    }

    private boolean handleSetProgress(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendError(sender, "Usage: /goal admin setprogress <id> <amount>");
            return true;
        }

        String goalId = args[1];
        long amount;
        
        try {
            amount = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            sendError(sender, "Amount must be a number");
            return true;
        }

        Goal goal = getGoalOrError(sender, goalId);
        if (goal == null) return true;

        tracker.setProgress(goalId, amount);
        sendSuccess(sender, "Progress set to " + amount + " for " + goal.getName());
        return true;
    }

    private boolean handleComplete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendError(sender, "Usage: /goal admin complete <id>");
            return true;
        }

        String goalId = args[1];
        Goal goal = getGoalOrError(sender, goalId);
        if (goal == null) return true;

        tracker.setProgress(goalId, goal.getTargetProgress());
        sendSuccess(sender, "Goal completed: " + goal.getName());
        return true;
    }

    private boolean handleSetState(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendError(sender, "Usage: /goal admin setstate <id> <state>");
            sendInfo(sender, "States: ACTIVE, PAUSED, COMPLETED, CANCELLED");
            return true;
        }

        String goalId = args[1];
        String stateName = args[2].toUpperCase();

        Goal goal = getGoalOrError(sender, goalId);
        if (goal == null) return true;

        try {
            State state = State.valueOf(stateName);
            if (state == State.COMPLETED) {
                tracker.setProgress(goalId, goal.getTargetProgress());
                sendSuccess(sender, "Goal completed: " + goal.getName());
            } else {
                goal.setState(state);
                persistence.saveGoal(goal);
                sendSuccess(sender, "State set to " + state.getDisplayName() + " for " + goal.getName());
            }
            if (queueManager != null && queueManager.isEnabled()) {
                queueManager.refreshStates(goal.getWorldName());
            }
            return true;
        } catch (IllegalArgumentException e) {
            sendError(sender, "Unknown state: " + stateName);
            sendInfo(sender, "Valid states: ACTIVE, PAUSED, COMPLETED, CANCELLED");
            return true;
        }
    }

    private boolean handleSave(CommandSender sender) {
        tracker.saveAllGoals();
        sendSuccess(sender, "All goals saved to file");
        return true;
    }

    private boolean handleSetReward(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendError(sender, "Usage: /goal admin setreward <id> <amount>");
            return true;
        }

        String goalId = args[1];
        double reward;
        try {
            reward = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sendError(sender, "Reward must be a number");
            return true;
        }
        if (reward < 0) {
            sendError(sender, "Reward must be zero or positive");
            return true;
        }

        Goal goal = getGoalOrError(sender, goalId);
        if (goal == null) {
            return true;
        }

        goal.setRewardExpansion(reward);
        persistence.saveGoal(goal);
        sendSuccess(sender, "Reward set to " + reward + " blocks for " + goal.getName());
        return true;
    }

    private boolean handleBorder(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("?6?l=== Border Commands ===");
            sender.sendMessage("?7/goal admin border [world] info - Show border information");
            sender.sendMessage("?7/goal admin border [world] set <size> - Set border size");
            sender.sendMessage("?7/goal admin border [world] expand [amount] - Expand border");
            sender.sendMessage("?7/goal admin border [world] center <x> <z> - Set border center");
            return true;
        }

        String worldName = resolveDefaultWorld(sender);
        String borderCmd = args[1].toLowerCase();
        int cmdIndex = 1;

        if (!isBorderSubcommand(borderCmd)) {
            worldName = args[1];
            if (args.length < 3) {
                sendError(sender, "Usage: /goal admin border <world> <info|set|expand|center>");
                return true;
            }
            borderCmd = args[2].toLowerCase();
            cmdIndex = 2;
        }

        BorderExpansionManager borderManager = borderRegistry.getManager(worldName);
        if (borderManager == null) {
            sendError(sender, "No border configured for world: " + worldName);
            return true;
        }

        switch (borderCmd) {
            case "info":
                return handleBorderInfo(sender, borderManager);
            case "set":
                return handleBorderSet(sender, borderManager, args, cmdIndex + 1, worldName);
            case "expand":
                return handleBorderExpand(sender, borderManager, args, cmdIndex + 1, worldName);
            case "center":
                return handleBorderCenter(sender, borderManager, args, cmdIndex + 1, worldName);
            default:
                sendError(sender, "Unknown border command: " + borderCmd);
                return true;
        }
    }

    private boolean handleQueue(CommandSender sender, String[] args) {
        if (queueManager == null || !queueManager.isEnabled()) {
            sendError(sender, "Goal queue system is disabled in config.");
            return true;
        }

        if (args.length < 2) {
            sendQueueUsage(sender);
            return true;
        }

        String worldName = resolveDefaultWorld(sender);
        String action = args[1].toLowerCase();
        int actionIndex = 1;

        if (!isQueueAction(action)) {
            worldName = args[1];
            if (args.length < 3) {
                sendQueueUsage(sender);
                return true;
            }
            action = args[2].toLowerCase();
            actionIndex = 2;
        }

        if (!borderRegistry.hasWorld(worldName)) {
            sendError(sender, "No border configured for world: " + worldName);
            return true;
        }

        switch (action) {
            case "list":
                return handleQueueList(sender, worldName);
            case "add":
                return handleQueueAdd(sender, args, actionIndex + 1, worldName);
            case "remove":
                return handleQueueRemove(sender, args, actionIndex + 1, worldName);
            case "move":
                return handleQueueMove(sender, args, actionIndex + 1, worldName);
            case "next":
                return handleQueueNext(sender, args, actionIndex + 1, worldName);
            default:
                sendQueueUsage(sender);
                return true;
        }
    }

    private boolean handleQueueList(CommandSender sender, String worldName) {
        var queue = queueManager.getQueue(worldName);
        sender.sendMessage("");
        sender.sendMessage("?6?l=== Goal Queue ===");
        sender.sendMessage("?7World: ?f" + worldName);
        if (queue.isEmpty()) {
            sender.sendMessage("?7No goals in queue.");
            sender.sendMessage("");
            return true;
        }

        int index = 1;
        for (String goalId : queue) {
            Goal goal = tracker.getGoal(goalId);
            if (goal == null) {
                continue;
            }
            String marker = index == 1 ? "?a[ACTIVE]" : "?7[QUEUED]";
            sender.sendMessage("?7" + index + ". " + marker + " ?f" + goal.getName() + " ?7(" + goal.getId() + ")");
            index++;
        }
        sender.sendMessage("");
        return true;
    }

    private boolean handleQueueAdd(CommandSender sender, String[] args, int idIndex, String worldName) {
        if (args.length <= idIndex) {
            sendError(sender, "Usage: /goal admin queue [world] add <id>");
            return true;
        }
        String goalId = args[idIndex];
        Goal goal = getGoalOrError(sender, goalId);
        if (goal == null) {
            return true;
        }
        if (!goal.getWorldName().equalsIgnoreCase(worldName)) {
            sendError(sender, "Goal belongs to world " + goal.getWorldName() + ", not " + worldName);
            return true;
        }
        queueManager.addToQueue(worldName, goalId);
        sendSuccess(sender, "Added goal to queue: " + goal.getName());
        return true;
    }

    private boolean handleQueueRemove(CommandSender sender, String[] args, int idIndex, String worldName) {
        if (args.length <= idIndex) {
            sendError(sender, "Usage: /goal admin queue [world] remove <id>");
            return true;
        }
        String goalId = args[idIndex];
        Goal goal = getGoalOrError(sender, goalId);
        if (goal == null) {
            return true;
        }
        if (!goal.getWorldName().equalsIgnoreCase(worldName)) {
            sendError(sender, "Goal belongs to world " + goal.getWorldName() + ", not " + worldName);
            return true;
        }
        queueManager.removeFromQueue(worldName, goalId);
        sendSuccess(sender, "Removed goal from queue: " + goal.getName());
        return true;
    }

    private boolean handleQueueMove(CommandSender sender, String[] args, int idIndex, String worldName) {
        if (args.length <= idIndex + 1) {
            sendError(sender, "Usage: /goal admin queue [world] move <id|position> <position>");
            return true;
        }
        var queue = queueManager.getQueue(worldName);
        String goalId = resolveQueueGoalId(queue, args[idIndex]);
        if (goalId == null) {
            sendError(sender, "Goal not found in queue.");
            return true;
        }
        int position;
        try {
            position = Integer.parseInt(args[idIndex + 1]);
        } catch (NumberFormatException e) {
            sendError(sender, "Position must be a number.");
            return true;
        }
        if (queueManager.moveInQueue(worldName, goalId, position - 1)) {
            sendSuccess(sender, "Moved goal in queue.");
        } else {
            sendError(sender, "Goal not found in queue.");
        }
        return true;
    }

    private boolean handleQueueNext(CommandSender sender, String[] args, int idIndex, String worldName) {
        if (args.length <= idIndex) {
            queueManager.advanceToNextGoal(worldName);
            sendSuccess(sender, "Queue updated. Next goal is now active.");
            return true;
        }
        var queue = queueManager.getQueue(worldName);
        String goalId = resolveQueueGoalId(queue, args[idIndex]);
        if (goalId == null) {
            sendError(sender, "Goal not found in queue.");
            return true;
        }
        if (queueManager.activateGoal(worldName, goalId)) {
            sendSuccess(sender, "Queue updated. Selected goal is now active.");
        } else {
            sendError(sender, "Goal not found in queue.");
        }
        return true;
    }

    private String resolveQueueGoalId(java.util.List<String> queue, String arg) {
        for (String id : queue) {
            if (id.equalsIgnoreCase(arg)) {
                return id;
            }
        }
        try {
            int position = Integer.parseInt(arg);
            if (position < 1 || position > queue.size()) {
                return null;
            }
            return queue.get(position - 1);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void sendQueueUsage(CommandSender sender) {
        sender.sendMessage("§6§l=== Queue Commands ===");
        sender.sendMessage("§7/goal admin queue [world] list - Show queued goals");
        sender.sendMessage("§7/goal admin queue [world] add <id> - Add goal to queue");
        sender.sendMessage("§7/goal admin queue [world] remove <id> - Remove goal from queue");
        sender.sendMessage("§7/goal admin queue [world] move <id|position> <position> - Move goal to position");
        sender.sendMessage("§7/goal admin queue [world] next [id|position] - Activate next or selected goal");
    }

    private boolean handleBorderInfo(CommandSender sender, BorderExpansionManager borderManager) {
        BorderExpansionManager.BorderInfo info = borderManager.getInfo();
        sender.sendMessage("");
        sender.sendMessage("?6?l=== World Border Info ===");
        sender.sendMessage("?7World: ?f" + info.worldName);
        sender.sendMessage("?7Center: ?f(" + info.centerX + ", " + info.centerZ + ")");
        sender.sendMessage("?7Current Size: ?f" + info.currentSize + " blocks");
        sender.sendMessage("?7Expansion Amount: ?f" + info.expansionAmount + " blocks");
        sender.sendMessage("");
        return true;
    }

    private boolean handleBorderSet(CommandSender sender, BorderExpansionManager borderManager, String[] args, int sizeIndex, String worldName) {
        if (args.length <= sizeIndex) {
            sendError(sender, "Usage: /goal admin border [world] set <size>");
            return true;
        }

        try {
            double size = Double.parseDouble(args[sizeIndex]);
            if (borderManager.setSize(size)) {
                sendSuccess(sender, "Border size set to " + size + " blocks");
                saveBorderConfig(worldName);
            } else {
                sendError(sender, "Failed to set border size");
            }
        } catch (NumberFormatException e) {
            sendError(sender, "Size must be a number");
        }
        return true;
    }

    private boolean handleBorderExpand(CommandSender sender, BorderExpansionManager borderManager, String[] args, int amountIndex, String worldName) {
        double amount = borderManager.getBorderConfig().getExpansionAmount();

        if (args.length > amountIndex) {
            try {
                amount = Double.parseDouble(args[amountIndex]);
            } catch (NumberFormatException e) {
                sendError(sender, "Amount must be a number");
                return true;
            }
        }

        if (borderManager.expandBorder(amount)) {
            sendSuccess(sender, "Border expanded by " + amount + " blocks");
            saveBorderConfig(worldName);
        } else {
            sendError(sender, "Failed to expand border");
        }
        return true;
    }

    private boolean handleBorderCenter(CommandSender sender, BorderExpansionManager borderManager, String[] args, int coordIndex, String worldName) {
        if (args.length <= coordIndex + 1) {
            sendError(sender, "Usage: /goal admin border [world] center <x> <z>");
            return true;
        }

        try {
            double x = Double.parseDouble(args[coordIndex]);
            double z = Double.parseDouble(args[coordIndex + 1]);

            if (borderManager.setCenter(x, z)) {
                sendSuccess(sender, "Border center set to (" + x + ", " + z + ")");
                saveBorderConfig(worldName);
            } else {
                sendError(sender, "Failed to set border center");
            }
        } catch (NumberFormatException e) {
            sendError(sender, "Coordinates must be numbers");
        }
        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage("§6§l=== Goal Admin Commands ===");
        sender.sendMessage("§7/goal admin create <id> <name> <target> [reward] [world] [description]");
        sender.sendMessage("§7/goal admin delete <id>");
        sender.sendMessage("§7/goal admin info <id>");
        sender.sendMessage("§7/goal admin list");
        sender.sendMessage("§7/goal admin setprogress <id> <amount>");
        sender.sendMessage("§7/goal admin complete <id>");
        sender.sendMessage("§7/goal admin setstate <id> <state>");
        sender.sendMessage("§7/goal admin setreward <id> <amount>");
        sender.sendMessage("§7/goal admin save");
        sender.sendMessage("§7/goal admin border [world] - Border management commands");
        sender.sendMessage("§7/goal admin queue [world] - Queue management commands");
        sender.sendMessage("");
    }

    private String createProgressBar(double percentage) {
        int filled = (int) (percentage / 10);
        int empty = 10 - filled;
        
        String bar = "§a";
        for (int i = 0; i < filled; i++) bar += "█";
        bar += "§7";
        for (int i = 0; i < empty; i++) bar += "█";
        
        return bar;
    }

    /**
     * Parse command arguments handling quoted strings
     */
    private String[] parseQuotedArgs(String[] args) {
        java.util.List<String> result = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        for (String arg : args) {
            if (arg.startsWith("\"") && arg.endsWith("\"") && arg.length() > 1) {
                // Single word in quotes
                result.add(arg.substring(1, arg.length() - 1));
            } else if (arg.startsWith("\"")) {
                // Start of quoted string
                inQuotes = true;
                current.append(arg.substring(1));
            } else if (arg.endsWith("\"") && inQuotes) {
                // End of quoted string
                current.append(" ").append(arg.substring(0, arg.length() - 1));
                result.add(current.toString());
                current.setLength(0);
                inQuotes = false;
            } else if (inQuotes) {
                // Middle of quoted string
                if (current.length() > 0) {
                    current.append(" ");
                }
                current.append(arg);
            } else {
                // Regular argument
                result.add(arg);
            }
        }
        
        // Handle unclosed quotes
        if (inQuotes && current.length() > 0) {
            result.add(current.toString());
        }
        
        return result.toArray(new String[0]);
    }

    private String resolveDefaultWorld(CommandSender sender) {
        if (sender instanceof Player) {
            String worldName = ((Player) sender).getWorld().getName();
            BorderExpansionManager manager = borderRegistry.getManager(worldName);
            if (manager != null) {
                return manager.getBorderConfig().getWorldName();
            }
        }
        return borderRegistry.getDefaultWorld();
    }

    private String resolveWorldName(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        BorderExpansionManager manager = borderRegistry.getManager(candidate);
        if (manager == null) {
            return null;
        }
        return manager.getBorderConfig().getWorldName();
    }

    private boolean isBorderSubcommand(String value) {
        return value.equals("info") || value.equals("set") || value.equals("expand") || value.equals("center");
    }

    private boolean isQueueAction(String value) {
        return value.equals("list") || value.equals("add") || value.equals("remove") || value.equals("move") || value.equals("next");
    }

    /**
     * Save current border configuration to config file
     */
    private void saveBorderConfig(String worldName) {
        try {
            borderRegistry.saveBorderConfig(configManager, worldName);
        } catch (Exception e) {
            logger.warning("Failed to save border configuration: " + e.getMessage());
        }
    }
}
