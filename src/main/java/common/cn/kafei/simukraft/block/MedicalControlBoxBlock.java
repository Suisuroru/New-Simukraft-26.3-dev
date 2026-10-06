package common.cn.kafei.simukraft.block;

import common.cn.kafei.simukraft.medical.MedicalControlBoxService;
import common.cn.kafei.simukraft.network.medical.MedicalControlBoxOpenRequestPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

/** 医疗控制箱方块，负责打开管理界面和移除清理。 */

public final class MedicalControlBoxBlock extends Block {
    public MedicalControlBoxBlock(Properties properties) {
        super(properties);
    }

    /** useWithoutItem：玩家空手右键打开医疗控制箱。 */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            MedicalControlBoxOpenRequestPacket.openFor(serverLevel, serverPlayer, pos);
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        MedicalControlBoxService.onRemoved(level, pos);
    }
}
