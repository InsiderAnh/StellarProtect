package io.github.insideranh.stellarprotect.database.types.mongo;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.InsertManyOptions;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.blocks.BlockTemplate;
import io.github.insideranh.stellarprotect.database.repositories.BlocksRepository;
import io.github.insideranh.stellarprotect.utils.Debugger;
import org.bson.Document;

import java.util.List;
import java.util.stream.Collectors;

public class BlocksRepositoryMongo implements BlocksRepository {

    private final StellarProtect stellarProtect = StellarProtect.getInstance();
    private final MongoDatabase database;
    private final MongoCollection<Document> item_templates;

    public BlocksRepositoryMongo(MongoDatabase database) {
        this.database = database;
        this.item_templates = database.getCollection(StellarProtect.getInstance().getConfigManager().getTablesItemTemplates());
    }

    @Override
    public void loadBlockDatas() {
        stellarProtect.getExecutor().execute(() -> {
            try {
                FindIterable<Document> documents = item_templates.find();

                int count = 0;
                for (Document doc : documents) {
                    Long id = doc.getLong("id");
                    String blockData = doc.getString("block_data");

                    if (id != null && blockData != null) {
                        stellarProtect.getBlocksManager().loadBlockData(id.intValue(), blockData);
                        count++;
                    }
                }

                stellarProtect.getBlocksManager().getCurrentId().set(count + 1);
                stellarProtect.getLogger().info("Loaded " + count + " block templates from MongoDB.");
            } catch (Exception e) {
                stellarProtect.getLogger().info("Error en loadBlockDatas MongoDB: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public void saveBlocks(List<BlockTemplate> blockTemplates) {
        stellarProtect.getExecutor().execute(() -> {

            try {
                List<Document> documents = blockTemplates.stream()
                    .map(template -> new Document()
                        .append("id", template.getId())
                        .append("block_data", template.getDataBlock().getBlockDataString()))
                    .collect(Collectors.toList());

                int batchSize = 1000;
                for (int i = 0; i < documents.size(); i += batchSize) {
                    int endIndex = Math.min(i + batchSize, documents.size());
                    List<Document> batch = documents.subList(i, endIndex);

                    List<Long> ids = batch.stream()
                        .map(doc -> doc.getLong("id"))
                        .collect(Collectors.toList());

                    item_templates.deleteMany(Filters.in("id", ids));

                    item_templates.insertMany(batch, new InsertManyOptions().ordered(false));
                }

                Debugger.debugSave("Saved " + blockTemplates.size() + " block templates in MongoDB");
            } catch (Exception e) {
                stellarProtect.getLogger().info("Error on save blocks in MongoDB : " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

}
