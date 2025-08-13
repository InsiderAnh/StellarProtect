package io.github.insideranh.stellarprotect.database.types.mysql;

import com.zaxxer.hikari.HikariDataSource;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.database.repositories.PlayerRepository;
import io.github.insideranh.stellarprotect.managers.ConfigManager;
import lombok.SneakyThrows;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

public class PlayerRepositoryMySQL implements PlayerRepository {

    private final StellarProtect stellarProtect = StellarProtect.getInstance();
    private final ConfigManager configManager = stellarProtect.getConfigManager();
    private final HikariDataSource dataSource;

    public PlayerRepositoryMySQL(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public PlayerProtect loadOrCreatePlayer(Player player) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement select = connection.prepareStatement(
                "SELECT id FROM " + configManager.getTablesPlayers() + " WHERE uuid = ?")) {

                select.setString(1, player.getUniqueId().toString());

                try (ResultSet result = select.executeQuery()) {
                    if (result.next()) {
                        long id = result.getLong("id");
                        return new PlayerProtect(player.getUniqueId(), player.getName(), id);
                    }
                }
            }

            long newId = generateNextId();

            try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO " + configManager.getTablesPlayers() + " (id, uuid, name, realname) VALUES (?, ?, ?, ?)")) {

                insert.setLong(1, newId);
                insert.setString(2, player.getUniqueId().toString());
                insert.setString(3, player.getName().toLowerCase());
                insert.setString(4, player.getName());
                insert.executeUpdate();
            }

            return new PlayerProtect(player.getUniqueId(), player.getName(), newId);

        } catch (SQLException e) {
            stellarProtect.getLogger().log(Level.SEVERE, "Error in loadOrCreatePlayer", e);
            return null;
        }
    }

    @Override
    public List<Long> getIdsByNames(List<String> names) {
        if (names.isEmpty()) return new ArrayList<>();

        StringBuilder query = new StringBuilder("SELECT id, name FROM " + configManager.getTablesPlayers() + " WHERE name IN (");
        for (int i = 0; i < names.size(); i++) {
            query.append("?");
            if (i != names.size() - 1) query.append(", ");
        }
        query.append(")");

        HashMap<String, Long> foundPlayers = new HashMap<>();
        try (Connection connection = getConnection();
             PreparedStatement select = connection.prepareStatement(
                 query.toString())) {

            for (int i = 0; i < names.size(); i++) {
                select.setString(i + 1, names.get(i));
            }

            try (ResultSet result = select.executeQuery()) {
                List<Long> ids = new LinkedList<>();
                while (result.next()) {
                    long id = result.getLong("id");
                    String name = result.getString("name");

                    ids.add(id);
                    foundPlayers.put(name.toLowerCase(), id);
                }
                for (String name : names) {
                    if (!foundPlayers.containsKey(name.toLowerCase())) {
                        ids.add(-2L);
                    }
                }
                return ids;
            }
        } catch (SQLException e) {
            stellarProtect.getLogger().log(Level.SEVERE, "Error in getIdsByNames", e);
            return new ArrayList<>();
        }
    }

    @Override
    public long generateNextId() {
        try (Connection connection = getConnection();
             PreparedStatement getCurrentId = connection.prepareStatement(
                 "SELECT current_id FROM " + configManager.getTablesIdCounter() + " WHERE table_name = '" + configManager.getTablesPlayers() + "'");
             ResultSet result = getCurrentId.executeQuery()) {

            if (result.next()) {
                long currentId = result.getLong("current_id");
                long newId = currentId + 1;

                try (PreparedStatement updateId = connection.prepareStatement(
                    "UPDATE " + configManager.getTablesIdCounter() + " SET current_id = ? WHERE table_name = '" + configManager.getTablesPlayers() + "'")) {
                    updateId.setLong(1, newId);
                    updateId.executeUpdate();
                }

                return newId;
            } else {
                try (PreparedStatement initId = connection.prepareStatement(
                    "INSERT INTO " + configManager.getTablesIdCounter() + " (table_name, current_id) VALUES ('" + configManager.getTablesPlayers() + "', 1)")) {
                    initId.executeUpdate();
                }
                return 1;
            }
        } catch (SQLException e) {
            stellarProtect.getLogger().log(Level.SEVERE, "Error in generateNextId", e);
            return 0;
        }
    }

    @SneakyThrows
    public Connection getConnection() {
        return dataSource.getConnection();
    }

}