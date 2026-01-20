package com.community.goals.npc;

import de.oliver.fancynpcs.api.Npc;
import org.bukkit.Location;

/**
 * Wrapper class for an NPC associated with a goal.
 */
public class GoalNPC {
    private final Npc npc;
    private final String npcName;
    private final String goalId;
    private final Location location;

    public GoalNPC(Npc npc, String goalId, Location location) {
        this.npc = npc;
        this.goalId = goalId;
        this.location = location;
        this.npcName = npc != null ? npc.getData().getName() : null;
    }

    /**
     * Get the underlying NPC
     */
    public Npc getNPC() {
        return npc;
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
        if (npc != null && npc.getData().getLocation() != null) {
            return npc.getData().getLocation();
        }
        return location;
    }

    /**
     * Check if the NPC is spawned
     */
    public boolean isSpawned() {
        return npc != null;
    }

    /**
     * Despawn the NPC
     */
    public void despawn() {
        if (npc != null) {
            npc.removeForAll();
        }
    }

    /**
     * Teleport the NPC
     */
    public void teleport(Location location) {
        if (npc != null && location != null) {
            npc.getData().setLocation(location);
            npc.updateForAll();
        }
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
                ", spawned=" + (npc != null) +
                '}';
    }
}
