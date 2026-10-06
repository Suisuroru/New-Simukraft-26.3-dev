package client.cn.kafei.simukraft.client.rts;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import common.cn.kafei.simukraft.entity.CitizenEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/**
 * RTS 市民选择渲染器：绘制已选市民脚底框与本次移动目标线。
 */
public final class RtsCitizenSelectionRenderer {
    private static final int COLOR_SELECTION = 0xEE22DDFF;
    private static final double SEARCH_RADIUS = 256.0D;
    private static final double FOOT_RING_OFFSET = 0.025D;
    private static final double FOOT_RING_THICKNESS = 0.035D;
    private static final double TARGET_ARRIVAL_DISTANCE_SQR = 1.0D;

    private RtsCitizenSelectionRenderer() {
    }

    /**
     * onRender: 提交当前多选市民的脚底框与移动线。
     */
    public static void onRender(SubmitCustomGeometryEvent event) {
        if (!RtsSelectionManager.isActive() || !RtsSelectionManager.hasCitizenSelection()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        Vec3 cameraPos = event.getLevelRenderState().cameraRenderState.pos;
        AABB searchBounds = new AABB(cameraPos.x - SEARCH_RADIUS, cameraPos.y - SEARCH_RADIUS,
                cameraPos.z - SEARCH_RADIUS, cameraPos.x + SEARCH_RADIUS, cameraPos.y + SEARCH_RADIUS,
                cameraPos.z + SEARCH_RADIUS);
        var citizens = minecraft.level.getEntitiesOfClass(CitizenEntity.class, searchBounds,
                citizen -> citizen.isAlive() && !citizen.isRemoved()
                        && RtsSelectionManager.isCitizenSelected(citizen.getUUID()));
        if (citizens.isEmpty()) {
            return;
        }
        renderSelections(event.getPoseStack(), event, cameraPos, citizens, RtsSelectionManager.citizenMoveTarget());
    }

    /**
     * renderSelections: 批量提交市民脚底框和移动目标线。
     */
    private static void renderSelections(PoseStack poseStack, SubmitCustomGeometryEvent event, Vec3 cameraPos,
                                         Iterable<CitizenEntity> citizens, BlockPos targetPos) {
        event.getSubmitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.linesTranslucentNoDepthWrite(), (pose, buffer) -> {
            buffer.setLineWidth(4.0F);
            Vec3 target = targetPos == null ? null : Vec3.atBottomCenterOf(targetPos);
            for (CitizenEntity citizen : citizens) {
                renderFootRing(buffer, pose, cameraPos, citizen);
                renderMoveLine(buffer, pose, cameraPos, citizen, target);
            }
        });
    }

    /**
     * renderFootRing: 在市民碰撞箱底部绘制双层矩形线框。
     */
    private static void renderFootRing(VertexConsumer buffer, PoseStack.Pose pose, Vec3 cameraPos, CitizenEntity citizen) {
        AABB bounds = citizen.getBoundingBox();
        double y = bounds.minY + FOOT_RING_OFFSET;
        addRing(buffer, pose, cameraPos, bounds.minX, y, bounds.minZ, bounds.maxX, bounds.maxZ);
        addRing(buffer, pose, cameraPos, bounds.minX - FOOT_RING_THICKNESS, y, bounds.minZ - FOOT_RING_THICKNESS,
                bounds.maxX + FOOT_RING_THICKNESS, bounds.maxZ + FOOT_RING_THICKNESS);
    }

    /**
     * renderMoveLine: 从市民脚底连接至仍未到达的 RTS 移动目标。
     */
    private static void renderMoveLine(VertexConsumer buffer, PoseStack.Pose pose, Vec3 cameraPos, CitizenEntity citizen,
                                       Vec3 target) {
        if (target == null || citizen.position().distanceToSqr(target) <= TARGET_ARRIVAL_DISTANCE_SQR) {
            return;
        }
        double y = citizen.getBoundingBox().minY + FOOT_RING_OFFSET;
        addLine(buffer, pose, cameraPos, citizen.getX(), y, citizen.getZ(), target.x, target.y + FOOT_RING_OFFSET,
                target.z);
    }

    /**
     * addRing: 追加脚底矩形的四条线段。
     */
    private static void addRing(VertexConsumer buffer, PoseStack.Pose pose, Vec3 cameraPos, double minX, double y,
                                double minZ, double maxX, double maxZ) {
        addLine(buffer, pose, cameraPos, minX, y, minZ, maxX, y, minZ);
        addLine(buffer, pose, cameraPos, maxX, y, minZ, maxX, y, maxZ);
        addLine(buffer, pose, cameraPos, maxX, y, maxZ, minX, y, maxZ);
        addLine(buffer, pose, cameraPos, minX, y, maxZ, minX, y, minZ);
    }

    /**
     * addLine: 将世界坐标线段转换为相机相对坐标并写入颜色顶点。
     */
    private static void addLine(VertexConsumer buffer, PoseStack.Pose pose, Vec3 cameraPos, double x1, double y1,
                                double z1, double x2, double y2, double z2) {
        float red = ((COLOR_SELECTION >> 16) & 0xFF) / 255.0F;
        float green = ((COLOR_SELECTION >> 8) & 0xFF) / 255.0F;
        float blue = (COLOR_SELECTION & 0xFF) / 255.0F;
        float alpha = ((COLOR_SELECTION >> 24) & 0xFF) / 255.0F;
        buffer.addVertex(pose, (float) (x1 - cameraPos.x), (float) (y1 - cameraPos.y),
                (float) (z1 - cameraPos.z)).setColor(red, green, blue, alpha);
        buffer.addVertex(pose, (float) (x2 - cameraPos.x), (float) (y2 - cameraPos.y),
                (float) (z2 - cameraPos.z)).setColor(red, green, blue, alpha);
    }
}
