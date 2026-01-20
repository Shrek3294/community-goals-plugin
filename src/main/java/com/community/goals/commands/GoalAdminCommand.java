package com.community.goals.commands;

import com.community.goals.Goal;
import com.community.goals.State;
import com.community.goals.features.BorderExpansionManager;
import com.community.goals.logic.GoalProgressTracker;
import com.community.goals.logic.GoalQueueManager;
import com.community.goals.persistence.ConfigManager;
import com.community.goals.persistence.PersistenceManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * Handles /goal admin commands
 */
public class GoalAdminCommand extends BaseCommand {
    private final BorderExpansionManager borderManager;
    private final ConfigManager configManager;
    private final GoalQueueManager queueManager;
    private final Logger logger;
    
    public GoalAdminCommand(GoalProgressTracker tracker, PersistenceManager persistence, BorderExpansionManager borderManager, ConfigManager configManager, GoalQueueManager queueManager, Logger logger) {
        super(tracker, persistence);
        this.borderManager = borderManager;
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
            sendError(sender, "Usage: /goal admin create <id> <name> <target> [reward] [description]");
            sendInfo(sender, "Example: /goal admin create diamonds \"Diamond Collection\" 10 200 \"Collect diamonds\"");
            return true;
        }

        // Parse arguments handling quoted strings
        String[] parsedArgs = parseQuotedArgs(args);
        
        if (parsedArgs.length < 4) {
            sendError(sender, "Usage: /goal admin create <id> <name> <target> [reward] [description]");
            sendInfo(sender, "Example: /goal admin create diamonds \"Diamond Collection\" 10 200 \"Collect diamonds\"");
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

        String description = parsedArgs.length > descriptionIndex ? parsedArgs[descriptionIndex] : "No description";

        try {
            Goal goal = tracker.createGoal(id, name, description, target);
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
            queueManager.handleGoalDeleted(goalId);
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
            sender.sendMessage(String.format(
                "%s §f%s%s §7[%s] %.1f%%",
                status,
                goal.getName(),
                rewardLabel,
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
                queueManager.refreshStates();
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
            sender.sendMessage("§6§l=== Border Commands ===");
            sender.sendMessage("§7/goal admin border info - Show border information");
            sender.sendMessage("§7/goal admin border set <size> - Set border size");
            sender.sendMessage("§7/goal admin border expand [amount] - Expand border");
            sender.sendMessage("§7/goal admin border center <x> <z> - Set border center");
            return true;
        }

        String borderCmd = args[1].toLowerCase();
        
        switch (borderCmd) {
            case "info":
                return handleBorderInfo(sender);
            case "set":
                return handleBorderSet(sender, args);
            case "expand":
                return handleBorderExpand(sender, args);
            case "center":
                return handleBorderCenter(sender, args);
            default:
                sendError(sender, "Unknown border command: " + borderCmd);
                return handleBorder(sender, new String[]{"border"});
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

        String action = args[1].toLowerCase();
        switch (action) {
            case "list":
                return handleQueueList(sender);
            case "add":
                return handleQueueAdd(sender, args);
            case "remove":
                return handleQueueRemove(sender, args);
            case "move":
                return handleQueueMove(sender, args);
            case "next":
                return handleQueueNext(sender);
            default:
                sendQueueUsage(sender);
                return true;
        }
    }

    private boolean handleQueueList(CommandSender sender) {
        var queue = queueManager.getQueue();
        sender.sendMessage("");
        sender.sendMessage("§6§l=== Goal Queue ===");
        if (queue.isEmpty()) {
            sender.sendMessage("§7No goals in queue.");
            sender.sendMessage("");
            return true;
        }

        int index = 1;
        for (String goalId : queue) {
            Goal goal = tracker.getGoal(goalId);
            if (goal == null) {
                continue;
            }
            String marker = index == 1 ? "§a[ACTIVE]" : "§7[QUEUED]";
            sender.sendMessage("§7" + index + ". " + marker + " §f" + goal.getName() + " §7(" + goal.getId() + ")");
            index++;
        }
        sender.sendMessage("");
        return true;
    }

    private boolean handleQueueAdd(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendError(sender, "Usage: /goal admin queue add <id>");
            return true;
        }
        String goalId = args[2];
        Goal goal = getGoalOrError(sender, goalId);
        if (goal == null) {
            return true;
        }
        queueManager.addToQueue(goalId);
        sendSuccess(sender, "Added goal to queue: " + goal.getName());
        return true;
    }

    private boolean handleQueueRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendError(sender, "Usage: /goal admin queue remove <id>");
            return true;
        }
        String goalId = args[2];
        Goal goal = getGoalOrError(sender, goalId);
        if (goal == null) {
            return true;
        }
        queueManager.removeFromQueue(goalId);
        sendSuccess(sender, "Removed goal from queue: " + goal.getName());
        return true;
    }

