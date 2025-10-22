package io.github.insideranh.stellarprotect.entities;

import org.bukkit.entity.Entity;

import java.util.HashMap;

public interface DataEntity {

    HashMap<String, Object> getData();

    void applyToEntity(Entity entity);

}