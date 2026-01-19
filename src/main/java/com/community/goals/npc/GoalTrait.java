package com.community.goals.npc;

/**
 * Helper class for managing goal metadata on FancyNPCs
 */
public class GoalTrait {
    private String goalId;

    public GoalTrait(String goalId) {
        this.goalId = goalId;
    }

    /**
     * Get the associated goal ID
     */
    public String getGoalId() {
        return goalId;
    }

    /**
     * Set the goal ID
     */
    public void setGoalId(String goalId) {
        this.goalId = goalId;
    }
}
