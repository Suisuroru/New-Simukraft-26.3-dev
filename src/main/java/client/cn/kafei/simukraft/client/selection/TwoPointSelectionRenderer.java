package client.cn.kafei.simukraft.client.selection;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

public final class TwoPointSelectionRenderer {
    private static final int COLOR_POINT_1 = 0xD8FF3333;
    private static final int COLOR_POINT_2 = 0xD8FFE066;
    private static final int COLOR_SELECTION = 0xC866CCFF;
    private static final double POINT_INFLATE = 0.04D;

    private TwoPointSelectionRenderer() {
    }

    public static void onRender(SubmitCustomGeometryEvent event) {
        if (!TwoPointSelectionManager.isActive()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        TwoPointSelectionManager.SelectionState state = TwoPointSelectionManager.state();
        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getLevelRenderState().cameraRenderState.pos;
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        renderPoint(collector, poseStack, cameraPos, state.point1(), COLOR_POINT_1);
        renderPoint(collector, poseStack, cameraPos, state.point2(), COLOR_POINT_2);
        AABB selection = TwoPointSelectionManager.selectedAabb();
        if (selection != null) {
            renderWireBox(collector, poseStack, cameraPos, selection.inflate(POINT_INFLATE), COLOR_SELECTION);
        }
    }

    private static void renderPoint(SubmitNodeCollector collector, PoseStack poseStack, Vec3 cameraPos, BlockPos pos, int color) {
        if (pos == null) {
            return;
        }
        renderWireBox(collector, poseStack, cameraPos, new AABB(pos).inflate(POINT_INFLATE), color);
    }

    private static void renderWireBox(SubmitNodeCollector collector, PoseStack poseStack, Vec3 cameraPos, AABB bounds, int color) {
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        collector.submitShapeOutline(poseStack, Shapes.create(bounds), RenderTypes.linesTranslucentNoDepthWrite(), color, 2.5F, true);
        poseStack.popPose();
    }
}
