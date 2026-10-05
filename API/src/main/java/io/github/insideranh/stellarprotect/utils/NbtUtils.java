package io.github.insideranh.stellarprotect.utils;

import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTReflectionUtil;
import org.bukkit.block.TileState;

import java.util.function.Consumer;

public final class NbtUtils {

    private NbtUtils() {
    }

    public static void writeBlockEntity(TileState ts, Consumer<String> sink) {
        try {
            NBTContainer nbt = new NBTContainer();
            NBTReflectionUtil.setEntityNBTTag(ts, nbt);
            sink.accept(nbt.toString());
        } catch (Throwable ignored) {
        }
    }

    public static void applyBlockEntity(TileState ts, String nbtJson) {
        if (nbtJson == null || nbtJson.isEmpty()) return;
        try {
            NBTContainer nbt = new NBTContainer(nbtJson);
            NBTReflectionUtil.setEntityNBTTag(ts, nbt);
            ts.update(true, false);
        } catch (Throwable ignored) {
        }
    }

}