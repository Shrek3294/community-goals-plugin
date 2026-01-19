package com.community.goals;

/**
 * Represents the world border configuration
 */
public class Border {
    private double centerX;
    private double centerZ;
    private double size;
    private double expansionAmount;
    private String worldName;

    public Border(String worldName, double centerX, double centerZ, double size, double expansionAmount) {
        this.worldName = worldName;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.size = size;
        this.expansionAmount = expansionAmount;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public double getCenterX() {
        return centerX;
    }

    public void setCenterX(double centerX) {
        this.centerX = centerX;
    }

    public double getCenterZ() {
        return centerZ;
    }

    public void setCenterZ(double centerZ) {
        this.centerZ = centerZ;
    }

    public double getSize() {
        return size;
    }

    public void setSize(double size) {
        this.size = size;
    }

    public double getExpansionAmount() {
        return expansionAmount;
    }

    public void setExpansionAmount(double expansionAmount) {
        this.expansionAmount = expansionAmount;
    }

    public void expandBorder(double amount) {
        this.size += amount;
    }

    public void expandByDefault() {
        expandBorder(expansionAmount);
    }

    @Override
    public String toString() {
        return "Border{" +
                "worldName='" + worldName + '\'' +
                ", centerX=" + centerX +
                ", centerZ=" + centerZ +
                ", size=" + size +
                ", expansionAmount=" + expansionAmount +
                '}';
    }
}
