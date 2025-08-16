package io.github.insideranh.stellarprotect.managers;

import io.github.insideranh.stellarprotect.StellarProtect;
import io.github.insideranh.stellarprotect.blocks.BlockTemplate;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class BlocksManager {

    private final StellarProtect plugin = StellarProtect.getInstance();

    // String -> BlockId
    private final Map<Integer, Integer> blockHashToId = new Int2ObjectOpenHashMap<>(100);
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
        BlockData blockData = Bukkit.createBlockData(blockDataString);
        int hashCode = blockData.hashCode();

        BlockTemplate template = new BlockTemplate(id, blockData, blockDataString);
        blockHashToId.put(hashCode, template.getId());
        idToBlockTemplate.put(template.getId(), template);
    }

    public BlockTemplate getBlockTemplate(String blockDataString) {
        BlockData blockData = Bukkit.createBlockData(blockDataString);
        int hashCode = blockData.hashCode();
        Integer id = blockHashToId.get(hashCode);
        if (id != null) {
            return idToBlockTemplate.get(id);
        }
        return createItemTemplate(blockData, hashCode);
    }

    public BlockTemplate getBlockTemplate(BlockData blockData) {
        int hashCode = blockData.hashCode();
        Integer id = blockHashToId.get(hashCode);
        if (id != null) {
            return idToBlockTemplate.get(id);
        }
        return createItemTemplate(blockData, hashCode);
    }

    public BlockTemplate createItemTemplate(BlockData blockData, int hashCode) {
        int id = currentId.getAndIncrement();

        BlockTemplate template = new BlockTemplate(id, blockData, blockData.getAsString());
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