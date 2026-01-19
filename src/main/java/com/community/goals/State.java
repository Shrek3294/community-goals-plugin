package com.community.goals;

/**
 * Enum representing the possible states of a community goal
 */
public enum State {
    ACTIVE("Active", "§2"),      // Green
    PAUSED("Paused", "§6"),      // Gold
    COMPLETED("Completed", "§a"), // Bright Green
    CANCELLED("Cancelled", "§c"); // Red

    private final String displayName;
    private final String chatColor;

    State(String displayName, String chatColor) {
        this.displayName = displayName;
        this.chatColor = chatColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getChatColor() {
        return chatColor;
    }

    public String getColoredName() {
        return chatColor + displayName + "§r";
    }
}
