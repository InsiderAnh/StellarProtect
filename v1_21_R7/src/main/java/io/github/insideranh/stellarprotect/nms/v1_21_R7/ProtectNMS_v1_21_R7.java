package io.github.insideranh.stellarprotect.nms.v1_21_R7;

import io.github.insideranh.stellarprotect.api.ProtectNMS;
import io.github.insideranh.stellarprotect.callback.CallbackBucket;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.utils.PaginationUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.function.Function;

public class ProtectNMS_v1_21_R7 extends ProtectNMS {

    @Override
    public boolean canGrow(Block block) {
        if (block.getBlockData() instanceof Ageable) {
            Ageable ageable = (Ageable) block.getBlockData();
            return ageable.getAge() < ageable.getMaximumAge();
        }
        return true;
    }

    @Override
    public Block readMaterial(Location location, String data) {
        Block toBlock = location.getBlock();
        BlockData blockData = Bukkit.createBlockData(data);
        toBlock.setBlockData(blockData);
        return toBlock;
    }

    @Override
    public String getBlockData(Block block) {
        return block.getBlockData().getAsString(true);
    }

    @Override
    public Location getBlockLocation(Player player, Inventory inventory) {
        Location location = inventory.getLocation();
        if (location == null) {
            return player.getLocation();
        }
        return location;
    }

    @Override
    public int getAge(Block block) {
        if (block.getBlockData() instanceof Ageable) {
            Ageable ageable = (Ageable) block.getBlockData();
            return ageable.getAge();
        }
        return 0;
    }

    @Override
    public boolean isMaxAge(Block block) {
        if (block.getBlockData() instanceof Ageable) {
            Ageable ageable = (Ageable) block.getBlockData();
            return ageable.getAge() >= ageable.getMaximumAge();
        }
        return false;
    }

    @Override
    public String[] getSignLines(Block block) {
        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            return sign.getLines();
        }
        return new String[]{"", "", "", ""};
    }

    @Override
    public void teleport(Player player, Location location) {
        player.teleport(location);
    }

    @Override
    public void sendActionTitle(Player player, String title, String tooltipDetails, String command, Function<String, String> replacer) {
        TextComponent fullMessage = new TextComponent(replacer.apply(title));
        fullMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        fullMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(replacer.apply(tooltipDetails))));
        player.spigot().sendMessage(fullMessage);
    }

    @Override
    public void sendPageButtons(Player player, String pageString, String clickPage, int page, int perPage, int maxPages) {
        PlayerProtect playerProtect = PlayerProtect.getPlayer(player);
        TextComponent fullMessage = PaginationUtils.buildPagination(pageString, clickPage, page, perPage, maxPages, playerProtect);

        player.spigot().sendMessage(fullMessage);
    }

    @Override
    public CallbackBucket<Block, String, Material> getBucketData(Block block, BlockFace blockFace, Material bucket) {
        if (bucket.equals(Material.LAVA_BUCKET)) {
            return new CallbackBucket<>(block, block.getBlockData().getAsString(), Material.LAVA);
        }
        BlockData blockData = block.getBlockData();
        if (blockData instanceof Waterlogged) {
            boolean isWaterlogged = ((Waterlogged) blockData).isWaterlogged();
            if (isWaterlogged) {
                return new CallbackBucket<>(block, block.getBlockData().getAsString(), Material.WATER);
            }
        }

        Block relativeBlock = block.getRelative(blockFace);
        return new CallbackBucket<>(relativeBlock, block.getBlockData().getAsString(), Material.WATER);
    }

    @Override
    public ItemStack getItemInHand(Player player) {
        return player.getInventory().getItemInMainHand();
    }

}