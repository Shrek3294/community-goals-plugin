package com.community.goals.logic;

import com.community.goals.Goal;
import com.community.goals.State;
import com.community.goals.persistence.PersistenceManager;

import java.util.*;

public class GoalQueueManager {
    private final GoalProgressTracker tracker;
    private final PersistenceManager persistence;
    private final boolean queueEnabled;
    private final Map<String, List<String>> queues;
    private final String defaultWorld;

    public GoalQueueManager(GoalProgressTracker tracker, PersistenceManager persistence, boolean queueEnabled, String defaultWorld) {
        this.tracker = tracker;
        this.persistence = persistence;
        this.queueEnabled = queueEnabled;
        this.defaultWorld = defaultWorld;
        this.queues = new HashMap<>();
        loadQueue();
        if (queueEnabled) {
            syncQueueWithGoals();
        }
    }

    public boolean isEnabled() {
        return queueEnabled;
    }

    public List<String> getQueue(String worldName) {
        if (!queueEnabled) {
            return Collections.emptyList();
        }
        List<String> queue = queues.get(normalize(worldName));
        if (queue == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(queue);
    }

    public void handleGoalCreated(Goal goal) {
        if (!queueEnabled || goal == null) {
            return;
        }
        List<String> queue = getOrCreateQueue(goal.getWorldName());
        if (!queue.contains(goal.getId())) {
            queue.add(goal.getId());
        }
        enforceQueueStates(normalize(goal.getWorldName()));
        saveQueue();
    }

    public void handleGoalCompleted(Goal goal) {
        if (!queueEnabled || goal == null) {
            return;
        }
        String worldKey = normalize(goal.getWorldName());
        boolean wasActive = isActiveGoal(worldKey, goal.getId());
        List<String> queue = queues.get(worldKey);
        if (queue != null) {
            queue.removeIf(id -> id.equalsIgnoreCase(goal.getId()));
            if (wasActive) {
                activateNextGoal(goal.getWorldName());
            }
            saveQueue();
        }
    }

    public void handleGoalDeleted(Goal goal) {
        if (!queueEnabled || goal == null) {
            return;
        }
        String worldKey = normalize(goal.getWorldName());
        boolean wasActive = isActiveGoal(worldKey, goal.getId());
        List<String> queue = queues.get(worldKey);
        if (queue != null) {
            queue.removeIf(id -> id.equalsIgnoreCase(goal.getId()));
            if (wasActive) {
                activateNextGoal(goal.getWorldName());
            }
            saveQueue();
        }
    }

    public void addToQueue(String worldName, String goalId) {
        if (!queueEnabled) {
            return;
        }
        List<String> queue = getOrCreateQueue(worldName);
        if (!queue.contains(goalId)) {
            queue.add(goalId);
        }
        enforceQueueStates(normalize(worldName));
        saveQueue();
    }

    public void removeFromQueue(String worldName, String goalId) {
        if (!queueEnabled) {
            return;
        }
        List<String> queue = queues.get(normalize(worldName));
        if (queue == null) {
            return;
        }
        boolean wasActive = isActiveGoal(normalize(worldName), goalId);
        queue.removeIf(id -> id.equalsIgnoreCase(goalId));
        if (wasActive) {
            activateNextGoal(worldName);
        }
        saveQueue();
    }

    public boolean moveInQueue(String worldName, String goalId, int newIndex) {
        if (!queueEnabled) {
            return false;
        }
        List<String> queue = queues.get(normalize(worldName));
        if (queue == null) {
            return false;
        }
        int currentIndex = indexOf(queue, goalId);
        if (currentIndex < 0) {
            return false;
        }
        queue.remove(currentIndex);
        int targetIndex = Math.max(0, Math.min(queue.size(), newIndex));
        queue.add(targetIndex, goalId);
        enforceQueueStates(normalize(worldName));
        saveQueue();
        return true;
    }

    public void activateNextGoal(String worldName) {
        if (!queueEnabled) {
            return;
        }
        enforceQueueStates(normalize(worldName));
        saveQueue();
    }

    public void advanceToNextGoal(String worldName) {
        if (!queueEnabled) {
            return;
        }
        List<String> queue = queues.get(normalize(worldName));
        if (queue == null || queue.size() <= 1) {
            enforceQueueStates(normalize(worldName));
            saveQueue();
            return;
        }
        String current = queue.remove(0);
        queue.add(current);
        enforceQueueStates(normalize(worldName));
        saveQueue();
    }

    public boolean activateGoal(String worldName, String goalId) {
        if (!queueEnabled) {
            return false;
        }
        List<String> queue = queues.get(normalize(worldName));
        if (queue == null) {
            return false;
        }
        int index = indexOf(queue, goalId);
        if (index < 0) {
            return false;
        }
        if (index != 0) {
            queue.remove(index);
            queue.add(0, goalId);
        }
        enforceQueueStates(normalize(worldName));
        saveQueue();
        return true;
    }

    public void refreshStates() {
        if (!queueEnabled) {
            return;
        }
        for (String worldKey : new HashSet<>(queues.keySet())) {
            enforceQueueStates(worldKey);
        }
        saveQueue();
    }

    public void refreshStates(String worldName) {
        if (!queueEnabled) {
            return;
        }
        enforceQueueStates(normalize(worldName));
        saveQueue();
    }

    private void loadQueue() {
        queues.clear();
        queues.putAll(persistence.loadGoalQueues(defaultWorld));
    }

    private void saveQueue() {
        persistence.saveGoalQueues(queues);
    }

    private void syncQueueWithGoals() {
        Map<String, List<Goal>> goalsByWorld = new HashMap<>();
        for (Goal goal : tracker.getAllGoals()) {
            if (goal.getState() == State.COMPLETED || goal.getState() == State.CANCELLED) {
                continue;
            }
            String worldKey = normalize(goal.getWorldName());
            goalsByWorld.computeIfAbsent(worldKey, key -> new ArrayList<>()).add(goal);
        }

        for (Map.Entry<String, List<String>> entry : queues.entrySet()) {
            String worldKey = entry.getKey();
            List<String> queue = entry.getValue();
            Set<String> validGoals = new HashSet<>();
            List<Goal> goals = goalsByWorld.getOrDefault(worldKey, Collections.emptyList());
            for (Goal goal : goals) {
                validGoals.add(goal.getId());
            }
            queue.removeIf(id -> !validGoals.contains(id));
        }

        for (Map.Entry<String, List<Goal>> entry : goalsByWorld.entrySet()) {
            String worldKey = entry.getKey();
            List<String> queue = queues.get(worldKey);
            if (queue == null || queue.isEmpty()) {
                queue = new ArrayList<>();
                List<Goal> goals = entry.getValue();
                goals.sort(Comparator.comparingLong(Goal::getCreatedAt));
                for (Goal goal : goals) {
                    queue.add(goal.getId());
                }
                queues.put(worldKey, queue);
            }
        }

        for (String worldKey : new HashSet<>(queues.keySet())) {
            enforceQueueStates(worldKey);
        }
        saveQueue();
    }

    private void enforceQueueStates(String worldKey) {
        List<String> queue = queues.get(worldKey);
        if (queue == null) {
            return;
        }

        queue.removeIf(id -> {
            Goal goal = tracker.getGoal(id);
            if (goal == null) {
                return true;
            }
            if (!worldKey.equals(normalize(goal.getWorldName()))) {
                return true;
            }
            return goal.getState() == State.COMPLETED || goal.getState() == State.CANCELLED;
        });

        if (queue.isEmpty()) {
            return;
        }

        while (!queue.isEmpty()) {
            Goal activeGoal = tracker.getGoal(queue.get(0));
            if (activeGoal == null || activeGoal.getState() == State.COMPLETED || activeGoal.getState() == State.CANCELLED) {
                queue.remove(0);
                continue;
            }
            break;
        }

        if (queue.isEmpty()) {
            return;
        }

        String activeId = queue.get(0);
        for (String id : queue) {
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

    private boolean isActiveGoal(String worldKey, String goalId) {
        List<String> queue = queues.get(worldKey);
        if (queue == null || queue.isEmpty()) {
            return false;
        }
        return queue.get(0).equalsIgnoreCase(goalId);
    }

    private List<String> getOrCreateQueue(String worldName) {
        String worldKey = normalize(worldName);
        return queues.computeIfAbsent(worldKey, key -> new ArrayList<>());
    }

    private int indexOf(List<String> queue, String goalId) {
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).equalsIgnoreCase(goalId)) {
                return i;
            }
        }
        return -1;
    }

    private static String normalize(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return "";
        }
        return worldName.toLowerCase(Locale.ROOT);
    }
}
