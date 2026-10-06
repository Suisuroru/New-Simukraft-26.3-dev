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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** PregnancyBellyArmorLayer：为已装备胸甲的孕期 NPC 绘制外扩腹部盔甲。 */
public final class PregnancyBellyArmorLayer extends RenderLayer<CitizenRenderState, CitizenModel> {
    private static final Identifier FALLBACK_ARMOR_TEXTURE = Identifier.withDefaultNamespace("textures/entity/equipment/humanoid/iron.png");
    private static final int ARMOR_TEXTURE_U = 17;
    private static final int ARMOR_TEXTURE_V = 21;
    private static final float BELLY_TOP_Y = 5.5F;
    private static final float BELLY_HEIGHT = 5.0F;
    private static final float BELLY_DEPTH = 4.0F;
    private final ModelPart belly;

    public PregnancyBellyArmorLayer(RenderLayerParent<CitizenRenderState, CitizenModel> parent) {
        super(parent);
        this.belly = createArmorBellyModel();
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
            CitizenRenderState state, float yRot, float xRot) {
        if (state.isInvisible || state.childNpc) {
            return;
        }
        float scale = PregnancyBellyLayer.scaleForStage(state.pregnancyStage);
        if (scale <= 0.0F) {
            return;
        }
        ItemStack chestArmor = state.chestEquipment;
        if (chestArmor == null || chestArmor.isEmpty()) {
            return;
        }
        PregnancyBellyLayer.configureBellyModel(belly, getParentModel().body, scale);
        submitNodeCollector.submitModelPart(
                belly,
                poseStack,
                RenderTypes.armorCutoutNoCull(FALLBACK_ARMOR_TEXTURE),
                lightCoords,
                OverlayTexture.NO_OVERLAY,
                null,
                -1,
                state.outlineColor);
    }

    private static ModelPart createArmorBellyModel() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("belly",
                CubeListBuilder.create()
                        .texOffs(ARMOR_TEXTURE_U, ARMOR_TEXTURE_V)
                        .addBox(-3.0F, BELLY_TOP_Y, -4.0F, 6.0F, BELLY_HEIGHT, BELLY_DEPTH,
                                new CubeDeformation(1.0F)),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32).bakeRoot().getChild("belly");
    }
}
