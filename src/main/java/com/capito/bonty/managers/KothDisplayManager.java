package com.capito.bonty.managers;

import com.capito.bonty.Bonty;
import com.capito.bonty.data.KothSession;
import com.capito.bonty.data.KothZone;
import com.capito.bonty.enums.KothMode;
import com.capito.bonty.utils.ColorUtil;
import com.capito.bonty.utils.TimeUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KothDisplayManager {
    private final Bonty plugin;
    private final Map<String, BossBar> bossBars;
    private final Map<UUID, Scoreboard> scoreboards;

    public KothDisplayManager(Bonty plugin) {
        this.plugin = plugin;
        this.bossBars = new HashMap<>();
        this.scoreboards = new HashMap<>();
    }

    public void updateDisplays() {
        for (KothZone zone : plugin.getKothManager().getAllZones()) {
            if (!zone.isActive()) {
                removeBossBar(zone.getName());
                continue;
            }

            KothSession session = plugin.getKothManager().getSession(zone.getName());
            if (session == null) continue;

            List<Player> viewers = getViewers(zone);
            updateBossbar(zone, session, viewers);
            updateScoreboard(zone, session, viewers);
            updateActionbar(zone, session, viewers);
        }

        cleanupOrphanedScoreboards();
    }

    public void cleanup() {
        for (BossBar bar : bossBars.values()) {
            bar.removeAll();
        }
        bossBars.clear();

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        scoreboards.clear();
    }

    private List<Player> getViewers(KothZone zone) {
        List<Player> viewers = new ArrayList<>();
        Location center = centerOf(zone);

        for (Player player : Bukkit.getOnlinePlayers()) {
            // Only filter by world if pos1 has a valid world reference
            if (zone.getPos1() != null && zone.getPos1().getWorld() != null
                    && !player.getWorld().equals(zone.getPos1().getWorld())) continue;

            int distanceLimit = plugin.getConfig().getInt("limitations.bossbar-distance", -1);
            if (distanceLimit >= 0 && center != null && center.getWorld() != null
                    && center.getWorld().equals(player.getWorld())) {
                if (center.distanceSquared(player.getLocation()) > (distanceLimit * distanceLimit)) {
                    continue;
                }
            }
            viewers.add(player);
        }

        return viewers;
    }

    private void updateBossbar(KothZone zone, KothSession session, List<Player> viewers) {
        boolean enabled = plugin.getConfig().getBoolean("features.bossbar.enabled", true) && zone.isBossbarEnabled();
        if (!enabled) {
            removeBossBar(zone.getName());
            return;
        }

        BossBar bar = bossBars.computeIfAbsent(zone.getName(), key -> Bukkit.createBossBar("", parseBarColor(), parseBarStyle()));
        bar.setColor(parseBarColor());
        bar.setStyle(parseBarStyle());
        bar.setTitle(ColorUtil.color(format(plugin.getConfig().getString("features.bossbar.title", "&e{koth}"), zone, session, null)));

        double progress = Math.max(0.0D, Math.min(1.0D, plugin.getKothManager().getProgressPercent(zone, session) / 100.0D));
        bar.setProgress(progress);

        for (Player player : Bukkit.getOnlinePlayers()) {
            bar.removePlayer(player);
        }
        for (Player player : viewers) {
            bar.addPlayer(player);
        }
    }

    // 16 unique invisible color-code strings used as scoreboard entry keys
    private static final String[] COLOR_ENTRIES = {
        "\u00a70","\u00a71","\u00a72","\u00a73","\u00a74","\u00a75","\u00a76","\u00a77",
        "\u00a78","\u00a79","\u00a7a","\u00a7b","\u00a7c","\u00a7d","\u00a7e","\u00a7f"
    };

    private void updateScoreboard(KothZone zone, KothSession session, List<Player> viewers) {
        boolean enabled = plugin.getConfig().getBoolean("features.scoreboard.enabled", true) && zone.isScoreboardEnabled();
        if (!enabled) {
            for (Player player : viewers) {
                removeScoreboard(player);
            }
            return;
        }

        List<String> lines = plugin.getConfig().getStringList("features.scoreboard.lines");
        String titleRaw = plugin.getConfig().getString("features.scoreboard.title", "&6&lKoTH");

        for (Player player : viewers) {
            Scoreboard scoreboard = scoreboards.get(player.getUniqueId());
            boolean isNew = scoreboard == null;
            if (isNew) {
                scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
                scoreboards.put(player.getUniqueId(), scoreboard);
            }

            Objective objective = scoreboard.getObjective("bonty");
            if (objective == null) {
                objective = scoreboard.registerNewObjective("bonty", "dummy",
                    ColorUtil.color(format(titleRaw, zone, session, player)));
                objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            } else {
                objective.setDisplayName(ColorUtil.color(format(titleRaw, zone, session, player)));
            }

            // Ensure teams exist (created once per scoreboard instance)
            if (isNew) {
                for (int i = 0; i < COLOR_ENTRIES.length; i++) {
                    Team t = scoreboard.registerNewTeam("bonty_" + i);
                    t.addEntry(COLOR_ENTRIES[i]);
                }
            }

            // Clear previous scores safely (copy to avoid ConcurrentModificationException)
            for (String entry : new ArrayList<>(scoreboard.getEntries())) {
                scoreboard.resetScores(entry);
            }

            int score = lines.size();
            for (int i = 0; i < lines.size() && i < COLOR_ENTRIES.length; i++) {
                String line = ColorUtil.color(format(lines.get(i), zone, session, player));
                Team team = scoreboard.getTeam("bonty_" + i);
                if (team != null) {
                    team.setPrefix(line);
                    team.setSuffix("");
                }
                objective.getScore(COLOR_ENTRIES[i]).setScore(score - i);
            }

            player.setScoreboard(scoreboard);
        }
    }

    private void updateActionbar(KothZone zone, KothSession session, List<Player> viewers) {
        if (!plugin.getConfig().getBoolean("features.actionbar.enabled", true)) return;

        String text = plugin.getConfig().getString("features.actionbar.text", "&e{koth} &7- &f{progress}%");
        for (Player player : viewers) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(ColorUtil.color(format(text, zone, session, player))));
        }
    }

    private String format(String text, KothZone zone, KothSession session, Player player) {
        String capturerName = "None";
        if (session.getCurrentCapturer() != null) {
            Player capturer = Bukkit.getPlayer(session.getCurrentCapturer());
            capturerName = capturer != null ? capturer.getName() : session.getCurrentCapturer().toString();
        }

        int percent = plugin.getKothManager().getProgressPercent(zone, session);
        int secondsLeft = plugin.getKothManager().getSecondsLeft(zone);

        String result = text == null ? "" : text;
        result = result.replace("{koth}", zone.getDisplayName());
        result = result.replace("{mode}", zone.getMode().name());
        result = result.replace("{capturer}", capturerName == null ? "None" : capitalize(capturerName));
        result = result.replace("{progress}", String.valueOf(percent));
        result = result.replace("{time_left}", TimeUtil.formatTime(secondsLeft));

        if (player != null) {
            int playerScore = session.getScore(player.getUniqueId());
            result = result.replace("{player}", player.getName());
            result = result.replace("{score}", String.valueOf(playerScore));
        }

        return result;
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0,1).toUpperCase() + text.substring(1).toLowerCase();
    }

    private void removeBossBar(String zoneName) {
        BossBar bar = bossBars.remove(zoneName);
        if (bar != null) {
            bar.removeAll();
        }
    }

    private void removeScoreboard(Player player) {
        if (!scoreboards.containsKey(player.getUniqueId())) return;
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        scoreboards.remove(player.getUniqueId());
    }

    private void cleanupOrphanedScoreboards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean seeingAny = false;
            for (KothZone zone : plugin.getKothManager().getAllZones()) {
                if (!zone.isActive()) continue;
                // If pos1/world is null, the zone is considered global — keep scoreboard
                if (zone.getPos1() == null || zone.getPos1().getWorld() == null) {
                    seeingAny = true;
                    break;
                }
                if (player.getWorld().equals(zone.getPos1().getWorld())) {
                    seeingAny = true;
                    break;
                }
            }
            if (!seeingAny) {
                removeScoreboard(player);
            }
        }
    }

    private Location centerOf(KothZone zone) {
        if (zone.getPos1() == null || zone.getPos2() == null) return null;
        if (!zone.getPos1().getWorld().equals(zone.getPos2().getWorld())) return null;

        double x = (zone.getPos1().getX() + zone.getPos2().getX()) / 2.0D;
        double y = (zone.getPos1().getY() + zone.getPos2().getY()) / 2.0D;
        double z = (zone.getPos1().getZ() + zone.getPos2().getZ()) / 2.0D;
        return new Location(zone.getPos1().getWorld(), x, y, z);
    }

    private BarColor parseBarColor() {
        String value = plugin.getConfig().getString("features.bossbar.color", "PURPLE");
        try {
            return BarColor.valueOf(value.toUpperCase());
        } catch (Exception ignored) {
            return BarColor.PURPLE;
        }
    }

    private BarStyle parseBarStyle() {
        String value = plugin.getConfig().getString("features.bossbar.style", "SOLID");
        try {
            return BarStyle.valueOf(value.toUpperCase());
        } catch (Exception ignored) {
            return BarStyle.SOLID;
        }
    }
}
