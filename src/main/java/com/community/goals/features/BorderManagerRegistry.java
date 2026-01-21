package com.community.goals.features;

import com.community.goals.Border;
import com.community.goals.persistence.ConfigManager;

import java.util.*;
import java.util.logging.Logger;

/**
 * Loads and manages per-world border expansion managers.
 */
public class BorderManagerRegistry {
    private final Map<String, BorderExpansionManager> managers;
    private final String defaultWorld;
    private final boolean multiWorldConfig;

    private BorderManagerRegistry(Map<String, BorderExpansionManager> managers, String defaultWorld, boolean multiWorldConfig) {
        this.managers = managers;
        this.defaultWorld = defaultWorld;
        this.multiWorldConfig = multiWorldConfig;
    }

    public static BorderManagerRegistry fromConfig(ConfigManager configManager, Logger logger) {
        Map<String, BorderExpansionManager> managers = new HashMap<>();
        Object rawWorlds = configManager.get("world-borders.worlds");
        boolean multiWorldConfig = rawWorlds instanceof Map;

        if (multiWorldConfig) {
            @SuppressWarnings("unchecked")
            Map<String, Object> worlds = (Map<String, Object>) rawWorlds;
            for (Map.Entry<String, Object> entry : worlds.entrySet()) {
                if (!(entry.getValue() instanceof Map)) {
                    continue;
                }
                String worldName = entry.getKey();
                @SuppressWarnings("unchecked")
                Map<String, Object> worldConfig = (Map<String, Object>) entry.getValue();
                boolean enabled = getBoolean(worldConfig, "enabled", true);
                if (!enabled) {
                    continue;
                }
                long centerX = getLong(worldConfig, "center-x", 0L);
                long centerZ = getLong(worldConfig, "center-z", 0L);
                long initialSize = getLong(worldConfig, "initial-size", 50L);
                long expansionAmount = getLong(worldConfig, "expansion-amount", 20L);
                Border borderConfig = new Border(worldName, centerX, centerZ, initialSize, expansionAmount);
                BorderExpansionManager manager = new BorderExpansionManager(borderConfig, logger);
                managers.put(normalize(worldName), manager);
            }
        } else {
            boolean enabled = configManager.getBoolean("world-border.enabled", true);
            if (enabled) {
                String worldName = configManager.getString("world-border.world", "world");
                long centerX = configManager.getLong("world-border.center-x", 0L);
                long centerZ = configManager.getLong("world-border.center-z", 0L);
                long initialSize = configManager.getLong("world-border.initial-size", 50L);
                long expansionAmount = configManager.getLong("world-border.expansion-amount", 20L);
                Border borderConfig = new Border(worldName, centerX, centerZ, initialSize, expansionAmount);
                BorderExpansionManager manager = new BorderExpansionManager(borderConfig, logger);
                managers.put(normalize(worldName), manager);
            }
        }

        String defaultWorld = configManager.getString(
            "world-borders.default-world",
            configManager.getString("world-border.world", "world")
        );
        if (!managers.containsKey(normalize(defaultWorld)) && !managers.isEmpty()) {
            defaultWorld = managers.values().iterator().next().getBorderConfig().getWorldName();
        }

        return new BorderManagerRegistry(managers, defaultWorld, multiWorldConfig);
    }

    public BorderExpansionManager getManager(String worldName) {
        if (worldName == null) {
            return null;
        }
        return managers.get(normalize(worldName));
    }

    public boolean hasWorld(String worldName) {
        return getManager(worldName) != null;
    }

    public Set<String> getWorldNames() {
        Set<String> names = new LinkedHashSet<>();
        for (BorderExpansionManager manager : managers.values()) {
            names.add(manager.getBorderConfig().getWorldName());
        }
        return names;
    }

    public String getDefaultWorld() {
        return defaultWorld;
    }

    public boolean saveBorderConfig(ConfigManager configManager, String worldName) {
        BorderExpansionManager manager = getManager(worldName);
        if (manager == null) {
            return false;
        }
        BorderExpansionManager.BorderInfo info = manager.getInfo();
        String canonicalWorld = manager.getBorderConfig().getWorldName();

        if (multiWorldConfig) {
            String prefix = "world-borders.worlds." + canonicalWorld + ".";
            configManager.set(prefix + "center-x", info.centerX);
            configManager.set(prefix + "center-z", info.centerZ);
            configManager.set(prefix + "initial-size", info.currentSize);
            configManager.set(prefix + "expansion-amount", info.expansionAmount);
            configManager.set(prefix + "enabled", true);
            if (configManager.get("world-borders.default-world") == null) {
                configManager.set("world-borders.default-world", defaultWorld);
            }
        } else {
            configManager.set("world-border.world", canonicalWorld);
            configManager.set("world-border.center-x", info.centerX);
            configManager.set("world-border.center-z", info.centerZ);
            configManager.set("world-border.initial-size", info.currentSize);
            configManager.set("world-border.expansion-amount", info.expansionAmount);
        }
        configManager.saveConfig();
        return true;
    }

    private static boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    private static long getLong(Map<String, Object> map, String key, long defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }

    private static String normalize(String worldName) {
        return worldName.toLowerCase(Locale.ROOT);
    }
}
