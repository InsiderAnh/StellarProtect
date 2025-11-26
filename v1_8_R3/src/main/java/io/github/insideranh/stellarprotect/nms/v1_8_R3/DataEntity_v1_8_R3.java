package io.github.insideranh.stellarprotect.nms.v1_8_R3;

import io.github.insideranh.stellarprotect.entities.DataEntity;
import io.github.insideranh.stellarprotect.entities.DataEntityType;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Rotation;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

import java.util.HashMap;

public class DataEntity_v1_8_R3 implements DataEntity {

    private final HashMap<String, Object> data = new HashMap<>();
    private ItemStack[] armor;
    private ItemStack mainHand;
    private ItemStack offHand;

    public DataEntity_v1_8_R3(Entity entity) {
        readEntityData(entity);
    }

    public DataEntity_v1_8_R3(HashMap<String, Object> data) {
        this.data.putAll(data);
    }

    public DataEntity_v1_8_R3() {
    }

    private void readEntityData(Entity entity) {
        setData(DataEntityType.ENTITY_TYPE, entity.getType().name());
        if (entity.getCustomName() != null) {
            setData(DataEntityType.CUSTOM_NAME, entity.getCustomName());
        }
        setData(DataEntityType.CUSTOM_NAME_VISIBLE, entity.isCustomNameVisible());

        if (entity instanceof LivingEntity) {
            readLivingEntityData((LivingEntity) entity);
        }

        if (entity instanceof ArmorStand) {
            readArmorStandData((ArmorStand) entity);
        }

        if (entity instanceof ItemFrame) {
            readItemFrameData((ItemFrame) entity);
        }
    }

    private void readLivingEntityData(LivingEntity entity) {
        setData(DataEntityType.HEALTH, entity.getHealth());

        readSpecificMobData(entity);
    }

    private void readSpecificMobData(LivingEntity entity) {
        if (entity instanceof Villager) {
            Villager villager = (Villager) entity;
            setData(DataEntityType.VILLAGER_PROFESSION, villager.getProfession().name());
        } else if (entity instanceof Wolf) {
            Wolf wolf = (Wolf) entity;
            setData(DataEntityType.ANGRY, wolf.isAngry());
            if (wolf.isTamed()) {
                Color collarColor = wolf.getCollarColor().getColor();
                setData(DataEntityType.COLLAR_COLOR, collarColor.getRed() + "," + collarColor.getGreen() + "," + collarColor.getBlue());
            }
        } else if (entity instanceof Horse) {
            Horse horse = (Horse) entity;
            setData(DataEntityType.HORSE_COLOR, horse.getColor().name());
            setData(DataEntityType.HORSE_STYLE, horse.getStyle().name());
            setData(DataEntityType.JUMP_STRENGTH, horse.getJumpStrength());
            if (horse.getInventory().getSaddle() != null) {
                setData(DataEntityType.HAS_SADDLE, true);
            }
            if (horse.getInventory().getArmor() != null) {
                setData(DataEntityType.HORSE_ARMOR, horse.getInventory().getArmor());
            }
        } else if (entity instanceof Rabbit) {
            Rabbit rabbit = (Rabbit) entity;
            setData(DataEntityType.RABBIT_TYPE, rabbit.getRabbitType().name());
        } else if (entity instanceof Sheep) {
            Sheep sheep = (Sheep) entity;
            setData(DataEntityType.SHEEP_COLOR, sheep.getColor().name());
            setData(DataEntityType.SHEEP_SHEARED, sheep.isSheared());
        } else if (entity instanceof Pig) {
            Pig pig = (Pig) entity;
            setData(DataEntityType.PIG_SADDLED, pig.hasSaddle());
        } else if (entity instanceof Creeper) {
            Creeper creeper = (Creeper) entity;
            setData(DataEntityType.CREEPER_POWERED, creeper.isPowered());
        } else if (entity instanceof Enderman) {
            Enderman enderman = (Enderman) entity;
            /*if (enderman.getCarriedMaterial() != null) {
                setData(DataEntityType.ENDERMAN_CARRIED_BLOCK, enderman.getCarriedMaterial().name());
            }*/
        } else if (entity instanceof Slime) {
            Slime slime = (Slime) entity;
            setData(DataEntityType.SLIME_SIZE, slime.getSize());
        } else if (entity instanceof Zombie) {
            Zombie zombie = (Zombie) entity;
            setData(DataEntityType.ZOMBIE_BABY, zombie.isBaby());
        } else if (entity instanceof IronGolem) {
            IronGolem golem = (IronGolem) entity;
            setData(DataEntityType.IRON_GOLEM_PLAYER_CREATED, golem.isPlayerCreated());
        }
    }

