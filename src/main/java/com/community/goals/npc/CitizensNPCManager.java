package com.community.goals.npc;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Manages NPCs for the community goals plugin
 * 
 * Note: NPC integration requires FancyNpcs plugin to be installed.
 * This is a simplified stub implementation.
 */
public class CitizensNPCManager {

    public CitizensNPCManager() {
        // Stub implementation - NPC features disabled
    }

    /**
     * Create a new goal NPC at a location
     */
    public GoalNPC createGoalNPC(String npcName, String goalId, Location location) {
        throw new UnsupportedOperationException("NPC integration requires FancyNpcs plugin");
    }

    /**
     * Get an existing NPC by name
     */
    public Object getNPC(String npcName) {
        return null;
    }

    /**
     * Get all NPCs for a goal
     */
    public Collection<?> getGoalNPCs(String goalId) {
        return new ArrayList<>();
    }

    /**
     * Delete an NPC
     */
    public boolean deleteNPC(String npcName) {
        return false;
    }

    /**
     * Check if FancyNpcs is available
     */
    public boolean canCreateNPCs() {
        return false;
    }

    /**
     * Get all registered NPCs
     */
    public Collection<?> getAllNPCs() {
        return new ArrayList<>();
    }

    /**
     * Get NPC count
     */
    public int getNPCCount() {
        return 0;
    }

    /**
     * Save all NPCs
     */
    public void saveNPCs() {
        // Stub
    }

    /**
     * Reload NPCs from storage
     */
    public void reloadNPCs() {
        // Stub
    }
}
