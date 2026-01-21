package com.community.goals.npc;

import com.community.goals.Goal;
import com.community.goals.gui.GoalGuiManager;
import com.community.goals.logic.GoalProgressTracker;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.events.NpcInteractEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Handles interactions with goal NPCs via FancyNpcs.
 */
public class NPCInteractionHandler implements Listener {
    private final FancyNpcManager npcManager;
    private final GoalProgressTracker progressTracker;
    private final GoalGuiManager guiManager;

    public NPCInteractionHandler(FancyNpcManager npcManager, GoalProgressTracker progressTracker, GoalGuiManager guiManager) {
        this.npcManager = npcManager;
        this.progressTracker = progressTracker;
        this.guiManager = guiManager;
    }

    /**
     * Display goal information to a player
     */
    public void displayGoalInfo(Player player, Goal goal) {
        player.sendMessage("");
        player.sendMessage("§6§l=== " + goal.getName() + " ===");
        player.sendMessage("§7Description: " + goal.getDescription());
        player.sendMessage("§7World: §f" + goal.getWorldName());
        player.sendMessage("§7Progress: " + goal.getCurrentProgress() + " / " + goal.getTargetProgress());
        player.sendMessage(String.format("§7Completion: §a%.1f%%", goal.getProgressPercentage()));
        if (goal.getRewardExpansion() > 0) {
            player.sendMessage("§7Reward Expansion: §f" + goal.getRewardExpansion() + " blocks");
        }
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
    public boolean isFancyNpcsAvailable() {
        return npcManager.canCreateNPCs();
    }

    @EventHandler
    public void onNpcInteract(NpcInteractEvent event) {
        if (!npcManager.canCreateNPCs()) {
            return;
        }

        Npc npc = event.getNpc();
        if (npc == null || npc.getData() == null) {
            return;
        }

        String npcName = npc.getData().getName();
        String centralWorld = npcManager.getCentralWorldForNpc(npcName);
        if (centralWorld != null) {
            guiManager.openGoalsMenu(event.getPlayer(), centralWorld);
            event.setCancelled(true);
            return;
        }

        String goalId = npcManager.getGoalIdForNpc(npcName);
        if (goalId == null) {
            return;
        }

        Goal goal = progressTracker.getGoal(goalId);
        if (goal == null) {
            return;
        }

        displayGoalInfo(event.getPlayer(), goal);
        event.setCancelled(true);
    }
}
