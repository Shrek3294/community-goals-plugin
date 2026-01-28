package com.community.goals.placeholders;

import com.community.goals.CommunityGoalsPlugin;
import com.community.goals.Goal;
import com.community.goals.features.BorderManagerRegistry;
import com.community.goals.logic.GoalProgressTracker;
import com.community.goals.logic.GoalQueueManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class GoalExpansion extends PlaceholderExpansion {
    private final CommunityGoalsPlugin plugin;
    private final GoalProgressTracker tracker;
    private final GoalQueueManager queueManager;
    private final BorderManagerRegistry borderRegistry;

    public GoalExpansion(
            CommunityGoalsPlugin plugin,
            GoalProgressTracker tracker,
            GoalQueueManager queueManager,
            BorderManagerRegistry borderRegistry
    ) {
        this.plugin = plugin;
        this.tracker = tracker;
        this.queueManager = queueManager;
        this.borderRegistry = borderRegistry;
    }

    @Override
    public String getIdentifier() {
        return "goal";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        String worldName = getWorldName(player);
        Goal goal = getActiveGoalForWorld(worldName);
        if (goal == null && borderRegistry != null) {
            goal = getActiveGoalForWorld(borderRegistry.getDefaultWorld());
        }

        String key = params == null ? "" : params.toLowerCase(Locale.ROOT);
        if (goal == null) {
            switch (key) {
                case "progress":
                case "required":
                case "percent":
                case "remaining":
                    return "0";
                default:
                    return "";
            }
        }

        switch (key) {
            case "id":
                return goal.getId();
            case "name":
                return goal.getName();
            case "description":
                return goal.getDescription();
            case "progress":
                return String.valueOf(goal.getCurrentProgress());
            case "required":
                return String.valueOf(goal.getTargetProgress());
            case "remaining":
                return String.valueOf(Math.max(0L, goal.getTargetProgress() - goal.getCurrentProgress()));
            case "percent":
                return formatPercent(goal.getProgressPercentage());
            case "state":
                return goal.getState().name().toLowerCase(Locale.ROOT);
            case "world":
                return goal.getWorldName();
            default:
                return "";
        }
    }

    private String getWorldName(Player player) {
        if (player == null || player.getWorld() == null) {
            return borderRegistry != null ? borderRegistry.getDefaultWorld() : null;
        }
        return player.getWorld().getName();
    }

    private Goal getActiveGoalForWorld(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return null;
        }

        if (queueManager != null && queueManager.isEnabled()) {
            List<String> queue = queueManager.getQueue(worldName);
            if (queue != null && !queue.isEmpty()) {
                Goal queuedGoal = tracker.getGoal(queue.get(0));
                if (queuedGoal != null && !queuedGoal.isCompleted()) {
                    return queuedGoal;
                }
            }
        }

        List<Goal> activeGoals = tracker.getActiveGoalsForWorld(worldName);
        activeGoals.sort(Comparator.comparingLong(Goal::getCreatedAt));
        return activeGoals.isEmpty() ? null : activeGoals.get(0);
    }

    private String formatPercent(double percent) {
        if (Double.isNaN(percent) || Double.isInfinite(percent)) {
            return "0";
        }
        return String.format(Locale.US, "%.1f", percent);
    }
}
