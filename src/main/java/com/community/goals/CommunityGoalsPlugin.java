package com.community.goals;

import com.community.goals.commands.*;
import com.community.goals.features.BorderExpansionManager;
import com.community.goals.features.ProgressAnnouncementManager;
import com.community.goals.logic.GoalProgressTracker;
import com.community.goals.logic.TurnInHandler;
import com.community.goals.npc.FancyNpcManager;
import com.community.goals.npc.NPCInteractionHandler;
import com.community.goals.persistence.ConfigManager;
import com.community.goals.persistence.PersistenceManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Paths;

/**
 * Main plugin class for the Community Goals plugin
 */
public class CommunityGoalsPlugin extends JavaPlugin {

    private GoalProgressTracker goalProgressTracker;
    private PersistenceManager persistenceManager;
    private ConfigManager configManager;
    private BorderExpansionManager borderExpansionManager;
    private ProgressAnnouncementManager announcementManager;
    private FancyNpcManager npcManager;
    private NPCInteractionHandler npcInteractionHandler;
    private TurnInHandler turnInHandler;

    @Override
    public void onEnable() {
        getLogger().info("Enabling Community Goals plugin...");

        try {
            // Create data folder if it doesn't exist
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }

            // Initialize configuration
            saveDefaultConfig();
            String configPath = Paths.get(getDataFolder().getAbsolutePath(), "config.yml").toString();
            configManager = new ConfigManager(configPath);

            // Initialize persistence
            String dataPath = Paths.get(getDataFolder().getAbsolutePath(), "data").toString();
            persistenceManager = new PersistenceManager(dataPath);

            // Initialize core logic
            goalProgressTracker = new GoalProgressTracker(persistenceManager);
            turnInHandler = new TurnInHandler(goalProgressTracker);
            
            // Register goal completion listener for border expansion and announcements
            goalProgressTracker.addListener(new GoalCompletionHandler());

            // Initialize features
            String worldName = configManager.getString("world-border.world", "world");
            long centerX = configManager.getLong("world-border.center-x", 0L);
            long centerZ = configManager.getLong("world-border.center-z", 0L);
            long initialSize = configManager.getLong("world-border.initial-size", 500L);
            long expansionAmount = configManager.getLong("world-border.expansion-amount", 100L);

            Border borderConfig = new Border(worldName, centerX, centerZ, initialSize, expansionAmount);
            borderExpansionManager = new BorderExpansionManager(borderConfig);

            announcementManager = new ProgressAnnouncementManager();

            // Initialize NPC system
            npcManager = new FancyNpcManager(this);
            npcInteractionHandler = new NPCInteractionHandler(npcManager, goalProgressTracker);

            // Register commands
            registerCommands();

            // Register event listeners
            getServer().getPluginManager().registerEvents(npcInteractionHandler, this);

            getLogger().info("Community Goals plugin enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable Community Goals plugin: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling Community Goals plugin...");

        // Save all data on disable
        if (persistenceManager != null && goalProgressTracker != null) {
            persistenceManager.saveGoals(goalProgressTracker.getAllGoals());
        }

        // Clean up NPC system
        if (npcManager != null) {
            npcManager.saveNPCs();
        }

        getLogger().info("Community Goals plugin disabled.");
    }

    /**
     * Register all plugin commands
     */
    private void registerCommands() {
        // Player commands
        GoalPlayerCommand playerCommand = new GoalPlayerCommand(goalProgressTracker, persistenceManager, turnInHandler);
        getCommand("goal").setExecutor(playerCommand);

        // Admin commands
        GoalAdminCommand adminCommand = new GoalAdminCommand(goalProgressTracker, persistenceManager, borderExpansionManager, configManager);
        getCommand("goal-admin").setExecutor(adminCommand);

        // NPC commands
        NPCCommand npcCommand = new NPCCommand(goalProgressTracker, persistenceManager, npcManager);
        getCommand("goal-npc").setExecutor(npcCommand);

        getLogger().info("Commands registered successfully!");
    }

    /**
     * Get the goal progress tracker
     */
    public GoalProgressTracker getGoalProgressTracker() {
        return goalProgressTracker;
    }

    /**
     * Get the persistence manager
     */
    public PersistenceManager getPersistenceManager() {
        return persistenceManager;
    }

    /**
     * Get the border expansion manager
     */
    public BorderExpansionManager getBorderExpansionManager() {
        return borderExpansionManager;
    }

    /**
     * Get the announcement manager
     */
    public ProgressAnnouncementManager getAnnouncementManager() {
        return announcementManager;
    }

    /**
     * Get the NPC manager
     */
    public FancyNpcManager getNpcManager() {
        return npcManager;
    }

    /**
     * Handles goal completion events
     */
    private class GoalCompletionHandler implements GoalProgressTracker.ProgressListener {
        @Override
        public void onProgressUpdated(Goal goal, long previousProgress, long amountAdded) {
            // Handle progress updates if needed
        }

        @Override
        public void onGoalCompleted(Goal goal) {
            // Broadcast server-wide message
            String message = String.format("§6§l[Community Goals] §a%s completed! §7Expanding world border...", goal.getName());
            getServer().broadcastMessage(message);
            
            // Get border info before expansion
            BorderExpansionManager.BorderInfo beforeInfo = borderExpansionManager.getInfo();
            getLogger().info("Border before expansion: " + beforeInfo.currentSize);
            
            // Expand the border
            if (borderExpansionManager.expandBorder()) {
                BorderExpansionManager.BorderInfo afterInfo = borderExpansionManager.getInfo();
                getLogger().info("Border after expansion: " + afterInfo.currentSize);
                
                String borderMessage = String.format("§6§l[Community Goals] §7World border expanded to §f%.0f blocks§7!", afterInfo.currentSize);
                getServer().broadcastMessage(borderMessage);
                
                // Save border configuration
                saveBorderConfig();
                getLogger().info("Border configuration saved to config file");
            } else {
                getServer().broadcastMessage("§c§l[Community Goals] §7Failed to expand world border!");
            }
        }

        @Override
        public void onGoalCreated(Goal goal) {
            // Handle goal creation if needed
        }

        @Override
        public void onGoalDeleted(Goal goal) {
            // Handle goal deletion if needed
        }

        @Override
        public void onGoalUpdated(Goal goal) {
            // Handle goal updates if needed
        }
    }

    /**
     * Save current border configuration to config
     */
    private void saveBorderConfig() {
        try {
            BorderExpansionManager.BorderInfo info = borderExpansionManager.getInfo();
            configManager.set("world-border.center-x", info.centerX);
            configManager.set("world-border.center-z", info.centerZ);
            configManager.set("world-border.initial-size", info.currentSize);
            configManager.saveConfig();
        } catch (Exception e) {
            getLogger().warning("Failed to save border configuration: " + e.getMessage());
        }
    }
}
