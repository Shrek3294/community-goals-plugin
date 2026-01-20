package com.community.goals.commands;

import com.community.goals.Goal;
import com.community.goals.logic.GoalProgressTracker;
import com.community.goals.logic.TurnInHandler;
import com.community.goals.persistence.PersistenceManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Handles /goal player commands (view, turn-in, etc.)
 */
public class GoalPlayerCommand extends BaseCommand {
    private final TurnInHandler turnInHandler;

    public GoalPlayerCommand(GoalProgressTracker tracker, PersistenceManager persistence, 
                            TurnInHandler turnInHandler) {
        super(tracker, persistence);
        this.turnInHandler = turnInHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, PermissionManager.GOAL_USE)) {
            sendError(sender, "You don't have permission to use goal commands");
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "view":
                return handleView(sender, args);
            case "list":
                return handleList(sender);
            case "progress":
                return handleProgress(sender, args);
            case "info":
                return handleInfo(sender, args);
            case "turnin":
                return handleTurnIn(sender, args);
            default:
                showHelp(sender);
                return true;
        }
    }

    private boolean handleView(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendError(sender, "Usage: /goal view <id>");
            return true;
        }

        String goalId = args[1];
        Goal goal = getGoalOrError(sender, goalId);
        if (goal == null) return true;

        displayGoalInfo(sender, goal);
        return true;
    }

    private boolean handleList(CommandSender sender) {
        Collection<Goal> goals = tracker.getActiveGoals();

        sender.sendMessage("");
        sender.sendMessage("§6§l=== Active Community Goals ===");
        sender.sendMessage("§7Total: " + goals.size());
        sender.sendMessage("");

        for (Goal goal : goals) {
            String progressBar = createProgressBar(goal.getProgressPercentage());
            sender.sendMessage(String.format(
                "§f%s §7[%s] %.1f%%",
                goal.getName(),
                progressBar,
                goal.getProgressPercentage()
            ));
        }

        sender.sendMessage("");
        sender.sendMessage("§7Use /goal view <id> for more details");
        sender.sendMessage("");
        return true;
    }

    private boolean handleProgress(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendError(sender, "Usage: /goal progress <id>");
            return true;
        }

        String goalId = args[1];
        Goal goal = getGoalOrError(sender, goalId);
        if (goal == null) return true;

        sender.sendMessage("");
        sender.sendMessage("§6" + goal.getName());
        sender.sendMessage(String.format("§7Progress: §f%d / %d", 
            goal.getCurrentProgress(), goal.getTargetProgress()));
        sender.sendMessage(String.format("§7Complete: §a%.1f%%", goal.getProgressPercentage()));

        long remaining = goal.getTargetProgress() - goal.getCurrentProgress();
        if (remaining > 0) {
            sender.sendMessage(String.format("§7Remaining: §e%d", remaining));
        } else {
            sender.sendMessage("§a✓ Goal completed!");
        }
        sender.sendMessage("");
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendError(sender, "Usage: /goal info <id>");
            return true;
        }

        String goalId = args[1];
        Goal goal = getGoalOrError(sender, goalId);
        if (goal == null) return true;

        displayGoalInfo(sender, goal);
        return true;
    }

    @SuppressWarnings("deprecation")
    private boolean handleTurnIn(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sendError(sender, "Only players can use turn-in");
            return true;
        }

        if (args.length < 3) {
            sendError(sender, "Usage: /goal turnin <id> <amount> [item_type]");
            sendInfo(sender, "Example: /goal turnin diamonds 10 DIAMOND");
            return true;
        }

        String goalId = args[1];
        long amount;
        String itemType = null;

        try {
            amount = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            sendError(sender, "Amount must be a number");
            return true;
        }

        // Get item type from command or try to detect from goal ID
        if (args.length >= 4) {
            itemType = args[3].toUpperCase();
        } else {
            // Try to guess item type from goal ID
            itemType = guessItemTypeFromGoalId(goalId);
        }

        if (itemType == null) {
            sendError(sender, "Please specify item type: /goal turnin <id> <amount> <item_type>");
            sendInfo(sender, "Example: /goal turnin diamonds 10 DIAMOND");
            return true;
        }

        Player player = getPlayer(sender);

        // Validate turn-in
        TurnInHandler.ValidationResult validation = turnInHandler.validateTurnIn(goalId, amount);
        if (!validation.isValid()) {
            sendError(sender, validation.getReason());
            return true;
        }

        // Check if player has the required items
        org.bukkit.Material material;
        try {
            material = org.bukkit.Material.valueOf(itemType);
        } catch (IllegalArgumentException e) {
            sendError(sender, "Invalid item type: " + itemType);
            sendInfo(sender, "Use Minecraft material names like DIAMOND, IRON_INGOT, OAK_LOG, etc.");
            return true;
        }

        // Count items in inventory
        int itemCount = countItemsInInventory(player, material);
        if (itemCount < amount) {
            sendError(sender, String.format("You don't have enough %s! You have %d, need %d", 
                material.name().toLowerCase().replace("_", " "), itemCount, amount));
            return true;
        }

        // Remove items from inventory
        if (!removeItemsFromInventory(player, material, (int) amount)) {
            sendError(sender, "Failed to remove items from inventory");
            return true;
        }

        // Process turn-in
        TurnInHandler.TurnInResult result = turnInHandler.processTurnIn(goalId, amount, player.getName());
        if (!result.isSuccess()) {
            sendError(sender, result.getMessage());
            // Return items if turn-in failed
            player.getInventory().addItem(new org.bukkit.inventory.ItemStack(material, (int) amount));
            return true;
        }

        sendSuccess(sender, String.format("Turned in %d %s! Progress: %d / %d (%.1f%%)", 
            amount, 
            material.name().toLowerCase().replace("_", " "),
            result.getNewProgress(), 
            result.getTargetProgress(),
            result.getProgressPercentage()));
        
        if (result.isGoalCompleted()) {
            //noinspection deprecation
            org.bukkit.Bukkit.getServer().broadcastMessage("§a§l✓ Goal completed by " + player.getName() + "! " + result.getGoalId());
        }

        return true;
    }

    /**
     * Count specific items in player's inventory
     */
    private int countItemsInInventory(Player player, org.bukkit.Material material) {
        int count = 0;
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * Remove specific amount of items from player's inventory
     */
    private boolean removeItemsFromInventory(Player player, org.bukkit.Material material, int amount) {
        int remaining = amount;
        org.bukkit.inventory.ItemStack[] contents = player.getInventory().getContents();
        
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            org.bukkit.inventory.ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
                int itemAmount = item.getAmount();
                if (itemAmount <= remaining) {
                    // Remove entire stack
                    remaining -= itemAmount;
                    contents[i] = null;
                } else {
                    // Remove partial stack
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }
            }
        }
        
        player.getInventory().setContents(contents);
        return remaining == 0;
    }

    /**
     * Try to guess item type from goal ID
     */
    private String guessItemTypeFromGoalId(String goalId) {
        String id = goalId.toLowerCase();
        
        // Common mappings
        if (id.contains("diamond")) return "DIAMOND";
        if (id.contains("iron")) return "IRON_INGOT";
        if (id.contains("gold")) return "GOLD_INGOT";
        if (id.contains("emerald")) return "EMERALD";
        if (id.contains("coal")) return "COAL";
        if (id.contains("wood") || id.contains("log")) return "OAK_LOG";
        if (id.contains("stone")) return "STONE";
        if (id.contains("cobble")) return "COBBLESTONE";
        if (id.contains("wheat")) return "WHEAT";
        if (id.contains("carrot")) return "CARROT";
        if (id.contains("potato")) return "POTATO";
        if (id.contains("beef")) return "BEEF";
        if (id.contains("pork")) return "PORKCHOP";
        if (id.contains("chicken")) return "CHICKEN";
        if (id.contains("fish")) return "COD";
        if (id.contains("netherite")) return "NETHERITE_INGOT";
        
        return null; // Couldn't guess
    }

    private void displayGoalInfo(CommandSender sender, Goal goal) {
        sender.sendMessage("");
        sender.sendMessage("§6§l=== " + goal.getName() + " ===");
        sender.sendMessage("§7Description: " + goal.getDescription());
        sender.sendMessage("§7Progress: " + goal.getCurrentProgress() + " / " + goal.getTargetProgress());
        sender.sendMessage(String.format("§7Completion: §a%.1f%%", goal.getProgressPercentage()));
        if (goal.getRewardExpansion() > 0) {
            sender.sendMessage("§7Reward Expansion: §f" + goal.getRewardExpansion() + " blocks");
        }
        sender.sendMessage("§7Status: " + goal.getState().getColoredName());
        sender.sendMessage("");
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage("§6§l=== Goal Commands ===");
        sender.sendMessage("§7/goal list - Show all active goals");
        sender.sendMessage("§7/goal view <id> - View goal details");
        sender.sendMessage("§7/goal progress <id> - Check goal progress");
        sender.sendMessage("§7/goal info <id> - Display goal information");
        sender.sendMessage("§7/goal turnin <id> <amount> [item_type] - Turn in items");
        sender.sendMessage("§8Example: /goal turnin diamonds 10 DIAMOND");
        sender.sendMessage("");
    }

    private String createProgressBar(double percentage) {
        int filled = (int) (percentage / 10);
        int empty = 10 - filled;

        String bar = "§a";
        for (int i = 0; i < filled; i++) bar += "█";
        bar += "§7";
        for (int i = 0; i < empty; i++) bar += "█";

        return bar;
    }
}
