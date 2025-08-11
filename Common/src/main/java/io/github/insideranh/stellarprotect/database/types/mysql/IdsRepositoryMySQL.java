package io.github.insideranh.stellarprotect.database.types.mysql;

import com.zaxxer.hikari.HikariDataSource;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.database.repositories.IdsRepository;
import io.github.insideranh.stellarprotect.utils.Debugger;
import io.github.insideranh.stellarprotect.utils.PlayerUtils;
import io.github.insideranh.stellarprotect.utils.WorldUtils;
import lombok.SneakyThrows;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class IdsRepositoryMySQL implements IdsRepository {

    private final StellarProtect stellarProtect = StellarProtect.getInstance();
    private final HikariDataSource dataSource;

    public IdsRepositoryMySQL(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void loadWorlds() {
        try (Connection connection = getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM " + stellarProtect.getConfigManager().getTablesWorlds()
            )) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String world = rs.getString("name");
                    int id = rs.getInt("id");

                    WorldUtils.cacheWorld(world, id);
                    Debugger.debugExtras("Loaded world " + world + " with id " + id);
                }
            }
        } catch (SQLException e) {
            stellarProtect.getLogger().warning("Error on loadWorlds: " + e.getMessage());
        }
    }

    @Override
    public void loadEntityIds() {
        try (Connection connection = getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM " + stellarProtect.getConfigManager().getTablesEntityIds()
            )) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String entityType = rs.getString("entityType");
                    int id = rs.getInt("id");

                    PlayerUtils.cacheEntityId(entityType, id);
                    Debugger.debugExtras("Loaded entity " + entityType + " with id " + id);
                }

                PlayerUtils.loadEntityIds();
            }
        } catch (SQLException e) {
            stellarProtect.getLogger().warning("Error on loadEntityIds: " + e.getMessage());
        }
    }

    @Override
    public void saveWorld(String world, int id) {
        try {
            try (Connection connection = getConnection()) {
                try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO " + stellarProtect.getConfigManager().getTablesWorlds() + " (id, name) VALUES (?, ?)"
                )) {
                    stmt.setInt(1, id);
                    stmt.setString(2, world);
                    stmt.executeUpdate();

                    Debugger.debugExtras("Saved world " + world + " with id " + id);
                }
            }
        } catch (SQLException e) {
            stellarProtect.getLogger().warning("Error on saveWorld: " + e.getMessage());
        }
    }

    @Override
    public void saveEntityId(String entityType, long id) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO " + stellarProtect.getConfigManager().getTablesEntityIds() + " (entityType, id) VALUES (?, ?)"
            )) {
                stmt.setString(1, entityType);
                stmt.setLong(2, id);
                stmt.executeUpdate();

                Debugger.debugExtras("Saved entity " + entityType + " with id " + id);
            }
        } catch (SQLException e) {
            stellarProtect.getLogger().warning("Error on saveEntityId: " + e.getMessage());
        }
    }

    @SneakyThrows
    public Connection getConnection() {
        return dataSource.getConnection();
    }


}