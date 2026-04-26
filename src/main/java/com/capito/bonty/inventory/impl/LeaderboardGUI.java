package com.capito.bonty.inventory.impl;

import com.capito.bonty.Bonty;
import com.capito.bonty.data.LeaderboardEntry;
import com.capito.bonty.inventory.InventoryButton;
import com.capito.bonty.inventory.InventoryGUI;
import com.capito.bonty.utils.ColorUtil;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardGUI extends InventoryGUI {
    private final Bonty plugin;
    private final String period;
    
    public LeaderboardGUI(Bonty plugin, String period) {
        this.plugin = plugin;
        this.period = period;
    }
    
    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, ColorUtil.color("&6&lLeaderboard - &e" + period));
    }
    
    @Override
    public void decorate(Player player) {
        List<LeaderboardEntry> entries = plugin.getLeaderboardManager().getTopPlayers(period, 45);
        
        int slot = 0;
        int rank = 1;
        for (LeaderboardEntry entry : entries) {
            if (slot >= 45) break;
            
            final int finalRank = rank;
            addButton(slot, new InventoryButton()
                .creator(p -> createPlayerHead(entry, finalRank))
                .consumer(event -> {})
            );
            slot++;
            rank++;
        }
        
        addButton(49, new InventoryButton()
            .creator(p -> createItem(XMaterial.BARRIER, "&cClose", "&7Click to close"))
            .consumer(event -> event.getWhoClicked().closeInventory())
        );
        
        super.decorate(player);
    }
    
    private ItemStack createPlayerHead(LeaderboardEntry entry, int rank) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = skull.getItemMeta();
        if (meta != null) {
            if (meta instanceof SkullMeta) {
                ((SkullMeta) meta).setOwner(entry.getPlayerName());
            }
            meta.setDisplayName(ColorUtil.color("&6#" + rank + " &e" + entry.getPlayerName()));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.color("&7Captures: &e" + entry.getCaptures()));
            lore.add(ColorUtil.color("&7Wins: &e" + entry.getWins()));
            meta.setLore(lore);
            skull.setItemMeta(meta);
        }
        return skull;
    }
    
    private ItemStack createItem(XMaterial material, String name, String... lore) {
        ItemStack item = material.parseItem();
        if (item == null) return new ItemStack(org.bukkit.Material.STONE);
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(name));
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(ColorUtil.color(line));
            }
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return item;
    }
}