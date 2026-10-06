package common.cn.kafei.simukraft.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class CityChunkManagerTest {
    @Test
    void countsDisconnectedComponentsAsEnclaves() {
        CityChunkManager manager = new CityChunkManager();
        UUID cityId = UUID.randomUUID();
        ChunkPos core = new ChunkPos(0, 0);

        manager.assignInitialArea(cityId, core);
        assertEquals(0, manager.countEnclaves(cityId, core.pack()));
        assertTrue(manager.isConnectedToCore(cityId, ChunkPos.pack(2, 0), core.pack()));

        manager.claimChunk(cityId, ChunkPos.pack(10, 10));
        manager.claimChunk(cityId, ChunkPos.pack(11, 10));
        assertFalse(manager.isConnectedToCore(cityId, ChunkPos.pack(12, 10), core.pack()));
        assertEquals(1, manager.countEnclaves(cityId, core.pack()));

        manager.claimChunk(cityId, ChunkPos.pack(20, 20));
        assertEquals(2, manager.countEnclaves(cityId, core.pack()));
    }
}
