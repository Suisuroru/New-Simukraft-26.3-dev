package client.cn.kafei.simukraft.client.buildbox;

import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.List;

public final class PreviewMesh implements AutoCloseable {
    public static final PreviewMesh EMPTY = new PreviewMesh(BlockPos.ZERO, Collections.emptyList());

    private BlockPos origin;
    private List<PreviewBlockData> blocks;

    public PreviewMesh(BlockPos origin, List<PreviewBlockData> blocks) {
        this.origin = origin;
        this.blocks = blocks == null ? List.of() : List.copyOf(blocks);
    }

    public BlockPos origin() {
        return origin;
    }

    public void offsetOrigin(int dx, int dy, int dz) {
        if (this == EMPTY) {
            return;
        }
        origin = origin.offset(dx, dy, dz);
        if (!blocks.isEmpty()) {
            blocks = blocks.stream()
                    .map(block -> new PreviewBlockData(block.pos().offset(dx, dy, dz), block.state(), block.packedLight(), block.copyBlockEntityData()))
                    .toList();
        }
    }

    public List<PreviewBlockData> blocks() {
        return blocks;
    }

    public List<PreviewBlockData> entityBlocks() {
        return blocks;
    }

    public boolean isEmpty() {
        return blocks.isEmpty();
    }

    @Override
    public void close() {
        if (this == EMPTY) {
            return;
        }
        blocks = List.of();
    }
}
