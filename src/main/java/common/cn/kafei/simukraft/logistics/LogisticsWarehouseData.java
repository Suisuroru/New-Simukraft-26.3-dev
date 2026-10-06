package common.cn.kafei.simukraft.logistics;

import common.cn.kafei.simukraft.util.NbtUuid;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public record LogisticsWarehouseData(UUID warehouseId,
                                     BlockPos boxPos,
                                     UUID cityId,
                                     String dimensionId,
                                     List<BlockPos> containers,
                                     long updatedAt) {
    public LogisticsWarehouseData {
        warehouseId = warehouseId != null ? warehouseId : UUID.randomUUID();
        boxPos = boxPos != null ? boxPos.immutable() : BlockPos.ZERO;
        dimensionId = dimensionId != null ? dimensionId : "";
        containers = containers != null
                ? containers.stream().filter(pos -> pos != null).map(BlockPos::immutable).distinct().toList()
                : List.of();
        updatedAt = Math.max(0L, updatedAt);
    }

    public LogisticsWarehouseData withContainers(List<BlockPos> nextContainers, long gameTime) {
        return new LogisticsWarehouseData(warehouseId, boxPos, cityId, dimensionId, nextContainers, gameTime);
    }

    public LogisticsWarehouseData withNoCityId() {
        return new LogisticsWarehouseData(warehouseId, boxPos, null, dimensionId, containers, updatedAt);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        NbtUuid.put(tag, "WarehouseId", warehouseId);
        tag.putLong("BoxPos", boxPos.asLong());
        if (cityId != null) {
            NbtUuid.put(tag, "CityId", cityId);
        }
        tag.putString("DimensionId", dimensionId);
        tag.putLong("UpdatedAt", updatedAt);
        ListTag containerTags = new ListTag();
        containers.forEach(pos -> {
            CompoundTag container = new CompoundTag();
            container.putLong("Pos", pos.asLong());
            containerTags.add(container);
        });
        tag.put("Containers", containerTags);
        return tag;
    }

    public static LogisticsWarehouseData fromTag(CompoundTag tag) {
        List<BlockPos> containers = new ArrayList<>();
        ListTag containerTags = tag.getList("Containers").get();
        for (int i = 0; i < containerTags.size(); i++) {
            containers.add(BlockPos.of(containerTags.getCompound(i).get().getLong("Pos").get()));
        }
        return new LogisticsWarehouseData(
                tag.contains("WarehouseId") ? NbtUuid.readOrNull(tag, "WarehouseId") : UUID.randomUUID(),
                BlockPos.of(tag.getLong("BoxPos").get()),
                tag.contains("CityId") ? NbtUuid.readOrNull(tag, "CityId") : null,
                tag.getString("DimensionId").get(),
                containers,
                tag.getLong("UpdatedAt").get());
    }
}
