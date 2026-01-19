package com.community.goals.persistence;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages loading and accessing plugin configuration
 */
public class ConfigManager {
    private final Path configPath;
    private Map<String, Object> configData;
    private final Yaml yaml;

    public ConfigManager(String configFilePath) {
        this.configPath = Paths.get(configFilePath);
        this.yaml = new Yaml();
        loadConfig();
    }

    /**
     * Load configuration from file
     */
    private void loadConfig() {
        try {
            if (!Files.exists(configPath)) {
                System.err.println("Config file not found: " + configPath);
                configData = new HashMap<>();
                return;
            }

            try (FileInputStream fis = new FileInputStream(configPath.toFile())) {
                configData = yaml.load(fis);
                if (configData == null) {
                    configData = new HashMap<>();
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load config: " + e.getMessage());
            configData = new HashMap<>();
        }
    }

    /**
     * Get a configuration value by dot-notation path (e.g., "goals.announcements-enabled")
     */
    @SuppressWarnings("unchecked")
    public Object get(String path) {
        String[] parts = path.split("\\.");
        Object current = configData;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
                if (current == null) {
                    return null;
                }
            } else {
                return null;
            }
        }

        return current;
    }

    /**
     * Get a boolean configuration value with default
     */
    public boolean getBoolean(String path, boolean defaultValue) {
        Object value = get(path);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    /**
     * Get a string configuration value with default
     */
    public String getString(String path, String defaultValue) {
        Object value = get(path);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }

    /**
     * Get an integer configuration value with default
     */
    public int getInt(String path, int defaultValue) {
        Object value = get(path);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /**
     * Get a long configuration value with default
     */
    public long getLong(String path, long defaultValue) {
        Object value = get(path);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }

    /**
     * Get a double configuration value with default
     */
    public double getDouble(String path, double defaultValue) {
        Object value = get(path);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    /**
     * Reload configuration from file
     */
    public void reload() {
        loadConfig();
    }

    /**
     * Get raw configuration data
     */
    public Map<String, Object> getRawConfig() {
        return configData;
    }

    /**
     * Set a configuration value by dot-notation path
     */
    @SuppressWarnings("unchecked")
    public void set(String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = configData;

        // Navigate to the parent of the target key
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            Object next = current.get(part);
            
            if (!(next instanceof Map)) {
                // Create new map if it doesn't exist or isn't a map
                next = new HashMap<String, Object>();
                current.put(part, next);
            }
            
            current = (Map<String, Object>) next;
        }

        // Set the final value
        current.put(parts[parts.length - 1], value);
    }

    /**
     * Save configuration to file
     */
    public void saveConfig() {
        try {
            // Ensure parent directories exist
            Files.createDirectories(configPath.getParent());
            
            try (FileWriter writer = new FileWriter(configPath.toFile())) {
                yaml.dump(configData, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }
}
