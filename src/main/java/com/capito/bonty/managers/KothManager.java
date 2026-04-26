package com.capito.bonty.managers;

import com.capito.bonty.Bonty;
import com.capito.bonty.data.KothSession;
import com.capito.bonty.data.KothZone;
import com.capito.bonty.enums.KothMode;
import com.capito.bonty.enums.ZoneStatus;
import com.capito.bonty.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class KothManager {
    private final Bonty plugin;
    private final Map<String, KothZone> zones;
    private final Map<String, KothSession> activeSessions;
    private final Map<UUID, Location> selectionPos1;
    private final Map<UUID, Location> selectionPos2;
    
    public KothManager(Bonty plugin) {
        this.plugin = plugin;
        this.zones = new ConcurrentHashMap<>();
        this.activeSessions = new ConcurrentHashMap<>();
        this.selectionPos1 = new ConcurrentHashMap<>();
        this.selectionPos2 = new ConcurrentHashMap<>();
    }
    
    public void loadAllZones() {
        plugin.getDatabaseManager().loadAllZones().thenAccept(loadedZones -> {
            zones.clear();
            for (KothZone zone : loadedZones) {
                zones.put(zone.getName(), zone);
            }
            plugin.getLogger().info("Loaded " + zones.size() + " KoTH zones");
        });
    }
    
    public void createZone(String name) {
        KothZone zone = new KothZone(name);
        zones.put(name, zone);
        saveZone(zone);
    }
    
    public void deleteZone(String name) {
        zones.remove(name);
        plugin.getDatabaseManager().deleteZone(name);
    }
    
    public KothZone getZone(String name) {
        return zones.get(name);
    }
    
    public List<KothZone> getAllZones() {
        return new ArrayList<>(zones.values());
    }
    
    public void saveZone(KothZone zone) {
        plugin.getDatabaseManager().saveZone(zone);
    }
    
    public void startKoth(String name) {
        KothZone zone = zones.get(name);
        if (zone == null) return;
        if (zone.isActive()) return;
        
        zone.setStatus(ZoneStatus.ACTIVE);
        zone.setStartTime(System.currentTimeMillis());
        
        KothSession session = new KothSession(name);
        activeSessions.put(name, session);
        
        saveZone(zone);
        
        String message = plugin.getConfig().getString("messages.koth-started")
            .replace("{name}", zone.getDisplayName());
        Bukkit.broadcastMessage(ColorUtil.color(plugin.getConfig().getString("messages.prefix") + message));

        plugin.getWebhookManager().sendEvent("start", placeholders(zone, null));
    }
    
    public void stopKoth(String name) {
        KothZone zone = zones.get(name);
        if (zone == null) return;
        if (!zone.isActive()) return;
        
        zone.setStatus(ZoneStatus.INACTIVE);
        KothSession session = activeSessions.remove(name);
        
        if (session != null) {
            UUID winner = session.getTopPlayer();
            if (winner != null) {
                Player player = Bukkit.getPlayer(winner);
                if (player != null) {
                    giveRewards(player, zone, session);
                    plugin.getWebhookManager().sendEvent("win", placeholders(zone, player));
                }
            }
        }
        
        saveZone(zone);
        
        String message = plugin.getConfig().getString("messages.koth-stopped")
            .replace("{name}", zone.getDisplayName());
        Bukkit.broadcastMessage(ColorUtil.color(plugin.getConfig().getString("messages.prefix") + message));

        plugin.getWebhookManager().sendEvent("stop", placeholders(zone, null));
    }
    
    public KothSession getSession(String name) {
        return activeSessions.get(name);
    }
    
    public Map<String, KothSession> getActiveSessions() {
        return activeSessions;
    }
    
    private void giveRewards(Player player, KothZone zone, KothSession session) {
        List<String> commands = getRewardCommands(zone);
        
        for (String command : commands) {
            String cmd = command.replace("{player}", player.getName())
                .replace("{koth}", zone.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }

        String broadcast = getRewardBroadcast(zone)
            .replace("{player}", player.getName())
            .replace("{koth}", zone.getDisplayName())
            .replace("{score}", String.valueOf(session.getScore(player.getUniqueId())));
        Bukkit.broadcastMessage(ColorUtil.color(broadcast));

        plugin.getLeaderboardManager().addWin(player.getUniqueId(), player.getName(), "daily");
    }
    
    public KothZone getZoneAt(Player player) {
        for (KothZone zone : zones.values()) {
            if (zone.isInZone(player.getLocation())) {
                return zone;
            }
        }
        return null;
    }

    public void setSelection(UUID playerId, Location location, boolean firstPos) {
        if (firstPos) {
            selectionPos1.put(playerId, location);
        } else {
            selectionPos2.put(playerId, location);
        }
    }

    public Location getSelection(UUID playerId, boolean firstPos) {
        return firstPos ? selectionPos1.get(playerId) : selectionPos2.get(playerId);
    }

    public void tickSessions() {
        List<String> toStop = new ArrayList<>();

        for (Map.Entry<String, KothSession> entry : activeSessions.entrySet()) {
            String kothName = entry.getKey();
            KothSession session = entry.getValue();
            KothZone zone = zones.get(kothName);

            if (zone == null || !zone.isActive()) {
                toStop.add(kothName);
                continue;
            }

            int elapsed = (int) ((System.currentTimeMillis() - zone.getStartTime()) / 1000L);
            if (elapsed >= zone.getDuration()) {
                toStop.add(kothName);
                continue;
            }

            if (zone.getMode() == KothMode.CAPTURE) {
                handleCaptureMode(zone, session, toStop);
            } else {
                handleScoreMode(zone, session, toStop);
            }
        }

        for (String kothName : toStop) {
            stopKoth(kothName);
        }
    }

    public int getProgressPercent(KothZone zone, KothSession session) {
        if (zone.getMode() == KothMode.CAPTURE) {
            if (zone.getCaptureTime() <= 0) return 100;
            double raw = (session.getCaptureProgress() * 100.0D) / zone.getCaptureTime();
            int pct = (int) Math.ceil(raw);
            if (pct < 0) pct = 0;
            return Math.min(100, pct);
        }

        int maxScore = Math.max(1, zone.getMaxScore());
        int top = session.getTopPlayer() == null ? 0 : session.getScore(session.getTopPlayer());
        double raw = (top * 100.0D) / maxScore;
        int pct = (int) Math.ceil(raw);
        if (pct < 0) pct = 0;
        return Math.min(100, pct);
    }

    public int getSecondsLeft(KothZone zone) {
        if (zone.getMode() == KothMode.CAPTURE) {
            KothSession session = activeSessions.get(zone.getName());
            if (session != null) {
                return Math.max(0, zone.getCaptureTime() - session.getCaptureProgress());
            }
            return zone.getCaptureTime();
        }
        int elapsed = (int) ((System.currentTimeMillis() - zone.getStartTime()) / 1000L);
        return Math.max(0, zone.getDuration() - elapsed);
    }

    private void handleCaptureMode(KothZone zone, KothSession session, List<String> toStop) {
        // if nobody capturing yet, pick first player inside
        if (session.getCurrentCapturer() == null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (zone.isInZone(p.getLocation())) {
                    session.setCurrentCapturer(p.getUniqueId());
                    session.setCaptureProgress(0);
                    p.sendMessage(ColorUtil.color(plugin.getConfig().getString("messages.prefix") +
                        plugin.getConfig().getString("messages.capture-started")
                            .replace("{player}", p.getName())
                            .replace("{koth}", zone.getDisplayName())));
                    plugin.getWebhookManager().sendEvent("capture-start", placeholders(zone, p));
                    break;
                }
            }
        }

        UUID capturerId = session.getCurrentCapturer();
        if (capturerId == null) return;

        Player capturer = Bukkit.getPlayer(capturerId);
        if (capturer == null || !capturer.isOnline() || !zone.isInZone(capturer.getLocation())) {
            session.setCurrentCapturer(null);
            session.setCaptureProgress(0);
            if (capturer != null) {
                plugin.getWebhookManager().sendEvent("capture-stop", placeholders(zone, capturer));
            }
            return;
        }

        session.setCaptureProgress(session.getCaptureProgress() + 1);
        plugin.getLeaderboardManager().addCapture(capturerId, capturer.getName(), "daily");

        if (session.getCaptureProgress() >= zone.getCaptureTime()) {
            session.addScore(capturerId, zone.getCaptureTime());
            toStop.add(zone.getName());
        }
    }

    private void handleScoreMode(KothZone zone, KothSession session, List<String> toStop) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (zone.isInZone(player.getLocation())) {
                session.addScore(player.getUniqueId(), 1);
                plugin.getLeaderboardManager().addTime(player.getUniqueId(), player.getName(), 1000L, "daily");
            }
        }

        UUID topPlayer = session.getTopPlayer();
        if (topPlayer != null && session.getScore(topPlayer) >= zone.getMaxScore()) {
            toStop.add(zone.getName());
        }
    }

    private List<String> getRewardCommands(KothZone zone) {
        String modePath = zone.getMode().name().toLowerCase() + "-mode";
        List<String> custom = plugin.getConfig().getStringList("rewards.koths." + zone.getName() + "." + modePath + ".commands");
        if (!custom.isEmpty()) return custom;
        return plugin.getConfig().getStringList("rewards." + modePath + ".commands");
    }

    private String getRewardBroadcast(KothZone zone) {
        String modePath = zone.getMode().name().toLowerCase() + "-mode";
        String custom = plugin.getConfig().getString("rewards.koths." + zone.getName() + "." + modePath + ".broadcast");
        if (custom != null && !custom.isEmpty()) return custom;
        return plugin.getConfig().getString("rewards." + modePath + ".broadcast", "&a{player} won {koth}!");
    }

    private Map<String, String> placeholders(KothZone zone, Player player) {
        Map<String, String> data = new HashMap<>();
        data.put("koth", zone.getDisplayName());
        data.put("mode", zone.getMode().name());
        data.put("player", player == null ? "Unknown" : player.getName());
        return data;
    }
}