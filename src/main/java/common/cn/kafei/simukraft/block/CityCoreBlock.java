package common.cn.kafei.simukraft.block;

import common.cn.kafei.simukraft.building.PlacedBuildingService;
import common.cn.kafei.simukraft.city.*;
import common.cn.kafei.simukraft.network.city.core.CityCoreOpenRequestPacket;
import common.cn.kafei.simukraft.network.toast.InfoToastService;
import common.cn.kafei.simukraft.registry.ModBlocks;
import common.cn.kafei.simukraft.registry.ModSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;


public final class CityCoreBlock extends Block {
    public CityCoreBlock(Properties properties) {
        super(properties);
    }

    // getDrops: 已绑定城市核心会被保护恢复，不产生掉落以避免复制。
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        Vec3 origin = params.getOptionalParameter(LootContextParams.ORIGIN);
        if (origin != null && (CityService.findCityByCorePos(params.getLevel(), BlockPos.containing(origin)).isPresent()
                || DistrictManager.get(params.getLevel()).byChunk(ChunkPos.containing(BlockPos.containing(origin))).flatMap(d -> d.cores().stream().filter(v -> v == BlockPos.containing(origin).asLong()).findAny().map(v -> d)).isPresent())) {
            return List.of();
        }
        return super.getDrops(state, params);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            level.playSound(null, pos, ModSoundEvents.CITY_CORE_OPEN.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            DistrictData district = DistrictManager.get(serverLevel).byChunk(ChunkPos.containing(pos)).orElse(null);
            if (district != null && district.cores().stream().noneMatch(v -> v == pos.asLong())) {
                boolean allowed = CityService.hasPermission(serverLevel, district.parentCityId(), serverPlayer.getUUID(), common.cn.kafei.simukraft.city.CityPermissionLevel.MAYOR)
                        || district.hasPermission(serverPlayer.getUUID(), DistrictRole.OFFICIAL);
                if (allowed && PlacedBuildingService.findByContainedPos(serverLevel, pos) == null
                        && DistrictManager.get(serverLevel).bindCore(district.districtId(), ChunkPos.containing(pos), pos.asLong())) {
                    InfoToastService.success(serverPlayer, Component.translatable("message.simukraft.district.core_bound", district.name()));
                    CityCoreOpenRequestPacket.openFor(serverLevel, serverPlayer, pos);
                } else if (!allowed) {
                    InfoToastService.warning(serverPlayer, Component.translatable("message.simukraft.district.no_permission"));
                }
                return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
            }
            CityCoreOpenRequestPacket.openFor(serverLevel, serverPlayer, pos);
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            level.playSound(null, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        CityData city = CityService.findCityByCorePos(level, pos).orElse(null);
        DistrictData district = DistrictManager.get(level).byChunk(ChunkPos.containing(pos)).orElse(null);
        boolean districtCore = district != null && district.cores().stream().anyMatch(v -> v == pos.asLong());
        if (city == null && !districtCore) {
            return;
        }
        level.setBlock(pos, ModBlocks.CITY_CORE.get().defaultBlockState(), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.players().forEach(player -> {
            if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) < 100.0D) {
                if (player instanceof ServerPlayer serverPlayer) {
                    InfoToastService.warning(serverPlayer, Component.translatable("message.simukraft.city_core.protected"));
                }
            }
        });
    }
}
