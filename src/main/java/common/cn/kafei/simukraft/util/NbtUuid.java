package common.cn.kafei.simukraft.util;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;


/**
 * 26.3 起 CompoundTag 不再提供 putUUID/getUUID/hasUUID。
 * UUID 以 int[4] 写入（与旧版 putUUID 二进制兼容），读取同时接受 int 数组和字符串。
 */
public final class NbtUuid {
    private NbtUuid() {
    }

    public static void put(CompoundTag tag, String key, UUID uuid) {
        if (tag == null || key == null || uuid == null) {
            return;
        }
        tag.store(key, UUIDUtil.CODEC, uuid);
    }

    public static UUID readOrNull(CompoundTag tag, String key) {
        if (tag == null || key == null) {
            return null;
        }
        return tag.read(key, UUIDUtil.LENIENT_CODEC).orElse(null);
    }

    public static UUID read(CompoundTag tag, String key) {
        UUID uuid = readOrNull(tag, key);
        if (uuid == null) {
            throw new IllegalArgumentException("Missing UUID tag: " + key);
        }
        return uuid;
    }

    public static boolean has(CompoundTag tag, String key) {
        return readOrNull(tag, key) != null;
    }

    public static String toStringOrNull(CompoundTag tag, String key) {
        UUID uuid = readOrNull(tag, key);
        return uuid == null ? null : uuid.toString();
    }

    public static String toStringOrEmpty(CompoundTag tag, String key) {
        UUID uuid = readOrNull(tag, key);
        return uuid == null ? "" : uuid.toString();
    }
}
