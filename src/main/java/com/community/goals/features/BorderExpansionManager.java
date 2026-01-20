package com.community.goals.features;

import com.community.goals.Border;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;

import java.util.logging.Logger;

/**
 * Manages world border expansion for community goals
 */
public class BorderExpansionManager {
    private final Border borderConfig;
    private final Logger logger;

    public BorderExpansionManager(Border borderConfig, Logger logger) {
        this.borderConfig = borderConfig;
        this.logger = logger;
        initializeBorder();
    }

    /**
     * Initialize the world border with current configuration
     */
    private void initializeBorder() {
        try {
            World world = Bukkit.getWorld(borderConfig.getWorldName());
            if (world != null) {
                WorldBorder wb = world.getWorldBorder();
                
                // Set center from config
                wb.setCenter(borderConfig.getCenterX(), borderConfig.getCenterZ());
                
                // Set size from config (this restores the border on server restart)
                wb.setSize(borderConfig.getSize());
                
                logger.info("World border initialized: Center(" +
                    borderConfig.getCenterX() + ", " + borderConfig.getCenterZ() +
                    ") Size: " + borderConfig.getSize());
            }
        } catch (Exception e) {
            logger.warning("Failed to initialize world border: " + e.getMessage());
        }
    }

    /**
     * Expand the world border by the configured amount
     */
    public boolean expandBorder() {
        return expandBorder(borderConfig.getExpansionAmount());
    }

    /**
     * Expand the world border by a custom amount
     */
    public boolean expandBorder(double amount) {
        try {
            World world = Bukkit.getWorld(borderConfig.getWorldName());
            if (world == null) {
                logger.warning("World not found: " + borderConfig.getWorldName());
                return false;
            }

            WorldBorder wb = world.getWorldBorder();
            double currentSize = wb.getSize();
            double newSize = currentSize + amount;

            // Update our internal config immediately (before animation)
            borderConfig.expandBorder(amount);
            
            // Animate the border expansion over 30 seconds
            wb.setSize(newSize, 30);
            
            logger.info("Border expanded from " + currentSize + " to " + newSize + " (+" + amount + ")");
            return true;
        } catch (Exception e) {
            logger.warning("Failed to expand world border: " + e.getMessage());
            return false;
        }
    }

    /**
     * Set the world border to a specific size
     */
    public boolean setSize(double size) {
        try {
            World world = Bukkit.getWorld(borderConfig.getWorldName());
            if (world == null) {
                logger.warning("World not found: " + borderConfig.getWorldName());
                return false;
            }

            WorldBorder wb = world.getWorldBorder();
            wb.setSize(size, 30);
            borderConfig.setSize(size);
            return true;
        } catch (Exception e) {
            logger.warning("Failed to set world border size: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get current world border size
     */
    public double getCurrentSize() {
        try {
            World world = Bukkit.getWorld(borderConfig.getWorldName());
            if (world != null) {
                return world.getWorldBorder().getSize();
            }
        } catch (Exception e) {
            logger.warning("Failed to get world border size: " + e.getMessage());
        }
        return borderConfig.getSize();
    }

    /**
     * Get the border center
     */
    public org.bukkit.util.Vector getBorderCenter() {
        return new org.bukkit.util.Vector(
            borderConfig.getCenterX(),
            0,
            borderConfig.getCenterZ()
        );
    }

    /**
     * Update border center
     */
    public boolean setCenter(double x, double z) {
        try {
            World world = Bukkit.getWorld(borderConfig.getWorldName());
            if (world == null) {
                logger.warning("World not found: " + borderConfig.getWorldName());
                return false;
            }

            WorldBorder wb = world.getWorldBorder();
            wb.setCenter(x, z);
            borderConfig.setCenterX(x);
            borderConfig.setCenterZ(z);
            return true;
        } catch (Exception e) {
            logger.warning("Failed to set world border center: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get the configured border
     */
    public Border getBorderConfig() {
        return borderConfig;
    }

    /**
     * Reload border from config
     */
    public void reload() {
        initializeBorder();
    }

    /**
     * Get information about current border state
     */
    public BorderInfo getInfo() {
        World world = Bukkit.getWorld(borderConfig.getWorldName());
        if (world != null) {
            WorldBorder wb = world.getWorldBorder();
            return new BorderInfo(
                borderConfig.getWorldName(),
                wb.getCenter().getX(),
                wb.getCenter().getZ(),
                borderConfig.getSize(), // Use config size (target size) instead of current animated size
                borderConfig.getExpansionAmount()
            );
        }
        return new BorderInfo(
            borderConfig.getWorldName(),
            borderConfig.getCenterX(),
            borderConfig.getCenterZ(),
            borderConfig.getSize(),
            borderConfig.getExpansionAmount()
        );
    }

    /**
     * Information about a world border
     */
    public static class BorderInfo {
        public final String worldName;
        public final double centerX;
        public final double centerZ;
        public final double currentSize;
        public final double expansionAmount;

        public BorderInfo(String worldName, double centerX, double centerZ, 
                         double currentSize, double expansionAmount) {
            this.worldName = worldName;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.currentSize = currentSize;
            this.expansionAmount = expansionAmount;
        }

        @Override
        public String toString() {
            return String.format(
                "Border: %s | Center: (%.1f, %.1f) | Size: %.1f | Expansion: %.1f",
                worldName, centerX, centerZ, currentSize, expansionAmount
            );
        }
    }
}
