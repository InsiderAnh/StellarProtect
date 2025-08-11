package io.github.insideranh.stellarprotect.database.types.mysql;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.zaxxer.hikari.HikariDataSource;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.database.repositories.ItemsRepository;
import io.github.insideranh.stellarprotect.items.ItemTemplate;
import io.github.insideranh.stellarprotect.utils.Debugger;
import io.github.insideranh.stellarprotect.utils.InventorySerializable;
import io.github.insideranh.stellarprotect.utils.StringCleanerUtils;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ItemsRepositoryMySQL implements ItemsRepository {

    private final StellarProtect stellarProtect = StellarProtect.getInstance();
    private final HikariDataSource dataSource;

    public ItemsRepositoryMySQL(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void saveItems(List<ItemTemplate> itemTemplates) {
        stellarProtect.getExecutor().execute(() -> {
            String sql = "INSERT INTO " + stellarProtect.getConfigManager().getTablesItemTemplates() + " (id, base64, s) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE base64 = VALUES(base64)";

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                connection.setAutoCommit(false);

                int batchSize = 0;
                int maxBatchSize = 1000;

                for (ItemTemplate template : itemTemplates) {
                    statement.setLong(1, template.getId());
                    statement.setString(2, template.getBase64());
                    statement.setByte(3, template.getShorted());
                    statement.addBatch();

                    batchSize++;

                    if (batchSize >= maxBatchSize) {
                        statement.executeBatch();
                        connection.commit();
                        statement.clearBatch();
                        batchSize = 0;
                    }
                }

                if (batchSize > 0) {
                    statement.executeBatch();
                }

                connection.commit();
                Debugger.debugSave("Saved " + itemTemplates.size() + " item templates in MySQL");
            } catch (SQLException e) {
                Debugger.debugSave("Error on save items in MySQL: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public void updateItemUsageInDatabase(long templateId, int quantity) {

    }

    @Override
    public void loadMostUsedItems() {
        ListeningExecutorService executor = MoreExecutors.listeningDecorator(
            new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1024))
        );

        executor.execute(() -> {
            String sql = "SELECT id, base64, s, access_count, last_accessed, total_quantity_used, created_at " +
                "FROM " + stellarProtect.getConfigManager().getTablesItemTemplates() + " " +
                "ORDER BY access_count DESC, total_quantity_used DESC " +
                "LIMIT 5000";

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    long id = resultSet.getLong("id");
                    String base64 = resultSet.getString("base64");
                    byte shorted = resultSet.getByte("s");

                    String fullBase64 = (shorted == 1) ? StringCleanerUtils.COMMON_BASE64 + base64 : base64;

                    ItemStack bukkitItem = InventorySerializable.itemStackFromBase64(fullBase64);

                    ItemTemplate template = new ItemTemplate(id, bukkitItem, fullBase64);

                    stellarProtect.getItemsManager().loadItemReference(template, fullBase64);
                }

                stellarProtect.getLogger().info("Loaded " + stellarProtect.getItemsManager().getItemReferenceCount() + " item references.");
            } catch (SQLException e) {
                stellarProtect.getLogger().info("Error en loadMostUsedItems: " + e.getMessage());
            }
        });

        executor.shutdown();
    }

}
