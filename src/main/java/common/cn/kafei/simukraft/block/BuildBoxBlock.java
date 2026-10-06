package common.cn.kafei.simukraft.block;

import common.cn.kafei.simukraft.building.BuilderConstructionService;
import common.cn.kafei.simukraft.building.PlacedBuildingRecord;
import common.cn.kafei.simukraft.building.PlacedBuildingService;
import common.cn.kafei.simukraft.clientbridge.ClientInteractionBridge;
import common.cn.kafei.simukraft.job.CitizenEmploymentService;
import common.cn.kafei.simukraft.registry.ModSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class BuildBoxBlock extends Block {
    public BuildBoxBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            ClientInteractionBridge.openBuildBox(pos);
        } else {
            level.playSound(null, pos, ModSoundEvents.BUILD_BOX_OPEN.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            level.playSound(null, pos, ModSoundEvents.BUILD_BOX_PLACE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        level.playSound(null, pos, ModSoundEvents.BUILD_BOX_BREAK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        BuilderConstructionService.interruptTasksByBuildBox(level, pos, "build_box_removed");
        common.cn.kafei.simukraft.planner.PlannerWorkService.interruptTasksByBuildBox(level, pos, "build_box_removed");
        releaseAssignedCitizen(level, pos, "builder");
        releaseAssignedCitizen(level, pos, "planner");
        PlacedBuildingRecord building = PlacedBuildingService.findByContainedPos(level, pos);
        if (building != null) {
            PlacedBuildingService.unregister(level, building.buildingId());
        }
    }

    private static void releaseAssignedCitizen(ServerLevel level, BlockPos pos, String role) {
        CitizenEmploymentService.fireAssigned(level, CitizenEmploymentService.workplaceId("build_box", role, pos), "build_box", role, pos, "build_box_removed");
    }
}