    private void readArmorStandData(ArmorStand stand) {
        setData(DataEntityType.ARMOR_STAND_ARMS, stand.hasArms());
        setData(DataEntityType.ARMOR_STAND_BASE_PLATE, stand.hasBasePlate());
        setData(DataEntityType.ARMOR_STAND_MARKER, stand.isMarker());
        setData(DataEntityType.ARMOR_STAND_SMALL, stand.isSmall());
        setData(DataEntityType.ARMOR_STAND_VISIBLE, stand.isVisible());

        setData(DataEntityType.ARMOR_STAND_HEAD_POSE, eulerToString(stand.getHeadPose()));
        setData(DataEntityType.ARMOR_STAND_BODY_POSE, eulerToString(stand.getBodyPose()));
        setData(DataEntityType.ARMOR_STAND_LEFT_ARM_POSE, eulerToString(stand.getLeftArmPose()));
        setData(DataEntityType.ARMOR_STAND_RIGHT_ARM_POSE, eulerToString(stand.getRightArmPose()));
        setData(DataEntityType.ARMOR_STAND_LEFT_LEG_POSE, eulerToString(stand.getLeftLegPose()));
        setData(DataEntityType.ARMOR_STAND_RIGHT_LEG_POSE, eulerToString(stand.getRightLegPose()));

        if (stand.getEquipment() != null) {
            EntityEquipment eq = stand.getEquipment();
            this.armor = eq.getArmorContents();
            this.mainHand = eq.getItemInHand();
        }
    }

    private void readItemFrameData(ItemFrame frame) {
        setData(DataEntityType.ITEM_FRAME_ITEM, frame.getItem());
        setData(DataEntityType.ITEM_FRAME_ROTATION, frame.getRotation().name());
    }

    public void applyToEntity(Entity entity) {
        if (hasData(DataEntityType.CUSTOM_NAME)) {
            entity.setCustomName((String) getData(DataEntityType.CUSTOM_NAME));
        }
        entity.setCustomNameVisible(getBoolean(DataEntityType.CUSTOM_NAME_VISIBLE));

        if (entity instanceof LivingEntity) {
            applyLivingEntityData((LivingEntity) entity);
        }

        if (entity instanceof ArmorStand) {
            applyArmorStandData((ArmorStand) entity);
        }

        if (entity instanceof ItemFrame) {
            applyItemFrameData((ItemFrame) entity);
        }
    }

    private void applyLivingEntityData(LivingEntity entity) {
        if (hasData(DataEntityType.HEALTH)) {
            entity.setHealth(Math.max(getDouble(DataEntityType.HEALTH), 1.0));
        }

        applySpecificMobData(entity);
    }

