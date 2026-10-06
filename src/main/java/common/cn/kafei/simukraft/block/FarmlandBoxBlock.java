package common.cn.kafei.simukraft.block;

import common.cn.kafei.simukraft.farmland.FarmlandBoxService;
import common.cn.kafei.simukraft.network.farmland.FarmlandBoxOpenRequestPacket;
import common.cn.kafei.simukraft.registry.ModSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;


public final class FarmlandBoxBlock extends Block {
    public FarmlandBoxBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            FarmlandBoxOpenRequestPacket.openFor(serverLevel, serverPlayer, pos);
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            level.playSound(null, pos, ModSoundEvents.FARMLAND_BOX_PLACE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        level.playSound(null, pos, ModSoundEvents.FARMLAND_BOX_BREAK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        FarmlandBoxService.onRemoved(level, pos);
    }
}
