package com.community.goals.commands;

/**
 * Manages command permissions
 */
public class PermissionManager {
    public static final String GOAL_USE = "goal.use";
    public static final String GOAL_ADMIN = "goal.admin";
    public static final String GOAL_CREATE = "goal.create";
    public static final String GOAL_DELETE = "goal.delete";
    public static final String GOAL_EDIT = "goal.edit";
    public static final String GOAL_VIEW = "goal.view";
    public static final String BORDER_ADMIN = "goal.border.admin";

    /**
     * Check if permission string is valid
     */
    public static boolean isValidPermission(String permission) {
        return permission.startsWith("goal.");
    }

    /**
     * Get permission description
     */
    public static String getDescription(String permission) {
        switch (permission) {
            case GOAL_USE:
                return "Use goal commands";
            case GOAL_ADMIN:
                return "Use admin goal commands";
            case GOAL_CREATE:
                return "Create new goals";
            case GOAL_DELETE:
                return "Delete goals";
            case GOAL_EDIT:
                return "Edit goal properties";
            case GOAL_VIEW:
                return "View goal information";
            case BORDER_ADMIN:
                return "Manage world border";
            default:
                return "Unknown permission";
        }
    }
}
