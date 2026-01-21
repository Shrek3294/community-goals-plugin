package com.community.goals.logic;

import com.community.goals.Goal;
import com.community.goals.persistence.PersistenceManager;

import java.util.*;

/**
 * Manages goal progress tracking and state
 */
public class GoalProgressTracker {
    private final Map<String, Goal> goals;
    private final PersistenceManager persistenceManager;
    private final List<ProgressListener> listeners;

    public GoalProgressTracker(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
        this.goals = new HashMap<>();
        this.listeners = new ArrayList<>();
        loadGoalsFromStorage();
    }

    /**
     * Load goals from persistent storage
     */
    private void loadGoalsFromStorage() {
        List<Goal> loadedGoals = persistenceManager.loadGoals();
        for (Goal goal : loadedGoals) {
            goals.put(goal.getId(), goal);
        }
    }

    /**
     * Create a new goal
     */
    public Goal createGoal(String id, String name, String description, long targetProgress, String worldName) {
        if (goals.containsKey(id)) {
            throw new IllegalArgumentException("Goal with id '" + id + "' already exists");
        }

        Goal goal = new Goal(id, name, description, targetProgress, worldName);
        goals.put(id, goal);
        persistenceManager.saveGoal(goal);
        notifyGoalCreated(goal);
        return goal;
    }

    /**
     * Get a goal by ID
     */
    public Goal getGoal(String goalId) {
        return goals.get(goalId);
    }

    /**
     * Get all goals
     */
    public Collection<Goal> getAllGoals() {
        return Collections.unmodifiableCollection(goals.values());
    }

    public Collection<Goal> getGoalsForWorld(String worldName) {
        List<Goal> result = new ArrayList<>();
        if (worldName == null) {
            return result;
        }
        for (Goal goal : goals.values()) {
            if (worldName.equalsIgnoreCase(goal.getWorldName())) {
                result.add(goal);
            }
        }
        return result;
    }

    /**
     * Get active goals only
     */
    public List<Goal> getActiveGoals() {
        List<Goal> activeGoals = new ArrayList<>();
        for (Goal goal : goals.values()) {
            if (!goal.isCompleted()) {
                activeGoals.add(goal);
            }
        }
        return activeGoals;
    }

    public List<Goal> getActiveGoalsForWorld(String worldName) {
        List<Goal> activeGoals = new ArrayList<>();
        if (worldName == null) {
            return activeGoals;
        }
        for (Goal goal : goals.values()) {
            if (!goal.isCompleted() && worldName.equalsIgnoreCase(goal.getWorldName())) {
                activeGoals.add(goal);
            }
        }
        return activeGoals;
    }

    /**
     * Get completed goals
     */
    public List<Goal> getCompletedGoals() {
        List<Goal> completedGoals = new ArrayList<>();
        for (Goal goal : goals.values()) {
            if (goal.isCompleted()) {
                completedGoals.add(goal);
            }
        }
        return completedGoals;
    }

    /**
     * Add progress to a goal
     */
    public void addProgress(String goalId, long amount) {
        Goal goal = goals.get(goalId);
        if (goal == null) {
            throw new IllegalArgumentException("Goal not found: " + goalId);
        }

        long oldProgress = goal.getCurrentProgress();
        boolean wasCompleted = goal.isCompleted();
        
        goal.addProgress(amount);
        persistenceManager.saveGoal(goal);
        
        notifyProgressUpdated(goal, oldProgress, amount);
        
        if (!wasCompleted && goal.isCompleted()) {
            notifyGoalCompleted(goal);
        }
    }

    /**
     * Set exact progress for a goal (useful for admin commands)
     */
    public void setProgress(String goalId, long amount) {
        Goal goal = goals.get(goalId);
        if (goal == null) {
            throw new IllegalArgumentException("Goal not found: " + goalId);
        }

        long oldProgress = goal.getCurrentProgress();
        boolean wasCompleted = goal.isCompleted();
        
        // Reset progress and add the new amount
        goal.addProgress(amount - oldProgress);
        persistenceManager.saveGoal(goal);
        
        notifyProgressUpdated(goal, oldProgress, amount - oldProgress);
        
        if (!wasCompleted && goal.isCompleted()) {
            notifyGoalCompleted(goal);
        }
    }

    /**
     * Delete a goal
     */
    public void deleteGoal(String goalId) {
        Goal goal = goals.remove(goalId);
        if (goal != null) {
            persistenceManager.deleteGoal(goalId);
            notifyGoalDeleted(goal);
        }
    }

    /**
     * Update a goal's target progress
     */
    public void updateTargetProgress(String goalId, long newTarget) {
        Goal goal = goals.get(goalId);
        if (goal == null) {
            throw new IllegalArgumentException("Goal not found: " + goalId);
        }

        goal.setTargetProgress(newTarget);
        persistenceManager.saveGoal(goal);
        notifyGoalUpdated(goal);
    }

    /**
     * Get goal progress as a percentage
     */
    public double getProgressPercentage(String goalId) {
        Goal goal = goals.get(goalId);
        if (goal == null) {
            return 0.0;
        }
        return goal.getProgressPercentage();
    }

    /**
     * Check if a goal exists
     */
    public boolean goalExists(String goalId) {
        return goals.containsKey(goalId);
    }

    /**
     * Get total community progress (sum of all percentages divided by goal count)
     */
    public double getTotalCommunityProgress() {
        if (goals.isEmpty()) {
            return 0.0;
        }

        double totalProgress = 0;
        for (Goal goal : goals.values()) {
            totalProgress += goal.getProgressPercentage();
        }

        return totalProgress / goals.size();
    }

    /**
     * Register a progress listener
     */
    public void addListener(ProgressListener listener) {
        listeners.add(listener);
    }

    /**
     * Unregister a progress listener
     */
    public void removeListener(ProgressListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify all listeners that progress was updated
     */
    private void notifyProgressUpdated(Goal goal, long previousProgress, long amountAdded) {
        for (ProgressListener listener : listeners) {
            listener.onProgressUpdated(goal, previousProgress, amountAdded);
        }
    }

    /**
     * Notify all listeners that a goal was completed
     */
    private void notifyGoalCompleted(Goal goal) {
        for (ProgressListener listener : listeners) {
            listener.onGoalCompleted(goal);
        }
    }

    /**
     * Notify all listeners that a goal was created
     */
    private void notifyGoalCreated(Goal goal) {
        for (ProgressListener listener : listeners) {
            listener.onGoalCreated(goal);
        }
    }

    /**
     * Notify all listeners that a goal was deleted
     */
    private void notifyGoalDeleted(Goal goal) {
        for (ProgressListener listener : listeners) {
            listener.onGoalDeleted(goal);
        }
    }

    /**
     * Notify all listeners that a goal was updated
     */
    private void notifyGoalUpdated(Goal goal) {
        for (ProgressListener listener : listeners) {
            listener.onGoalUpdated(goal);
        }
    }

    /**
     * Save all goals to storage (useful for periodic saves)
     */
    public void saveAllGoals() {
        persistenceManager.saveGoals(goals.values());
    }

    /**
     * Listener interface for goal progress events
     */
    public interface ProgressListener {
        void onProgressUpdated(Goal goal, long previousProgress, long amountAdded);
        void onGoalCompleted(Goal goal);
        void onGoalCreated(Goal goal);
        void onGoalDeleted(Goal goal);
        void onGoalUpdated(Goal goal);
    }
}
