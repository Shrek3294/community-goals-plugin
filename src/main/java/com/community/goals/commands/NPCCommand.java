package com.community.goals.commands;

import com.community.goals.Goal;
import com.community.goals.logic.GoalProgressTracker;
import com.community.goals.npc.CitizensNPCManager;
import com.community.goals.persistence.PersistenceManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles /goal npc commands for linking NPCs to goals
 */
public class NPCCommand extends BaseCommand {
    private final CitizensNPCManager npcManager;

    public NPCCommand(GoalProgressTracker tracker, PersistenceManager persistence, CitizensNPCManager npcManager) {
        super(tracker, persistence);
        this.npcManager = npcManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, PermissionManager.GOAL_ADMIN)) {
            sendError(sender, "You don't have permission to manage NPCs");
            return true;
        }

        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        String subcommand = args[1].toLowerCase();

        switch (subcommand) {
            case "link":
                return handleLink(sender, args);
            case "unlink":
                return handleUnlink(sender, args);
            case "list":
                return handleList(sender);
            default:
                sendUsage(sender);
                return true;
        }
    }

    /**
     * Handle /goal npc link <npc_name> <goal_id> command
     */
    private boolean handleLink(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sendError(sender, "Usage: /goal npc link <npc_name> <goal_id>");
            return true;
        }

        if (!npcManager.canCreateNPCs()) {
            sendError(sender, "NPC system is not available. Install FancyNpcs plugin.");
            return true;
        }

        String npcName = args[2];
        String goalId = args[3];

        // Verify goal exists
        Goal goal = tracker.getGoal(goalId);
        if (goal == null) {
            sendError(sender, "Goal not found: " + goalId);
            return true;
        }

        try {
            // Get the NPC location from the player if available
            Location location = null;
            if (sender instanceof Player) {
                location = ((Player) sender).getLocation();
            }

            // Create the goal NPC association
            npcManager.createGoalNPC(npcName, goalId, location);

            sendSuccess(sender, "§aLinked NPC §e" + npcName + "§a to goal §e" + goal.getName());
            sender.sendMessage("§7Players can now right-click the NPC to view goal information.");

            // Save changes
            persistence.saveGoals(tracker.getAllGoals());
            npcManager.saveNPCs();

            return true;
        } catch (Exception e) {
            sendError(sender, "Failed to link NPC: " + e.getMessage());
            return true;
        }
    }

    /**
     * Handle /goal npc unlink <npc_name> command
     */
    private boolean handleUnlink(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendError(sender, "Usage: /goal npc unlink <npc_name>");
            return true;
        }

        String npcName = args[2];

        try {
            if (npcManager.deleteNPC(npcName)) {
                sendSuccess(sender, "§aUnlinked NPC §e" + npcName);
                npcManager.saveNPCs();
                return true;
            } else {
                sendError(sender, "NPC not found: " + npcName);
                return true;
            }
        } catch (Exception e) {
            sendError(sender, "Failed to unlink NPC: " + e.getMessage());
            return true;
        }
    }

    /**
     * Handle /goal npc list command
     */
    private boolean handleList(CommandSender sender) {
        if (!npcManager.canCreateNPCs()) {
            sendError(sender, "NPC system is not available.");
            return true;
        }

        var allNpcs = npcManager.getAllNPCs();
        if (allNpcs.isEmpty()) {
            sendError(sender, "No NPCs are currently linked to goals.");
            return true;
        }

        sender.sendMessage("");
        sender.sendMessage("§6§l=== Linked NPCs ===");
        sender.sendMessage("§7Total: §e" + allNpcs.size());
        sender.sendMessage("");

        int index = 1;
        @SuppressWarnings("unused")
        var allNpcsArray = allNpcs.toArray();
        for (int i = 0; i < allNpcsArray.length; i++) {
            // The stub implementation returns null for NPC objects
            // In a real FancyNpcs integration, you would show actual NPC details
            sender.sendMessage("§7" + index + ". §e[NPC Linked]");
            index++;
        }

        sender.sendMessage("");
        return true;
    }

    /**
     * Send usage information
     */
    private void sendUsage(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage("§6§l=== NPC Command Usage ===");
        sender.sendMessage("§7/goal npc link <npc_name> <goal_id> - Link NPC to goal");
        sender.sendMessage("§7/goal npc unlink <npc_name> - Unlink NPC from goal");
        sender.sendMessage("§7/goal npc list - List all linked NPCs");
        sender.sendMessage("");
    }
}
