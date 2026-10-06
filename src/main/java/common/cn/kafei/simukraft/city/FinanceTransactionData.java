package common.cn.kafei.simukraft.city;

import common.cn.kafei.simukraft.util.NbtUuid;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;


public record FinanceTransactionData(long time, UUID actorId, String actorName, double amount, double balanceAfter,
                                     Type type, String reason) {
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Time", time);
        if (actorId != null) {
            NbtUuid.put(tag, "ActorId", actorId);
        }
        tag.putString("ActorName", actorName != null ? actorName : "");
        tag.putDouble("Amount", amount);
        tag.putDouble("BalanceAfter", balanceAfter);
        tag.putString("Type", type.name());
        tag.putString("Reason", reason != null ? reason : "");
        return tag;
    }

    public static FinanceTransactionData fromTag(CompoundTag tag) {
        UUID actorId = NbtUuid.readOrNull(tag, "ActorId");
        return new FinanceTransactionData(
                tag.getLong("Time").get(),
                actorId,
                tag.getString("ActorName").get(),
                tag.getDouble("Amount").get(),
                tag.getDouble("BalanceAfter").get(),
                Type.fromName(tag.getString("Type").get()),
                tag.getString("Reason").get()
        );
    }

    public enum Type {
        INCOME,
        EXPENSE,
        SYSTEM;

        public static Type fromName(String name) {
            for (Type type : values()) {
                if (type.name().equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return SYSTEM;
        }
    }
}
