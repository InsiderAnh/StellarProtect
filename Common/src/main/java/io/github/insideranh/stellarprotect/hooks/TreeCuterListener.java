package io.github.insideranh.stellarprotect.hooks;

import io.github.insideranh.stellarprotect.listeners.BlockListener;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import pl.norbit.treecuter.api.listeners.TreeCutEvent;

import java.util.List;

public class TreeCuterListener implements Listener {

    @EventHandler
    public void onTreeCut(TreeCutEvent event) {
        Player player = event.getPlayer();
        List<Block> blocks = event.getBlocks();

        for (Block block : blocks) {
            BlockListener.processBlockBreak(block, player, -2);
        }
    }

}