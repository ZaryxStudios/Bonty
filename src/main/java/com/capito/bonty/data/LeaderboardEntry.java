package com.capito.bonty.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntry {
    private UUID playerId;
    private String playerName;
    private int captures;
    private int wins;
    private long totalTime;
    private String period;
    
    public LeaderboardEntry(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.captures = 0;
        this.wins = 0;
        this.totalTime = 0;
    }
}