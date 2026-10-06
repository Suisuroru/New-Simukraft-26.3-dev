package common.cn.kafei.simukraft.city.poi;

import common.cn.kafei.simukraft.util.NbtUuid;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;


public record CityPoiData(UUID poiId, UUID cityId, BlockPos pos, CityPoiType type, int capacity, boolean active,
                          UUID unitId) {
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        NbtUuid.put(tag, "PoiId", poiId);
        NbtUuid.put(tag, "CityId", cityId);
        tag.putLong("Pos", pos.asLong());
        tag.putString("Type", type.name());
        tag.putInt("Capacity", capacity);
        tag.putBoolean("Active", active);
        if (unitId != null) NbtUuid.put(tag, "UnitId", unitId);
        return tag;
    }

    public static CityPoiData fromTag(CompoundTag tag) {
        return new CityPoiData(
                NbtUuid.readOrNull(tag, "PoiId"),
                NbtUuid.readOrNull(tag, "CityId"),
                BlockPos.of(tag.getLong("Pos").get()),
                CityPoiType.fromName(tag.getString("Type").get()),
                tag.getInt("Capacity").get(),
                tag.getBoolean("Active").get(),
                tag.contains("UnitId") ? NbtUuid.readOrNull(tag, "UnitId") : null
        );
    }

    public CityPoiData withActive(boolean active) {
        return new CityPoiData(poiId, cityId, pos, type, capacity, active, unitId);
    }

    public CityPoiData withUnitId(UUID unitId) {
        return new CityPoiData(poiId, cityId, pos, type, capacity, active, unitId);
    }
}
