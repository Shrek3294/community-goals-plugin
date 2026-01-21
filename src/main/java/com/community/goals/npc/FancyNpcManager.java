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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Manages FancyNpcs goal NPCs and persistence.
 */
public class FancyNpcManager {
    private static final String STORAGE_FILE = "npcs.yml";
    private static final String CENTRAL_NPC_KEY = "central";
    private static final String CENTRAL_NPCS_KEY = "central-npcs";

    private final JavaPlugin plugin;
    private final Path storagePath;
    private final Yaml yaml;
    private final Map<String, StoredNpc> npcByName;
    private final Map<String, StoredNpc> centralNpcs;

    public FancyNpcManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.storagePath = plugin.getDataFolder().toPath().resolve(STORAGE_FILE);
        this.yaml = new Yaml();
        this.npcByName = new HashMap<>();
        this.centralNpcs = new HashMap<>();
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

        NpcData data = createNpcData(npcName, goalId, location);
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

    public void setCentralNpc(String npcName, Location location) {
        if (!canCreateNPCs()) {
            throw new IllegalStateException("FancyNpcs plugin is not available");
        }
        if (location == null) {
            throw new IllegalArgumentException("NPC location is required");
        }
        if (location.getWorld() == null) {
            throw new IllegalArgumentException("NPC world is required");
        }

        setCentralNpc(location.getWorld().getName(), npcName, location);
    }

    public void setCentralNpc(String worldName, String npcName, Location location) {
        if (!canCreateNPCs()) {
            throw new IllegalStateException("FancyNpcs plugin is not available");
        }
        if (location == null) {
            throw new IllegalArgumentException("NPC location is required");
        }
        if (location.getWorld() == null) {
            throw new IllegalArgumentException("NPC world is required");
        }

        String worldKey = normalizeWorldName(worldName);
        if (centralNpcs.containsKey(worldKey)) {
            deleteCentralNpc(worldName);
        }

        NpcManager npcManager = getNpcManager();
        if (npcManager.getNpc(npcName) != null) {
            throw new IllegalArgumentException("NPC already exists: " + npcName);
        }

        NpcData data = createNpcData(npcName, CENTRAL_NPC_KEY, location);
        data.setDisplayName(npcName);
        data.setTurnToPlayer(true);

        Npc npc = createNpcFromData(data);
        npc.setSaveToFile(true);
        npcManager.registerNpc(npc);
        npc.create();
        npc.spawnForAll();

        centralNpcs.put(worldKey, new StoredNpc(npcName, CENTRAL_NPC_KEY, location));
        saveNPCs();
    }

