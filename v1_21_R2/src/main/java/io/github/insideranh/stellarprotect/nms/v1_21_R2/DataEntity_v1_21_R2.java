package io.github.insideranh.stellarprotect.nms.v1_21_R2;

import io.github.insideranh.stellarprotect.entities.DataEntity;
import io.github.insideranh.stellarprotect.entities.DataEntityType;
import io.github.insideranh.stellarprotect.utils.InventorySerializable;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.EulerAngle;

import java.util.*;

public class DataEntity_v1_21_R2 implements DataEntity {

    private final HashMap<String, Object> data = new HashMap<>();
    private ItemStack[] armor;
    private ItemStack mainHand;
    private ItemStack offHand;

    public DataEntity_v1_21_R2(Entity entity) {
        readEntityData(entity);
    }

    public DataEntity_v1_21_R2(HashMap<String, Object> data) {
        this.data.putAll(data);
    }

    public DataEntity_v1_21_R2() {
    }

    private void readEntityData(Entity entity) {
        setData(DataEntityType.ENTITY_TYPE, entity.getType().name());
        if (entity.getCustomName() != null) {
            setData(DataEntityType.CUSTOM_NAME, entity.getCustomName());
        }
        setData(DataEntityType.CUSTOM_NAME_VISIBLE, entity.isCustomNameVisible());
        setData(DataEntityType.GLOWING, entity.isGlowing());
        setData(DataEntityType.GRAVITY, entity.hasGravity());
        setData(DataEntityType.INVULNERABLE, entity.isInvulnerable());
        setData(DataEntityType.SILENT, entity.isSilent());

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
        setData(DataEntityType.MAX_HEALTH, entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        setData(DataEntityType.AI, entity.hasAI());
        setData(DataEntityType.CAN_PICKUP_ITEMS, entity.getCanPickupItems());
        setData(DataEntityType.COLLIDABLE, entity.isCollidable());
        setData(DataEntityType.REMAINING_AIR, entity.getRemainingAir());

        if (entity.getEquipment() != null) {
            EntityEquipment eq = entity.getEquipment();
            this.armor = eq.getArmorContents();
            this.mainHand = eq.getItemInMainHand();
            this.offHand = eq.getItemInOffHand();
        }

        Collection<PotionEffect> effects = entity.getActivePotionEffects();
        if (!effects.isEmpty()) {
            List<String> effectsList = new ArrayList<>();
            for (PotionEffect effect : effects) {
                effectsList.add(effect.getType().getName() + ":" + effect.getDuration() + ":" + effect.getAmplifier());
            }
            setData(DataEntityType.POTION_EFFECTS, String.join(";", effectsList));
        }

        if (entity instanceof Mob) {
            readMobData((Mob) entity);
        }

        if (entity instanceof Ageable) {
            Ageable ageable = (Ageable) entity;
            setData(DataEntityType.AGE, ageable.getAge());
            setData(DataEntityType.BABY, !ageable.isAdult());
            setData(DataEntityType.AGE_LOCK, ageable.getAgeLock());
        }

        if (entity instanceof Tameable) {
            Tameable tameable = (Tameable) entity;
            setData(DataEntityType.TAMED, tameable.isTamed());
            if (tameable.getOwner() != null) {
                setData(DataEntityType.OWNER_UUID, tameable.getOwner().getUniqueId().toString());
            }
        }

        readSpecificMobData(entity);
    }

    private void readMobData(Mob mob) {
        setData(DataEntityType.AWARE, mob.isAware());
        if (mob.getTarget() != null) {
            setData(DataEntityType.TARGET_UUID, mob.getTarget().getUniqueId().toString());
        }
    }

    private void readSpecificMobData(LivingEntity entity) {
        if (entity instanceof Villager) {
            Villager villager = (Villager) entity;
            setData(DataEntityType.VILLAGER_PROFESSION, villager.getProfession().getKey().getKey());
            setData(DataEntityType.VILLAGER_TYPE, villager.getVillagerType().getKey().getKey());
            setData(DataEntityType.VILLAGER_LEVEL, villager.getVillagerLevel());
            setData(DataEntityType.VILLAGER_EXPERIENCE, villager.getVillagerExperience());

            if (villager.getRecipes() != null && !villager.getRecipes().isEmpty()) {
                List<String> tradesData = new ArrayList<>();
                for (MerchantRecipe recipe : villager.getRecipes()) {
                    // Formato: resultado|ingrediente1|ingrediente2|uses|maxUses|exp|priceMultiplier
                    StringBuilder tradeStr = new StringBuilder();
                    tradeStr.append(InventorySerializable.itemStackToBase64(recipe.getResult())).append("|");
                    tradeStr.append(InventorySerializable.itemStackToBase64(recipe.getIngredients().get(0))).append("|");
                    tradeStr.append(recipe.getIngredients().size() > 1 ? InventorySerializable.itemStackToBase64(recipe.getIngredients().get(1)) : "null").append("|");
                    tradeStr.append(recipe.getUses()).append("|");
                    tradeStr.append(recipe.getMaxUses()).append("|");
                    tradeStr.append(recipe.getVillagerExperience()).append("|");
                    tradeStr.append(recipe.getPriceMultiplier());
                    tradesData.add(tradeStr.toString());
                }
                setData(DataEntityType.VILLAGER_TRADES, String.join(";;", tradesData));
            }
        } else if (entity instanceof ZombieVillager) {
            ZombieVillager zVillager = (ZombieVillager) entity;

            setData(DataEntityType.VILLAGER_PROFESSION, zVillager.getVillagerProfession().getKey().getKey());
            setData(DataEntityType.VILLAGER_TYPE, zVillager.getVillagerType().getKey().getKey());
            setData(DataEntityType.CONVERTING, zVillager.isConverting());
            if (zVillager.isConverting()) {
                setData(DataEntityType.CONVERSION_TIME, zVillager.getConversionTime());
            }
        } else if (entity instanceof Wolf) {
            Wolf wolf = (Wolf) entity;
            setData(DataEntityType.ANGRY, wolf.isAngry());
            setData(DataEntityType.WOLF_VARIANT, wolf.getVariant().getKey().getKey());
            if (wolf.isTamed()) {
                Color collarColor = wolf.getCollarColor().getColor();
                setData(DataEntityType.COLLAR_COLOR, collarColor.getRed() + "," + collarColor.getGreen() + "," + collarColor.getBlue());
            }
        } else if (entity instanceof Cat) {
            Cat cat = (Cat) entity;
            setData(DataEntityType.CAT_TYPE, cat.getCatType().getKey().getKey());
            if (cat.isTamed()) {
                Color collarColor = cat.getCollarColor().getColor();
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
        } else if (entity instanceof Llama) {
            Llama llama = (Llama) entity;
            setData(DataEntityType.LLAMA_COLOR, llama.getColor().name());
            setData(DataEntityType.LLAMA_STRENGTH, llama.getStrength());
            if (llama.getInventory().getDecor() != null) {
                setData(DataEntityType.LLAMA_CARPET, llama.getInventory().getDecor());
            }
        } else if (entity instanceof Parrot) {
            Parrot parrot = (Parrot) entity;
            setData(DataEntityType.PARROT_VARIANT, parrot.getVariant().name());
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
            setData(DataEntityType.CREEPER_MAX_FUSE, creeper.getMaxFuseTicks());
            setData(DataEntityType.CREEPER_EXPLOSION_RADIUS, creeper.getExplosionRadius());
        } else if (entity instanceof Enderman) {
            Enderman enderman = (Enderman) entity;
            if (enderman.getCarriedBlock() != null) {
                setData(DataEntityType.ENDERMAN_CARRIED_BLOCK, enderman.getCarriedBlock().getMaterial().name());
            }
        } else if (entity instanceof Slime) {
            Slime slime = (Slime) entity;
            setData(DataEntityType.SLIME_SIZE, slime.getSize());
        } else if (entity instanceof Phantom) {
            Phantom phantom = (Phantom) entity;
            setData(DataEntityType.PHANTOM_SIZE, phantom.getSize());
        } else if (entity instanceof TropicalFish) {
            TropicalFish fish = (TropicalFish) entity;
            setData(DataEntityType.TROPICAL_FISH_PATTERN, fish.getPattern().name());
            setData(DataEntityType.TROPICAL_FISH_BODY_COLOR, fish.getBodyColor().name());
            setData(DataEntityType.TROPICAL_FISH_PATTERN_COLOR, fish.getPatternColor().name());
        } else if (entity instanceof Axolotl) {
            Axolotl axolotl = (Axolotl) entity;
            setData(DataEntityType.AXOLOTL_VARIANT, axolotl.getVariant().name());
        } else if (entity instanceof Bee) {
            Bee bee = (Bee) entity;
            setData(DataEntityType.BEE_HAS_NECTAR, bee.hasNectar());
            setData(DataEntityType.BEE_HAS_STUNG, bee.hasStung());
            setData(DataEntityType.BEE_ANGER, bee.getAnger());
        } else if (entity instanceof Fox) {
            Fox fox = (Fox) entity;
            setData(DataEntityType.FOX_TYPE, fox.getFoxType().name());
            setData(DataEntityType.FOX_CROUCHING, fox.isCrouching());
            setData(DataEntityType.FOX_SLEEPING, fox.isSleeping());
        } else if (entity instanceof Panda) {
            Panda panda = (Panda) entity;
            setData(DataEntityType.PANDA_MAIN_GENE, panda.getMainGene().name());
            setData(DataEntityType.PANDA_HIDDEN_GENE, panda.getHiddenGene().name());
        } else if (entity instanceof Piglin) {
            Piglin piglin = (Piglin) entity;
            setData(DataEntityType.PIGLIN_IMMUNE_TO_ZOMBIFICATION, piglin.isImmuneToZombification());
            setData(DataEntityType.PIGLIN_BABY, piglin.isBaby());
        } else if (entity instanceof Hoglin) {
            Hoglin hoglin = (Hoglin) entity;
            setData(DataEntityType.HOGLIN_IMMUNE_TO_ZOMBIFICATION, hoglin.isImmuneToZombification());
            if (hoglin.isConverting()) {
                setData(DataEntityType.HOGLIN_HUNTING_COOLDOWN, hoglin.getConversionTime());
            }
        } else if (entity instanceof Strider) {
            Strider strider = (Strider) entity;
            setData(DataEntityType.STRIDER_SADDLED, strider.isShivering());
        } else if (entity instanceof Goat) {
            Goat goat = (Goat) entity;
            setData(DataEntityType.GOAT_SCREAMING, goat.isScreaming());
        } else if (entity instanceof Frog) {
            Frog frog = (Frog) entity;
            setData(DataEntityType.FROG_VARIANT, frog.getVariant().getKey().getKey());
        } else if (entity instanceof Allay) {
            Allay allay = (Allay) entity;
            setData(DataEntityType.ALLAY_CAN_DUPLICATE, allay.canDuplicate());
            setData(DataEntityType.ALLAY_DUPLICATION_COOLDOWN, allay.getDuplicationCooldown());
        } else if (entity instanceof Warden) {
            Warden warden = (Warden) entity;
            setData(DataEntityType.WARDEN_ANGER, warden.getAnger());
        } else if (entity instanceof Sniffer) {
            Sniffer sniffer = (Sniffer) entity;
            setData(DataEntityType.SNIFFER_STATE, sniffer.getState().name());
        } else if (entity instanceof Camel) {
            Camel camel = (Camel) entity;
            setData(DataEntityType.CAMEL_DASHING, camel.isDashing());
        } else if (entity instanceof Armadillo) {
            Armadillo armadillo = (Armadillo) entity;
        } else if (entity instanceof Zombie) {
            Zombie zombie = (Zombie) entity;
            setData(DataEntityType.ZOMBIE_BABY, zombie.isBaby());
            setData(DataEntityType.ZOMBIE_CONVERTING_DROWNED, zombie.isConverting());
        } else if (entity instanceof IronGolem) {
            IronGolem golem = (IronGolem) entity;
            setData(DataEntityType.IRON_GOLEM_PLAYER_CREATED, golem.isPlayerCreated());
        } else if (entity instanceof Snowman) {
            Snowman snowman = (Snowman) entity;
            setData(DataEntityType.SNOWMAN_DERP, snowman.isDerp());
        } else if (entity instanceof Shulker) {
            Shulker shulker = (Shulker) entity;
            if (shulker.getColor() != null) {
                setData(DataEntityType.SHULKER_COLOR, shulker.getColor().name());
            }
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
            this.mainHand = eq.getItemInMainHand();
            this.offHand = eq.getItemInOffHand();
        }
    }

    private void readItemFrameData(ItemFrame frame) {
        setData(DataEntityType.ITEM_FRAME_ITEM, frame.getItem());
        setData(DataEntityType.ITEM_FRAME_ROTATION, frame.getRotation().name());
        setData(DataEntityType.ITEM_FRAME_VISIBLE, frame.isVisible());
        setData(DataEntityType.ITEM_FRAME_FIXED, frame.isFixed());

        if (frame instanceof GlowItemFrame) {
            setData(DataEntityType.ITEM_FRAME_GLOWING, true);
        }
    }

    public void applyToEntity(Entity entity) {
        if (hasData(DataEntityType.CUSTOM_NAME)) {
            entity.setCustomName((String) getData(DataEntityType.CUSTOM_NAME));
        }
        entity.setCustomNameVisible(getBoolean(DataEntityType.CUSTOM_NAME_VISIBLE));
        entity.setGlowing(getBoolean(DataEntityType.GLOWING));
        entity.setGravity(getBoolean(DataEntityType.GRAVITY));
        entity.setInvulnerable(getBoolean(DataEntityType.INVULNERABLE));
        entity.setSilent(getBoolean(DataEntityType.SILENT));

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
        if (hasData(DataEntityType.MAX_HEALTH)) {
            entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(getDouble(DataEntityType.MAX_HEALTH));
        }
        entity.setAI(getBoolean(DataEntityType.AI));
        entity.setCanPickupItems(getBoolean(DataEntityType.CAN_PICKUP_ITEMS));
        entity.setCollidable(getBoolean(DataEntityType.COLLIDABLE));
        if (hasData(DataEntityType.REMAINING_AIR)) {
            entity.setRemainingAir(getInt(DataEntityType.REMAINING_AIR));
        }

        if (entity.getEquipment() != null) {
            EntityEquipment eq = entity.getEquipment();
            if (armor != null) eq.setArmorContents(armor);
            if (mainHand != null) eq.setItemInMainHand(mainHand);
            if (offHand != null) eq.setItemInOffHand(offHand);
        }

        if (hasData(DataEntityType.POTION_EFFECTS)) {
            String effectsStr = getString(DataEntityType.POTION_EFFECTS);
            String[] effects = effectsStr.split(";");
            for (String effect : effects) {
                String[] parts = effect.split(":");
                PotionEffectType type = PotionEffectType.getByName(parts[0]);
                int duration = Integer.parseInt(parts[1]);
                int amplifier = Integer.parseInt(parts[2]);
                entity.addPotionEffect(new PotionEffect(type, duration, amplifier));
            }
        }

        if (entity instanceof Mob) {
            applyMobData((Mob) entity);
        }

        if (entity instanceof Ageable) {
            Ageable ageable = (Ageable) entity;
            if (hasData(DataEntityType.AGE)) {
                ageable.setAge(getInt(DataEntityType.AGE));
            }
            if (hasData(DataEntityType.AGE_LOCK)) {
                ageable.setAgeLock(getBoolean(DataEntityType.AGE_LOCK));
            }
        }

        if (entity instanceof Tameable) {
            Tameable tameable = (Tameable) entity;
            if (hasData(DataEntityType.TAMED)) {
                tameable.setTamed(getBoolean(DataEntityType.TAMED));
            }
            if (hasData(DataEntityType.OWNER_UUID)) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(getString(DataEntityType.OWNER_UUID)));
                tameable.setOwner(offlinePlayer);
            }
        }

        applySpecificMobData(entity);
    }

