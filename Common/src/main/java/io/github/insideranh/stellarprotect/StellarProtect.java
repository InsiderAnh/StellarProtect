package io.github.insideranh.stellarprotect;

import com.cjcrafter.foliascheduler.util.MinecraftVersions;
import com.cjcrafter.foliascheduler.util.ServerVersions;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.github.insideranh.stellarprotect.api.ColorUtils;
import io.github.insideranh.stellarprotect.api.ItemsProviderRegistry;
import io.github.insideranh.stellarprotect.api.ProtectNMS;
import io.github.insideranh.stellarprotect.api.events.DecorativeLogicHandler;
import io.github.insideranh.stellarprotect.api.events.EventLogicHandler;
import io.github.insideranh.stellarprotect.blocks.DataBlock;
import io.github.insideranh.stellarprotect.blocks.adjacents.AdjacentType;
import io.github.insideranh.stellarprotect.bstats.MetricsLite;
import io.github.insideranh.stellarprotect.commands.StellarProtectCMD;
import io.github.insideranh.stellarprotect.database.ProtectDatabase;
import io.github.insideranh.stellarprotect.entities.DataEntity;
import io.github.insideranh.stellarprotect.enums.MinecraftVersion;
import io.github.insideranh.stellarprotect.hooks.*;
import io.github.insideranh.stellarprotect.hooks.itemsadder.ItemsAdderHook;
import io.github.insideranh.stellarprotect.hooks.itemsadder.ItemsAdderHookListener;
import io.github.insideranh.stellarprotect.hooks.nexo.NexoDefaultHook;
import io.github.insideranh.stellarprotect.hooks.nexo.NexoHook;
import io.github.insideranh.stellarprotect.hooks.nexo.NexoHookListener;
import io.github.insideranh.stellarprotect.hooks.tasks.BukkitTaskHook;
import io.github.insideranh.stellarprotect.hooks.tasks.FoliaTaskHook;
import io.github.insideranh.stellarprotect.hooks.vault.DefaultVaultHook;
import io.github.insideranh.stellarprotect.hooks.vault.VaultHook;
import io.github.insideranh.stellarprotect.inspect.InspectHandler;
import io.github.insideranh.stellarprotect.listeners.*;
import io.github.insideranh.stellarprotect.listeners.blocks.CropGrowListener;
import io.github.insideranh.stellarprotect.listeners.versions.DecorativeEventHandler;
import io.github.insideranh.stellarprotect.listeners.versions.EventVersionHandler;
import io.github.insideranh.stellarprotect.managers.*;
import io.github.insideranh.stellarprotect.nms.v1_12_R2.ColorUtils_v1_12_R2;
import io.github.insideranh.stellarprotect.nms.v1_12_R2.ProtectNMS_v1_12_R2;
import io.github.insideranh.stellarprotect.nms.v1_13_R2.ColorUtils_v1_13_R2;
import io.github.insideranh.stellarprotect.nms.v1_13_R2.ProtectNMS_v1_13_R2;
import io.github.insideranh.stellarprotect.nms.v1_16_R5.ColorUtils_v1_16_R5;
import io.github.insideranh.stellarprotect.nms.v1_16_R5.ProtectNMS_v1_16_R5;
import io.github.insideranh.stellarprotect.nms.v1_17_R1.ProtectNMS_v1_17_R1;
import io.github.insideranh.stellarprotect.nms.v1_8_R3.ColorUtils_v1_8_R3;
import io.github.insideranh.stellarprotect.nms.v1_8_R3.ProtectNMS_v1_8_R3;
import io.github.insideranh.stellarprotect.nms.v1_9_R4.ColorUtils_v1_9_R4;
import io.github.insideranh.stellarprotect.nms.v1_9_R4.ProtectNMS_v1_9_R4;
import io.github.insideranh.stellarprotect.providers.ItemsProviderImpl;
import io.github.insideranh.stellarprotect.restore.BlockRestore;
import io.github.insideranh.stellarprotect.trackers.BlockTracker;
import io.github.insideranh.stellarprotect.trackers.ChestTransactionTracker;
import io.github.insideranh.stellarprotect.utils.UpdateChecker;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Getter
public class StellarProtect extends JavaPlugin {

