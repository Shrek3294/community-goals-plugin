package com.community.goals;

/**
 * Represents a community goal with progress tracking
 */
public class Goal {
    private final String id;
    private final String name;
    private final String description;
    private long currentProgress;
    private long targetProgress;
    private double rewardExpansion;
    private State state;
    private long createdAt;
    private long completedAt;

    public Goal(String id, String name, String description, long targetProgress) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.targetProgress = targetProgress;
        this.currentProgress = 0;
        this.state = State.ACTIVE;
        this.createdAt = System.currentTimeMillis();
        this.completedAt = 0;
        this.rewardExpansion = 0;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public long getCurrentProgress() {
        return currentProgress;
    }

    public void addProgress(long amount) {
        this.currentProgress += amount;
        if (currentProgress >= targetProgress) {
            complete();
        }
    }

    public long getTargetProgress() {
        return targetProgress;
    }

    public void setTargetProgress(long targetProgress) {
        this.targetProgress = targetProgress;
    }

    public double getRewardExpansion() {
        return rewardExpansion;
    }

    public void setRewardExpansion(double rewardExpansion) {
        this.rewardExpansion = rewardExpansion;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public double getProgressPercentage() {
        if (targetProgress <= 0) return 0;
        return (double) currentProgress / targetProgress * 100;
    }

    public boolean isCompleted() {
        return state == State.COMPLETED;
    }

    public void complete() {
        this.state = State.COMPLETED;
        this.completedAt = System.currentTimeMillis();
        this.currentProgress = targetProgress;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    @Override
    public String toString() {
        return "Goal{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", currentProgress=" + currentProgress +
                ", targetProgress=" + targetProgress +
                ", rewardExpansion=" + rewardExpansion +
                ", state=" + state +
                ", percentage=" + String.format("%.2f", getProgressPercentage()) + "%" +
                '}';
    }
}
