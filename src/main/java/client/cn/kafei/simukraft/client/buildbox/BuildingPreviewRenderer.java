package client.cn.kafei.simukraft.client.buildbox;

import com.mojang.blaze3d.vertex.PoseStack;
import common.cn.kafei.simukraft.SimuKraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

@EventBusSubscriber(modid = SimuKraft.MOD_ID, value = Dist.CLIENT)
public final class BuildingPreviewRenderer {
    private static boolean loggedOnce;

    private BuildingPreviewRenderer() {
    }

    @SubscribeEvent
    public static void onSubmitGeometry(SubmitCustomGeometryEvent event) {
        if (!BuildingPreviewManager.isPreviewActive()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        submitMesh(BuildingPreviewManager.getCachedMesh(), event);
    }

    /**
     * submitMesh: 把预览方块提交进 26.3 的几何收集器。
     */
    public static void submitMesh(PreviewMesh mesh, SubmitCustomGeometryEvent event) {
        if (mesh == null || mesh.isEmpty()) {
            if (!loggedOnce) {
                SimuKraft.LOGGER.warn("SimuKraft: Preview mesh is empty during render");
                loggedOnce = true;
            }
            return;
        }
        loggedOnce = false;
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.level instanceof ClientLevel level)) {
            return;
        }
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        for (PreviewBlockData block : mesh.blocks()) {
            submitBlock(poseStack, collector, level, camera, block);
        }
    }

    private static void submitBlock(PoseStack poseStack, SubmitNodeCollector collector, ClientLevel level, Vec3 camera, PreviewBlockData block) {
        BlockPos pos = block.pos();
        MovingBlockRenderState renderState = new MovingBlockRenderState();
        renderState.blockPos = pos;
        renderState.randomSeedPos = pos;
        renderState.blockState = block.state();
        renderState.biome = level.getBiome(pos);
        if (level instanceof BlockAndTintGetter tintGetter) {
            renderState.cardinalLighting = tintGetter.cardinalLighting();
            renderState.lightEngine = tintGetter.getLightEngine();
        }
        poseStack.pushPose();
        poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
        collector.submitMovingBlock(poseStack, renderState, 0);
        poseStack.popPose();
    }
}
