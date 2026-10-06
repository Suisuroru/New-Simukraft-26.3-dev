package common.cn.kafei.simukraft.storage;

import common.cn.kafei.simukraft.util.NbtUuid;

import net.minecraft.nbt.CompoundTag;

import java.sql.PreparedStatement;
import java.sql.SQLException;


final class SqliteNbtHelper {
    private SqliteNbtHelper() {
    }

    static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    static void putNullableUuid(CompoundTag tag, String key, String value) {
        if (value != null && !value.isBlank()) {
            NbtUuid.put(tag, key, java.util.UUID.fromString(value));
        }
    }
}
