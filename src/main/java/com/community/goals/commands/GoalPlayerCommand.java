package com.community.goals.commands;

import com.community.goals.Goal;
import com.community.goals.logic.GoalProgressTracker;
import com.community.goals.logic.TurnInHandler;
import com.community.goals.persistence.PersistenceManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Handles /goal player commands (view, turn-in, etc.)
 */
public class GoalPlayerCommand extends BaseCommand {
    private final TurnInHandler turnInHandler;

    public GoalPlayerCommand(GoalProgressTracker tracker, PersistenceManager persistence, 
                            TurnInHandler turnInHandler) {
        super(tracker, persistence);
        this.turnInHandler = turnInHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, PermissionManager.GOAL_USE)) {
            sendError(sender, "You don't have permission to use goal commands");
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "view":
                return handleView(sender, args);
            case "list":
                return handleList(sender);
            case "progress":
                return handleProgress(sender, args);
            case "info":
                return handleInfo(sender, args);
            case "turnin":
                return handleTurnIn(sender, args);
            default:
                showHelp(sender);
                return true;
        }
    }

    private boolean handleView(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendError(sender, "Usage: /goal view <id>");
            return true;
        }

        String goalId = args[1];
        Goal goal = getGoalOrError(sender, goalId);
        if (goal == null) return true;

        displayGoalInfo(sender, goal);
        return true;
    }

    private boolean handleList(CommandSender sender) {
        Collection<Goal> goals = tracker.getActiveGoals();

        sender.sendMessage("");
        sender.sendMessage("§6§l=== Active Community Goals ===");
        sender.sendMessage("§7Total: " + goals.size());
        sender.sendMessage("");

        for (Goal goal : goals) {
            String progressBar = createProgressBar(goal.getProgressPercentage());
            sender.sendMessage(String.format(
                "§f%s §7[%s] %.1f%%",
                goal.getName(),
                progressBar,
                goal.getProgressPercentage()
            ));
        }

        sender.sendMessage("");
        sender.sendMessage("§7Use /goal view <id> for more details");
        sender.sendMessage("");
        return true;
    }

    private boolean handleProgress(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendError(sender, "Usage: /goal progress <id>");
            return true;
        }

        String goalId = args[1];
        Goal goal = getGoalOrError(sender, goalId);
        if (goal == null) return true;

        sender.sendMessage("");
        sender.sendMessage("§6" + goal.getName());
        sender.sendMessage(String.format("§7Progress: §f%d / %d", 
            goal.getCurrentProgress(), goal.getTargetProgress()));
        sender.sendMessage(String.format("§7Complete: §a%.1f%%", goal.getProgressPercentage()));

        long remaining = goal.getTargetProgress() - goal.getCurrentProgress();
        if (remaining > 0) {
            sender.sendMessage(String.format("§7Remaining: §e%d", remaining));
        } else {
            sender.sendMessage("§a✓ Goal completed!");
        }
        sender.sendMessage("");
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendError(sender, "Usage: /goal info <id>");
            return true;
        }

        String goalId = args[1];
        Goal goal = getGoalOrError(sender, goalId);
        if (goal == null) return true;

        displayGoalInfo(sender, goal);
        return true;
    }

    @SuppressWarnings("deprecation")
    private boolean handleTurnIn(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendError(sender, "Only players can use turn-in");
            return true;
        }

        if (args.length < 3) {
            sendError(sender, "Usage: /goal turnin <id> <amount>");
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

        Player player = getPlayer(sender);

        // Validate turn-in
        TurnInHandler.ValidationResult validation = turnInHandler.validateTurnIn(goalId, amount);
        if (!validation.isValid()) {
            sendError(sender, validation.getReason());
            return true;
        }

        // Process turn-in
        TurnInHandler.TurnInResult result = turnInHandler.processTurnIn(goalId, amount, player.getName());
        if (!result.isSuccess()) {
            sendError(sender, result.getMessage());
            return true;
        }

        sendSuccess(sender, "Progress added! " + result.getNewProgress() + " / " + result.getTargetProgress());
        
        if (result.isGoalCompleted()) {
            //noinspection deprecation
            org.bukkit.Bukkit.getServer().broadcastMessage("§a§l✓ Goal completed! " + result.getGoalId());
        }

        return true;
    }

    private void displayGoalInfo(CommandSender sender, Goal goal) {
        sender.sendMessage("");
        sender.sendMessage("§6§l=== " + goal.getName() + " ===");
        sender.sendMessage("§7Description: " + goal.getDescription());
        sender.sendMessage("§7Progress: " + goal.getCurrentProgress() + " / " + goal.getTargetProgress());
        sender.sendMessage(String.format("§7Completion: §a%.1f%%", goal.getProgressPercentage()));
        sender.sendMessage("§7Status: " + goal.getState().getColoredName());
        sender.sendMessage("");
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage("§6§l=== Goal Commands ===");
        sender.sendMessage("§7/goal list");
        sender.sendMessage("§7/goal view <id>");
        sender.sendMessage("§7/goal progress <id>");
        sender.sendMessage("§7/goal info <id>");
        sender.sendMessage("§7/goal turnin <id> <amount>");
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
}
