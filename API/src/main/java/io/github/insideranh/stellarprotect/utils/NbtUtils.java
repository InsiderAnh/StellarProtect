package io.github.insideranh.stellarprotect.utils;

import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTTileEntity;
import org.bukkit.Bukkit;
import org.bukkit.block.TileState;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class NbtUtils {

    private static final AtomicLong lastWarning = new AtomicLong();

    private NbtUtils() {
    }

    private static void warn(String operation, Throwable error) {
        long now = System.currentTimeMillis();
        long previous = lastWarning.get();
        if (now - previous >= 60000L && lastWarning.compareAndSet(previous, now)) {
            Bukkit.getLogger().warning("[StellarProtect] Block NBT " + operation + " failed: " + error.getMessage());
        }
    }

    public static void writeBlockEntity(TileState ts, Consumer<String> sink) {
        if (ts == null) return;
        try {
            NBTTileEntity tileEntity = new NBTTileEntity(ts);
            String nbt = tileEntity.toString();
            if (nbt != null && !nbt.isEmpty() && !nbt.equals("{}")) {
                sink.accept(nbt);
            }
        } catch (Throwable error) {
            warn("capture", error);
        }
    }

    public static void applyBlockEntity(TileState ts, String nbtJson) {
        if (ts == null || nbtJson == null || nbtJson.isEmpty() || nbtJson.equals("{}")) return;
        try {
            NBTTileEntity tileEntity = new NBTTileEntity(ts);
            tileEntity.mergeCompound(new NBTContainer(nbtJson));
            ts.update(true, false);
        } catch (Throwable error) {
            warn("restore", error);
        }
    }

}
