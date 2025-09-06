package io.github.insideranh.stellarprotect.nms.v1_8_R3;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.insideranh.stellarprotect.api.ProtectNMS;
import io.github.insideranh.stellarprotect.callback.CallbackBucket;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import io.github.insideranh.stellarprotect.utils.PaginationUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;

public class ProtectNMS_v1_8_R3 extends ProtectNMS {

    private final Gson gson = new Gson();
    private final ArrayList<String> cropGrow = new ArrayList<>(Arrays.asList("WHEAT", "POTATO", "CARROT", "MELON_STEM", "PUMPKIN_STEM", "COCOA_BEANS"));

    @Override
    public int modelDataHashCode(ItemMeta itemMeta) {
        return 0;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getHashBlockData(Block block) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("m", block.getType().name());
        jsonObject.addProperty("d", block.getData());
        return jsonObject.toString().hashCode();
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean canGrow(Block block) {
        BlockState state = block.getState();
        MaterialData data = state.getData();
        return data.getData() < 7 && cropGrow.contains(block.getType().name());
    }

    @SuppressWarnings("deprecation")
    @Override
    public Block readMaterial(Location location, String data) {
        Block toBlock = location.getBlock();
        JsonObject jsonObject = gson.fromJson(data, JsonObject.class);
        Material material = Material.getMaterial(jsonObject.get("m").getAsString());
        byte blockData = jsonObject.get("d").getAsByte();
        toBlock.setType(material);
        toBlock.setData(blockData);
        return toBlock;
    }

    @SuppressWarnings("deprecation")
    @Override
    public String getBlockData(Block block) {
        JsonObject obj = new JsonObject();

        obj.addProperty("m", block.getType().name());
        obj.addProperty("d", block.getData());

        return obj.toString();
    }

    @Override
    public Location getBlockLocation(Player player, Inventory inventory) {
        return player.getLocation();
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getAge(Block block) {
        BlockState state = block.getState();
        MaterialData data = state.getData();
        return data.getData();
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isMaxAge(Block block) {
        BlockState state = block.getState();
        MaterialData data = state.getData();
        return data.getData() >= 7;
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

    @SuppressWarnings("deprecation")
    @Override
    public CallbackBucket<Block, String, Material> getBucketData(Block block, BlockFace blockFace, Material bucket) {
        Block relativeBlock = block.getRelative(blockFace);
        return new CallbackBucket<>(relativeBlock, String.valueOf(relativeBlock.getData()), relativeBlock.getType());
    }

    @Override
    public ItemStack getItemInHand(Player player) {
        return player.getInventory().getItemInHand();
    }

}