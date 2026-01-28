package com.community.goals.placeholders;

import com.community.goals.CommunityGoalsPlugin;
import com.community.goals.features.BorderExpansionManager;
import com.community.goals.features.BorderManagerRegistry;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.util.Locale;

public class WorldBorderExpansion extends PlaceholderExpansion {
    private final CommunityGoalsPlugin plugin;
    private final BorderManagerRegistry borderRegistry;

    public WorldBorderExpansion(CommunityGoalsPlugin plugin, BorderManagerRegistry borderRegistry) {
        this.plugin = plugin;
        this.borderRegistry = borderRegistry;
    }

    @Override
    public String getIdentifier() {
        return "worldborder";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (borderRegistry == null) {
            return "";
        }

        String worldName = getWorldName(player);
        BorderExpansionManager manager = borderRegistry.getManager(worldName);
        if (manager == null) {
            manager = borderRegistry.getManager(borderRegistry.getDefaultWorld());
        }
        if (manager == null) {
            return "";
        }

        BorderExpansionManager.BorderInfo info = manager.getInfo();
        String key = params == null ? "" : params.toLowerCase(Locale.ROOT);
        switch (key) {
            case "size":
                return formatNumber(manager.getCurrentSize());
            case "center_x":
                return formatNumber(info.centerX);
            case "center_z":
                return formatNumber(info.centerZ);
            case "expansion":
                return formatNumber(info.expansionAmount);
            case "world":
                return info.worldName;
            default:
                return "";
        }
    }

    private String getWorldName(Player player) {
        if (player == null || player.getWorld() == null) {
            return borderRegistry.getDefaultWorld();
        }
        return player.getWorld().getName();
    }

    private String formatNumber(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0";
        }
        long rounded = Math.round(value);
        if (Math.abs(value - rounded) < 0.0001) {
            return String.valueOf(rounded);
        }
        return String.format(Locale.US, "%.1f", value);
    }
}
