package com.community.goals.features;

import com.community.goals.Goal;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Manages progress announcements and displays
 */
public class ProgressAnnouncementManager {
    private final Map<String, BossBar> goalBossBars;
    private final Set<Player> subscribedPlayers;

    public ProgressAnnouncementManager() {
        this.goalBossBars = new HashMap<>();
        this.subscribedPlayers = new HashSet<>();
    }

    /**
     * Create or update a boss bar for a goal
     */
    public BossBar createGoalBossBar(Goal goal) {
        String goalId = goal.getId();
        
        // Remove old boss bar if exists
        if (goalBossBars.containsKey(goalId)) {
            BossBar old = goalBossBars.get(goalId);
            old.removeAll();
        }

        double progress = Math.min(1.0, goal.getProgressPercentage() / 100.0);
        BossBar bossBar = Bukkit.createBossBar(
            "§6" + goal.getName(),
            getColorForProgress(goal.getProgressPercentage()),
            BarStyle.SEGMENTED_10
        );
        bossBar.setProgress(progress);
        bossBar.setVisible(true);

        goalBossBars.put(goalId, bossBar);
        return bossBar;
    }

    /**
     * Update a goal's boss bar progress
     */
    public void updateGoalProgress(Goal goal) {
        BossBar bossBar = goalBossBars.get(goal.getId());
        if (bossBar != null) {
            double progress = Math.min(1.0, goal.getProgressPercentage() / 100.0);
            bossBar.setProgress(progress);
            
            String title = String.format("§6%s §7[%.1f%%]", 
                goal.getName(), goal.getProgressPercentage());
            bossBar.setTitle(title);
            bossBar.setColor(getColorForProgress(goal.getProgressPercentage()));
        }
    }

    /**
     * Show a goal's boss bar to a player
     */
    public void showGoalBossBar(Player player, Goal goal) {
        BossBar bossBar = goalBossBars.get(goal.getId());
        if (bossBar != null) {
            bossBar.addPlayer(player);
            subscribedPlayers.add(player);
        }
    }

    /**
     * Hide a goal's boss bar from a player
     */
    public void hideGoalBossBar(Player player, Goal goal) {
        BossBar bossBar = goalBossBars.get(goal.getId());
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
    }

    /**
     * Show all boss bars to a player
     */
    public void showAllBossBars(Player player) {
        for (BossBar bossBar : goalBossBars.values()) {
            bossBar.addPlayer(player);
        }
        subscribedPlayers.add(player);
    }

    /**
     * Hide all boss bars from a player
     */
    public void hideAllBossBars(Player player) {
        for (BossBar bossBar : goalBossBars.values()) {
            bossBar.removePlayer(player);
        }
        subscribedPlayers.remove(player);
    }

    /**
     * Send an action bar message to a player
     */
    @SuppressWarnings("deprecation")
    public void sendActionBar(Player player, String message) {
        player.sendActionBar(message);
    }

    @SuppressWarnings("deprecation")
    public void broadcastGoalCompletion(Goal goal) {
        String message = String.format(
            "§a§l✓ §aGoal completed! §e%s §7has been accomplished!",
            goal.getName()
        );
        org.bukkit.Bukkit.getServer().broadcastMessage(message);
    }

    /**
     * Broadcast a progress update to all players
     */
    @SuppressWarnings("deprecation")
    public void broadcastProgressUpdate(Goal goal, long amountAdded) {
        String message = String.format(
            "§7[%s] §6Progress: %d/%d (%.1f%%) §7+%d",
            goal.getName(),
            goal.getCurrentProgress(),
            goal.getTargetProgress(),
            goal.getProgressPercentage(),
            amountAdded
        );
        org.bukkit.Bukkit.getServer().broadcastMessage(message);
    }

    /**
     * Get the bar color for a progress percentage
     */
    private BarColor getColorForProgress(double percentage) {
        if (percentage >= 100) {
            return BarColor.GREEN;
        } else if (percentage >= 75) {
            return BarColor.YELLOW;
        } else if (percentage >= 50) {
            return BarColor.BLUE;
        } else if (percentage >= 25) {
            return BarColor.PURPLE;
        } else {
            return BarColor.RED;
        }
    }

    /**
     * Remove a goal's boss bar
     */
    public void removeGoalBossBar(String goalId) {
        BossBar bossBar = goalBossBars.remove(goalId);
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    /**
     * Remove all boss bars
     */
    public void removeAllBossBars() {
        for (BossBar bossBar : goalBossBars.values()) {
            bossBar.removeAll();
        }
        goalBossBars.clear();
        subscribedPlayers.clear();
    }

    /**
     * Get all tracked boss bars
     */
    public Map<String, BossBar> getAllBossBars() {
        return Collections.unmodifiableMap(goalBossBars);
    }

    /**
     * Get subscribed players
     */
    public Set<Player> getSubscribedPlayers() {
        return Collections.unmodifiableSet(subscribedPlayers);
    }
}
