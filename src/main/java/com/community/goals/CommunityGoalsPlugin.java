package com.community.goals;

import com.community.goals.commands.*;
import com.community.goals.features.BorderExpansionManager;
import com.community.goals.features.BorderManagerRegistry;
import com.community.goals.features.ProgressAnnouncementManager;
import com.community.goals.gui.GoalGuiManager;
import com.community.goals.logic.GoalProgressTracker;
import com.community.goals.logic.GoalQueueManager;
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
    private BorderManagerRegistry borderRegistry;
    private ProgressAnnouncementManager announcementManager;
    private FancyNpcManager npcManager;
    private NPCInteractionHandler npcInteractionHandler;
    private TurnInHandler turnInHandler;
    private GoalGuiManager goalGuiManager;
    private GoalQueueManager goalQueueManager;

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
            configManager = new ConfigManager(configPath, getLogger());

            // Initialize border managers
            borderRegistry = BorderManagerRegistry.fromConfig(configManager, getLogger());

            // Initialize persistence
            String dataPath = Paths.get(getDataFolder().getAbsolutePath(), "data").toString();
            persistenceManager = new PersistenceManager(dataPath, getLogger(), borderRegistry.getDefaultWorld());

            // Initialize core logic
            goalProgressTracker = new GoalProgressTracker(persistenceManager);
            turnInHandler = new TurnInHandler(goalProgressTracker);
            goalGuiManager = new GoalGuiManager(goalProgressTracker, turnInHandler);
            boolean queueEnabled = configManager.getBoolean("goals.queue-enabled", false);
            goalQueueManager = new GoalQueueManager(goalProgressTracker, persistenceManager, queueEnabled, borderRegistry.getDefaultWorld());
            
            // Register goal completion listener for border expansion and announcements
            goalProgressTracker.addListener(new GoalCompletionHandler());

            // Initialize features
            announcementManager = new ProgressAnnouncementManager();

            // Initialize NPC system
            npcManager = new FancyNpcManager(this);
            npcInteractionHandler = new NPCInteractionHandler(npcManager, goalProgressTracker, goalGuiManager);

            // Register commands
            registerCommands();

            // Register event listeners
            getServer().getPluginManager().registerEvents(npcInteractionHandler, this);
            getServer().getPluginManager().registerEvents(goalGuiManager, this);

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
        GoalAdminCommand adminCommand = new GoalAdminCommand(goalProgressTracker, persistenceManager, borderRegistry, configManager, goalQueueManager, getLogger());
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
    public BorderManagerRegistry getBorderRegistry() {
        return borderRegistry;
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

    public GoalQueueManager getGoalQueueManager() {
        return goalQueueManager;
    }

    /**
     * Handles goal completion events
     */
    private class GoalCompletionHandler implements GoalProgressTracker.ProgressListener {
        @Override
        public void onProgressUpdated(Goal goal, long previousProgress, long amountAdded) {
            // Handle progress updates if needed
            if (goalGuiManager != null) {
                goalGuiManager.refreshOpenGoalsMenus();
            }
        }

        @Override
        public void onGoalCompleted(Goal goal) {
            // Broadcast server-wide message
            String message = String.format("§6§l[Community Goals] §a%s completed! §7Expanding world border...", goal.getName());
            getServer().broadcastMessage(message);
            BorderExpansionManager borderManager = borderRegistry.getManager(goal.getWorldName());
            if (borderManager == null) {
                getServer().broadcastMessage("AcAl[Community Goals] A7No border configured for world: " + goal.getWorldName());
            } else {
                BorderExpansionManager.BorderInfo beforeInfo = borderManager.getInfo();
                getLogger().info("Border before expansion: " + beforeInfo.currentSize);

                double expansionAmount = goal.getRewardExpansion() > 0
                    ? goal.getRewardExpansion()
                    : borderManager.getBorderConfig().getExpansionAmount();

                if (borderManager.expandBorder(expansionAmount)) {
                    BorderExpansionManager.BorderInfo afterInfo = borderManager.getInfo();
                    getLogger().info("Border after expansion: " + afterInfo.currentSize);

                    String borderMessage = String.format(
                        "A6Al[Community Goals] A7World border expanded to Af%.0f blocksA7!",
                        afterInfo.currentSize
                    );
                    getServer().broadcastMessage(borderMessage);

                    saveBorderConfig(goal.getWorldName());
                    getLogger().info("Border configuration saved to config file");
                } else {
                    getServer().broadcastMessage("AcAl[Community Goals] A7Failed to expand world border!");
                }
            }

            npcManager.deleteNPCsForGoal(goal.getId());
            if (goalQueueManager != null && goalQueueManager.isEnabled()) {
                goalQueueManager.handleGoalCompleted(goal);
            }
            goalProgressTracker.deleteGoal(goal.getId());
            goalGuiManager.refreshOpenGoalsMenus();
        }

        @Override
        public void onGoalCreated(Goal goal) {
            // Handle goal creation if needed
            if (goalGuiManager != null) {
                goalGuiManager.refreshOpenGoalsMenus();
            }
        }

        @Override
        public void onGoalDeleted(Goal goal) {
            // Handle goal deletion if needed
            if (goalGuiManager != null) {
                goalGuiManager.refreshOpenGoalsMenus();
            }
        }

        @Override
        public void onGoalUpdated(Goal goal) {
            // Handle goal updates if needed
            if (goalGuiManager != null) {
                goalGuiManager.refreshOpenGoalsMenus();
            }
        }
    }

    /**
     * Save current border configuration to config
     */
    private void saveBorderConfig(String worldName) {
        try {
            borderRegistry.saveBorderConfig(configManager, worldName);
        } catch (Exception e) {
            getLogger().warning("Failed to save border configuration: " + e.getMessage());
        }
    }
}
