package com.capito.bonty.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KothSession {
    private String kothName;
    private long startTime;
    private Map<UUID, Integer> playerScores;
    private UUID currentCapturer;
    private int captureProgress;
    private boolean active;
    
    public KothSession(String kothName) {
        this.kothName = kothName;
        this.startTime = System.currentTimeMillis();
        this.playerScores = new HashMap<>();
        this.active = true;
        this.captureProgress = 0;
    }
    
    public void addScore(UUID player, int amount) {
        playerScores.put(player, playerScores.getOrDefault(player, 0) + amount);
    }
    
    public int getScore(UUID player) {
        return playerScores.getOrDefault(player, 0);
    }
    
    public UUID getTopPlayer() {
        return playerScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
}