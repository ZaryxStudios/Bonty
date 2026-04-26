package com.capito.bonty.listeners;

import com.capito.bonty.Bonty;
import com.capito.bonty.data.KothSession;
import com.capito.bonty.data.KothZone;
import com.capito.bonty.enums.KothMode;
import com.capito.bonty.utils.ColorUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public class KothListener implements Listener {
    private final Bonty plugin;
    
    public KothListener(Bonty plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        
        Player player = event.getPlayer();
        KothZone zone = plugin.getKothManager().getZoneAt(player);
        
        if (zone != null && zone.isActive()) {
            KothSession session = plugin.getKothManager().getSession(zone.getName());
            if (session != null) {
                if (zone.getMode() == KothMode.CAPTURE) {
                    if (session.getCurrentCapturer() == null) {
                        session.setCurrentCapturer(player.getUniqueId());
                        session.setCaptureProgress(0);
                        String msg = plugin.getConfig().getString("messages.capture-started")
                            .replace("{player}", player.getName())
                            .replace("{koth}", zone.getDisplayName());
                        player.sendMessage(ColorUtil.color(plugin.getConfig().getString("messages.prefix") + msg));
                        plugin.getWebhookManager().sendEvent("capture-start", placeholders(zone, player));
                    } else if (!session.getCurrentCapturer().equals(player.getUniqueId())) {
                        Player oldCapturer = org.bukkit.Bukkit.getPlayer(session.getCurrentCapturer());
                        session.setCurrentCapturer(player.getUniqueId());
                        session.setCaptureProgress(0);
                        if (oldCapturer != null) {
                            plugin.getWebhookManager().sendEvent("capture-stop", placeholders(zone, oldCapturer));
                        }
                    }
                } else if (zone.getMode() == KothMode.SCORE) {
                    session.addScore(player.getUniqueId(), 1);
                }
            }
        }
    }

    @EventHandler
    public void onWandUse(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (!isWand(item)) return;

        if (event.getAction().name().contains("LEFT_CLICK")) {
            Location loc = event.getClickedBlock() != null ? event.getClickedBlock().getLocation() : event.getPlayer().getLocation();
            plugin.getKothManager().setSelection(event.getPlayer().getUniqueId(), loc, true);
            event.getPlayer().sendMessage(msg("position-set")
                .replace("{pos}", "1")
                .replace("{x}", String.valueOf(loc.getBlockX()))
                .replace("{y}", String.valueOf(loc.getBlockY()))
                .replace("{z}", String.valueOf(loc.getBlockZ())));
            event.setCancelled(true);
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setUseItemInHand(Event.Result.DENY);
            return;
        }

        if (event.getAction().name().contains("RIGHT_CLICK")) {
            Location loc = event.getClickedBlock() != null ? event.getClickedBlock().getLocation() : event.getPlayer().getLocation();
            plugin.getKothManager().setSelection(event.getPlayer().getUniqueId(), loc, false);
            event.getPlayer().sendMessage(msg("position-set")
                .replace("{pos}", "2")
                .replace("{x}", String.valueOf(loc.getBlockX()))
                .replace("{y}", String.valueOf(loc.getBlockY()))
                .replace("{z}", String.valueOf(loc.getBlockZ())));
            event.setCancelled(true);
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setUseItemInHand(Event.Result.DENY);
        }
    }

    @EventHandler
    public void onWandBreak(BlockBreakEvent event) {
        if (isWand(event.getPlayer().getInventory().getItemInMainHand())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        for (KothSession session : plugin.getKothManager().getActiveSessions().values()) {
            if (session.getCurrentCapturer() != null && session.getCurrentCapturer().equals(player.getUniqueId())) {
                session.setCurrentCapturer(null);
                session.setCaptureProgress(0);
            }
        }
    }

    private boolean isWand(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;

        Material configured = Material.matchMaterial(plugin.getConfig().getString("wand.item", "GOLDEN_AXE"));
        if (configured == null) configured = Material.GOLDEN_AXE;
        if (item.getType() != configured) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;

        String expectedName = ColorUtil.color(plugin.getConfig().getString("wand.name", "&6KoTH Selection Wand"));
        return meta.getDisplayName().equals(expectedName);
    }

    private String msg(String key) {
        return ColorUtil.color(plugin.getConfig().getString("messages.prefix") +
            plugin.getConfig().getString("messages." + key));
    }

    private Map<String, String> placeholders(KothZone zone, Player player) {
        Map<String, String> data = new HashMap<>();
        data.put("player", player.getName());
        data.put("koth", zone.getDisplayName());
        data.put("mode", zone.getMode().name());
        return data;
    }
}