package io.github.insideranh.stellarprotect;

import com.cjcrafter.foliascheduler.util.MinecraftVersions;
import com.cjcrafter.foliascheduler.util.ServerVersions;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.github.insideranh.stellarprotect.api.ColorUtils;
import io.github.insideranh.stellarprotect.api.ProtectNMS;
import io.github.insideranh.stellarprotect.api.events.DecorativeLogicHandler;
import io.github.insideranh.stellarprotect.api.events.EventLogicHandler;
import io.github.insideranh.stellarprotect.blocks.DataBlock;
import io.github.insideranh.stellarprotect.blocks.adjacents.AdjacentType;
import io.github.insideranh.stellarprotect.bstats.MetricsLite;
import io.github.insideranh.stellarprotect.commands.StellarProtectCMD;
import io.github.insideranh.stellarprotect.database.ProtectDatabase;
import io.github.insideranh.stellarprotect.enums.MinecraftVersion;
import io.github.insideranh.stellarprotect.hooks.ShopGUIHookListener;
import io.github.insideranh.stellarprotect.hooks.StellarTaskHook;
import io.github.insideranh.stellarprotect.hooks.XPlayerKitsListener;
import io.github.insideranh.stellarprotect.hooks.nexo.NexoDefaultHook;
import io.github.insideranh.stellarprotect.hooks.nexo.NexoHook;
import io.github.insideranh.stellarprotect.hooks.nexo.NexoHookListener;
import io.github.insideranh.stellarprotect.hooks.tasks.BukkitTaskHook;
import io.github.insideranh.stellarprotect.hooks.tasks.FoliaTaskHook;
import io.github.insideranh.stellarprotect.inspect.InspectHandler;
import io.github.insideranh.stellarprotect.listeners.*;
import io.github.insideranh.stellarprotect.listeners.blocks.CropGrowListener;
import io.github.insideranh.stellarprotect.listeners.versions.DecorativeEventHandler;
import io.github.insideranh.stellarprotect.listeners.versions.EventVersionHandler;
import io.github.insideranh.stellarprotect.managers.*;
import io.github.insideranh.stellarprotect.restore.BlockRestore;
import io.github.insideranh.stellarprotect.trackers.ChestTransactionTracker;
import io.github.insideranh.stellarprotect.utils.UpdateChecker;
import lombok.Getter;
import lombok.SneakyThrows;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
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
    private final TrackManager trackManager;
    private final EconomyManager economyManager;
    private final BlocksManager blocksManager;
    private final ProtectDatabase protectDatabase;
    private final HooksManager hooksManager;
    private final InspectHandler inspectHandler;
    private final EventLogicHandler eventLogicHandler;
    private final DecorativeLogicHandler decorativeLogicHandler;
    private NexoDefaultHook nexoHook = new NexoDefaultHook();
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
    private Economy economy;
    private boolean isFolia;

    public StellarProtect() {
        instance = this;
        this.configManager = new ConfigManager();
        this.langManager = new LangManager();
        this.cacheManager = new CacheManager();
        this.itemsManager = new ItemsManager();
        this.restoreManager = new RestoreManager();
        this.trackManager = new TrackManager();
        this.blocksManager = new BlocksManager();
        this.hooksManager = new HooksManager();
        this.economyManager = new EconomyManager();
        this.protectDatabase = new ProtectDatabase();
        this.inspectHandler = new InspectHandler();
        this.eventLogicHandler = new EventVersionHandler();
        this.decorativeLogicHandler = new DecorativeEventHandler();
    }

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();

        isFolia = MinecraftVersions.WILD_UPDATE.isAtLeast() && ServerVersions.isFolia();

        this.lookupExecutor = MoreExecutors.listeningDecorator(new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1024)));
        this.joinExecutor = MoreExecutors.listeningDecorator(new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1024)));

        this.lookupExecutor.execute(AdjacentType::initializeCache);

        this.configManager.load();
        this.hooksManager.load();

        this.protectDatabase.connect();

        setupEconomy();
        reload();

        this.executor = MoreExecutors.listeningDecorator(new ThreadPoolExecutor(configManager.getMaxCores(), configManager.getMaxCores(), 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1024)));

        this.protectDatabase.load();
        this.cacheManager.load();

        this.chestTransactionTracker = new ChestTransactionTracker();

        for (Listener listener : getListeners()) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
        getServer().getPluginManager().registerEvents(chestTransactionTracker, this);

        getCommand("stellarprotect").setExecutor(new StellarProtectCMD());

        bStats = new MetricsLite(this, 26624);

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
        this.economyManager.load();
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
            if (hooksManager.isXPlayerKitsHook() && getServer().getPluginManager().isPluginEnabled("XPlayerKits")) {
                Bukkit.getPluginManager().registerEvents(new XPlayerKitsListener(), this);
                getLogger().info("XPlayerKits detected, enabling XPlayerKits hook...");
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
            new EntityListener()));
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
        } else if (localVersion.equals(MinecraftVersion.v1_9)) {
            this.completer = "v1_9_R4";
        } else if (localVersion.equals(MinecraftVersion.v1_12)) {
            this.completer = "v1_12_R2";
        } else if (localVersion.equals(MinecraftVersion.v1_13)) {
            this.completer = "v1_13_R2";
        } else if (localVersion.equals(MinecraftVersion.v1_16)) {
            this.completer = "v1_16_R5";
        } else if (localVersion.equals(MinecraftVersion.v1_17)) {
            this.completer = "v1_17_R1";
        } else {
            this.completer = localVersion.name();
        }

        getLogger().info("Loaded " + completer + " version.");

        this.protectNMS = Class.forName("io.github.insideranh.stellarprotect.nms." + completer + ".ProtectNMS_" + completer).asSubclass(ProtectNMS.class).getConstructor().newInstance();
        this.colorUtils = Class.forName("io.github.insideranh.stellarprotect.nms." + completer + ".ColorUtils_" + completer).asSubclass(ColorUtils.class).getConstructor().newInstance();

        Listener listener = Class.forName("io.github.insideranh.stellarprotect.nms." + completer + ".listeners.BlockListener_" + completer).asSubclass(Listener.class).getConstructor(EventLogicHandler.class).newInstance(this.eventLogicHandler);
        getServer().getPluginManager().registerEvents(listener, this);
    }

    @SneakyThrows
    public BlockRestore getBlockRestore(String data) {
        if (completer == null) {
            loadNMS();
        }
        return Class.forName("io.github.insideranh.stellarprotect.nms." + completer + ".BlockRestore_" + completer).asSubclass(BlockRestore.class).getConstructor(String.class).newInstance(data);
    }

    @SneakyThrows
    public DataBlock getDataBlock(Block block) {
        if (completer == null) {
            loadNMS();
        }
        return Class.forName("io.github.insideranh.stellarprotect.nms." + completer + ".DataBlock_" + completer).asSubclass(DataBlock.class).getConstructor(Block.class).newInstance(block);
    }

    @SneakyThrows
    public DataBlock getDataBlock(String blockDataString) {
        if (completer == null) {
            loadNMS();
        }
        return Class.forName("io.github.insideranh.stellarprotect.nms." + completer + ".DataBlock_" + completer).asSubclass(DataBlock.class).getConstructor(String.class).newInstance(blockDataString);
    }

    public StellarTaskHook getStellarTaskHook(Runnable runnable) {
        if (isFolia) {
            return new FoliaTaskHook(runnable);
        }
        return new BukkitTaskHook(runnable);
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return;
        }

        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider == null) {
            return;
        }

        economy = economyProvider.getProvider();
    }

}