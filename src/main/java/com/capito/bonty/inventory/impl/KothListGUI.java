package com.capito.bonty.inventory.impl;

import com.capito.bonty.Bonty;
import com.capito.bonty.data.KothZone;
import com.capito.bonty.inventory.InventoryButton;
import com.capito.bonty.inventory.InventoryGUI;
import com.capito.bonty.utils.ColorUtil;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class KothListGUI extends InventoryGUI {
    private final Bonty plugin;
    
    public KothListGUI(Bonty plugin) {
        this.plugin = plugin;
    }
    
    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, ColorUtil.color("&6&lKoTH List"));
    }
    
    @Override
    public void decorate(Player player) {
        List<KothZone> zones = plugin.getKothManager().getAllZones();
        
        int slot = 0;
        for (KothZone zone : zones) {
            if (slot >= 45) break;
            
            final String zoneName = zone.getName();
            addButton(slot, new InventoryButton()
                .creator(p -> createZoneItem(zone))
                .consumer(event -> {
                    Player clicker = (Player) event.getWhoClicked();
                    if (event.isLeftClick()) {
                        KothZone current = plugin.getKothManager().getZone(zoneName);
                        if (current != null && current.isActive()) {
                            clicker.sendMessage(ColorUtil.color("&8[&6Bonty&8] &cNo puedes editar un KoTH que está en marcha. Detente primero."));
                            return;
                        }
                        plugin.getGuiManager().openGUI(new KothEditorGUI(plugin, zoneName), clicker);
                    } else if (event.isRightClick()) {
                        if (zone.isActive()) {
                            plugin.getKothManager().stopKoth(zoneName);
                            clicker.sendMessage(ColorUtil.color("&aKoTH stopped!"));
                        } else {
                            plugin.getKothManager().startKoth(zoneName);
                            clicker.sendMessage(ColorUtil.color("&aKoTH started!"));
                        }
                        clicker.closeInventory();
                    }
                })
            );
            slot++;
        }
        
        addButton(49, new InventoryButton()
            .creator(p -> createItem(XMaterial.BARRIER, "&cClose", "&7Click to close"))
            .consumer(event -> event.getWhoClicked().closeInventory())
        );
        
        super.decorate(player);
    }
    
    private ItemStack createZoneItem(KothZone zone) {
        XMaterial material = zone.isActive() ? XMaterial.LIME_WOOL : XMaterial.RED_WOOL;
        String name = "&6" + zone.getDisplayName();
        List<String> lore = new ArrayList<>();
        lore.add("&7Mode: &e" + zone.getMode().name());
        lore.add("&7Status: " + (zone.isActive() ? "&aActive" : "&cInactive"));
        lore.add("");
        lore.add(zone.isActive() ? "&cLeft Click &7(disabled — KoTH is running)" : "&eLeft Click &7to edit");
        lore.add("&eRight Click &7to " + (zone.isActive() ? "stop" : "start"));
        
        return createItem(material, name, lore.toArray(new String[0]));
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