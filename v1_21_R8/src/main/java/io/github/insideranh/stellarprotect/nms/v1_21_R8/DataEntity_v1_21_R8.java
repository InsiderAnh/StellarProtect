package io.github.insideranh.stellarprotect.nms.v1_21_R8;

import io.github.insideranh.stellarprotect.entities.DataEntity;
import io.github.insideranh.stellarprotect.entities.DataEntityType;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Keyed;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class DataEntity_v1_21_R8 implements DataEntity {

    private final HashMap<String, Object> data = new HashMap<>();
    private ItemStack[] armor;

    public DataEntity_v1_21_R8(Entity entity) {
        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;
            setData(DataEntityType.HEALTH, livingEntity.getHealth());

            if (livingEntity.getEquipment() != null) {
                this.armor = livingEntity.getEquipment().getArmorContents();
            }
        }
    }

    void getSpecialData(LivingEntity livingEntity) {
        if (livingEntity instanceof Wolf) {
            readDogData((Wolf) livingEntity);
        } else if (livingEntity instanceof Cat) {
            readCatData((Cat) livingEntity);
        }
    }

    @SuppressWarnings("deprecation")
    void readDogData(Wolf wolf) {
        Color collarColor = wolf.getCollarColor().getColor();
        Wolf.Variant variant = wolf.getVariant();
        if (wolf.getCollarColor() != DyeColor.RED) {
            setData(DataEntityType.COLLAR_COLOR, collarColor.getRed() + "," + collarColor.getGreen() + "," + collarColor.getBlue());
        }
        if (variant != Wolf.Variant.PALE) {
            setData(DataEntityType.DOG_VARIANT, variant.getKey().getKey());
        }
        if (wolf.isAngry()) {
            setData(DataEntityType.DOG_ANGRY, true);
        }
        if (wolf.isWet()) {
            setData(DataEntityType.DOG_WET, true);
        }
    }

    @SuppressWarnings("deprecation")
    void readCatData(Cat cat) {
        Color collarColor = cat.getCollarColor().getColor();
        Cat.Type variant = cat.getCatType();
        if (cat.getCollarColor() != DyeColor.RED) {
            setData(DataEntityType.COLLAR_COLOR, collarColor.getRed() + "," + collarColor.getGreen() + "," + collarColor.getBlue());
        }
        if (variant != Cat.Type.SIAMESE) {
            setData(DataEntityType.CAT_VARIANT, variant.getKey().getKey());
        }
    }

    void setData(Enum<?> key, boolean value) {
        data.put(key.name(), value);
    }

    void setData(Enum<?> key, int value) {
        data.put(key.name(), value);
    }

    void setData(Enum<?> key, double value) {
        data.put(key.name(), value);
    }

    void setData(Enum<?> key, String value) {
        data.put(key.name(), value);
    }

}