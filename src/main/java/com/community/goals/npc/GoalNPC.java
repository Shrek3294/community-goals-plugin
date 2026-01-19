package com.community.goals.npc;

import org.bukkit.Location;

/**
 * Wrapper class for an NPC associated with a goal
 * 
 * Note: This is a simplified stub implementation.
 */
public class GoalNPC {
    private final String npcName;
    private final String goalId;
    private final Location location;

    public GoalNPC(Object npc, String goalId) {
        this.goalId = goalId;
        this.npcName = "NPC_" + goalId;
        this.location = null;
    }

    /**
     * Get the underlying NPC
     */
    public Object getNPC() {
        return null;
    }

    /**
     * Get the goal ID associated with this NPC
     */
    public String getGoalId() {
        return goalId;
    }

    /**
     * Get the NPC name
     */
    public String getName() {
        return npcName;
    }

    /**
     * Get the NPC location
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Check if the NPC is spawned
     */
    public boolean isSpawned() {
        return false;
    }

    /**
     * Despawn the NPC
     */
    public void despawn() {
        // Stub
    }

    /**
     * Teleport the NPC
     */
    public void teleport(Location location) {
        // Stub
    }

    /**
     * Get the goal metadata
     */
    public String getGoalMetadata() {
        return goalId;
    }

    @Override
    public String toString() {
        return "GoalNPC{" +
                "name='" + npcName + '\'' +
                ", goalId='" + goalId + '\'' +
                ", spawned=false" +
                '}';
    }
}
