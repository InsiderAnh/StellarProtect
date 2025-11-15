package io.github.insideranh.stellarprotect.managers;

import com.google.common.collect.Sets;
import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.blocks.BlockTemplate;
import io.github.insideranh.stellarprotect.blocks.DataBlock;
import io.github.insideranh.stellarprotect.cache.BlocksCache;
import lombok.Getter;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class BlocksManager {

    private final StellarProtect plugin = StellarProtect.getInstance();

    private final BlocksCache blocksCache = new BlocksCache();

    // String -> BlockTemplate
    private final Map<String, BlockTemplate> blockHashToId = new ConcurrentHashMap<>(100);
    // BlockId -> BlockTemplate
    private final Map<Integer, BlockTemplate> idToBlockTemplate = new ConcurrentHashMap<>(100);
    private final Set<Integer> unsavedBlocks = Sets.newConcurrentHashSet();
    private final AtomicInteger currentId = new AtomicInteger(0);

    public void load() {

    }

    public BlockTemplate getBlockTemplate(int id) {
        return idToBlockTemplate.get(id);
    }

    public void loadBlockData(int id, String blockDataString) {
        DataBlock dataBlock = plugin.getDataBlock(blockDataString);
        BlockTemplate template = new BlockTemplate(id, dataBlock);
        blockHashToId.put(blockDataString, template);
        blocksCache.put(template);
        idToBlockTemplate.put(template.getId(), template);
    }

    public BlockTemplate getBlockTemplate(String blockDataString) {
        DataBlock dataBlock = plugin.getDataBlock(blockDataString);
        BlockTemplate id = blockHashToId.get(blockDataString);
        if (id != null) {
            return id;
        }
        return createBlockTemplate(dataBlock);
    }

    public BlockTemplate getBlockTemplate(BlockState block) {
        String blockDataString = plugin.getProtectNMS().getBlockData(block);
        BlockTemplate id = blockHashToId.get(blockDataString);
        if (id != null) {
            return id;
        }
        DataBlock dataBlock = plugin.getDataBlock(block);
        return createBlockTemplate(dataBlock);
    }

    public BlockTemplate getBlockTemplate(Block block) {
        String blockDataString = plugin.getProtectNMS().getBlockData(block);
        BlockTemplate id = blockHashToId.get(blockDataString);
        if (id != null) {
            return id;
        }
        DataBlock dataBlock = plugin.getDataBlock(block);
        return createBlockTemplate(dataBlock);
    }

    public BlockTemplate createBlockTemplate(DataBlock dataBlock) {
        int id = currentId.getAndIncrement();

        BlockTemplate template = new BlockTemplate(id, dataBlock);
        blocksCache.put(template);
        unsavedBlocks.add(id);
        blockHashToId.put(dataBlock.getBlockDataString(), template);
        idToBlockTemplate.put(id, template);

        return template;
    }

    public void saveBlocks() {
        if (unsavedBlocks.isEmpty()) return;

        List<BlockTemplate> templates = new ArrayList<>();
        unsavedBlocks.forEach(id -> templates.add(idToBlockTemplate.get(id)));
        unsavedBlocks.clear();
        plugin.getProtectDatabase().saveBlocks(templates);
    }

    public int getBlockDataCount() {
        return blockHashToId.size();
    }

}