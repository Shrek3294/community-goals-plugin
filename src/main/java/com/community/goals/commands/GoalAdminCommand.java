package com.community.goals.commands;

import com.community.goals.Goal;
import com.community.goals.State;
import com.community.goals.features.BorderExpansionManager;
import com.community.goals.logic.GoalProgressTracker;
import com.community.goals.persistence.ConfigManager;
import com.community.goals.persistence.PersistenceManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Collection;

/**
 * Handles /goal admin commands
 */
public class GoalAdminCommand extends BaseCommand {
    private final BorderExpansionManager borderManager;
    private final ConfigManager configManager;
    
    public GoalAdminCommand(GoalProgressTracker tracker, PersistenceManager persistence, BorderExpansionManager borderManager, ConfigManager configManager) {
        super(tracker, persistence);
        this.borderManager = borderManager;
        this.configManager = configManager;
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
            case "save":
                return handleSave(sender);
            case "border":
                return handleBorder(sender, args);
            default:
                showHelp(sender);
                return true;
        }
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sendError(sender, "Usage: /goal admin create <id> <name> <target> [description]");
            sendInfo(sender, "Example: /goal admin create diamonds \"Diamond Collection\" 10 \"Collect diamonds\"");
            return true;
        }

        // Parse arguments handling quoted strings
        String[] parsedArgs = parseQuotedArgs(args);
        
        if (parsedArgs.length < 4) {
            sendError(sender, "Usage: /goal admin create <id> <name> <target> [description]");
            sendInfo(sender, "Example: /goal admin create diamonds \"Diamond Collection\" 10 \"Collect diamonds\"");
            return true;
        }

        String id = parsedArgs[1];
        String name = parsedArgs[2];
        long target;
        
        try {
            target = Long.parseLong(parsedArgs[3]);
        } catch (NumberFormatException e) {
            sendError(sender, "Target must be a number");
            return true;
        }

        String description = parsedArgs.length > 4 ? parsedArgs[4] : "No description";

        try {
            Goal goal = tracker.createGoal(id, name, description, target);
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
            sender.sendMessage(String.format(
                "%s §f%s §7[%s] %.1f%%",
                status,
                goal.getName(),
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

        goal.complete();
        persistence.saveGoal(goal);
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
            goal.setState(state);
            persistence.saveGoal(goal);
            sendSuccess(sender, "State set to " + state.getDisplayName() + " for " + goal.getName());
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
        sender.sendMessage("§7/goal admin save");
        sender.sendMessage("§7/goal admin border - Border management commands");
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
            System.err.println("Failed to save border configuration: " + e.getMessage());
        }
    }
}
