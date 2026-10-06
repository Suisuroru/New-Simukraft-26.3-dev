package client.cn.kafei.simukraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/** PregnancyBellyLayer：按同步的孕期阶段绘制成年 NPC 的腹部几何体。 */
public final class PregnancyBellyLayer extends RenderLayer<CitizenRenderState, CitizenModel> {
    private static final float EARLY_SCALE = 0.45F;
    private static final float MIDDLE_SCALE = 0.75F;
    private static final float LATE_SCALE = 1.0F;
    private static final float SKIN_DEFORMATION = 0.65F;
    private static final float BELLY_BOTTOM_Y = 10.5F;
    private static final float BELLY_HEIGHT = 5.0F;
    private final ModelPart belly;

    public PregnancyBellyLayer(RenderLayerParent<CitizenRenderState, CitizenModel> parent) {
        super(parent);
        this.belly = createBellyModel(SKIN_DEFORMATION, 64);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
            CitizenRenderState state, float yRot, float xRot) {
        if (state.isInvisible || state.childNpc) {
            return;
        }
        float scale = scaleForStage(state.pregnancyStage);
        if (scale <= 0.0F) {
            return;
        }
        configureBellyModel(belly, getParentModel().body, scale);
        submitNodeCollector.submitModelPart(
                belly,
                poseStack,
                RenderTypes.entityCutout(state.texture != null ? state.texture : CitizenRenderer.DEFAULT_TEXTURE),
                lightCoords,
                LivingEntityRenderer.getOverlayCoords(state, 0.0F),
                null,
                -1,
                state.outlineColor);
    }

    static ModelPart createBellyModel(float deformation, int textureHeight) {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("belly",
                CubeListBuilder.create()
                        .texOffs(16, 16)
                        .addBox(-3.0F, BELLY_BOTTOM_Y - BELLY_HEIGHT, -4.0F, 6.0F, BELLY_HEIGHT, 4.5F,
                                new CubeDeformation(deformation)),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, textureHeight).bakeRoot().getChild("belly");
    }

    static void configureBellyModel(ModelPart belly, ModelPart body, float scale) {
        CitizenModel.copyPart(body, belly);
        belly.y += (1.0F - scale) * BELLY_BOTTOM_Y;
        belly.xScale = scale;
        belly.yScale = scale;
        belly.zScale = scale;
    }

    static float scaleForStage(String stage) {
        if (stage == null) {
            return 0.0F;
        }
        return switch (stage) {
            case "early" -> EARLY_SCALE;
            case "middle" -> MIDDLE_SCALE;
            case "late" -> LATE_SCALE;
            default -> 0.0F;
        };
    }
}