    @Getter
    private static StellarProtect instance;
    private final ConfigManager configManager;
    private final LangManager langManager;
    private final CacheManager cacheManager;
    private final ItemsManager itemsManager;
    private final RestoreManager restoreManager;
    private final RestoreSessionManager restoreSessionManager;
    private final UndoManager undoManager;
    private final UndoSessionManager undoSessionManager;
    private final ChestRollbackSessionManager chestRollbackSessionManager;
    private final TrackManager trackManager;
    private final BlocksManager blocksManager;
    private final ProtectDatabase protectDatabase;
    private final HooksManager hooksManager;
    private final InspectHandler inspectHandler;
    private final EventLogicHandler eventLogicHandler;
    private final DecorativeLogicHandler decorativeLogicHandler;
    @Nullable
    private NexoDefaultHook nexoHook;
    @Nullable
    private ItemsAdderHook itemsAdderHook;
    @Nullable
    private WorldEditHook worldEditHook;
    private DefaultVaultHook vaultHook = new DefaultVaultHook();
    private ChestTransactionTracker chestTransactionTracker;
    private ListeningExecutorService executor;
    private ListeningExecutorService lookupExecutor;
    private ListeningExecutorService joinExecutor;
    private ProtectNMS protectNMS;
    private ColorUtils colorUtils;
    private String version;
    private MinecraftVersion localVersion;
    private String completer;
    private MetricsLite bStats;
    private UpdateChecker updateChecker;
    private boolean isFolia;

    public StellarProtect() {
        instance = this;
        this.configManager = new ConfigManager();
        this.langManager = new LangManager();
        this.cacheManager = new CacheManager();
        this.itemsManager = new ItemsManager();
        this.restoreManager = new RestoreManager();
        this.restoreSessionManager = new RestoreSessionManager();
        this.undoManager = new UndoManager();
        this.undoSessionManager = new UndoSessionManager();
        this.chestRollbackSessionManager = new ChestRollbackSessionManager();
        this.trackManager = new TrackManager();
        this.blocksManager = new BlocksManager();
        this.hooksManager = new HooksManager();
        this.protectDatabase = new ProtectDatabase();
        this.inspectHandler = new InspectHandler();
        this.eventLogicHandler = new EventVersionHandler();
        this.decorativeLogicHandler = new DecorativeEventHandler();
    }

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();

        this.isFolia = MinecraftVersions.WILD_UPDATE.isAtLeast() && ServerVersions.isFolia();
        this.loadNMS();

