package io.github.insideranh.stellarprotect.utils;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;

public class ItemUtils {

    private final ItemMeta itemMeta;
    private ItemStack item;

    public ItemUtils(Material material) {
        this(material, 1, (short) 0);
    }

    public ItemUtils(Material material, int amount) {
        this(material, amount, (short) 0);
    }

    public ItemUtils(Material material, int amount, short data) {
        this.item = new ItemStack(material, amount, data);
        this.itemMeta = item.getItemMeta();
    }

    public ItemUtils(ItemStack item) {
        this.item = item.clone();
        if (item.getType().name().equals("AIR")) {
            item.setType(Material.STONE);
        }
        this.itemMeta = item.getItemMeta();
    }

    public ItemUtils type(Material material) {
        item.setType(material);
        return this;
    }

    public ItemUtils data(short data) {
        this.item = new ItemStack(item.getType(), item.getAmount(), data);
        return this;
    }

    public ItemUtils displayName(String displayName) {
        itemMeta.setDisplayName(displayName);
        return this;
    }

    public int enchantLevel(Enchantment enchantment) {
        return item.getEnchantments().getOrDefault(enchantment, 0);
    }

    public ItemUtils unEnchant(Enchantment enchantment) {
        itemMeta.removeEnchant(enchantment);
        return this;
    }

    public ItemUtils durability(int damage) {
        item.setDurability((short) Math.max(item.getType().getMaxDurability() - Math.max(damage, 1), 0));
        return this;
    }

    public int durability() {
        return item.getType().getMaxDurability() - item.getDurability();
    }

    public int maxDurability() {
        return item.getType().getMaxDurability();
    }

    public ItemUtils amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public int amount() {
        return item.getAmount();
    }

    public ItemUtils lore(String lore) {
        itemMeta.setLore(lore.isEmpty() ? new ArrayList<>() : Arrays.asList(lore.split("\\n")));
        return this;
    }

    public ItemUtils owner(String owner) {
        if (!item.getType().name().contains("SKULL_ITEM") && !item.getType().name().contains("PLAYER_HEAD"))
            return this;
        if (owner.isEmpty()) return this;
        SkullMeta headMeta = (SkullMeta) itemMeta;
        headMeta.setOwner(owner);
        item.setItemMeta(headMeta);
        return this;
    }

    public ItemUtils item(ItemStack item) {
        this.item = item;
        return this;
    }

    public ItemUtils enchant(Enchantment enchantment, int level) {
        itemMeta.addEnchant(enchantment, level, true);
        return this;
    }

    public ItemUtils addPattern(DyeColor dyeColor, PatternType patternType) {
        BannerMeta bannerMeta = (BannerMeta) itemMeta;
        if (bannerMeta != null) {
            bannerMeta.addPattern(new Pattern(dyeColor, patternType));
        }
        return this;
    }

    public ItemUtils addPattern(Pattern pattern) {
        BannerMeta bannerMeta = (BannerMeta) itemMeta;
        if (bannerMeta != null) {
            bannerMeta.addPattern(pattern);
        }
        return this;
    }

    public ItemStack build() {
        item.setItemMeta(itemMeta);
        return item;
    }

}