    private void applyMobData(Mob mob) {
        if (hasData(DataEntityType.AWARE)) {
            mob.setAware(getBoolean(DataEntityType.AWARE));
        }
    }

    private void applySpecificMobData(LivingEntity entity) {
        if (entity instanceof Villager) {
            Villager villager = (Villager) entity;
            if (hasData(DataEntityType.VILLAGER_PROFESSION)) {
                String typeKey = getString(DataEntityType.VILLAGER_PROFESSION);
                for (Villager.Profession profession : Villager.Profession.values()) {
                    if (profession.getKey().getKey().equals(typeKey)) {
                        villager.setProfession(profession);
                        break;
                    }
                }
            }
            if (hasData(DataEntityType.VILLAGER_TYPE)) {
                String typeKey = getString(DataEntityType.VILLAGER_TYPE);
                for (Villager.Type type : Villager.Type.values()) {
                    if (type.getKey().getKey().equals(typeKey)) {
                        Bukkit.getLogger().info("Type " + typeKey);
                        villager.setVillagerType(type);
                        break;
                    }
                }
            }
            if (hasData(DataEntityType.VILLAGER_LEVEL)) {
                villager.setVillagerLevel(getInt(DataEntityType.VILLAGER_LEVEL));
            }
            if (hasData(DataEntityType.VILLAGER_EXPERIENCE)) {
                villager.setVillagerExperience(getInt(DataEntityType.VILLAGER_EXPERIENCE));
            }

            if (hasData(DataEntityType.VILLAGER_TRADES)) {
                String tradesStr = getString(DataEntityType.VILLAGER_TRADES);
                String[] tradesArray = tradesStr.split(";;");
                List<MerchantRecipe> recipes = new ArrayList<>();

                for (String tradeData : tradesArray) {
                    String[] parts = tradeData.split("\\|");
                    ItemStack result = InventorySerializable.itemStackFromBase64(parts[0]);
                    ItemStack ingredient1 = InventorySerializable.itemStackFromBase64(parts[1]);
                    ItemStack ingredient2 = InventorySerializable.itemStackFromBase64(parts[2]);
                    int uses = Integer.parseInt(parts[3]);
                    int maxUses = Integer.parseInt(parts[4]);
                    int exp = Integer.parseInt(parts[5]);
                    float priceMultiplier = Float.parseFloat(parts[6]);

                    MerchantRecipe recipe = new MerchantRecipe(result, maxUses);
                    recipe.addIngredient(ingredient1);
                    if (ingredient2 != null) {
                        recipe.addIngredient(ingredient2);
                    }
                    recipe.setUses(uses);
                    recipe.setVillagerExperience(exp);
                    recipe.setPriceMultiplier(priceMultiplier);

                    recipes.add(recipe);
                }

                villager.setRecipes(recipes);
            }
        } else if (entity instanceof ZombieVillager) {
            ZombieVillager zVillager = (ZombieVillager) entity;
            if (hasData(DataEntityType.VILLAGER_PROFESSION)) {
                String typeKey = getString(DataEntityType.VILLAGER_PROFESSION);
                for (Villager.Profession profession : Villager.Profession.values()) {
                    if (profession.getKey().getKey().equals(typeKey)) {
                        zVillager.setVillagerProfession(profession);
                        break;
                    }
                }
            }
            if (hasData(DataEntityType.VILLAGER_TYPE)) {
                String typeKey = getString(DataEntityType.VILLAGER_TYPE);
                for (Villager.Type type : Villager.Type.values()) {
                    if (type.getKey().getKey().equals(typeKey)) {
                        zVillager.setVillagerType(type);
                        break;
                    }
                }
            }
        } else if (entity instanceof Wolf) {
            Wolf wolf = (Wolf) entity;
            if (hasData(DataEntityType.ANGRY)) {
                wolf.setAngry(getBoolean(DataEntityType.ANGRY));
            }
            if (hasData(DataEntityType.WOLF_VARIANT)) {
                String variantKey = getString(DataEntityType.WOLF_VARIANT);
                wolf.setVariant(getWolfVariant(variantKey));
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
        } else if (entity instanceof Cat) {
            Cat cat = (Cat) entity;
            if (hasData(DataEntityType.CAT_TYPE)) {
                String typeKey = getString(DataEntityType.CAT_TYPE);
                for (Cat.Type type : Cat.Type.values()) {
                    if (type.getKey().getKey().equals(typeKey)) {
                        cat.setCatType(type);
                        break;
                    }
                }
            }
            if (hasData(DataEntityType.COLLAR_COLOR) && cat.isTamed()) {
                String[] rgb = getString(DataEntityType.COLLAR_COLOR).split(",");
                Color color = Color.fromRGB(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
                for (DyeColor dyeColor : DyeColor.values()) {
                    if (dyeColor.getColor().equals(color)) {
                        cat.setCollarColor(dyeColor);
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
        } else if (entity instanceof Llama) {
            Llama llama = (Llama) entity;
            if (hasData(DataEntityType.LLAMA_COLOR)) {
                llama.setColor(Llama.Color.valueOf(getString(DataEntityType.LLAMA_COLOR)));
            }
            if (hasData(DataEntityType.LLAMA_STRENGTH)) {
                llama.setStrength(getInt(DataEntityType.LLAMA_STRENGTH));
            }
            if (hasData(DataEntityType.LLAMA_CARPET)) {
                llama.getInventory().setDecor((ItemStack) getData(DataEntityType.LLAMA_CARPET));
            }
        } else if (entity instanceof Parrot) {
            Parrot parrot = (Parrot) entity;
            if (hasData(DataEntityType.PARROT_VARIANT)) {
                parrot.setVariant(Parrot.Variant.valueOf(getString(DataEntityType.PARROT_VARIANT)));
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
            if (hasData(DataEntityType.CREEPER_MAX_FUSE)) {
                creeper.setMaxFuseTicks(getInt(DataEntityType.CREEPER_MAX_FUSE));
            }
            if (hasData(DataEntityType.CREEPER_EXPLOSION_RADIUS)) {
                creeper.setExplosionRadius(getInt(DataEntityType.CREEPER_EXPLOSION_RADIUS));
            }
        } else if (entity instanceof Slime) {
            Slime slime = (Slime) entity;
            if (hasData(DataEntityType.SLIME_SIZE)) {
                slime.setSize(getInt(DataEntityType.SLIME_SIZE));
            }
        } else if (entity instanceof Phantom) {
            Phantom phantom = (Phantom) entity;
            if (hasData(DataEntityType.PHANTOM_SIZE)) {
                phantom.setSize(getInt(DataEntityType.PHANTOM_SIZE));
            }
        } else if (entity instanceof TropicalFish) {
            TropicalFish fish = (TropicalFish) entity;
            if (hasData(DataEntityType.TROPICAL_FISH_PATTERN)) {
                fish.setPattern(TropicalFish.Pattern.valueOf(getString(DataEntityType.TROPICAL_FISH_PATTERN)));
            }
            if (hasData(DataEntityType.TROPICAL_FISH_BODY_COLOR)) {
                fish.setBodyColor(DyeColor.valueOf(getString(DataEntityType.TROPICAL_FISH_BODY_COLOR)));
            }
            if (hasData(DataEntityType.TROPICAL_FISH_PATTERN_COLOR)) {
                fish.setPatternColor(DyeColor.valueOf(getString(DataEntityType.TROPICAL_FISH_PATTERN_COLOR)));
            }
        } else if (entity instanceof Axolotl) {
            Axolotl axolotl = (Axolotl) entity;
            if (hasData(DataEntityType.AXOLOTL_VARIANT)) {
                axolotl.setVariant(Axolotl.Variant.valueOf(getString(DataEntityType.AXOLOTL_VARIANT)));
            }
        } else if (entity instanceof Bee) {
            Bee bee = (Bee) entity;
            if (hasData(DataEntityType.BEE_HAS_NECTAR)) {
                bee.setHasNectar(getBoolean(DataEntityType.BEE_HAS_NECTAR));
            }
            if (hasData(DataEntityType.BEE_HAS_STUNG)) {
                bee.setHasStung(getBoolean(DataEntityType.BEE_HAS_STUNG));
            }
            if (hasData(DataEntityType.BEE_ANGER)) {
                bee.setAnger(getInt(DataEntityType.BEE_ANGER));
            }
        } else if (entity instanceof Fox) {
            Fox fox = (Fox) entity;
            if (hasData(DataEntityType.FOX_TYPE)) {
                fox.setFoxType(Fox.Type.valueOf(getString(DataEntityType.FOX_TYPE)));
            }
            if (hasData(DataEntityType.FOX_CROUCHING)) {
                fox.setCrouching(getBoolean(DataEntityType.FOX_CROUCHING));
            }
            if (hasData(DataEntityType.FOX_SLEEPING)) {
                fox.setSleeping(getBoolean(DataEntityType.FOX_SLEEPING));
            }
        } else if (entity instanceof Panda) {
            Panda panda = (Panda) entity;
            if (hasData(DataEntityType.PANDA_MAIN_GENE)) {
                panda.setMainGene(Panda.Gene.valueOf(getString(DataEntityType.PANDA_MAIN_GENE)));
            }
            if (hasData(DataEntityType.PANDA_HIDDEN_GENE)) {
                panda.setHiddenGene(Panda.Gene.valueOf(getString(DataEntityType.PANDA_HIDDEN_GENE)));
            }
        } else if (entity instanceof Piglin) {
            Piglin piglin = (Piglin) entity;
            if (hasData(DataEntityType.PIGLIN_IMMUNE_TO_ZOMBIFICATION)) {
                piglin.setImmuneToZombification(getBoolean(DataEntityType.PIGLIN_IMMUNE_TO_ZOMBIFICATION));
            }
            if (hasData(DataEntityType.PIGLIN_BABY)) {
                piglin.setBaby(getBoolean(DataEntityType.PIGLIN_BABY));
            }
        } else if (entity instanceof Hoglin) {
            Hoglin hoglin = (Hoglin) entity;
            if (hasData(DataEntityType.HOGLIN_IMMUNE_TO_ZOMBIFICATION)) {
                hoglin.setImmuneToZombification(getBoolean(DataEntityType.HOGLIN_IMMUNE_TO_ZOMBIFICATION));
            }
        } else if (entity instanceof Strider) {
            Strider strider = (Strider) entity;
            if (hasData(DataEntityType.STRIDER_SADDLED)) {
                strider.setSaddle(getBoolean(DataEntityType.STRIDER_SADDLED));
            }
        } else if (entity instanceof Goat) {
            Goat goat = (Goat) entity;
            if (hasData(DataEntityType.GOAT_SCREAMING)) {
                goat.setScreaming(getBoolean(DataEntityType.GOAT_SCREAMING));
            }
        } else if (entity instanceof Frog) {
            Frog frog = (Frog) entity;
            if (hasData(DataEntityType.FROG_VARIANT)) {
                String variantKey = getString(DataEntityType.FROG_VARIANT);
                for (Frog.Variant variant : Frog.Variant.values()) {
                    if (variant.getKey().getKey().equals(variantKey)) {
                        frog.setVariant(variant);
                        break;
                    }
                }
            }
        } else if (entity instanceof Allay) {
            Allay allay = (Allay) entity;
            if (hasData(DataEntityType.ALLAY_CAN_DUPLICATE)) {
                allay.setCanDuplicate(getBoolean(DataEntityType.ALLAY_CAN_DUPLICATE));
            }
        } else if (entity instanceof Warden) {
            Warden warden = (Warden) entity;

        } else if (entity instanceof Sniffer) {
            Sniffer sniffer = (Sniffer) entity;
            if (hasData(DataEntityType.SNIFFER_STATE)) {
                sniffer.setState(Sniffer.State.valueOf(getString(DataEntityType.SNIFFER_STATE)));
            }
        } else if (entity instanceof Camel) {
            Camel camel = (Camel) entity;
            if (hasData(DataEntityType.CAMEL_DASHING)) {
                camel.setDashing(getBoolean(DataEntityType.CAMEL_DASHING));
            }
        } else if (entity instanceof Armadillo) {
            Armadillo armadillo = (Armadillo) entity;

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
        } else if (entity instanceof Snowman) {
            Snowman snowman = (Snowman) entity;
            if (hasData(DataEntityType.SNOWMAN_DERP)) {
                snowman.setDerp(getBoolean(DataEntityType.SNOWMAN_DERP));
            }
        } else if (entity instanceof Shulker) {
            Shulker shulker = (Shulker) entity;
            if (hasData(DataEntityType.SHULKER_COLOR)) {
                shulker.setColor(DyeColor.valueOf(getString(DataEntityType.SHULKER_COLOR)));
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
            if (mainHand != null) eq.setItemInMainHand(mainHand);
            if (offHand != null) eq.setItemInOffHand(offHand);
        }
    }

    private void applyItemFrameData(ItemFrame frame) {
        if (hasData(DataEntityType.ITEM_FRAME_ITEM)) {
            frame.setItem((ItemStack) getData(DataEntityType.ITEM_FRAME_ITEM));
        }
        if (hasData(DataEntityType.ITEM_FRAME_ROTATION)) {
            frame.setRotation(Rotation.valueOf(getString(DataEntityType.ITEM_FRAME_ROTATION)));
        }
        if (hasData(DataEntityType.ITEM_FRAME_VISIBLE)) {
            frame.setVisible(getBoolean(DataEntityType.ITEM_FRAME_VISIBLE));
        }
        if (hasData(DataEntityType.ITEM_FRAME_FIXED)) {
            frame.setFixed(getBoolean(DataEntityType.ITEM_FRAME_FIXED));
        }
    }

    public Wolf.Variant getWolfVariant(String key) {
        switch (key) {
            case "pale":
                return Wolf.Variant.PALE;
            case "spotted":
                return Wolf.Variant.SPOTTED;
            case "snowy":
                return Wolf.Variant.SNOWY;
            case "black":
                return Wolf.Variant.BLACK;
            case "ashen":
                return Wolf.Variant.ASHEN;
            case "rusty":
                return Wolf.Variant.RUSTY;
            case "woods":
                return Wolf.Variant.WOODS;
            case "chestnut":
                return Wolf.Variant.CHESTNUT;
            default:
                return Wolf.Variant.STRIPED;
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