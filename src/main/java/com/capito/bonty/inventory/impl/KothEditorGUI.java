package com.capito.bonty.inventory.impl;

import com.capito.bonty.Bonty;
import com.capito.bonty.data.KothZone;
import com.capito.bonty.enums.KothMode;
import com.capito.bonty.inventory.InventoryButton;
import com.capito.bonty.inventory.InventoryGUI;
import com.capito.bonty.utils.ColorUtil;
import com.capito.bonty.utils.TimeUtil;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class KothEditorGUI extends InventoryGUI {
    private final Bonty plugin;
    private final String kothName;
    
    public KothEditorGUI(Bonty plugin, String kothName) {
        this.plugin = plugin;
        this.kothName = kothName;
    }
    
    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, ColorUtil.color("&6Edit: &e" + kothName));
    }
    
    @Override
    public void decorate(Player player) {
        KothZone zone = plugin.getKothManager().getZone(kothName);
        if (zone == null) {
            player.closeInventory();
            return;
        }

        if (zone.isActive()) {
            player.sendMessage(ColorUtil.color("&8[&6Bonty&8] &cYou cannot edit a KoTH that is in progress. Stop it first."));
            org.bukkit.Bukkit.getScheduler().runTask(plugin, player::closeInventory);
            return;
        }
        
        addButton(10, new InventoryButton()
            .creator(p -> createItem(XMaterial.COMPASS, "&6Mode",
                "&7Current: &e" + zone.getMode().name(),
                "&aClick to toggle"))
            .consumer(event -> {
                KothMode newMode = zone.getMode() == KothMode.CAPTURE ? KothMode.SCORE : KothMode.CAPTURE;
                zone.setMode(newMode);
                plugin.getKothManager().saveZone(zone);
                Player clicker = (Player) event.getWhoClicked();
                clicker.sendMessage(ColorUtil.color("&aMode changed to &e" + newMode.name()));
                plugin.getGuiManager().openGUI(new KothEditorGUI(plugin, kothName), clicker);
            })
        );
        
        addButton(11, new InventoryButton()
            .creator(p -> createItem(XMaterial.CLOCK, "&6Capture Time",
                "&7Current: &e" + TimeUtil.formatTime(zone.getCaptureTime()),
                "&aLeft Click: &e+30s",
                "&cRight Click: &e-30s"))
            .consumer(event -> {
                int change = event.isLeftClick() ? 30 : -30;
                zone.setCaptureTime(Math.max(10, zone.getCaptureTime() + change));
                plugin.getKothManager().saveZone(zone);
                Player clicker = (Player) event.getWhoClicked();
                plugin.getGuiManager().openGUI(new KothEditorGUI(plugin, kothName), clicker);
            })
        );
        
        addButton(12, new InventoryButton()
            .creator(p -> createItem(XMaterial.GOLDEN_APPLE, "&6Max Score",
                "&7Current: &e" + zone.getMaxScore(),
                "&aLeft Click: &e+10",
                "&cRight Click: &e-10"))
            .consumer(event -> {
                int change = event.isLeftClick() ? 10 : -10;
                zone.setMaxScore(Math.max(10, zone.getMaxScore() + change));
                plugin.getKothManager().saveZone(zone);
                Player clicker = (Player) event.getWhoClicked();
                plugin.getGuiManager().openGUI(new KothEditorGUI(plugin, kothName), clicker);
            })
        );
        
        addButton(13, new InventoryButton()
            .creator(p -> createItem(XMaterial.REDSTONE, "&6Duration",
                "&7Current: &e" + TimeUtil.formatTime(zone.getDuration()),
                "&aLeft Click: &e+30s",
                "&cRight Click: &e-30s"))
            .consumer(event -> {
                int change = event.isLeftClick() ? 30 : -30;
                zone.setDuration(Math.max(10, zone.getDuration() + change));
                plugin.getKothManager().saveZone(zone);
                Player clicker = (Player) event.getWhoClicked();
                plugin.getGuiManager().openGUI(new KothEditorGUI(plugin, kothName), clicker);
            })
        );
        
        addButton(14, new InventoryButton()
            .creator(p -> createItem(XMaterial.ENDER_EYE, "&6Boss Bar",
                "&7Enabled: " + (zone.isBossbarEnabled() ? "&aYes" : "&cNo"),
                "&aClick to toggle"))
            .consumer(event -> {
                zone.setBossbarEnabled(!zone.isBossbarEnabled());
                plugin.getKothManager().saveZone(zone);
                Player clicker = (Player) event.getWhoClicked();
                plugin.getGuiManager().openGUI(new KothEditorGUI(plugin, kothName), clicker);
            })
        );
        
        addButton(15, new InventoryButton()
            .creator(p -> createItem(XMaterial.PAPER, "&6Scoreboard",
                "&7Enabled: " + (zone.isScoreboardEnabled() ? "&aYes" : "&cNo"),
                "&aClick to toggle"))
            .consumer(event -> {
                zone.setScoreboardEnabled(!zone.isScoreboardEnabled());
                plugin.getKothManager().saveZone(zone);
                Player clicker = (Player) event.getWhoClicked();
                plugin.getGuiManager().openGUI(new KothEditorGUI(plugin, kothName), clicker);
            })
        );
        
        addButton(16, new InventoryButton()
            .creator(p -> createItem(XMaterial.EMERALD, "&6Set Spawn",
                "&7Teleport location",
                "&aClick to set"))
            .consumer(event -> {
                Player clicker = (Player) event.getWhoClicked();
                zone.setSpawnLocation(clicker.getLocation());
                plugin.getKothManager().saveZone(zone);
                clicker.sendMessage(ColorUtil.color("&aSpawn location set!"));
                clicker.closeInventory();
            })
        );
        
        addButton(22, new InventoryButton()
            .creator(p -> createItem(XMaterial.BARRIER, "&cClose", "&7Click to close"))
            .consumer(event -> event.getWhoClicked().closeInventory())
        );
        
        super.decorate(player);
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