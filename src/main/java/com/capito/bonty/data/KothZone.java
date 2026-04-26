package com.capito.bonty.data;

import com.capito.bonty.enums.KothMode;
import com.capito.bonty.enums.ZoneStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KothZone {
    private String name;
    private Location pos1;
    private Location pos2;
    private Location spawnLocation;
    private KothMode mode;
    private int captureTime;
    private int maxScore;
    private int duration;
    private ZoneStatus status;
    private String currentCapturer;
    private long startTime;
    private List<String> rewards;
    private boolean bossbarEnabled;
    private boolean scoreboardEnabled;
    private String displayName;
    
    public KothZone(String name) {
        this.name = name;
        this.mode = KothMode.CAPTURE;
        this.captureTime = 300;
        this.maxScore = 100;
        this.duration = 600;
        this.status = ZoneStatus.INACTIVE;
        this.rewards = new ArrayList<>();
        this.bossbarEnabled = true;
        this.scoreboardEnabled = true;
        this.displayName = name;
    }
    
    public boolean isActive() {
        return status == ZoneStatus.ACTIVE;
    }
    
    public boolean isInZone(Location location) {
        if (pos1 == null || pos2 == null) return false;
        if (pos1.getWorld() == null || !location.getWorld().equals(pos1.getWorld())) return false;

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        return location.getBlockX() >= minX && location.getBlockX() <= maxX &&
               location.getBlockY() >= minY && location.getBlockY() <= maxY &&
               location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ;
    }
}