        this.lookupExecutor = MoreExecutors.listeningDecorator(new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1024)));
        this.joinExecutor = MoreExecutors.listeningDecorator(new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1024)));

        this.lookupExecutor.execute(() -> {
            AdjacentType.initializeCache();
            BlockTracker.initializeCache();
        });

        this.configManager.load();
        this.hooksManager.load();

        this.protectDatabase.connect();

        if (getServer().getPluginManager().isPluginEnabled("Vault")) {
            this.vaultHook = new VaultHook();
            this.vaultHook.setupEconomy();

            getLogger().info("Vault detected, enabling Vault hook...");
        }
        reload();

        this.executor = MoreExecutors.listeningDecorator(new ThreadPoolExecutor(configManager.getMaxCores(), configManager.getMaxCores(), 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1024)));

        this.protectDatabase.load();
        this.cacheManager.load();

        ItemsProviderRegistry.register(new ItemsProviderImpl(this));

        this.chestTransactionTracker = new ChestTransactionTracker();

        for (Listener listener : getListeners()) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
        getServer().getPluginManager().registerEvents(chestTransactionTracker, this);

        getCommand("stellarprotect").setExecutor(new StellarProtectCMD());

        bStats = new MetricsLite(this, 26624);
        bStats.addCustomChart(new MetricsLite.SimplePie("databases", () -> getConfig().getString("databases.type", "h2").toLowerCase()));

        loadLastHooks();

        if (!configManager.isCheckUpdates()) return;
        updateChecker = new UpdateChecker();
    }

    public void reload() {
        this.reloadConfig();
        this.hooksManager.load();
        this.configManager.load();
        this.langManager.load();
        this.itemsManager.load();
        this.blocksManager.load();
        this.cacheManager.load();
        this.vaultHook.load();
        this.trackManager.load();
    }

    @Override
    public void onDisable() {
        this.protectDatabase.close();
        this.bStats.shutdown();
    }

    void loadLastHooks() {
        getStellarTaskHook(() -> {
            if (hooksManager.isShopGuiHook() && getServer().getPluginManager().isPluginEnabled("ShopGUIPlus")) {
                Bukkit.getPluginManager().registerEvents(new ShopGUIHookListener(), this);
                getLogger().info("ShopGUIPlus detected, enabling ShopGUIPlus hook...");
            }
            if (hooksManager.isNexoHook() && getServer().getPluginManager().isPluginEnabled("Nexo")) {
                this.nexoHook = new NexoHook();
                Bukkit.getPluginManager().registerEvents(new NexoHookListener(), this);
                getLogger().info("Nexo detected, enabling Nexo hook...");
            }
            if (hooksManager.isItemsAdderHook() && getServer().getPluginManager().isPluginEnabled("ItemsAdder")) {
                this.itemsAdderHook = new ItemsAdderHook();
                Bukkit.getPluginManager().registerEvents(new ItemsAdderHookListener(), this);
                getLogger().info("ItemsAdder detected, enabling ItemsAdder hook...");
            }
            if (hooksManager.isXPlayerKitsHook() && getServer().getPluginManager().isPluginEnabled("XPlayerKits")) {
                Bukkit.getPluginManager().registerEvents(new XPlayerKitsListener(), this);
                getLogger().info("XPlayerKits detected, enabling XPlayerKits hook...");
            }
            if (hooksManager.isTreeCuterHook() && getServer().getPluginManager().isPluginEnabled("TreeCuter")) {
                Bukkit.getPluginManager().registerEvents(new TreeCuterListener(), this);
                getLogger().info("TreeCuter detected, enabling TreeCuter hook...");
            }
            if (hooksManager.isWorldEditHook() && getServer().getPluginManager().isPluginEnabled("WorldEdit")) {
                this.worldEditHook = new WorldEditHook();
                getLogger().info("WorldEdit detected, enabling WorldEdit hook...");
            }
        }).runTask(10);
    }

    HashSet<Listener> getListeners() {
        return new HashSet<>(Arrays.asList(
            new BlockFormListener(), new SignListener(),
            new ExplodeListener(), new BucketListener(),
            new BlockListener(), new CropGrowListener(),
            new JoinQuitListener(), new InspectListener(),
            new CraftListener(), new ChatListener(),
            new PickUpDropListener(), new PlayerLogListener(),
            new EntityListener(), new InventoryRollbackListener()));
    }

    public ProtectNMS getProtectNMS() {
        if (protectNMS == null) {
            loadNMS();
        }
        return protectNMS;
    }

    public ColorUtils getColorUtils() {
        if (colorUtils == null) {
            loadNMS();
        }
        return colorUtils;
    }

    @SneakyThrows
    public void loadNMS() {
        String cbPackage = Bukkit.getServer().getClass().getPackage().getName();
        String detectedVersion = cbPackage.substring(cbPackage.lastIndexOf('.') + 1);
        if (!detectedVersion.startsWith("v")) {
            detectedVersion = Bukkit.getServer().getBukkitVersion();
        }

        version = detectedVersion;

        getLogger().info("Detected Minecraft version: " + version);

        localVersion = MinecraftVersion.get(version);
        if (localVersion == null) {
            Bukkit.getLogger().warning("[StellarProtect] No found Minecraft version " + version + ". If you want to support this version, contact InsiderAnh.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (localVersion.equals(MinecraftVersion.v1_8)) {
            this.completer = "v1_8_R3";
            this.protectNMS = new ProtectNMS_v1_8_R3();
            this.colorUtils = new ColorUtils_v1_8_R3();
        } else if (localVersion.equals(MinecraftVersion.v1_9)) {
            this.completer = "v1_9_R4";
            this.protectNMS = new ProtectNMS_v1_9_R4();
            this.colorUtils = new ColorUtils_v1_9_R4();
        } else if (localVersion.equals(MinecraftVersion.v1_12)) {
            this.completer = "v1_12_R2";
            this.protectNMS = new ProtectNMS_v1_12_R2();
            this.colorUtils = new ColorUtils_v1_12_R2();
        } else if (localVersion.equals(MinecraftVersion.v1_13)) {
            this.completer = "v1_13_R2";
            this.protectNMS = new ProtectNMS_v1_13_R2();
            this.colorUtils = new ColorUtils_v1_13_R2();
        } else if (localVersion.equals(MinecraftVersion.v1_16)) {
            this.completer = "v1_16_R5";
            this.protectNMS = new ProtectNMS_v1_16_R5();
            this.colorUtils = new ColorUtils_v1_16_R5();
        } else if (localVersion.equals(MinecraftVersion.v1_17)) {
            this.completer = "v1_17_R1";
            this.protectNMS = new ProtectNMS_v1_17_R1();
            this.colorUtils = new ColorUtils_v1_16_R5();
        } else {
            this.completer = localVersion.name();
            this.protectNMS = new ProtectNMS_v1_17_R1();
            this.colorUtils = new ColorUtils_v1_16_R5();
        }

        getLogger().info("Loaded " + completer + " version.");

        Listener listener = Class.forName("io.github.insideranh.stellarprotect.nms." + completer + ".listeners.BlockListener_" + completer).asSubclass(Listener.class).getConstructor(EventLogicHandler.class).newInstance(this.eventLogicHandler);
        getServer().getPluginManager().registerEvents(listener, this);
    }

    @SneakyThrows
    public BlockRestore getBlockRestore(String data) {
        return Class.forName("io.github.insideranh.stellarprotect.nms." + completer + ".BlockRestore_" + completer).asSubclass(BlockRestore.class).getConstructor(String.class).newInstance(data);
    }

    @SneakyThrows
    public BlockRestore getBlockRestore(String data, byte extraType, String extraData) {
        return Class.forName("io.github.insideranh.stellarprotect.nms." + completer + ".BlockRestore_" + completer).asSubclass(BlockRestore.class).getConstructor(String.class, byte.class, String.class).newInstance(data, extraType, extraData);
    }

    @SneakyThrows
    public DataBlock getDataBlock(Block block) {
        return Class.forName("io.github.insideranh.stellarprotect.nms." + completer + ".DataBlock_" + completer).asSubclass(DataBlock.class).getConstructor(Block.class).newInstance(block);
    }

    @SneakyThrows
    public DataBlock getDataBlock(BlockState blockState) {
        return Class.forName("io.github.insideranh.stellarprotect.nms." + completer + ".DataBlock_" + completer).asSubclass(DataBlock.class).getConstructor(BlockState.class).newInstance(blockState);
    }

    @SneakyThrows
    public DataBlock getDataBlock(String blockDataString) {
        return Class.forName("io.github.insideranh.stellarprotect.nms." + completer + ".DataBlock_" + completer).asSubclass(DataBlock.class).getConstructor(String.class).newInstance(blockDataString);
    }

    @SneakyThrows
    public DataEntity getDataEntity(Entity entity) {
        return Class.forName("io.github.insideranh.stellarprotect.nms." + completer + ".DataEntity_" + completer).asSubclass(DataEntity.class).getConstructor(Entity.class).newInstance(entity);
    }

    @SneakyThrows
    public DataEntity getDataEntity(HashMap<String, Object> map) {
        return Class.forName("io.github.insideranh.stellarprotect.nms." + completer + ".DataEntity_" + completer).asSubclass(DataEntity.class).getConstructor(HashMap.class).newInstance(map);
    }

    public StellarTaskHook getStellarTaskHook(Runnable runnable) {
        if (isFolia) {
            return new FoliaTaskHook(runnable);
        }
        return new BukkitTaskHook(runnable);
    }

}