    public boolean deleteCentralNpc(String worldName) {
        String worldKey = normalizeWorldName(worldName);
        StoredNpc stored = centralNpcs.remove(worldKey);
        if (stored == null) {
            return false;
        }
        String npcName = stored.name;

        NpcManager npcManager = getNpcManager();
        if (npcManager != null) {
            Npc npc = npcManager.getNpc(npcName);
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

    public int deleteNPCsForGoal(String goalId) {
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, StoredNpc> entry : npcByName.entrySet()) {
            if (entry.getValue().goalId.equalsIgnoreCase(goalId)) {
                toRemove.add(entry.getKey());
            }
        }
        int removed = 0;
        for (String key : toRemove) {
            StoredNpc stored = npcByName.remove(key);
            if (stored == null) {
                continue;
            }
            removed++;
            NpcManager npcManager = getNpcManager();
            if (npcManager != null) {
                Npc npc = npcManager.getNpc(stored.name);
                if (npc != null) {
                    npc.removeForAll();
                    npcManager.removeNpc(npc);
                }
            }
        }
        if (removed > 0) {
            saveNPCs();
        }
        return removed;
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

    public boolean isCentralNpc(String npcName) {
        return getCentralWorldForNpc(npcName) != null;
    }

    public String getCentralNpcName(String worldName) {
        StoredNpc stored = centralNpcs.get(normalizeWorldName(worldName));
        return stored == null ? null : stored.name;
    }

    public Map<String, String> getCentralNpcNames() {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, StoredNpc> entry : centralNpcs.entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), entry.getValue().name);
            }
        }
        return result;
    }

    public String getCentralWorldForNpc(String npcName) {
        String key = normalizeName(npcName);
        for (Map.Entry<String, StoredNpc> entry : centralNpcs.entrySet()) {
            StoredNpc stored = entry.getValue();
            if (stored != null && normalizeName(stored.name).equals(key)) {
                return entry.getKey();
            }
        }
        return null;
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
        Map<String, Object> centralMap = new LinkedHashMap<>();
        for (Map.Entry<String, StoredNpc> entry : centralNpcs.entrySet()) {
            StoredNpc stored = entry.getValue();
            if (stored == null || stored.location == null || stored.location.getWorld() == null) {
                continue;
            }
            Map<String, Object> central = new LinkedHashMap<>();
            central.put("name", stored.name);
            central.put("world", stored.location.getWorld().getName());
            central.put("x", stored.location.getX());
            central.put("y", stored.location.getY());
            central.put("z", stored.location.getZ());
            central.put("yaw", stored.location.getYaw());
            central.put("pitch", stored.location.getPitch());
            centralMap.put(entry.getKey(), central);
        }
        if (!centralMap.isEmpty()) {
            root.put(CENTRAL_NPCS_KEY, centralMap);
        }

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
        centralNpcs.clear();
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
                raw = Collections.emptyList();
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

            Object centralMultiRaw = data.get(CENTRAL_NPCS_KEY);
            if (centralMultiRaw instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> centralMap = (Map<String, Object>) centralMultiRaw;
                for (Map.Entry<String, Object> entry : centralMap.entrySet()) {
                    if (!(entry.getValue() instanceof Map)) {
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    Map<String, Object> centralEntry = (Map<String, Object>) entry.getValue();
                    String name = asString(centralEntry.get("name"));
                    if (name == null) {
                        continue;
                    }
                    Location location = toLocation(centralEntry);
                    if (location == null || location.getWorld() == null) {
                        continue;
                    }
                    centralNpcs.put(normalizeWorldName(location.getWorld().getName()), new StoredNpc(name, CENTRAL_NPC_KEY, location));
                }
            }

            Object centralRaw = data.get(CENTRAL_NPC_KEY);
            if (centralRaw instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> centralEntry = (Map<String, Object>) centralRaw;
                String name = asString(centralEntry.get("name"));
                if (name != null) {
                    Location location = toLocation(centralEntry);
                    if (location != null && location.getWorld() != null) {
                        centralNpcs.put(normalizeWorldName(location.getWorld().getName()), new StoredNpc(name, CENTRAL_NPC_KEY, location));
                    }
                }
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
            NpcData data = createNpcData(stored.name, stored.goalId, stored.location);
            data.setDisplayName(stored.name);
            data.setTurnToPlayer(true);
            Npc npc = createNpcFromData(data);
            npc.setSaveToFile(true);
            npcManager.registerNpc(npc);
            npc.create();
            npc.spawnForAll();
        }

        for (StoredNpc stored : centralNpcs.values()) {
            if (stored == null || stored.location == null) {
                continue;
            }
            if (npcManager.getNpc(stored.name) != null) {
                continue;
            }
            NpcData data = createNpcData(stored.name, CENTRAL_NPC_KEY, stored.location);
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

    private NpcData createNpcData(String npcName, String goalId, Location location) {
        UUID uuid = UUID.nameUUIDFromBytes(
            (goalId + ":" + npcName).getBytes(StandardCharsets.UTF_8)
        );
        return new NpcData(npcName, uuid, location);
    }

    private String normalizeWorldName(String worldName) {
        return worldName == null ? "" : worldName.toLowerCase(Locale.ROOT);
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