    private void applySpecificMobData(LivingEntity entity) {
        if (entity instanceof Villager) {
            Villager villager = (Villager) entity;
            if (hasData(DataEntityType.VILLAGER_PROFESSION)) {
                String typeKey = getString(DataEntityType.VILLAGER_PROFESSION);
                for (Villager.Profession profession : Villager.Profession.values()) {
                    if (profession.name().equals(typeKey)) {
                        villager.setProfession(profession);
                        break;
                    }
                }
            }
        } else if (entity instanceof Wolf) {
            Wolf wolf = (Wolf) entity;
            if (hasData(DataEntityType.ANGRY)) {
                wolf.setAngry(getBoolean(DataEntityType.ANGRY));
            }
            if (hasData(DataEntityType.COLLAR_COLOR) && wolf.isTamed()) {
                String[] rgb = getString(DataEntityType.COLLAR_COLOR).split(",");
                Color color = Color.fromRGB(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
                for (DyeColor dyeColor : DyeColor.values()) {
                    if (dyeColor.getColor().equals(color)) {
                        wolf.setCollarColor(dyeColor);
                        break;
                    }
                }
            }
        } else if (entity instanceof Horse) {
            Horse horse = (Horse) entity;
            if (hasData(DataEntityType.HORSE_COLOR)) {
                horse.setColor(Horse.Color.valueOf(getString(DataEntityType.HORSE_COLOR)));
            }
            if (hasData(DataEntityType.HORSE_STYLE)) {
                horse.setStyle(Horse.Style.valueOf(getString(DataEntityType.HORSE_STYLE)));
            }
            if (hasData(DataEntityType.JUMP_STRENGTH)) {
                horse.setJumpStrength(getDouble(DataEntityType.JUMP_STRENGTH));
            }
            if (hasData(DataEntityType.HORSE_ARMOR)) {
                horse.getInventory().setArmor((ItemStack) getData(DataEntityType.HORSE_ARMOR));
            }
        } else if (entity instanceof Rabbit) {
            Rabbit rabbit = (Rabbit) entity;
            if (hasData(DataEntityType.RABBIT_TYPE)) {
                rabbit.setRabbitType(Rabbit.Type.valueOf(getString(DataEntityType.RABBIT_TYPE)));
            }
        } else if (entity instanceof Sheep) {
            Sheep sheep = (Sheep) entity;
            if (hasData(DataEntityType.SHEEP_COLOR)) {
                sheep.setColor(DyeColor.valueOf(getString(DataEntityType.SHEEP_COLOR)));
            }
            if (hasData(DataEntityType.SHEEP_SHEARED)) {
                sheep.setSheared(getBoolean(DataEntityType.SHEEP_SHEARED));
            }
        } else if (entity instanceof Pig) {
            Pig pig = (Pig) entity;
            if (hasData(DataEntityType.PIG_SADDLED)) {
                pig.setSaddle(getBoolean(DataEntityType.PIG_SADDLED));
            }
        } else if (entity instanceof Creeper) {
            Creeper creeper = (Creeper) entity;
            if (hasData(DataEntityType.CREEPER_POWERED)) {
                creeper.setPowered(getBoolean(DataEntityType.CREEPER_POWERED));
            }
        } else if (entity instanceof Enderman) {
            Enderman enderman = (Enderman) entity;
            /*if (hasData(DataEntityType.ENDERMAN_CARRIED_BLOCK)) {
                enderman.setCarriedMaterial(Material.valueOf(getString(DataEntityType.ENDERMAN_CARRIED_BLOCK)));
            }*/
        } else if (entity instanceof Slime) {
            Slime slime = (Slime) entity;
            if (hasData(DataEntityType.SLIME_SIZE)) {
                slime.setSize(getInt(DataEntityType.SLIME_SIZE));
            }
        } else if (entity instanceof Zombie) {
            Zombie zombie = (Zombie) entity;
            if (hasData(DataEntityType.ZOMBIE_BABY)) {
                zombie.setBaby(getBoolean(DataEntityType.ZOMBIE_BABY));
            }
        } else if (entity instanceof IronGolem) {
            IronGolem golem = (IronGolem) entity;
            if (hasData(DataEntityType.IRON_GOLEM_PLAYER_CREATED)) {
                golem.setPlayerCreated(getBoolean(DataEntityType.IRON_GOLEM_PLAYER_CREATED));
            }
        }
    }

    private void applyArmorStandData(ArmorStand stand) {
        if (hasData(DataEntityType.ARMOR_STAND_ARMS)) {
            stand.setArms(getBoolean(DataEntityType.ARMOR_STAND_ARMS));
        }
        if (hasData(DataEntityType.ARMOR_STAND_BASE_PLATE)) {
            stand.setBasePlate(getBoolean(DataEntityType.ARMOR_STAND_BASE_PLATE));
        }
        if (hasData(DataEntityType.ARMOR_STAND_MARKER)) {
            stand.setMarker(getBoolean(DataEntityType.ARMOR_STAND_MARKER));
        }
        if (hasData(DataEntityType.ARMOR_STAND_SMALL)) {
            stand.setSmall(getBoolean(DataEntityType.ARMOR_STAND_SMALL));
        }
        if (hasData(DataEntityType.ARMOR_STAND_VISIBLE)) {
            stand.setVisible(getBoolean(DataEntityType.ARMOR_STAND_VISIBLE));
        }

        // Poses
        if (hasData(DataEntityType.ARMOR_STAND_HEAD_POSE)) {
            stand.setHeadPose(stringToEuler(getString(DataEntityType.ARMOR_STAND_HEAD_POSE)));
        }
        if (hasData(DataEntityType.ARMOR_STAND_BODY_POSE)) {
            stand.setBodyPose(stringToEuler(getString(DataEntityType.ARMOR_STAND_BODY_POSE)));
        }
        if (hasData(DataEntityType.ARMOR_STAND_LEFT_ARM_POSE)) {
            stand.setLeftArmPose(stringToEuler(getString(DataEntityType.ARMOR_STAND_LEFT_ARM_POSE)));
        }
        if (hasData(DataEntityType.ARMOR_STAND_RIGHT_ARM_POSE)) {
            stand.setRightArmPose(stringToEuler(getString(DataEntityType.ARMOR_STAND_RIGHT_ARM_POSE)));
        }
        if (hasData(DataEntityType.ARMOR_STAND_LEFT_LEG_POSE)) {
            stand.setLeftLegPose(stringToEuler(getString(DataEntityType.ARMOR_STAND_LEFT_LEG_POSE)));
        }
        if (hasData(DataEntityType.ARMOR_STAND_RIGHT_LEG_POSE)) {
            stand.setRightLegPose(stringToEuler(getString(DataEntityType.ARMOR_STAND_RIGHT_LEG_POSE)));
        }

        // Equipment
        if (stand.getEquipment() != null) {
            EntityEquipment eq = stand.getEquipment();
            if (armor != null) eq.setArmorContents(armor);
            if (mainHand != null) eq.setItemInHand(mainHand);
        }
    }

    private void applyItemFrameData(ItemFrame frame) {
        if (hasData(DataEntityType.ITEM_FRAME_ITEM)) {
            frame.setItem((ItemStack) getData(DataEntityType.ITEM_FRAME_ITEM));
        }
        if (hasData(DataEntityType.ITEM_FRAME_ROTATION)) {
            frame.setRotation(Rotation.valueOf(getString(DataEntityType.ITEM_FRAME_ROTATION)));
        }
    }

    private void setData(Enum<?> key, boolean value) {
        data.put(key.name(), value);
    }

    private void setData(Enum<?> key, int value) {
        data.put(key.name(), value);
    }

    private void setData(Enum<?> key, double value) {
        data.put(key.name(), value);
    }

    private void setData(Enum<?> key, String value) {
        if (value != null && !value.isEmpty()) {
            data.put(key.name(), value);
        }
    }

    private void setData(Enum<?> key, Object value) {
        if (value != null) {
            data.put(key.name(), value);
        }
    }

    private boolean hasData(Enum<?> key) {
        return data.containsKey(key.name());
    }

    private Object getData(Enum<?> key) {
        return data.get(key.name());
    }

    private boolean getBoolean(Enum<?> key) {
        return hasData(key) && (boolean) getData(key);
    }

    private int getInt(Enum<?> key) {
        return hasData(key) ? ((Number) getData(key)).intValue() : 0;
    }

    private double getDouble(Enum<?> key) {
        return hasData(key) ? ((Number) getData(key)).doubleValue() : 0.0;
    }

    private String getString(Enum<?> key) {
        return hasData(key) ? (String) getData(key) : "";
    }

    private String eulerToString(EulerAngle angle) {
        return angle.getX() + "," + angle.getY() + "," + angle.getZ();
    }

    private EulerAngle stringToEuler(String str) {
        String[] parts = str.split(",");
        return new EulerAngle(
            Double.parseDouble(parts[0]),
            Double.parseDouble(parts[1]),
            Double.parseDouble(parts[2])
        );
    }

    public HashMap<String, Object> getData() {
        return data;
    }

    public ItemStack[] getArmor() {
        return armor;
    }

    public ItemStack getMainHand() {
        return mainHand;
    }

    public ItemStack getOffHand() {
        return offHand;
    }

}
