package com.community.goals.npc;

import com.community.goals.Goal;
import com.community.goals.logic.GoalProgressTracker;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

/**
 * Handles interactions with goal NPCs
 * 
 * Note: This is a simplified stub implementation.
 * NPC integration requires FancyNpcs plugin to be installed.
 */
public class NPCInteractionHandler implements Listener {
    private final CitizensNPCManager npcManager;

    public NPCInteractionHandler(CitizensNPCManager npcManager, GoalProgressTracker progressTracker) {
        this.npcManager = npcManager;
    }

    /**
     * Display goal information to a player
     */
    public void displayGoalInfo(Player player, Goal goal) {
        player.sendMessage("");
        player.sendMessage("§6§l=== " + goal.getName() + " ===");
        player.sendMessage("§7Description: " + goal.getDescription());
        player.sendMessage("§7Progress: " + goal.getCurrentProgress() + " / " + goal.getTargetProgress());
        player.sendMessage(String.format("§7Completion: §a%.1f%%", goal.getProgressPercentage()));
        player.sendMessage("§7Status: " + goal.getState().getColoredName());
        player.sendMessage("");
    }

    /**
     * Create and position a goal NPC
     */
    public GoalNPC createGoalNPC(Goal goal, String npcName, org.bukkit.Location location) {
        try {
            return npcManager.createGoalNPC(npcName, goal.getId(), location);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create goal NPC: " + e.getMessage(), e);
        }
    }

    /**
     * Check if NPC support is available
     */
    public boolean isCitizensAvailable() {
        return npcManager.canCreateNPCs();
    }
}
