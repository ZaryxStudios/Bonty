package com.capito.bonty.managers;

import com.capito.bonty.Bonty;
import com.capito.bonty.data.LeaderboardEntry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LeaderboardManager {
    private final Bonty plugin;
    private final Map<String, List<LeaderboardEntry>> cachedLeaderboards;
    
    public LeaderboardManager(Bonty plugin) {
        this.plugin = plugin;
        this.cachedLeaderboards = new ConcurrentHashMap<>();
    }
    
    public void updateLeaderboards() {
        if (!plugin.getConfig().getBoolean("features.leaderboards.enabled")) return;
        
        List<String> types = plugin.getConfig().getStringList("features.leaderboards.types");
        for (String type : types) {
            plugin.getDatabaseManager().getTopPlayers(type, 100).thenAccept(entries -> {
                cachedLeaderboards.put(type, entries);
            });
        }
    }
    
    public List<LeaderboardEntry> getTopPlayers(String period, int limit) {
        List<LeaderboardEntry> entries = cachedLeaderboards.getOrDefault(period, new ArrayList<>());
        return entries.subList(0, Math.min(limit, entries.size()));
    }
    
    public void addWin(UUID playerId, String playerName, String period) {
        LeaderboardEntry entry = new LeaderboardEntry(playerId, playerName);
        entry.setWins(1);
        entry.setPeriod(period);
        plugin.getDatabaseManager().saveLeaderboardEntry(entry);
    }
    
    public void addCapture(UUID playerId, String playerName, String period) {
        LeaderboardEntry entry = new LeaderboardEntry(playerId, playerName);
        entry.setCaptures(1);
        entry.setPeriod(period);
        plugin.getDatabaseManager().saveLeaderboardEntry(entry);
    }
    
    public void addTime(UUID playerId, String playerName, long time, String period) {
        LeaderboardEntry entry = new LeaderboardEntry(playerId, playerName);
        entry.setTotalTime(time);
        entry.setPeriod(period);
        plugin.getDatabaseManager().saveLeaderboardEntry(entry);
    }
}