    private boolean handleQueueMove(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sendError(sender, "Usage: /goal admin queue move <id> <position>");
            return true;
        }
        String goalId = args[2];
        int position;
        try {
            position = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sendError(sender, "Position must be a number.");
            return true;
        }
        if (queueManager.moveInQueue(goalId, position - 1)) {
            sendSuccess(sender, "Moved goal in queue.");
        } else {
            sendError(sender, "Goal not found in queue.");
        }
        return true;
    }

    private boolean handleQueueNext(CommandSender sender) {
        queueManager.advanceToNextGoal();
        sendSuccess(sender, "Queue updated. Next goal is now active.");
        return true;
    }

    private void sendQueueUsage(CommandSender sender) {
        sender.sendMessage("§6§l=== Queue Commands ===");
        sender.sendMessage("§7/goal admin queue list - Show queued goals");
        sender.sendMessage("§7/goal admin queue add <id> - Add goal to queue");
        sender.sendMessage("§7/goal admin queue remove <id> - Remove goal from queue");
        sender.sendMessage("§7/goal admin queue move <id> <position> - Move goal to position");
        sender.sendMessage("§7/goal admin queue next - Activate next goal");
    }

    private boolean handleBorderInfo(CommandSender sender) {
        BorderExpansionManager.BorderInfo info = borderManager.getInfo();
        sender.sendMessage("");
        sender.sendMessage("§6§l=== World Border Info ===");
        sender.sendMessage("§7World: §f" + info.worldName);
        sender.sendMessage("§7Center: §f(" + info.centerX + ", " + info.centerZ + ")");
        sender.sendMessage("§7Current Size: §f" + info.currentSize + " blocks");
        sender.sendMessage("§7Expansion Amount: §f" + info.expansionAmount + " blocks");
        sender.sendMessage("");
        return true;
    }

    private boolean handleBorderSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendError(sender, "Usage: /goal admin border set <size>");
            return true;
        }

        try {
            double size = Double.parseDouble(args[2]);
            if (borderManager.setSize(size)) {
                sendSuccess(sender, "Border size set to " + size + " blocks");
                saveBorderConfig();
            } else {
                sendError(sender, "Failed to set border size");
            }
        } catch (NumberFormatException e) {
            sendError(sender, "Size must be a number");
        }
        return true;
    }

    private boolean handleBorderExpand(CommandSender sender, String[] args) {
        double amount = borderManager.getBorderConfig().getExpansionAmount();
        
        if (args.length >= 3) {
            try {
                amount = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                sendError(sender, "Amount must be a number");
                return true;
            }
        }

        if (borderManager.expandBorder(amount)) {
            sendSuccess(sender, "Border expanded by " + amount + " blocks");
            saveBorderConfig();
        } else {
            sendError(sender, "Failed to expand border");
        }
        return true;
    }

    private boolean handleBorderCenter(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sendError(sender, "Usage: /goal admin border center <x> <z>");
            return true;
        }

        try {
            double x = Double.parseDouble(args[2]);
            double z = Double.parseDouble(args[3]);
            
            if (borderManager.setCenter(x, z)) {
                sendSuccess(sender, "Border center set to (" + x + ", " + z + ")");
                saveBorderConfig();
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
        sender.sendMessage("§7/goal admin create <id> <name> <target> [description]");
        sender.sendMessage("§7/goal admin delete <id>");
        sender.sendMessage("§7/goal admin info <id>");
        sender.sendMessage("§7/goal admin list");
        sender.sendMessage("§7/goal admin setprogress <id> <amount>");
        sender.sendMessage("§7/goal admin complete <id>");
        sender.sendMessage("§7/goal admin setstate <id> <state>");
        sender.sendMessage("§7/goal admin setreward <id> <amount>");
        sender.sendMessage("§7/goal admin save");
        sender.sendMessage("§7/goal admin border - Border management commands");
        sender.sendMessage("§7/goal admin queue - Queue management commands");
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

    /**
     * Save current border configuration to config file
     */
    private void saveBorderConfig() {
        try {
            BorderExpansionManager.BorderInfo info = borderManager.getInfo();
            configManager.set("world-border.center-x", info.centerX);
            configManager.set("world-border.center-z", info.centerZ);
            configManager.set("world-border.initial-size", info.currentSize);
            configManager.saveConfig();
        } catch (Exception e) {
            logger.warning("Failed to save border configuration: " + e.getMessage());
        }
    }
}
