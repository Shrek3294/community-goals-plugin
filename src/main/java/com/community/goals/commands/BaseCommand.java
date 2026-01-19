package com.community.goals.commands;

import com.community.goals.Goal;
import com.community.goals.logic.GoalProgressTracker;
import com.community.goals.persistence.PersistenceManager;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Base command handler with permission checks
 */
public abstract class BaseCommand implements CommandExecutor {
    protected final GoalProgressTracker tracker;
    protected final PersistenceManager persistence;
    protected final PermissionManager permissionManager;

    public BaseCommand(GoalProgressTracker tracker, PersistenceManager persistence) {
        this.tracker = tracker;
        this.persistence = persistence;
        this.permissionManager = new PermissionManager();
    }

    /**
     * Check if sender has permission
     */
    protected boolean hasPermission(CommandSender sender, String permission) {
        if (sender.isOp()) {
            return true;
        }
        if (sender instanceof Player) {
            return sender.hasPermission(permission);
        }
        return false;
    }

    /**
     * Send error message
     */
    protected void sendError(CommandSender sender, String message) {
        sender.sendMessage("§c✗ Error: " + message);
    }

    /**
     * Send success message
     */
    protected void sendSuccess(CommandSender sender, String message) {
        sender.sendMessage("§a✓ " + message);
    }

    /**
     * Send info message
     */
    protected void sendInfo(CommandSender sender, String message) {
        sender.sendMessage("§7ℹ " + message);
    }

    /**
     * Check if command sender is a player
     */
    protected boolean isPlayer(CommandSender sender) {
        return sender instanceof Player;
    }

    /**
     * Get player from sender
     */
    protected Player getPlayer(CommandSender sender) {
        if (sender instanceof Player) {
            return (Player) sender;
        }
        return null;
    }

    /**
     * Get a goal or send error if not found
     */
    protected Goal getGoalOrError(CommandSender sender, String goalId) {
        Goal goal = tracker.getGoal(goalId);
        if (goal == null) {
            sendError(sender, "Goal not found: " + goalId);
            return null;
        }
        return goal;
    }
}
