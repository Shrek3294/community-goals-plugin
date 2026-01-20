package com.community.goals.logic;

import com.community.goals.Goal;
import com.community.goals.State;
import com.community.goals.persistence.PersistenceManager;

import java.util.*;

public class GoalQueueManager {
    private final GoalProgressTracker tracker;
    private final PersistenceManager persistence;
    private final boolean queueEnabled;
    private final List<String> queue;

    public GoalQueueManager(GoalProgressTracker tracker, PersistenceManager persistence, boolean queueEnabled) {
        this.tracker = tracker;
        this.persistence = persistence;
        this.queueEnabled = queueEnabled;
        this.queue = new ArrayList<>();
        loadQueue();
        if (queueEnabled) {
            syncQueueWithGoals();
        }
    }

    public boolean isEnabled() {
        return queueEnabled;
    }

    public List<String> getQueue() {
        return Collections.unmodifiableList(queue);
    }

    public void handleGoalCreated(Goal goal) {
        if (!queueEnabled) {
            return;
        }
        if (!queue.contains(goal.getId())) {
            queue.add(goal.getId());
        }
        enforceQueueStates();
        saveQueue();
    }

    public void handleGoalCompleted(String goalId) {
        if (!queueEnabled) {
            return;
        }
        boolean wasActive = isActiveGoal(goalId);
        queue.removeIf(id -> id.equalsIgnoreCase(goalId));
        if (wasActive) {
            activateNextGoal();
        }
        saveQueue();
    }

    public void handleGoalDeleted(String goalId) {
        if (!queueEnabled) {
            return;
        }
        boolean wasActive = isActiveGoal(goalId);
        queue.removeIf(id -> id.equalsIgnoreCase(goalId));
        if (wasActive) {
            activateNextGoal();
        }
        saveQueue();
    }

    public void addToQueue(String goalId) {
        if (!queueEnabled) {
            return;
        }
        if (!queue.contains(goalId)) {
            queue.add(goalId);
        }
        enforceQueueStates();
        saveQueue();
    }

    public void removeFromQueue(String goalId) {
        if (!queueEnabled) {
            return;
        }
        handleGoalDeleted(goalId);
    }

    public boolean moveInQueue(String goalId, int newIndex) {
        if (!queueEnabled) {
            return false;
        }
        int currentIndex = indexOf(goalId);
        if (currentIndex < 0) {
            return false;
        }
        queue.remove(currentIndex);
        int targetIndex = Math.max(0, Math.min(queue.size(), newIndex));
        queue.add(targetIndex, goalId);
        enforceQueueStates();
        saveQueue();
        return true;
    }

    public void activateNextGoal() {
        if (!queueEnabled) {
            return;
        }
        enforceQueueStates();
        saveQueue();
    }

    public void advanceToNextGoal() {
        if (!queueEnabled) {
            return;
        }
        if (queue.size() <= 1) {
            enforceQueueStates();
            saveQueue();
            return;
        }
        String current = queue.remove(0);
        queue.add(current);
        enforceQueueStates();
        saveQueue();
    }

    public void refreshStates() {
        if (!queueEnabled) {
            return;
        }
        enforceQueueStates();
        saveQueue();
    }

    private void loadQueue() {
        queue.clear();
        queue.addAll(persistence.loadGoalQueue());
    }

    private void saveQueue() {
        persistence.saveGoalQueue(queue);
    }

    private void syncQueueWithGoals() {
        Set<String> validGoals = new HashSet<>();
        for (Goal goal : tracker.getAllGoals()) {
            if (goal.getState() == State.COMPLETED || goal.getState() == State.CANCELLED) {
                continue;
            }
            validGoals.add(goal.getId());
        }

        queue.removeIf(id -> !validGoals.contains(id));

        if (queue.isEmpty()) {
            List<Goal> goals = new ArrayList<>(tracker.getAllGoals());
            goals.removeIf(goal -> goal.getState() == State.COMPLETED || goal.getState() == State.CANCELLED);
            goals.sort(Comparator.comparingLong(Goal::getCreatedAt));
            for (Goal goal : goals) {
                queue.add(goal.getId());
            }
        }

        enforceQueueStates();
        saveQueue();
    }

    private void enforceQueueStates() {
        queue.removeIf(id -> tracker.getGoal(id) == null);
        if (queue.isEmpty()) {
            return;
        }

        Goal activeGoal = tracker.getGoal(queue.get(0));
        if (activeGoal == null || activeGoal.getState() == State.COMPLETED || activeGoal.getState() == State.CANCELLED) {
            queue.remove(0);
            enforceQueueStates();
            return;
        }

        String activeId = queue.get(0);
        for (int i = 0; i < queue.size(); i++) {
            String id = queue.get(i);
            Goal goal = tracker.getGoal(id);
            if (goal == null) {
                continue;
            }
            if (goal.getState() == State.COMPLETED || goal.getState() == State.CANCELLED) {
                continue;
            }
            if (id.equals(activeId)) {
                if (goal.getState() != State.ACTIVE) {
                    goal.setState(State.ACTIVE);
                    persistence.saveGoal(goal);
                }
            } else {
                if (goal.getState() != State.PAUSED) {
                    goal.setState(State.PAUSED);
                    persistence.saveGoal(goal);
                }
            }
        }
    }

    private boolean isActiveGoal(String goalId) {
        if (queue.isEmpty()) {
            return false;
        }
        return queue.get(0).equalsIgnoreCase(goalId);
    }

    private int indexOf(String goalId) {
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).equalsIgnoreCase(goalId)) {
                return i;
            }
        }
        return -1;
    }
}
