package com.community.goals.persistence;

import com.community.goals.Goal;
import com.community.goals.State;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

/**
 * Manages persistence of goals to YAML files
 */
public class PersistenceManager {
    private final Path dataFolder;
    private final Yaml yaml;
    private final String goalsFile = "goals.yml";
    private final String queueFile = "goal-queue.yml";
    private final Logger logger;
    private final String defaultWorldName;

    public PersistenceManager(String dataFolderPath, Logger logger, String defaultWorldName) {
        this.dataFolder = Paths.get(dataFolderPath);
        this.yaml = new Yaml();
        this.logger = logger;
        this.defaultWorldName = defaultWorldName;
        
        try {
            Files.createDirectories(dataFolder);
        } catch (IOException e) {
            logger.warning("Failed to create data folder: " + e.getMessage());
        }
    }

    /**
     * Save all goals to YAML file
     */
    public void saveGoals(Collection<Goal> goals) {
        try {
            List<Map<String, Object>> goalsList = new ArrayList<>();
            
            for (Goal goal : goals) {
                Map<String, Object> goalMap = goalToMap(goal);
                goalsList.add(goalMap);
            }

            Map<String, Object> root = new HashMap<>();
            root.put("goals", goalsList);
            root.put("last-updated", System.currentTimeMillis());

            Path filePath = dataFolder.resolve(goalsFile);
            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                yaml.dump(root, writer);
            }
        } catch (IOException e) {
            logger.warning("Failed to save goals: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load all goals from YAML file
     */
    @SuppressWarnings("unchecked")
    public List<Goal> loadGoals() {
        List<Goal> goals = new ArrayList<>();
        
        try {
            Path filePath = dataFolder.resolve(goalsFile);
            
            if (!Files.exists(filePath)) {
                return goals; // Return empty list if file doesn't exist
            }

            try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
                Map<String, Object> data = yaml.load(fis);
                
                if (data == null || !data.containsKey("goals")) {
                    return goals;
                }

                List<Map<String, Object>> goalsList = (List<Map<String, Object>>) data.get("goals");
                
                for (Map<String, Object> goalMap : goalsList) {
                    Goal goal = mapToGoal(goalMap);
                    if (goal != null) {
                        goals.add(goal);
                    }
                }
            }
        } catch (IOException e) {
            logger.warning("Failed to load goals: " + e.getMessage());
            e.printStackTrace();
        }
        
        return goals;
    }

    /**
     * Save a single goal
     */
    public void saveGoal(Goal goal) {
        List<Goal> goals = loadGoals();
        goals.removeIf(g -> g.getId().equals(goal.getId()));
        goals.add(goal);
        saveGoals(goals);
    }

    /**
     * Delete a goal by ID
     */
    public void deleteGoal(String goalId) {
        List<Goal> goals = loadGoals();
        goals.removeIf(g -> g.getId().equals(goalId));
        saveGoals(goals);
    }

    /**
     * Convert Goal object to Map for YAML serialization
     */
    private Map<String, Object> goalToMap(Goal goal) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", goal.getId());
        map.put("name", goal.getName());
        map.put("description", goal.getDescription());
        map.put("world", goal.getWorldName());
        map.put("current-progress", goal.getCurrentProgress());
        map.put("target-progress", goal.getTargetProgress());
        map.put("reward-expansion", goal.getRewardExpansion());
        map.put("state", goal.getState().name());
        map.put("created-at", goal.getCreatedAt());
        map.put("completed-at", goal.getCompletedAt());
        return map;
    }

    /**
     * Convert Map from YAML to Goal object
     */
    private Goal mapToGoal(Map<String, Object> map) {
        try {
            String id = (String) map.get("id");
            String name = (String) map.get("name");
            String description = (String) map.get("description");
            long targetProgress = ((Number) map.get("target-progress")).longValue();
            String worldName = map.containsKey("world") ? String.valueOf(map.get("world")) : defaultWorldName;

            Goal goal = new Goal(id, name, description, targetProgress, worldName);
            
            Long currentProgress = ((Number) map.get("current-progress")).longValue();
            if (currentProgress > 0) {
                goal.addProgress(currentProgress);
            }

            Object rewardRaw = map.get("reward-expansion");
            if (rewardRaw instanceof Number) {
                goal.setRewardExpansion(((Number) rewardRaw).doubleValue());
            }
            
            String stateName = (String) map.get("state");
            if (stateName != null) {
                goal.setState(State.valueOf(stateName));
            }

            return goal;
        } catch (Exception e) {
            logger.warning("Failed to deserialize goal from map: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if goals file exists
     */
    public boolean goalsFileExists() {
        return Files.exists(dataFolder.resolve(goalsFile));
    }

    /**
     * Save goal queue to YAML file
     */
    public void saveGoalQueue(List<String> queue) {
        Map<String, List<String>> queues = new LinkedHashMap<>();
        queues.put(defaultWorldName, new ArrayList<>(queue));
        saveGoalQueues(queues);
    }

    /**
     * Load goal queue from YAML file
     */
    @SuppressWarnings("unchecked")
    public List<String> loadGoalQueue() {
        Map<String, List<String>> queues = loadGoalQueues(defaultWorldName);
        List<String> queue = queues.get(defaultWorldName);
        if (queue == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(queue);
    }

    /**
     * Save per-world goal queues to YAML file
     */
    public void saveGoalQueues(Map<String, List<String>> queues) {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> queueMap = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : queues.entrySet()) {
            queueMap.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        root.put("queues", queueMap);
        root.put("last-updated", System.currentTimeMillis());

        Path filePath = dataFolder.resolve(queueFile);
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            yaml.dump(root, writer);
        } catch (IOException e) {
            logger.warning("Failed to save goal queues: " + e.getMessage());
        }
    }

    /**
     * Load per-world goal queues from YAML file
     */
    @SuppressWarnings("unchecked")
    public Map<String, List<String>> loadGoalQueues(String defaultWorld) {
        Map<String, List<String>> queues = new LinkedHashMap<>();
        Path filePath = dataFolder.resolve(queueFile);
        if (!Files.exists(filePath)) {
            return queues;
        }

        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            Map<String, Object> data = yaml.load(fis);
            if (data == null) {
                return queues;
            }

            Object rawQueues = data.get("queues");
            if (rawQueues instanceof Map) {
                Map<String, Object> rawMap = (Map<String, Object>) rawQueues;
                for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                    List<String> queue = new ArrayList<>();
                    Object list = entry.getValue();
                    if (list instanceof List) {
                        for (Object item : (List<?>) list) {
                            if (item != null) {
                                queue.add(String.valueOf(item));
                            }
                        }
                    }
                    queues.put(entry.getKey(), queue);
                }
                return queues;
            }

            Object rawQueue = data.get("queue");
            if (rawQueue instanceof List) {
                List<String> queue = new ArrayList<>();
                for (Object item : (List<?>) rawQueue) {
                    if (item != null) {
                        queue.add(String.valueOf(item));
                    }
                }
                queues.put(defaultWorld, queue);
            }
        } catch (IOException e) {
            logger.warning("Failed to load goal queues: " + e.getMessage());
        }

        return queues;
    }

    /**
     * Clear all goals
     */
    public void clearAllGoals() {
        saveGoals(new ArrayList<>());
    }
}
