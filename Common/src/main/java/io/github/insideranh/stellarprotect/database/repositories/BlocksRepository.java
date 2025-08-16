package io.github.insideranh.stellarprotect.database.repositories;

import io.github.insideranh.stellarprotect.blocks.BlockTemplate;

import java.util.List;

public interface BlocksRepository {

    void saveBlocks(List<BlockTemplate> blockTemplates);

    void loadBlockDatas();

}