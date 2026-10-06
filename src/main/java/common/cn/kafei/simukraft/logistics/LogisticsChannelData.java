package common.cn.kafei.simukraft.logistics;

import common.cn.kafei.simukraft.util.NbtUuid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public record LogisticsChannelData(UUID channelId,
                                   UUID warehouseId,
                                   UUID clientId,
                                   LogisticsDirection direction,
                                   String name,
                                   boolean enabled,
                                   List<LogisticsItemFilter> filters,
                                   long updatedAt,
                                   int keepSourceQuantity,
                                   int keepTargetQuantity) {
    public LogisticsChannelData {
        channelId = channelId != null ? channelId : UUID.randomUUID();
        direction = direction != null ? direction : LogisticsDirection.WAREHOUSE_TO_CLIENT;
        name = name != null && !name.isBlank() ? name.trim() : direction.name().toLowerCase(java.util.Locale.ROOT);
        filters = filters != null ? filters.stream().filter(LogisticsItemFilter::valid).toList() : List.of();
        updatedAt = Math.max(0L, updatedAt);
        keepSourceQuantity = Math.max(0, keepSourceQuantity);
        keepTargetQuantity = Math.max(0, keepTargetQuantity);
    }

    public LogisticsChannelData withEnabled(boolean nextEnabled, long gameTime) {
        return new LogisticsChannelData(channelId, warehouseId, clientId, direction, name, nextEnabled, filters, gameTime, keepSourceQuantity, keepTargetQuantity);
    }

    public LogisticsChannelData withKeepQuantities(int sourceQty, int targetQty, long gameTime) {
        return new LogisticsChannelData(channelId, warehouseId, clientId, direction, name, enabled, filters, gameTime, Math.max(0, sourceQty), Math.max(0, targetQty));
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        NbtUuid.put(tag, "ChannelId", channelId);
        if (warehouseId != null) {
            NbtUuid.put(tag, "WarehouseId", warehouseId);
        }
        if (clientId != null) {
            NbtUuid.put(tag, "ClientId", clientId);
        }
        tag.putString("Direction", direction.name());
        tag.putString("Name", name);
        tag.putBoolean("Enabled", enabled);
        tag.putLong("UpdatedAt", updatedAt);
        ListTag filterTags = new ListTag();
        filters.forEach(filter -> {
            CompoundTag filterTag = new CompoundTag();
            filterTag.putString("ItemId", filter.itemId());
            filterTag.putString("StackTag", filter.stackTag());
            filterTags.add(filterTag);
        });
        tag.put("Filters", filterTags);
        tag.putInt("KeepSourceQuantity", keepSourceQuantity);
        tag.putInt("KeepQuantity", keepTargetQuantity);
        return tag;
    }

    public static LogisticsChannelData fromTag(CompoundTag tag) {
        List<LogisticsItemFilter> filters = new ArrayList<>();
        ListTag filterTags = tag.getList("Filters").get();
        for (int i = 0; i < filterTags.size(); i++) {
            CompoundTag filter = filterTags.getCompound(i).get();
            filters.add(new LogisticsItemFilter(filter.getString("ItemId").get(), filter.getString("StackTag").get()));
        }
        return new LogisticsChannelData(
                tag.contains("ChannelId") ? NbtUuid.readOrNull(tag, "ChannelId") : UUID.randomUUID(),
                NbtUuid.readOrNull(tag, "WarehouseId"),
                NbtUuid.readOrNull(tag, "ClientId"),
                LogisticsDirection.fromName(tag.getString("Direction").get()),
                tag.getString("Name").get(),
                !tag.contains("Enabled") || tag.getBoolean("Enabled").get(),
                filters,
                tag.getLong("UpdatedAt").get(),
                tag.contains("KeepSourceQuantity") ? tag.getInt("KeepSourceQuantity").get() : 0,
                tag.contains("KeepQuantity") ? tag.getInt("KeepQuantity").get() : 0);
    }
}
