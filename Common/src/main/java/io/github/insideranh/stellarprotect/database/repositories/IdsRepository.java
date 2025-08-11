package io.github.insideranh.stellarprotect.database.repositories;

public interface IdsRepository {

    void loadWorlds();

    void loadEntityIds();

    void saveWorld(String world, int id);

    void saveEntityId(String entityType, long id);

}