package com.capito.bonty.managers;

import com.capito.bonty.Bonty;
import com.capito.bonty.data.KothZone;
import com.capito.bonty.data.LeaderboardEntry;
import com.capito.bonty.enums.KothMode;
import com.capito.bonty.enums.ZoneStatus;
import com.capito.bonty.utils.LocationUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {
    private final Bonty plugin;
    private HikariDataSource dataSource;
    private JedisPool jedisPool;
    private final String prefix;
    private String databaseType;

    public DatabaseManager(Bonty plugin) {
        this.plugin = plugin;
        this.prefix = plugin.getConfig().getString("database.table-prefix", "bonty_");
    }

    public void connect() {
        this.databaseType = plugin.getConfig().getString("database.type", "sqlite").toLowerCase();

        if (isRedis()) {
            String host = plugin.getConfig().getString("database.redis.host", "localhost");
            int port = plugin.getConfig().getInt("database.redis.port", 6379);
            String password = plugin.getConfig().getString("database.redis.password", "");
            int db = plugin.getConfig().getInt("database.redis.database", 0);

            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(Math.max(4, plugin.getConfig().getInt("database.pool-size", 10)));

            if (password == null || password.isEmpty()) {
                this.jedisPool = new JedisPool(config, host, port, 2000, null, db);
            } else {
                this.jedisPool = new JedisPool(config, host, port, 2000, password, db);
            }
            return;
        }

        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(plugin.getConfig().getInt("database.pool-size", 10));
        config.setConnectionTimeout(30000);

        String host = plugin.getConfig().getString("database.host", "localhost");
        int port = plugin.getConfig().getInt("database.port", 3306);
        String database = plugin.getConfig().getString("database.database", "bonty");
        String username = plugin.getConfig().getString("database.username", "root");
        String password = plugin.getConfig().getString("database.password", "");

        if (databaseType.equals("mysql")) {
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true");
            config.setUsername(username);
            config.setPassword(password);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        } else if (databaseType.equals("postgresql")) {
            config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database);
            config.setUsername(username);
            config.setPassword(password);
        } else if (databaseType.equals("h2")) {
            config.setJdbcUrl("jdbc:h2:" + plugin.getDataFolder().getAbsolutePath() + "/bonty;MODE=MySQL");
        } else {
            this.databaseType = "sqlite";
            config.setJdbcUrl("jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/bonty.db");
        }

        this.dataSource = new HikariDataSource(config);
        createTables();
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        if (jedisPool != null) {
            jedisPool.close();
        }
    }

    private void createTables() {
        if (isRedis()) return;

        String zones = "CREATE TABLE IF NOT EXISTS " + prefix + "zones (" +
            "name VARCHAR(64) PRIMARY KEY," +
            "pos1 TEXT," +
            "pos2 TEXT," +
            "spawn TEXT," +
            "mode VARCHAR(16)," +
            "capture_time INT," +
            "max_score INT," +
            "duration INT," +
            "status VARCHAR(16)," +
            "display_name VARCHAR(128)," +
            "bossbar_enabled BOOLEAN," +
            "scoreboard_enabled BOOLEAN" +
            ")";

        String leaderboard;
        if (databaseType.equals("postgresql")) {
            leaderboard = "CREATE TABLE IF NOT EXISTS " + prefix + "leaderboard (" +
                "id SERIAL PRIMARY KEY," +
                "player_uuid VARCHAR(36)," +
                "player_name VARCHAR(32)," +
                "captures INT DEFAULT 0," +
                "wins INT DEFAULT 0," +
                "total_time BIGINT DEFAULT 0," +
                "period VARCHAR(16)," +
                "timestamp BIGINT" +
                ")";
        } else if (databaseType.equals("sqlite")) {
            leaderboard = "CREATE TABLE IF NOT EXISTS " + prefix + "leaderboard (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "player_uuid VARCHAR(36)," +
                "player_name VARCHAR(32)," +
                "captures INT DEFAULT 0," +
                "wins INT DEFAULT 0," +
                "total_time BIGINT DEFAULT 0," +
                "period VARCHAR(16)," +
                "timestamp BIGINT" +
                ")";
        } else {
            leaderboard = "CREATE TABLE IF NOT EXISTS " + prefix + "leaderboard (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "player_uuid VARCHAR(36)," +
                "player_name VARCHAR(32)," +
                "captures INT DEFAULT 0," +
                "wins INT DEFAULT 0," +
                "total_time BIGINT DEFAULT 0," +
                "period VARCHAR(16)," +
                "timestamp BIGINT" +
                ")";
        }

        try (Connection conn = getConnection()) {
            conn.createStatement().execute(zones);
            conn.createStatement().execute(leaderboard);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public CompletableFuture<Void> saveZone(KothZone zone) {
        return CompletableFuture.runAsync(() -> {
            if (isRedis()) {
                try (Jedis jedis = jedisPool.getResource()) {
                    String key = prefix + "zone:" + zone.getName();
                    Map<String, String> data = new HashMap<>();
                    data.put("name", zone.getName());
                    data.put("pos1", nvl(LocationUtil.serialize(zone.getPos1())));
                    data.put("pos2", nvl(LocationUtil.serialize(zone.getPos2())));
                    data.put("spawn", nvl(LocationUtil.serialize(zone.getSpawnLocation())));
                    data.put("mode", zone.getMode().name());
                    data.put("capture_time", String.valueOf(zone.getCaptureTime()));
                    data.put("max_score", String.valueOf(zone.getMaxScore()));
                    data.put("duration", String.valueOf(zone.getDuration()));
                    data.put("status", zone.getStatus().name());
                    data.put("display_name", nvl(zone.getDisplayName()));
                    data.put("bossbar_enabled", String.valueOf(zone.isBossbarEnabled()));
                    data.put("scoreboard_enabled", String.valueOf(zone.isScoreboardEnabled()));
                    jedis.hset(key, data);
                    jedis.sadd(prefix + "zones:names", zone.getName());
                }
                return;
            }

            String deleteSql = "DELETE FROM " + prefix + "zones WHERE name = ?";
            String insertSql = "INSERT INTO " + prefix + "zones " +
                "(name, pos1, pos2, spawn, mode, capture_time, max_score, duration, status, display_name, bossbar_enabled, scoreboard_enabled) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (Connection conn = getConnection()) {
                try (PreparedStatement delete = conn.prepareStatement(deleteSql)) {
                    delete.setString(1, zone.getName());
                    delete.executeUpdate();
                }
                try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                    stmt.setString(1, zone.getName());
                    stmt.setString(2, LocationUtil.serialize(zone.getPos1()));
                    stmt.setString(3, LocationUtil.serialize(zone.getPos2()));
                    stmt.setString(4, LocationUtil.serialize(zone.getSpawnLocation()));
                    stmt.setString(5, zone.getMode().name());
                    stmt.setInt(6, zone.getCaptureTime());
                    stmt.setInt(7, zone.getMaxScore());
                    stmt.setInt(8, zone.getDuration());
                    stmt.setString(9, zone.getStatus().name());
                    stmt.setString(10, zone.getDisplayName());
                    stmt.setBoolean(11, zone.isBossbarEnabled());
                    stmt.setBoolean(12, zone.isScoreboardEnabled());
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<KothZone> loadZone(String name) {
        return CompletableFuture.supplyAsync(() -> {
            if (isRedis()) {
                try (Jedis jedis = jedisPool.getResource()) {
                    String key = prefix + "zone:" + name;
                    Map<String, String> data = jedis.hgetAll(key);
                    if (data == null || data.isEmpty()) return null;
                    return mapRedisZone(data);
                }
            }

            String sql = "SELECT * FROM " + prefix + "zones WHERE name = ?";

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, name);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return mapSqlZone(rs);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    public CompletableFuture<List<KothZone>> loadAllZones() {
        return CompletableFuture.supplyAsync(() -> {
            List<KothZone> zones = new ArrayList<>();

            if (isRedis()) {
                try (Jedis jedis = jedisPool.getResource()) {
                    Set<String> names = jedis.smembers(prefix + "zones:names");
                    for (String name : names) {
                        Map<String, String> data = jedis.hgetAll(prefix + "zone:" + name);
                        if (data != null && !data.isEmpty()) {
                            zones.add(mapRedisZone(data));
                        }
                    }
                }
                return zones;
            }

            String sql = "SELECT * FROM " + prefix + "zones";

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    zones.add(mapSqlZone(rs));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return zones;
        });
    }

    public CompletableFuture<Void> deleteZone(String name) {
        return CompletableFuture.runAsync(() -> {
            if (isRedis()) {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.del(prefix + "zone:" + name);
                    jedis.srem(prefix + "zones:names", name);
                }
                return;
            }

            String sql = "DELETE FROM " + prefix + "zones WHERE name = ?";

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, name);
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Void> saveLeaderboardEntry(LeaderboardEntry entry) {
        return CompletableFuture.runAsync(() -> {
            if (isRedis()) {
                try (Jedis jedis = jedisPool.getResource()) {
                    String key = prefix + "lb:" + entry.getPeriod() + ":" + entry.getPlayerId();
                    jedis.sadd(prefix + "lb:" + entry.getPeriod() + ":players", entry.getPlayerId().toString());
                    jedis.hset(key, "player_uuid", entry.getPlayerId().toString());
                    jedis.hset(key, "player_name", nvl(entry.getPlayerName()));
                    jedis.hincrBy(key, "captures", entry.getCaptures());
                    jedis.hincrBy(key, "wins", entry.getWins());
                    jedis.hincrBy(key, "total_time", entry.getTotalTime());
                }
                return;
            }

            String sql = "INSERT INTO " + prefix + "leaderboard " +
                "(player_uuid, player_name, captures, wins, total_time, period, timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, entry.getPlayerId().toString());
                stmt.setString(2, entry.getPlayerName());
                stmt.setInt(3, entry.getCaptures());
                stmt.setInt(4, entry.getWins());
                stmt.setLong(5, entry.getTotalTime());
                stmt.setString(6, entry.getPeriod());
                stmt.setLong(7, System.currentTimeMillis());
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<List<LeaderboardEntry>> getTopPlayers(String period, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<LeaderboardEntry> entries = new ArrayList<>();

            if (isRedis()) {
                try (Jedis jedis = jedisPool.getResource()) {
                    Set<String> players = jedis.smembers(prefix + "lb:" + period + ":players");
                    for (String playerId : players) {
                        Map<String, String> data = jedis.hgetAll(prefix + "lb:" + period + ":" + playerId);
                        if (data == null || data.isEmpty()) continue;
                        LeaderboardEntry entry = new LeaderboardEntry();
                        entry.setPlayerId(UUID.fromString(data.getOrDefault("player_uuid", playerId)));
                        entry.setPlayerName(data.getOrDefault("player_name", "Unknown"));
                        entry.setCaptures(parseInt(data.get("captures")));
                        entry.setWins(parseInt(data.get("wins")));
                        entry.setTotalTime(parseLong(data.get("total_time")));
                        entry.setPeriod(period);
                        entries.add(entry);
                    }
                    entries.sort(Comparator.comparingInt(LeaderboardEntry::getWins).reversed());
                    if (entries.size() > limit) {
                        return new ArrayList<>(entries.subList(0, limit));
                    }
                    return entries;
                }
            }

            String sql = "SELECT player_uuid, player_name, SUM(captures) as total_captures, " +
                "SUM(wins) as total_wins, SUM(total_time) as total_time " +
                "FROM " + prefix + "leaderboard WHERE period = ? " +
                "GROUP BY player_uuid, player_name ORDER BY total_wins DESC LIMIT ?";

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, period);
                stmt.setInt(2, limit);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    LeaderboardEntry entry = new LeaderboardEntry();
                    entry.setPlayerId(UUID.fromString(rs.getString("player_uuid")));
                    entry.setPlayerName(rs.getString("player_name"));
                    entry.setCaptures(rs.getInt("total_captures"));
                    entry.setWins(rs.getInt("total_wins"));
                    entry.setTotalTime(rs.getLong("total_time"));
                    entry.setPeriod(period);
                    entries.add(entry);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return entries;
        });
    }

    private boolean isRedis() {
        return "redis".equalsIgnoreCase(databaseType);
    }

    private KothZone mapSqlZone(ResultSet rs) throws SQLException {
        KothZone zone = new KothZone(rs.getString("name"));
        zone.setPos1(LocationUtil.deserialize(rs.getString("pos1")));
        zone.setPos2(LocationUtil.deserialize(rs.getString("pos2")));
        zone.setSpawnLocation(LocationUtil.deserialize(rs.getString("spawn")));
        zone.setMode(KothMode.valueOf(rs.getString("mode")));
        zone.setCaptureTime(rs.getInt("capture_time"));
        zone.setMaxScore(rs.getInt("max_score"));
        zone.setDuration(rs.getInt("duration"));
        zone.setStatus(ZoneStatus.valueOf(rs.getString("status")));
        zone.setDisplayName(rs.getString("display_name"));
        zone.setBossbarEnabled(rs.getBoolean("bossbar_enabled"));
        zone.setScoreboardEnabled(rs.getBoolean("scoreboard_enabled"));
        return zone;
    }

    private KothZone mapRedisZone(Map<String, String> data) {
        KothZone zone = new KothZone(data.getOrDefault("name", "unknown"));
        zone.setPos1(LocationUtil.deserialize(emptyToNull(data.get("pos1"))));
        zone.setPos2(LocationUtil.deserialize(emptyToNull(data.get("pos2"))));
        zone.setSpawnLocation(LocationUtil.deserialize(emptyToNull(data.get("spawn"))));
        zone.setMode(KothMode.valueOf(data.getOrDefault("mode", "CAPTURE")));
        zone.setCaptureTime(parseInt(data.get("capture_time"), 300));
        zone.setMaxScore(parseInt(data.get("max_score"), 100));
        zone.setDuration(parseInt(data.get("duration"), 600));
        zone.setStatus(ZoneStatus.valueOf(data.getOrDefault("status", "INACTIVE")));
        zone.setDisplayName(data.getOrDefault("display_name", zone.getName()));
        zone.setBossbarEnabled(Boolean.parseBoolean(data.getOrDefault("bossbar_enabled", "true")));
        zone.setScoreboardEnabled(Boolean.parseBoolean(data.getOrDefault("scoreboard_enabled", "true")));
        return zone;
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }

    private String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private int parseInt(String value) {
        return parseInt(value, 0);
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return 0L;
        }
    }
}
