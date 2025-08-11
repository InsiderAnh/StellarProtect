package io.github.insideranh.stellarprotect.database.types.mongo;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.database.repositories.ItemsRepository;
import io.github.insideranh.stellarprotect.items.ItemTemplate;
import io.github.insideranh.stellarprotect.utils.Debugger;
import io.github.insideranh.stellarprotect.utils.InventorySerializable;
import io.github.insideranh.stellarprotect.utils.StringCleanerUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ItemsRepositoryMongo implements ItemsRepository {

    private final StellarProtect stellarProtect = StellarProtect.getInstance();
    private final MongoDatabase database;
    private final MongoCollection<Document> item_templates;

    public ItemsRepositoryMongo(MongoDatabase database) {
        this.database = database;
        this.item_templates = database.getCollection(StellarProtect.getInstance().getConfigManager().getTablesItemTemplates());
    }

    @Override
    public void saveItems(List<ItemTemplate> itemTemplates) {
        stellarProtect.getExecutor().execute(() -> {
            try {
                List<Document> documents = new ArrayList<>();

                for (ItemTemplate template : itemTemplates) {
                    Document doc = new Document()
                        .append("_id", template.getId())
                        .append("base64", template.getBase64())
                        .append("s", template.getShorted())
                        .append("access_count", 0)
                        .append("last_accessed", new Date())
                        .append("total_quantity_used", 0)
                        .append("created_at", new Date());

                    documents.add(doc);
                }

                List<WriteModel<Document>> operations = new ArrayList<>();

                for (Document doc : documents) {
                    ReplaceOneModel<Document> replaceModel = new ReplaceOneModel<>(
                        Filters.eq("_id", doc.get("_id")),
                        doc,
                        new ReplaceOptions().upsert(true)
                    );
                    operations.add(replaceModel);
                }

                BulkWriteOptions bulkWriteOptions = new BulkWriteOptions()
                    .ordered(false)
                    .bypassDocumentValidation(false);

                int batchSize = 1000;
                int totalProcessed = 0;

                for (int i = 0; i < operations.size(); i += batchSize) {
                    int endIndex = Math.min(i + batchSize, operations.size());
                    List<WriteModel<Document>> batch = operations.subList(i, endIndex);

                    BulkWriteResult result = this.item_templates.bulkWrite(batch, bulkWriteOptions);
                    totalProcessed += batch.size();

                    Debugger.debugSave("Processed MongoDB: " + totalProcessed + "/" + operations.size() + " item templates " + result.getModifiedCount());
                }

                Debugger.debugSave("Saved " + itemTemplates.size() + " item templates in MongoDB");
            } catch (Exception e) {
                Debugger.debugSave("Error on save items in MongoDB: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public void updateItemUsageInDatabase(long templateId, int quantity) {

    }

    @Override
    public void loadMostUsedItems() {
        try {
            List<Bson> pipeline = Arrays.asList(
                Aggregates.sort(Sorts.orderBy(
                    Sorts.descending("access_count"),
                    Sorts.descending("total_quantity_used")
                )),
                Aggregates.limit(5000),
                Aggregates.project(Projections.fields(
                    Projections.include("_id", "base64", "s", "access_count",
                        "last_accessed", "total_quantity_used", "created_at")
                ))
            );

            try (MongoCursor<Document> cursor = item_templates.aggregate(pipeline).iterator()) {
                while (cursor.hasNext()) {
                    Document doc = cursor.next();

                    long id = doc.getLong("_id");
                    String base64 = doc.getString("base64");
                    byte shorted = (byte) doc.getInteger("s", 0);

                    String fullBase64 = (shorted == 1) ? StringCleanerUtils.COMMON_BASE64 + base64 : base64;

                    ItemStack bukkitItem = InventorySerializable.itemStackFromBase64(fullBase64);

                    ItemTemplate template = new ItemTemplate(id, bukkitItem, fullBase64);
                    stellarProtect.getItemsManager().loadItemReference(template, fullBase64);
                }
            }

            stellarProtect.getLogger().info("Loaded " + stellarProtect.getItemsManager().getItemReferenceCount() + " item references.");
        } catch (Exception e) {
            stellarProtect.getLogger().info("Error en loadMostUsedItems: " + e.getMessage());
        }
    }

}
