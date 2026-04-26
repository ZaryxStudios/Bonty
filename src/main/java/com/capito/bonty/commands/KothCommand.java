package com.capito.bonty.commands;

import com.capito.bonty.Bonty;
import com.capito.bonty.data.KothZone;
import com.capito.bonty.inventory.impl.KothEditorGUI;
import com.capito.bonty.inventory.impl.KothListGUI;
import com.capito.bonty.inventory.impl.LeaderboardGUI;
import com.capito.bonty.utils.ColorUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class KothCommand implements CommandExecutor, TabCompleter {
    private final Bonty plugin;
    private final Map<UUID, Location> selections = new HashMap<>();
    
    public KothCommand(Bonty plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String sub = args[0].toLowerCase();
        
        switch (sub) {
            case "help":
                sendHelp(sender);
                return true;
                
            case "create":
                if (!sender.hasPermission("bonty.create")) {
                    sender.sendMessage(msg("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.color("&cUsage: /koth create <name>"));
                    return true;
                }
                plugin.getKothManager().createZone(args[1]);
                sender.sendMessage(msg("koth-created").replace("{name}", args[1]));
                return true;
                
            case "delete":
                if (!sender.hasPermission("bonty.delete")) {
                    sender.sendMessage(msg("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.color("&cUsage: /koth delete <name>"));
                    return true;
                }
                plugin.getKothManager().deleteZone(args[1]);
                sender.sendMessage(msg("koth-deleted").replace("{name}", args[1]));
                return true;
                
            case "start":
                if (!sender.hasPermission("bonty.start")) {
                    sender.sendMessage(msg("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.color("&cUsage: /koth start <name>"));
                    return true;
                }
                KothZone startZone = plugin.getKothManager().getZone(args[1]);
                if (startZone == null) {
                    sender.sendMessage(msg("koth-not-found").replace("{name}", args[1]));
                    return true;
                }
                plugin.getKothManager().startKoth(args[1]);
                sender.sendMessage(msg("koth-started").replace("{name}", args[1]));
                return true;
                
            case "stop":
                if (!sender.hasPermission("bonty.stop")) {
                    sender.sendMessage(msg("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.color("&cUsage: /koth stop <name>"));
                    return true;
                }
                plugin.getKothManager().stopKoth(args[1]);
                sender.sendMessage(msg("koth-stopped").replace("{name}", args[1]));
                return true;
                
            case "edit":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ColorUtil.color("&cOnly players can use this command."));
                    return true;
                }
                if (!sender.hasPermission("bonty.edit")) {
                    sender.sendMessage(msg("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.color("&cUsage: /koth edit <name>"));
                    return true;
                }
                KothZone editZone = plugin.getKothManager().getZone(args[1]);
                if (editZone == null) {
                    sender.sendMessage(msg("koth-not-found").replace("{name}", args[1]));
                    return true;
                }
                if (editZone.isActive()) {
                    sender.sendMessage(ColorUtil.color("&8[&6Bonty&8] &cNo puedes editar un KoTH que est\u00e1 en marcha. Det\u00e9nte primero."));
                    return true;
                }
                plugin.getGuiManager().openGUI(new KothEditorGUI(plugin, args[1]), (Player) sender);
                return true;
                
            case "list":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ColorUtil.color("&cOnly players can use this command."));
                    return true;
                }
                if (!sender.hasPermission("bonty.list")) {
                    sender.sendMessage(msg("no-permission"));
                    return true;
                }
                plugin.getGuiManager().openGUI(new KothListGUI(plugin), (Player) sender);
                return true;
                
            case "leaderboard":
            case "lb":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ColorUtil.color("&cOnly players can use this command."));
                    return true;
                }
                if (!sender.hasPermission("bonty.leaderboard")) {
                    sender.sendMessage(msg("no-permission"));
                    return true;
                }
                String period = args.length > 1 ? args[1] : "daily";
                plugin.getGuiManager().openGUI(new LeaderboardGUI(plugin, period), (Player) sender);
                return true;
                
            case "wand":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ColorUtil.color("&cOnly players can use this command."));
                    return true;
                }
                if (!sender.hasPermission("bonty.wand")) {
                    sender.sendMessage(msg("no-permission"));
                    return true;
                }
                giveWand((Player) sender);
                sender.sendMessage(msg("wand-received"));
                sender.sendMessage(ColorUtil.color("&7Use left/right click with the wand to select Pos1/Pos2."));
                return true;
                
            case "setpos1":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ColorUtil.color("&cOnly players can use this command."));
                    return true;
                }
                if (!sender.hasPermission("bonty.create")) {
                    sender.sendMessage(msg("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.color("&cUsage: /koth setpos1 <name>"));
                    return true;
                }
                KothZone pos1Zone = plugin.getKothManager().getZone(args[1]);
                if (pos1Zone == null) {
                    sender.sendMessage(msg("koth-not-found").replace("{name}", args[1]));
                    return true;
                }
                Player pos1Player = (Player) sender;
                Location loc1 = plugin.getKothManager().getSelection(pos1Player.getUniqueId(), true);
                if (loc1 == null) {
                    loc1 = pos1Player.getLocation();
                }
                pos1Zone.setPos1(loc1);
                plugin.getKothManager().saveZone(pos1Zone);
                sender.sendMessage(msg("position-set")
                    .replace("{pos}", "1")
                    .replace("{x}", String.valueOf(loc1.getBlockX()))
                    .replace("{y}", String.valueOf(loc1.getBlockY()))
                    .replace("{z}", String.valueOf(loc1.getBlockZ())));
                return true;
                
            case "setpos2":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ColorUtil.color("&cOnly players can use this command."));
                    return true;
                }
                if (!sender.hasPermission("bonty.create")) {
                    sender.sendMessage(msg("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.color("&cUsage: /koth setpos2 <name>"));
                    return true;
                }
                KothZone pos2Zone = plugin.getKothManager().getZone(args[1]);
                if (pos2Zone == null) {
                    sender.sendMessage(msg("koth-not-found").replace("{name}", args[1]));
                    return true;
                }
                Player pos2Player = (Player) sender;
                Location loc2 = plugin.getKothManager().getSelection(pos2Player.getUniqueId(), false);
                if (loc2 == null) {
                    loc2 = pos2Player.getLocation();
                }
                pos2Zone.setPos2(loc2);
                plugin.getKothManager().saveZone(pos2Zone);
                sender.sendMessage(msg("position-set")
                    .replace("{pos}", "2")
                    .replace("{x}", String.valueOf(loc2.getBlockX()))
                    .replace("{y}", String.valueOf(loc2.getBlockY()))
                    .replace("{z}", String.valueOf(loc2.getBlockZ())));
                return true;
                
            case "reload":
                if (!sender.hasPermission("bonty.reload")) {
                    sender.sendMessage(msg("no-permission"));
                    return true;
                }
                plugin.reload();
                sender.sendMessage(msg("config-reloaded"));
                return true;
                
            default:
                sendHelp(sender);
                return true;
        }
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.color("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(ColorUtil.color("&6&lKoTH Commands:"));
        sender.sendMessage("");
        sender.sendMessage(ColorUtil.color("&f<> &7= &fRequire &8| &f[] &7= &fOptional"));
        sender.sendMessage("");
        sender.sendMessage(ColorUtil.color("&e/koth create <name> &7- Create a KoTH"));
        sender.sendMessage(ColorUtil.color("&e/koth delete <name> &7- Delete a KoTH"));
        sender.sendMessage(ColorUtil.color("&e/koth start <name> &7- Start a KoTH"));
        sender.sendMessage(ColorUtil.color("&e/koth stop <name> &7- Stop a KoTH"));
        sender.sendMessage(ColorUtil.color("&e/koth edit <name> &7- Open editor GUI"));
        sender.sendMessage(ColorUtil.color("&e/koth list &7- List all KoTHs"));
        sender.sendMessage(ColorUtil.color("&e/koth leaderboard [period] &7- View leaderboard"));
        sender.sendMessage(ColorUtil.color("&e/koth wand &7- Get selection wand"));
        sender.sendMessage(ColorUtil.color("&e/koth setpos1 <name> &7- Set position 1"));
        sender.sendMessage(ColorUtil.color("&e/koth setpos2 <name> &7- Set position 2"));
        sender.sendMessage(ColorUtil.color("&e/koth reload &7- Reload configuration"));
        sender.sendMessage(ColorUtil.color("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }
    
    private void giveWand(Player player) {
        Material wandMaterial = Material.matchMaterial(plugin.getConfig().getString("wand.item", "GOLDEN_AXE"));
        if (wandMaterial == null) {
            wandMaterial = Material.GOLDEN_AXE;
        }

        ItemStack wand = new ItemStack(wandMaterial);
        
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(plugin.getConfig().getString("wand.name")));
            List<String> lore = new ArrayList<>();
            for (String line : plugin.getConfig().getStringList("wand.lore")) {
                lore.add(ColorUtil.color(line));
            }
            meta.setLore(lore);
            wand.setItemMeta(meta);
        }
        
        player.getInventory().addItem(wand);
    }
    
    private String msg(String key) {
        return ColorUtil.color(plugin.getConfig().getString("messages.prefix") + 
            plugin.getConfig().getString("messages." + key));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "delete", "start", "stop", "edit", "list", "leaderboard", "wand", "setpos1", "setpos2", "reload", "help")
                .stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("start") || 
                args[0].equalsIgnoreCase("stop") || args[0].equalsIgnoreCase("edit") ||
                args[0].equalsIgnoreCase("setpos1") || args[0].equalsIgnoreCase("setpos2")) {
                return plugin.getKothManager().getAllZones().stream()
                    .map(KothZone::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
            
            if (args[0].equalsIgnoreCase("leaderboard") || args[0].equalsIgnoreCase("lb")) {
                return Arrays.asList("hourly", "daily", "weekly", "monthly")
                    .stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            }
        }
        
        return Collections.emptyList();
    }
}