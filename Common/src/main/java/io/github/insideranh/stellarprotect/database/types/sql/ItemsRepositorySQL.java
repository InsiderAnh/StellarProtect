package io.github.insideranh.stellarprotect.database.types.sql;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.database.repositories.ItemsRepository;
import io.github.insideranh.stellarprotect.items.ItemTemplate;
import io.github.insideranh.stellarprotect.utils.Debugger;
import io.github.insideranh.stellarprotect.utils.InventorySerializable;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ItemsRepositorySQL implements ItemsRepository {

    private final StellarProtect stellarProtect = StellarProtect.getInstance();
    private final Connection connection;

    public ItemsRepositorySQL(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void saveItems(List<ItemTemplate> itemTemplates) {
        stellarProtect.getExecutor().execute(() -> {
            String sql = "INSERT OR REPLACE INTO " + stellarProtect.getConfigManager().getTablesItemTemplates() + " (id, base64, s) VALUES (?, ?, ?)";

            try (PreparedStatement statement = connection.prepareStatement(sql)) {

                connection.setAutoCommit(false);

                int batchSize = 0;
                int maxBatchSize = 1000;

                statement.setByte(3, (byte) 0);

                for (ItemTemplate template : itemTemplates) {
                    statement.setLong(1, template.getId());
                    statement.setString(2, template.getBase64());
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
                Debugger.debugSave("Saved " + itemTemplates.size() + " item templates in SQLite");
            } catch (SQLException e) {
                Debugger.debugSave("Error on save items in SQLite: " + e.getMessage());
            }
        });
    }

    @Override
    public void updateItemUsageInDatabase(long templateId, int quantity) {
        stellarProtect.getExecutor().execute(() -> {
            String sql = "UPDATE " + stellarProtect.getConfigManager().getTablesItemTemplates() + " " +
                "SET access_count = access_count + 1," +
                "last_accessed = CURRENT_TIMESTAMP," +
                "total_quantity_used = total_quantity_used + ?" +
                "WHERE id = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, quantity);
                stmt.setLong(2, templateId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
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

            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    long id = resultSet.getLong("id");
                    String base64 = resultSet.getString("base64");

                    ItemStack bukkitItem = InventorySerializable.itemStackFromBase64(base64);

                    ItemTemplate template = new ItemTemplate(id, bukkitItem, base64);

                    stellarProtect.getItemsManager().loadItemReference(template, base64);
                }
                long count = stellarProtect.getItemsManager().getItemReferenceCount();

                stellarProtect.getItemsManager().getCurrentId().set(count + 1L);
                stellarProtect.getLogger().info("Loaded " + count + " item references.");
            } catch (SQLException e) {
                stellarProtect.getLogger().info("Error en loadMostUsedItems: " + e.getMessage());
            }
        });

        executor.shutdown();
    }

}
