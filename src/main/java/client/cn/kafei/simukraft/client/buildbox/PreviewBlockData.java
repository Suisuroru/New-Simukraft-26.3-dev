package client.cn.kafei.simukraft.client.buildbox;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public record PreviewBlockData(BlockPos pos, BlockState state, int packedLight, CompoundTag blockEntityData) {
    public PreviewBlockData(BlockPos pos, BlockState state, int packedLight) {
        this(pos, state, packedLight, null);
    }

    public PreviewBlockData {
        blockEntityData = blockEntityData != null ? blockEntityData.copy() : null;
    }

    public CompoundTag copyBlockEntityData() {
        return blockEntityData != null ? blockEntityData.copy() : null;
    }
}
