package com.community.goals.commands;

import com.community.goals.features.BorderExpansionManager;
import org.bukkit.command.CommandSender;

/**
 * Handles /goal border commands
 */
public class BorderCommand extends BaseCommand {
    private final BorderExpansionManager borderManager;

    public BorderCommand(BorderExpansionManager borderManager) {
        super(null, null);
        this.borderManager = borderManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        // Check permission
        if (!sender.isOp() && !sender.hasPermission(PermissionManager.BORDER_ADMIN)) {
            sendError(sender, "You don't have permission to manage the world border");
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "expand":
                return handleExpand(sender, args);
            case "setsize":
                return handleSetSize(sender, args);
            case "setcenter":
                return handleSetCenter(sender, args);
            case "setexpansion":
                return handleSetExpansion(sender, args);
            case "info":
                return handleInfo(sender);
            default:
                showHelp(sender);
                return true;
        }
    }

    private boolean handleExpand(CommandSender sender, String[] args) {
        double amount;
        
        if (args.length < 2) {
            // Use default expansion amount
            amount = borderManager.getBorderConfig().getExpansionAmount();
        } else {
            try {
                amount = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                sendError(sender, "Amount must be a number");
                return true;
            }
        }

        if (borderManager.expandBorder(amount)) {
            sendSuccess(sender, String.format("Border expanded by %.1f blocks", amount));
            return true;
        } else {
            sendError(sender, "Failed to expand border");
            return true;
        }
    }

    private boolean handleSetSize(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendError(sender, "Usage: /goal border setsize <size>");
            return true;
        }

        double size;
        try {
            size = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sendError(sender, "Size must be a number");
            return true;
        }

        if (borderManager.setSize(size)) {
            sendSuccess(sender, String.format("Border size set to %.1f", size));
            return true;
        } else {
            sendError(sender, "Failed to set border size");
            return true;
        }
    }

    private boolean handleSetCenter(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendError(sender, "Usage: /goal border setcenter <x> <z>");
            return true;
        }

        double x, z;
        try {
            x = Double.parseDouble(args[1]);
            z = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sendError(sender, "Coordinates must be numbers");
            return true;
        }

        if (borderManager.setCenter(x, z)) {
            sendSuccess(sender, String.format("Border center set to (%.1f, %.1f)", x, z));
            return true;
        } else {
            sendError(sender, "Failed to set border center");
            return true;
        }
    }

    private boolean handleSetExpansion(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendError(sender, "Usage: /goal border setexpansion <amount>");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sendError(sender, "Amount must be a number");
            return true;
        }

        borderManager.getBorderConfig().setExpansionAmount(amount);
        sendSuccess(sender, String.format("Border expansion amount set to %.1f", amount));
        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        BorderExpansionManager.BorderInfo info = borderManager.getInfo();
        sender.sendMessage("");
        sender.sendMessage("§6§l=== World Border Info ===");
        sender.sendMessage("§7World: §f" + info.worldName);
        sender.sendMessage(String.format("§7Center: §f(%.1f, %.1f)", info.centerX, info.centerZ));
        sender.sendMessage(String.format("§7Current Size: §f%.1f", info.currentSize));
        sender.sendMessage(String.format("§7Expansion Amount: §f%.1f", info.expansionAmount));
        sender.sendMessage("");
        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage("§6§l=== Border Commands ===");
        sender.sendMessage("§7/goal border expand [amount]");
        sender.sendMessage("§7/goal border setsize <size>");
        sender.sendMessage("§7/goal border setcenter <x> <z>");
        sender.sendMessage("§7/goal border setexpansion <amount>");
        sender.sendMessage("§7/goal border info");
        sender.sendMessage("");
    }

    protected void sendError(CommandSender sender, String message) {
        sender.sendMessage("§c" + message);
    }

    protected void sendSuccess(CommandSender sender, String message) {
        sender.sendMessage("§a✓ " + message);
    }
}
