package jcn.jwl;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class JWhitelistDatabase {
    private final JWhitelist plugin;
    private Connection connection;

    public JWhitelistDatabase(JWhitelist plugin) {
        this.plugin = plugin;
    }

    public void setupDatabase() {
        try {
            String dbType = plugin.getConfig().getString("database.type", "sqlite");
            if ("mysql".equalsIgnoreCase(dbType)) {
                openMySQLConnection();
            } else {
                openSQLiteConnection();
            }
            createTables();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not setup database", e);
        }
    }

    private void openMySQLConnection() throws SQLException {
        String dbHost = plugin.getConfig().getString("database.host", "localhost");
        String dbPort = plugin.getConfig().getString("database.port", "3306");
        String dbName = plugin.getConfig().getString("database.name", "whitelist");
        String dbUser = plugin.getConfig().getString("database.user", "root");
        String dbPassword = plugin.getConfig().getString("database.password", "");
        String url = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName;
        connection = DriverManager.getConnection(url, dbUser, dbPassword);
    }

    private void openSQLiteConnection() throws SQLException {
        String dbName = plugin.getConfig().getString("database.name", "whitelist");
        String url = "jdbc:sqlite:" + plugin.getDataFolder() + "/" + dbName + ".db";
        connection = DriverManager.getConnection(url);
    }

    private void createTables() throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS whitelist (" +
                "name VARCHAR(100) PRIMARY KEY," +
                "expiry BIGINT," +
                "last_login BIGINT)";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
        }
    }

    public void closeDatabase() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not close database connection", e);
            }
        }
    }

    public void addPlayerToWhitelist(String playerName, long expiry) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "REPLACE INTO whitelist (name, expiry, last_login) VALUES (?, ?, ?)")) {
            stmt.setString(1, playerName);
            stmt.setLong(2, expiry == -1 ? -1 : System.currentTimeMillis() + expiry);
            stmt.setLong(3, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not add player to whitelist", e);
        }
    }

    public void removePlayerFromWhitelist(String playerName) {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM whitelist WHERE name = ?")) {
            stmt.setString(1, playerName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not remove player from whitelist", e);
        }
        LuckPermsApi.removePermission(playerName);
    }

    public boolean isPlayerWhitelisted(String playerName) {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT expiry FROM whitelist WHERE name = ?")) {
            stmt.setString(1, playerName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long expiry = rs.getLong("expiry");
                    return expiry == -1 || expiry > System.currentTimeMillis();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Database error during whitelist check", e);
        }
        return false;
    }

    public void updateLastLogin(String playerName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE whitelist SET last_login = ? WHERE name = ?")) {
            stmt.setLong(1, System.currentTimeMillis());
            stmt.setString(2, playerName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not update last login for player", e);
        }
    }

    public void cleanupWhitelist() {
        long currentTime = System.currentTimeMillis();
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM whitelist WHERE expiry != -1 AND expiry <= ?")) {
            stmt.setLong(1, currentTime);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not perform whitelist cleanup", e);
        }
    }

    public List<String> getPlayersToRemove(long clearDuration) {
        List<String> playersToRemove = new ArrayList<>();
        long cutoffTime = System.currentTimeMillis() - clearDuration;
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT name FROM whitelist WHERE last_login < ?")) {
            stmt.setLong(1, cutoffTime);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    playersToRemove.add(rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not get players to remove from whitelist", e);
        }
        return playersToRemove;
    }

    public Map<String, Long> getAllWhitelistedPlayers() {
        Map<String, Long> whitelistedPlayers = new HashMap<>();
        long currentTime = System.currentTimeMillis();

        String query = "SELECT name, expiry FROM whitelist";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String playerName = rs.getString("name");
                long expiry = rs.getLong("expiry");

                if (expiry == -1) {
                    whitelistedPlayers.put(playerName, -1L);
                } else {
                    long remainingTime = expiry - currentTime;
                    if (remainingTime > 0) {
                        whitelistedPlayers.put(playerName, remainingTime);
                    } else {
                        whitelistedPlayers.put(playerName, -2L);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not retrieve whitelisted players", e);
        }
        return whitelistedPlayers;
    }

    public List<String> getAllPlayerNameInWhitelist() {
        List<String> whitelistedPlayersName = new ArrayList<>();
        String query = "SELECT name FROM whitelist";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                whitelistedPlayersName.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not retrieve all player names from whitelist", e);
        }
        return whitelistedPlayersName;
    }
}
