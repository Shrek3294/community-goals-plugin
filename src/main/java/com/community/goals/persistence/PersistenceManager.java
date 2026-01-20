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

    public PersistenceManager(String dataFolderPath, Logger logger) {
        this.dataFolder = Paths.get(dataFolderPath);
        this.yaml = new Yaml();
        this.logger = logger;
        
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

            Goal goal = new Goal(id, name, description, targetProgress);
            
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
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("queue", new ArrayList<>(queue));
        root.put("last-updated", System.currentTimeMillis());

        Path filePath = dataFolder.resolve(queueFile);
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            yaml.dump(root, writer);
        } catch (IOException e) {
            logger.warning("Failed to save goal queue: " + e.getMessage());
        }
    }

    /**
     * Load goal queue from YAML file
     */
    @SuppressWarnings("unchecked")
    public List<String> loadGoalQueue() {
        List<String> queue = new ArrayList<>();
        Path filePath = dataFolder.resolve(queueFile);
        if (!Files.exists(filePath)) {
            return queue;
        }

        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            Map<String, Object> data = yaml.load(fis);
            if (data == null) {
                return queue;
            }
            Object raw = data.get("queue");
            if (!(raw instanceof List)) {
                return queue;
            }
            for (Object entry : (List<?>) raw) {
                if (entry != null) {
                    queue.add(String.valueOf(entry));
                }
            }
        } catch (IOException e) {
            logger.warning("Failed to load goal queue: " + e.getMessage());
        }

        return queue;
    }

    /**
     * Clear all goals
     */
    public void clearAllGoals() {
        saveGoals(new ArrayList<>());
    }
}
