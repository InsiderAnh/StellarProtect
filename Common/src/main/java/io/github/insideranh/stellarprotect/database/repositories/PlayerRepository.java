package io.github.insideranh.stellarprotect.database.repositories;

import io.github.insideranh.stellarprotect.data.PlayerProtect;
import org.bukkit.entity.Player;

import java.util.List;

public interface PlayerRepository {

    PlayerProtect loadOrCreatePlayer(Player player);

    List<Long> getIdsByNames(List<String> names);

    long generateNextId();

}