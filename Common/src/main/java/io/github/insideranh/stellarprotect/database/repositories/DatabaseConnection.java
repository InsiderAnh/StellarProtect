package io.github.insideranh.stellarprotect.database.repositories;

public interface DatabaseConnection {

    void connect();

    void createIndexes();

    void vacuum();

    void close();

    IdsRepository getIdsRepository();

    LoggerRepository getLoggerRepository();

    PlayerRepository getPlayerRepository();

    ItemsRepository getItemsRepository();

    RestoreRepository getRestoreRepository();

    BlocksRepository getBlocksRepository();

}