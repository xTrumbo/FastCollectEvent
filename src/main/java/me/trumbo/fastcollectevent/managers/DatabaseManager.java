package me.trumbo.fastcollectevent.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.trumbo.fastcollectevent.FastCollectEvent;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private FastCollectEvent main;
    private HikariDataSource dataSource;

    private boolean enabled;

    public DatabaseManager(FastCollectEvent main) {
        this.main = main;
        this.enabled = main.getConfigManager().getFromConfig("config", "database", "enabled");

        if (enabled) {
            setupDatabase();
            createPlayerStatsTable();
        }
    }

    private void setupDatabase() {

        HikariConfig hikariConfig = new HikariConfig();
        String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC",
                main.getConfigManager().getFromConfig("config", "database", "host"),
                main.getConfigManager().getFromConfig("config", "database", "port"),
                main.getConfigManager().getFromConfig("config", "database", "database"));

        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(main.getConfigManager().getFromConfig("config", "database", "username"));
        hikariConfig.setPassword(main.getConfigManager().getFromConfig("config", "database", "password"));
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");

        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");

        try {
            dataSource = new HikariDataSource(hikariConfig);
            main.getLogger().info("Успешное подключение к базе данных!");
        } catch (Exception e) {
            main.getLogger().severe("Ошибка подключения к базе данных: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void createPlayerStatsTable() {
        String sql = "CREATE TABLE IF NOT EXISTS player_stats (" +
                "player_name VARCHAR(16) PRIMARY KEY," +
                "total_collected INT NOT NULL DEFAULT 0" +
                ")";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException ignored) {
        }
    }

    public void addOrUpdatePlayer(String playerName, int collectedAmount) {
        main.getServer().getScheduler().runTaskAsynchronously(main, () -> {
            String sql = "INSERT INTO player_stats (player_name, total_collected) " +
                    "VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE total_collected = total_collected + ?";
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerName);
                stmt.setInt(2, collectedAmount);
                stmt.setInt(3, collectedAmount);
                stmt.executeUpdate();
            } catch (SQLException ignored) {
            }
        });
    }

    public CompletableFuture<List[]> getTopPlayers(int limit) {
        if (!enabled) {
            return CompletableFuture.completedFuture(new List[]{new ArrayList<>(), new ArrayList<>()});
        }

        return CompletableFuture.supplyAsync(() -> {
            List<String> names = new ArrayList<>();
            List<Integer> scores = new ArrayList<>();
            String sql = "SELECT player_name, total_collected FROM player_stats ORDER BY total_collected DESC LIMIT ?";
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, limit);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    names.add(rs.getString("player_name"));
                    scores.add(rs.getInt("total_collected"));
                }
            } catch (SQLException ignored) {
            }
            return new List[]{names, scores};
        }, task -> main.getServer().getScheduler().runTaskAsynchronously(main, task));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
