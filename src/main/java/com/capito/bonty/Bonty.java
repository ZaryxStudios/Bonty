package com.capito.bonty;

import com.capito.bonty.commands.BontyCommand;
import com.capito.bonty.commands.KothCommand;
import com.capito.bonty.inventory.gui.GUIListener;
import com.capito.bonty.inventory.gui.GUIManager;
import com.capito.bonty.listeners.KothListener;
import com.capito.bonty.managers.DatabaseManager;
import com.capito.bonty.managers.KothDisplayManager;
import com.capito.bonty.managers.KothManager;
import com.capito.bonty.managers.LeaderboardManager;
import com.capito.bonty.managers.WebhookManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;

@Getter
public class Bonty extends JavaPlugin {
    private DatabaseManager databaseManager;
    private KothManager kothManager;
    private KothDisplayManager kothDisplayManager;
    private LeaderboardManager leaderboardManager;
    private WebhookManager webhookManager;
    private GUIManager guiManager;
    
    @Override
    public void onEnable() {
        saveDefaultConfig();

        databaseManager = new DatabaseManager(this);
        databaseManager.connect();

        // initialize bStats if available
        try {
            Class.forName("org.bstats.bukkit.Metrics");
            Metrics metrics = new Metrics(this, 4);
        } catch (ClassNotFoundException cnf) {
            getLogger().warning("bStats not found on server, skipping metrics");
        }

        kothManager = new KothManager(this);
        kothDisplayManager = new KothDisplayManager(this);
        leaderboardManager = new LeaderboardManager(this);
        webhookManager = new WebhookManager(this);
        guiManager = new GUIManager();

        kothManager.loadAllZones();

        registerCommands();
        registerListeners();

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (getConfig().getBoolean("features.leaderboards.enabled")) {
                leaderboardManager.updateLeaderboards();
            }
        }, 0L, getConfig().getInt("features.leaderboards.update-interval", 300) * 20L);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            kothManager.tickSessions();
        }, 20L, 20L);

        long scoreboardInterval = Math.max(20L, getConfig().getLong("features.scoreboard.update-interval", 20L));
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            kothDisplayManager.updateDisplays();
        }, 20L, scoreboardInterval);

        getLogger().info(" ");
        getLogger().info("╔═══════════════════════════════════════════════════════════╗");
        getLogger().info("║  Bonty | Started successfully.                            ║");
        getLogger().info("║  discord.zaryxstudios.dev                                 ║");
        getLogger().info("║  KoTH Plugin Free                                         ║");
        getLogger().info("╚═══════════════════════════════════════════════════════════╝");
        getLogger().info(" ");
    }
    
    @Override
    public void onDisable() {
        // if enable never completed, some managers may be null
        if (kothManager != null) {
            for (String name : kothManager.getActiveSessions().keySet()) {
                kothManager.stopKoth(name);
            }
        }
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        if (kothDisplayManager != null) {
            kothDisplayManager.cleanup();
        }

        getLogger().info("Bonty has been disabled!");
    }
    
    private void registerCommands() {
        getCommand("bonty").setExecutor(new BontyCommand(this));
        
        KothCommand kothCommand = new KothCommand(this);
        getCommand("koth").setExecutor(kothCommand);
        getCommand("koth").setTabCompleter(kothCommand);
    }
    
    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new KothListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GUIListener(guiManager), this);
    }
    
    public void reload() {
        reloadConfig();
        kothManager.loadAllZones();
        leaderboardManager.updateLeaderboards();
        kothDisplayManager.updateDisplays();
    }
}