package common.cn.kafei.simukraft.farmland;

import com.mojang.serialization.Codec;
import common.cn.kafei.simukraft.SimuKraft;
import common.cn.kafei.simukraft.storage.SimuSqliteStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 农田盒配置存储。主持久化是 SQLite（farmland_boxes 表），SavedData 作为兼容兜底。
 * 写法对齐 {@code CityPoiManager}：get(level) 时从 SQLite 懒加载，增量改动 upsert/delete，
 * 周期性整表 saveToSqlite 由服务端 tick/关服流程调用。
 */


public final class FarmlandBoxManager extends SavedData {
    private static final Identifier DATA_ID = Identifier.fromNamespaceAndPath(SimuKraft.MOD_ID, "farmland_boxes");
    private static final SavedDataType<FarmlandBoxManager> TYPE = createType();

    @SuppressWarnings("unchecked")
    private static SavedDataType<FarmlandBoxManager> createType() {
        Codec<FarmlandBoxManager> codec = CompoundTag.CODEC.xmap(FarmlandBoxManager::load, FarmlandBoxManager::serializeToTag);
        return new SavedDataType<>(DATA_ID, FarmlandBoxManager::new, codec);
    }

    private CompoundTag serializeToTag() {
        return save(new CompoundTag());
    }

    private final ConcurrentMap<BlockPos, FarmlandBoxData> boxes = new ConcurrentHashMap<>();
    private volatile boolean sqliteLoaded;
    private volatile ServerLevel level;

    public static FarmlandBoxManager get(ServerLevel level) {
        FarmlandBoxManager manager = level.getDataStorage().computeIfAbsent(TYPE);
        manager.level = level;
        manager.loadFromSqlite(level);
        return manager;
    }

    private static FarmlandBoxManager load(CompoundTag tag) {
        FarmlandBoxManager manager = new FarmlandBoxManager();
        ListTag list = tag.getList("Boxes").orElse(new ListTag());
        for (int i = 0; i < list.size(); i++) {
            FarmlandBoxData data = FarmlandBoxData.fromTag(list.getCompound(i).orElse(new CompoundTag()));
            manager.boxes.put(data.boxPos(), data);
        }
        return manager;
    }

    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        boxes.values().forEach(data -> list.add(data.toTag()));
        tag.put("Boxes", list);
        return tag;
    }

    public synchronized void saveToSqlite(ServerLevel level) {
        if (level != null) {
            SimuSqliteStorage.saveFarmlandBoxes(level, save(new CompoundTag()));
        }
    }

    public synchronized void reloadFromSqlite(ServerLevel level) {
        boxes.clear();
        sqliteLoaded = false;
        loadFromSqlite(level);
    }

    private synchronized void loadFromSqlite(ServerLevel level) {
        if (sqliteLoaded) {
            return;
        }
        sqliteLoaded = true;
        CompoundTag sqliteTag = SimuSqliteStorage.loadFarmlandBoxes(level);
        if (sqliteTag == null || sqliteTag.isEmpty()) {
            return;
        }
        FarmlandBoxManager loaded = load(sqliteTag);
        boxes.clear();
        boxes.putAll(loaded.boxes);
    }

    public FarmlandBoxData get(BlockPos boxPos) {
        return boxPos == null ? null : boxes.get(boxPos.immutable());
    }

    public FarmlandBoxData getOrCreate(BlockPos boxPos) {
        return boxes.computeIfAbsent(boxPos.immutable(), FarmlandBoxData::new);
    }

    // 配置改动后调用：内存更新 + SavedData 脏标记 + SQLite 增量写。
    // 写入的合并与排序由存储层写队列负责，upsert 与 remove 走同一条队列。
    public void persist(FarmlandBoxData data) {
        if (data == null) return;
        boxes.put(data.boxPos(), data);
        setDirty();
        ServerLevel lv = level;
        if (lv == null) return;
        SimuSqliteStorage.saveFarmlandBox(lv, data.toTag());
    }

    public void remove(BlockPos boxPos) {
        if (boxPos == null) {
            return;
        }
        BlockPos key = boxPos.immutable();
        if (boxes.remove(key) != null) {
            setDirty();
            if (level != null) {
                SimuSqliteStorage.deleteFarmlandBox(level, key.asLong());
            }
        }
    }

    public List<FarmlandBoxData> all() {
        return List.copyOf(boxes.values());
    }
}
