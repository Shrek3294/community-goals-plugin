package com.community.goals.features;

import com.community.goals.commands.PermissionManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class HelpBookManager implements Listener, CommandExecutor {
    private static final String BOOK_TITLE = "Community Goals Guide";
    private static final String BOOK_AUTHOR = "Community";
    private static final String HELP_COMMAND = "/help goalbook";

    private final JavaPlugin plugin;

    public HelpBookManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore()) {
            giveHelpBook(player, true);
        }
    }

    @EventHandler
    public void onHelpCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().trim();
        String[] parts = message.split("\\s+");
        if (parts.length >= 2 && parts[0].equalsIgnoreCase("/help") && parts[1].equalsIgnoreCase("goalbook")) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            if (!player.hasPermission(PermissionManager.GOALBOOK_USE)) {
                player.sendMessage(color("&cYou don't have permission to get the help book."));
                return;
            }
            giveHelpBook(player, true);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&cOnly players can use this command."));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission(PermissionManager.GOALBOOK_USE)) {
            player.sendMessage(color("&cYou don't have permission to get the help book."));
            return true;
        }

        giveHelpBook(player, true);
        return true;
    }

    private void giveHelpBook(Player player, boolean announce) {
        ItemStack book = createHelpBook();
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(book);
        for (ItemStack item : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
        if (announce) {
            player.sendMessage(color("&aHelp book added to your inventory."));
            player.sendMessage(color("&7Use &f" + HELP_COMMAND + " &7or &f/goalbook &7to get another copy."));
        }
    }

    private ItemStack createHelpBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK, 1);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) {
            return book;
        }

        meta.setTitle(BOOK_TITLE);
        meta.setAuthor(BOOK_AUTHOR);
        meta.setPages(
            color("&6&lWelcome to Community Goals\n"
                + "&7Premise:\n"
                + "&8Work together to complete community goals.\n"
                + "&8Each goal expands the world border for everyone.\n\n"
                + "&7Quick start:\n"
                + "&8/goal list\n"
                + "&8/goal view <id>\n"
                + "&8/goal turnin <id> <amount> [item]"),
            color("&6&lTurning In Goals\n"
                + "&7Step-by-step:\n"
                + "&81) /goal list\n"
                + "&82) /goal view <id>\n"
                + "&83) /goal turnin <id> <amount> [item]\n\n"
                + "&7If a Goals NPC is available, you can\n"
                + "&7right-click it to open goals.\n\n"
                + "&8Need another copy?\n"
                + "&8" + HELP_COMMAND + "\n"
                + "&8or /goalbook"),
            color("&6&lCustom Ore\n"
                + "&7Catalyst Ore\n"
                + "&8Looks like deepslate copper.\n"
                + "&8Mine it and smelt or blast it into\n"
                + "&eCatalyst Ingots&8.\n\n"
                + "&7Used for special tools and some goals."),
            color("&6&lCustom Tool\n"
                + "&7Catalyst Pickaxe\n"
                + "&8Recipe:\n"
                + "&8C C C\n"
                + "&8  S  \n"
                + "&8  E  \n\n"
                + "&7C = Catalyst Ingot\n"
                + "&7S = Stick\n"
                + "&7E = Echo Shard\n\n"
                + "&8Keep it for special recipes and goals.")
        );

        book.setItemMeta(meta);
        return book;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
