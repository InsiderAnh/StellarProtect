package io.github.insideranh.stellarprotect.database.types.sql;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.blocks.BlockTemplate;
import io.github.insideranh.stellarprotect.database.repositories.BlocksRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class BlocksRepositorySQL implements BlocksRepository {

    private final StellarProtect stellarProtect = StellarProtect.getInstance();
    private final Connection connection;

    public BlocksRepositorySQL(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void saveBlocks(List<BlockTemplate> blockTemplates) {
        stellarProtect.getExecutor().execute(() -> {
            String sql = "INSERT OR REPLACE INTO " + stellarProtect.getConfigManager().getTablesBlockTemplates() + " (id, block_data) VALUES (?, ?)";

            try (PreparedStatement statement = connection.prepareStatement(sql)) {

                connection.setAutoCommit(false);

                int batchSize = 0;
                int maxBatchSize = 1000;

                for (BlockTemplate template : blockTemplates) {
                    statement.setLong(1, template.getId());
                    statement.setString(2, template.getDataBlock().getBlockDataString());
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
                stellarProtect.getLogger().info("Saved " + blockTemplates.size() + " block templates in SQLite");
            } catch (Exception e) {
                stellarProtect.getLogger().info("Error on save items in SQLite: " + e.getMessage());
            }
        });
    }

    @Override
    public void loadBlockDatas() {
        stellarProtect.getExecutor().execute(() -> {
            String sql = "SELECT id, block_data FROM " + stellarProtect.getConfigManager().getTablesBlockTemplates();

            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String blockData = resultSet.getString("block_data");

                    stellarProtect.getBlocksManager().loadBlockData(id, blockData);
                }
                int count = stellarProtect.getBlocksManager().getBlockDataCount();

                stellarProtect.getBlocksManager().getCurrentId().set(count + 1);
                stellarProtect.getLogger().info("Loaded " + count + " block templates.");
            } catch (Exception e) {
                stellarProtect.getLogger().info("Error en loadMostUsedItems: " + e.getMessage());
            }
        });
    }

}