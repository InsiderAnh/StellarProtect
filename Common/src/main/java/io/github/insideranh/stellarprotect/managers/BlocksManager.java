package io.github.insideranh.stellarprotect.managers;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.blocks.BlockTemplate;
import io.github.insideranh.stellarprotect.blocks.DataBlock;
import io.github.insideranh.stellarprotect.maps.IntObjectMap;
import lombok.Getter;
import org.bukkit.block.Block;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class BlocksManager {

    private final StellarProtect plugin = StellarProtect.getInstance();

    // String -> BlockId
    private final IntObjectMap<Integer> blockHashToId = new IntObjectMap<>(1000);
    // BlockId -> BlockTemplate
    private final Map<Integer, BlockTemplate> idToBlockTemplate = new HashMap<>(100);
    private final Set<Integer> unsavedBlocks = new HashSet<>();
    private final AtomicInteger currentId = new AtomicInteger(0);

    public void load() {

    }

    public BlockTemplate getBlockTemplate(int id) {
        return idToBlockTemplate.get(id);
    }

    public void loadBlockData(int id, String blockDataString) {
        DataBlock dataBlock = plugin.getDataBlock(blockDataString);
        BlockTemplate template = new BlockTemplate(id, dataBlock);
        blockHashToId.put(dataBlock.hashCode(), template.getId());
        idToBlockTemplate.put(template.getId(), template);
    }

    public BlockTemplate getBlockTemplate(String blockDataString) {
        DataBlock dataBlock = plugin.getDataBlock(blockDataString);
        int hashCode = dataBlock.hashCode();
        Integer id = blockHashToId.get(hashCode);
        if (id != null) {
            return idToBlockTemplate.get(id);
        }
        return createBlockTemplate(dataBlock, hashCode);
    }

    public BlockTemplate getBlockTemplate(Block block) {
        int hashCode = plugin.getProtectNMS().getHashBlockData(block);
        Integer id = blockHashToId.get(hashCode);
        if (id != null) {
            return idToBlockTemplate.get(id);
        }
        DataBlock dataBlock = plugin.getDataBlock(block);
        return createBlockTemplate(dataBlock, hashCode);
    }

    public BlockTemplate createBlockTemplate(DataBlock dataBlock, int hashCode) {
        int id = currentId.getAndIncrement();

        BlockTemplate template = new BlockTemplate(id, dataBlock);
        unsavedBlocks.add(id);
        blockHashToId.put(hashCode, id);
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