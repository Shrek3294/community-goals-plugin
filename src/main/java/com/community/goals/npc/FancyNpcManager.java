package com.community.goals.npc;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcData;
import de.oliver.fancynpcs.api.NpcManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Manages FancyNpcs goal NPCs and persistence.
 */
public class FancyNpcManager {
    private static final String STORAGE_FILE = "npcs.yml";

    private final JavaPlugin plugin;
    private final Path storagePath;
    private final Yaml yaml;
    private final Map<String, StoredNpc> npcByName;

    public FancyNpcManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.storagePath = plugin.getDataFolder().toPath().resolve(STORAGE_FILE);
        this.yaml = new Yaml();
        this.npcByName = new HashMap<>();
        loadNPCs();
    }

    /**
     * Create a new goal NPC at a location
     */
    public GoalNPC createGoalNPC(String npcName, String goalId, Location location) {
        if (!canCreateNPCs()) {
            throw new IllegalStateException("FancyNpcs plugin is not available");
        }
        if (location == null) {
            throw new IllegalArgumentException("NPC location is required");
        }

        String key = normalizeName(npcName);
        if (npcByName.containsKey(key)) {
            throw new IllegalArgumentException("NPC already linked: " + npcName);
        }

        NpcManager npcManager = getNpcManager();
        if (npcManager.getNpc(npcName) != null) {
            throw new IllegalArgumentException("NPC already exists: " + npcName);
        }

        NpcData data = new NpcData(npcName, location);
        data.setDisplayName(npcName);
        data.setTurnToPlayer(true);

        Npc npc = createNpcFromData(data);
        npc.setSaveToFile(true);
        npcManager.registerNpc(npc);
        npc.create();
        npc.spawnForAll();

        npcByName.put(key, new StoredNpc(npcName, goalId, location));
        saveNPCs();

        return new GoalNPC(npc, goalId, location);
    }

    /**
     * Get an existing NPC by name
     */
    public Npc getNPC(String npcName) {
        NpcManager npcManager = getNpcManager();
        if (npcManager == null) {
            return null;
        }
        return npcManager.getNpc(npcName);
    }

    /**
     * Get all NPCs for a goal
     */
    public Collection<GoalNPC> getGoalNPCs(String goalId) {
        List<GoalNPC> result = new ArrayList<>();
        for (StoredNpc stored : npcByName.values()) {
            if (!stored.goalId.equalsIgnoreCase(goalId)) {
                continue;
            }
            Npc npc = getNPC(stored.name);
            result.add(new GoalNPC(npc, stored.goalId, stored.location));
        }
        return result;
    }

    /**
     * Delete an NPC
     */
    public boolean deleteNPC(String npcName) {
        String key = normalizeName(npcName);
        StoredNpc stored = npcByName.remove(key);
        if (stored == null) {
            return false;
        }

        NpcManager npcManager = getNpcManager();
        if (npcManager != null) {
            Npc npc = npcManager.getNpc(stored.name);
            if (npc != null) {
                npc.removeForAll();
                npcManager.removeNpc(npc);
            }
            npcManager.saveNpcs(true);
        }

        saveNPCs();
        return true;
    }

    /**
     * Check if FancyNpcs is available
     */
    public boolean canCreateNPCs() {
        Plugin fancy = Bukkit.getPluginManager().getPlugin("FancyNpcs");
        if (fancy == null || !fancy.isEnabled()) {
            return false;
        }
        try {
            return FancyNpcsPlugin.get() != null && getNpcManager() != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get all registered NPCs
     */
    public Collection<GoalNPC> getAllNPCs() {
        List<GoalNPC> result = new ArrayList<>();
        for (StoredNpc stored : npcByName.values()) {
            Npc npc = getNPC(stored.name);
            result.add(new GoalNPC(npc, stored.goalId, stored.location));
        }
        return result;
    }

    /**
     * Get NPC count
     */
    public int getNPCCount() {
        return npcByName.size();
    }

    /**
     * Get goal ID for an NPC name
     */
    public String getGoalIdForNpc(String npcName) {
        StoredNpc stored = npcByName.get(normalizeName(npcName));
        return stored == null ? null : stored.goalId;
    }

    /**
     * Save all NPCs
     */
    public void saveNPCs() {
        if (!Files.exists(storagePath.getParent())) {
            try {
                Files.createDirectories(storagePath.getParent());
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create npc storage folder: " + e.getMessage());
                return;
            }
        }

        List<Map<String, Object>> npcList = new ArrayList<>();
        for (StoredNpc stored : npcByName.values()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", stored.name);
            entry.put("goal-id", stored.goalId);
            if (stored.location != null && stored.location.getWorld() != null) {
                entry.put("world", stored.location.getWorld().getName());
                entry.put("x", stored.location.getX());
                entry.put("y", stored.location.getY());
                entry.put("z", stored.location.getZ());
                entry.put("yaw", stored.location.getYaw());
                entry.put("pitch", stored.location.getPitch());
            }
            npcList.add(entry);
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("npcs", npcList);

        try (FileWriter writer = new FileWriter(storagePath.toFile())) {
            yaml.dump(root, writer);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save NPCs: " + e.getMessage());
        }

        NpcManager npcManager = getNpcManager();
        if (npcManager != null) {
            npcManager.saveNpcs(true);
        }
    }

    /**
     * Reload NPCs from storage
     */
    public void reloadNPCs() {
        npcByName.clear();
        loadNPCs();

        NpcManager npcManager = getNpcManager();
        if (npcManager != null) {
            npcManager.reloadNpcs();
        }
    }

    private void loadNPCs() {
        if (!Files.exists(storagePath)) {
            return;
        }

        try (FileInputStream fis = new FileInputStream(storagePath.toFile())) {
            Map<String, Object> data = yaml.load(fis);
            if (data == null) {
                return;
            }
            Object raw = data.get("npcs");
            if (!(raw instanceof List)) {
                return;
            }

            List<?> list = (List<?>) raw;
            for (Object entryObj : list) {
                if (!(entryObj instanceof Map)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> entry = (Map<String, Object>) entryObj;
                String name = asString(entry.get("name"));
                String goalId = asString(entry.get("goal-id"));
                if (name == null || goalId == null) {
                    continue;
                }
                Location location = toLocation(entry);
                npcByName.put(normalizeName(name), new StoredNpc(name, goalId, location));
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load NPCs: " + e.getMessage());
        }

        if (canCreateNPCs()) {
            ensureNpcInstances();
        }
    }

    private void ensureNpcInstances() {
        NpcManager npcManager = getNpcManager();
        if (npcManager == null) {
            return;
        }

        for (StoredNpc stored : npcByName.values()) {
            if (npcManager.getNpc(stored.name) != null) {
                continue;
            }
            if (stored.location == null) {
                continue;
            }
            NpcData data = new NpcData(stored.name, stored.location);
            data.setDisplayName(stored.name);
            data.setTurnToPlayer(true);
            Npc npc = createNpcFromData(data);
            npc.setSaveToFile(true);
            npcManager.registerNpc(npc);
            npc.create();
            npc.spawnForAll();
        }
    }

    private NpcManager getNpcManager() {
        try {
            FancyNpcsPlugin api = FancyNpcsPlugin.get();
            if (api == null) {
                return null;
            }
            return api.getNpcManager();
        } catch (Exception e) {
            return null;
        }
    }

    private Npc createNpcFromData(NpcData data) {
        FancyNpcsPlugin api = FancyNpcsPlugin.get();
        if (api == null) {
            throw new IllegalStateException("FancyNpcs API is not available");
        }
        return api.getNpcAdapter().apply(data);
    }

    private String normalizeName(String npcName) {
        return npcName == null ? "" : npcName.toLowerCase(Locale.ROOT);
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Location toLocation(Map<String, Object> entry) {
        String worldName = asString(entry.get("world"));
        if (worldName == null) {
            return null;
        }
        if (Bukkit.getWorld(worldName) == null) {
            return null;
        }
        double x = asDouble(entry.get("x"), 0.0);
        double y = asDouble(entry.get("y"), 0.0);
        double z = asDouble(entry.get("z"), 0.0);
        float yaw = (float) asDouble(entry.get("yaw"), 0.0);
        float pitch = (float) asDouble(entry.get("pitch"), 0.0);
        return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
    }

    private double asDouble(Object value, double defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static class StoredNpc {
        private final String name;
        private final String goalId;
        private final Location location;

        private StoredNpc(String name, String goalId, Location location) {
            this.name = name;
            this.goalId = goalId;
            this.location = location;
        }
    }
}
