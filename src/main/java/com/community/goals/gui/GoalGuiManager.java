package com.community.goals.gui;

import com.community.goals.Goal;
import com.community.goals.State;
import com.community.goals.logic.GoalProgressTracker;
import com.community.goals.logic.GoalQueueManager;
import com.community.goals.logic.TurnInHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GoalGuiManager implements Listener {
    private static final String GOALS_TITLE = "Community Goals";
    private static final String TURN_IN_TITLE_PREFIX = "Turn In: ";
    private static final int[] GOAL_SLOTS = new int[] {11, 13, 15};

    private final GoalProgressTracker tracker;
    private final TurnInHandler turnInHandler;
    private final GoalQueueManager queueManager;
    private final Map<UUID, GoalsMenuHolder> openGoalsMenus;

    public GoalGuiManager(GoalProgressTracker tracker, TurnInHandler turnInHandler, GoalQueueManager queueManager) {
        this.tracker = tracker;
        this.turnInHandler = turnInHandler;
        this.queueManager = queueManager;
        this.openGoalsMenus = new HashMap<>();
    }

    public void openGoalsMenu(Player player) {
        openGoalsMenu(player, player.getWorld().getName());
    }

    public void openGoalsMenu(Player player, String worldName) {
        List<Goal> goals = getDisplayGoals(worldName);
        GoalsMenuHolder holder = new GoalsMenuHolder(worldName);
        Inventory inventory = Bukkit.createInventory(holder, 27, GOALS_TITLE);
        holder.setInventory(inventory);

        for (int i = 0; i < GOAL_SLOTS.length; i++) {
            if (i >= goals.size()) {
                continue;
            }
            Goal goal = goals.get(i);
            boolean locked = goal.getState() != State.ACTIVE;
            ItemStack item = buildGoalItem(goal, locked);
            inventory.setItem(GOAL_SLOTS[i], item);
            holder.setGoalForSlot(GOAL_SLOTS[i], goal.getId());
        }

        player.openInventory(inventory);
        openGoalsMenus.put(player.getUniqueId(), holder);
    }

    public void refreshOpenGoalsMenus() {
        for (Map.Entry<UUID, GoalsMenuHolder> entry : new ArrayList<>(openGoalsMenus.entrySet())) {
            Player player = Bukkit.getPlayer(entry.getKey());
            GoalsMenuHolder holder = entry.getValue();
            if (player == null || !player.isOnline()) {
                openGoalsMenus.remove(entry.getKey());
                continue;
            }
            Inventory inventory = holder.getInventory();
            if (inventory == null) {
                openGoalsMenus.remove(entry.getKey());
                continue;
            }
            populateGoalsMenu(inventory, holder);
        }
    }

    private void populateGoalsMenu(Inventory inventory, GoalsMenuHolder holder) {
        inventory.clear();
        holder.clearSlots();

        List<Goal> goals = getDisplayGoals(holder.getWorldName());
        for (int i = 0; i < GOAL_SLOTS.length; i++) {
            if (i >= goals.size()) {
                continue;
            }
            Goal goal = goals.get(i);
            boolean locked = goal.getState() != State.ACTIVE;
            ItemStack item = buildGoalItem(goal, locked);
            inventory.setItem(GOAL_SLOTS[i], item);
            holder.setGoalForSlot(GOAL_SLOTS[i], goal.getId());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Inventory inventory = event.getInventory();
        if (inventory == null || inventory.getHolder() == null) {
            return;
        }

        if (inventory.getHolder() instanceof GoalsMenuHolder) {
            event.setCancelled(true);
            GoalsMenuHolder holder = (GoalsMenuHolder) inventory.getHolder();
            String goalId = holder.getGoalForSlot(event.getRawSlot());
            if (goalId == null) {
                return;
            }
            Goal goal = tracker.getGoal(goalId);
            if (goal == null) {
                return;
            }
            if (goal.getState() != State.ACTIVE) {
                Player player = (Player) event.getWhoClicked();
                player.sendMessage("§cThat goal is locked right now.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.8f);
                return;
            }
            if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT) {
                openTurnInMenu((Player) event.getWhoClicked(), goal);
            }
            return;
        }

        if (inventory.getHolder() instanceof TurnInHolder) {
            if (event.getClick() == ClickType.DOUBLE_CLICK) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory == null || inventory.getHolder() == null) {
            return;
        }

        if (inventory.getHolder() instanceof GoalsMenuHolder) {
            openGoalsMenus.remove(event.getPlayer().getUniqueId());
            return;
        }

        if (inventory.getHolder() instanceof TurnInHolder) {
            TurnInHolder holder = (TurnInHolder) inventory.getHolder();
            if (!(event.getPlayer() instanceof Player)) {
                return;
            }
            handleTurnInClose((Player) event.getPlayer(), holder, inventory);
        }
    }

    private void openTurnInMenu(Player player, Goal goal) {
        TurnInHolder holder = new TurnInHolder(goal.getId());
        Inventory inventory = Bukkit.createInventory(holder, InventoryType.CHEST, TURN_IN_TITLE_PREFIX + goal.getName());
        holder.setInventory(inventory);
        player.openInventory(inventory);
    }

    private void handleTurnInClose(Player player, TurnInHolder holder, Inventory inventory) {
        Goal goal = tracker.getGoal(holder.getGoalId());
        if (goal == null || goal.getState() != State.ACTIVE) {
            returnItems(player, inventory.getContents());
            inventory.clear();
            if (goal == null) {
                player.sendMessage("§cThat goal is no longer available.");
            } else {
                player.sendMessage("§cThat goal is locked right now.");
            }
            return;
        }

        Material material = resolveMaterialForGoal(goal.getId());
        ItemStack[] contents = inventory.getContents();
        if (material == null) {
            returnItems(player, contents);
            inventory.clear();
            player.sendMessage("§cThis goal doesn't accept any items yet.");
            return;
        }

        int totalAllowed = countMaterial(contents, material);
        if (totalAllowed <= 0) {
            returnItems(player, contents);
            inventory.clear();
            return;
        }

        long remaining = goal.getTargetProgress() - goal.getCurrentProgress();
        int acceptedAmount = (int) Math.min(totalAllowed, remaining);
        if (acceptedAmount <= 0) {
            returnItems(player, contents);
            inventory.clear();
            player.sendMessage("§aThat goal is already completed.");
            return;
        }

        TurnInHandler.ValidationResult validation = turnInHandler.validateTurnIn(goal.getId(), acceptedAmount);
        if (!validation.isValid()) {
            returnItems(player, contents);
            inventory.clear();
            player.sendMessage("§c" + validation.getReason());
            return;
        }

        List<ItemStack> leftovers = removeAcceptedItems(contents, material, acceptedAmount);
        inventory.clear();

        TurnInHandler.TurnInResult result = turnInHandler.processTurnIn(goal.getId(), acceptedAmount, player.getName());
        if (!result.isSuccess()) {
            returnItems(player, contents);
            player.sendMessage("§c" + result.getMessage());
            return;
        }

        returnItems(player, leftovers.toArray(new ItemStack[0]));
        player.sendMessage(String.format("§aTurned in %d %s for %s.", acceptedAmount,
            material.name().toLowerCase().replace("_", " "), goal.getName()));
    }

    private List<ItemStack> removeAcceptedItems(ItemStack[] contents, Material material, int acceptedAmount) {
        List<ItemStack> leftovers = new ArrayList<>();
        int remaining = acceptedAmount;

        for (ItemStack item : contents) {
            if (item == null) {
                continue;
            }
            if (item.getType() != material) {
                leftovers.add(item.clone());
                continue;
            }

            int amount = item.getAmount();
            if (remaining <= 0) {
                leftovers.add(item.clone());
                continue;
            }

            if (amount <= remaining) {
                remaining -= amount;
            } else {
                ItemStack extra = item.clone();
                extra.setAmount(amount - remaining);
                leftovers.add(extra);
                remaining = 0;
            }
        }

        return leftovers;
    }

    private void returnItems(Player player, ItemStack[] items) {
        if (items == null || items.length == 0) {
            return;
        }
        List<ItemStack> toReturn = new ArrayList<>();
        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR) {
                toReturn.add(item);
            }
        }
        if (toReturn.isEmpty()) {
            return;
        }
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(toReturn.toArray(new ItemStack[0]));
        for (ItemStack item : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
    }

    private int countMaterial(ItemStack[] contents, Material material) {
        int total = 0;
        for (ItemStack item : contents) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private Material resolveMaterialForGoal(String goalId) {
        String id = goalId.toLowerCase(Locale.ROOT);
        Material direct = Material.matchMaterial(goalId.toUpperCase(Locale.ROOT));
        if (direct != null) {
            return direct;
        }

        if (id.contains("diamond")) return Material.DIAMOND;
        if (id.contains("iron")) return Material.IRON_INGOT;
        if (id.contains("gold")) return Material.GOLD_INGOT;
        if (id.contains("emerald")) return Material.EMERALD;
        if (id.contains("coal")) return Material.COAL;
        if (id.contains("wood") || id.contains("log")) return Material.OAK_LOG;
        if (id.contains("stone")) return Material.STONE;
        if (id.contains("cobble")) return Material.COBBLESTONE;
        if (id.contains("wheat")) return Material.WHEAT;
        if (id.contains("carrot")) return Material.CARROT;
        if (id.contains("potato")) return Material.POTATO;
        if (id.contains("beef")) return Material.BEEF;
        if (id.contains("pork")) return Material.PORKCHOP;
        if (id.contains("chicken")) return Material.CHICKEN;
        if (id.contains("fish")) return Material.COD;
        if (id.contains("leather") || id.contains("hide")) return Material.LEATHER;
        if (id.contains("netherite")) return Material.NETHERITE_INGOT;
        if (id.contains("blaze")) return Material.BLAZE_ROD;
        if (id.contains("wart")) return Material.NETHER_WART;
        if (id.contains("ghast")) return Material.GHAST_TEAR;
        if (id.contains("ancient") || id.contains("debris")) return Material.ANCIENT_DEBRIS;
        if (id.contains("quartz")) return Material.QUARTZ;
        if (id.contains("magma")) return Material.MAGMA_CREAM;

        return null;
    }

    private List<Goal> getDisplayGoals(String worldName) {
        List<Goal> all = new ArrayList<>(tracker.getGoalsForWorld(worldName));
        all.removeIf(goal -> goal.getState() == State.COMPLETED);

        Map<String, Goal> goalsById = new HashMap<>();
        for (Goal goal : all) {
            goalsById.put(goal.getId().toLowerCase(Locale.ROOT), goal);
        }

        List<Goal> ordered = new ArrayList<>();
        if (queueManager != null && queueManager.isEnabled()) {
            List<String> queue = queueManager.getQueue(worldName);
            if (!queue.isEmpty()) {
                for (String goalId : queue) {
                    Goal goal = goalsById.remove(goalId.toLowerCase(Locale.ROOT));
                    if (goal != null) {
                        ordered.add(goal);
                    }
                }
            }
        }

        if (!goalsById.isEmpty()) {
            List<Goal> remaining = new ArrayList<>(goalsById.values());
            remaining.sort(Comparator.comparingLong(Goal::getCreatedAt));
            ordered.addAll(remaining);
        }

        if (ordered.size() > GOAL_SLOTS.length) {
            return ordered.subList(0, GOAL_SLOTS.length);
        }
        return ordered;
    }

    private ItemStack buildGoalItem(Goal goal, boolean locked) {
        Material material = locked ? Material.BARRIER : Material.EMERALD;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName((locked ? "§cLocked: " : "§a") + goal.getName());
            List<String> lore = new ArrayList<>();
            lore.add("§7" + goal.getDescription());
            lore.add("§7World: §f" + goal.getWorldName());
            lore.add("§7Progress: §f" + goal.getCurrentProgress() + " / " + goal.getTargetProgress());
            lore.add(String.format("§7Completion: §a%.1f%%", goal.getProgressPercentage()));
            if (goal.getRewardExpansion() > 0) {
                lore.add("§7Reward: §f" + goal.getRewardExpansion() + " blocks");
            }
            lore.add("§7Status: " + goal.getState().getColoredName());
            if (locked) {
                lore.add("§cLocked");
            } else {
                lore.add("§eClick to turn in items");
            }
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static class GoalsMenuHolder implements InventoryHolder {
        private final Map<Integer, String> slotToGoalId = new HashMap<>();
        private final String worldName;
        private Inventory inventory;

        private GoalsMenuHolder(String worldName) {
            this.worldName = worldName;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        public void setGoalForSlot(int slot, String goalId) {
            slotToGoalId.put(slot, goalId);
        }

        public String getGoalForSlot(int slot) {
            return slotToGoalId.get(slot);
        }

        public void clearSlots() {
            slotToGoalId.clear();
        }

        public String getWorldName() {
            return worldName;
        }
    }

    private static class TurnInHolder implements InventoryHolder {
        private final String goalId;
        private Inventory inventory;

        private TurnInHolder(String goalId) {
            this.goalId = goalId;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        public String getGoalId() {
            return goalId;
        }
    }
}
