package client.cn.kafei.simukraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import common.cn.kafei.simukraft.SimuKraft;
import common.cn.kafei.simukraft.entity.CitizenEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

// 26.3：LivingEntityRenderer/MobRenderer 拆分为“实体 + 渲染状态 + 模型”三泛型参数，
// 且取贴图/缩放/头顶文字等钩子方法均改为读取 CitizenRenderState，不再直接访问实体。
public class CitizenRenderer extends MobRenderer<CitizenEntity, CitizenRenderState, CitizenModel> {
    static final Identifier DEFAULT_TEXTURE = Identifier.fromNamespaceAndPath(SimuKraft.MOD_ID, "textures/entity/male/custom_male_entity_0.png");
    private static final ThreadLocal<Boolean> HIDE_OVERHEAD_TEXT = ThreadLocal.withInitial(() -> false);

    private static final float ADULT_SCALE = 0.9375F;
    private static final float CHILD_MIN_SCALE = 0.45F;
    private static final double OVERHEAD_MAX_DISTANCE_SQ = 45.0D * 45.0D;

    public CitizenRenderer(EntityRendererProvider.Context context) {
        super(context, new CitizenModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5F);
        this.addLayer(new PregnancyBellyLayer(this));
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                ArmorModelSet.bake(ModelLayers.PLAYER_SLIM_ARMOR, context.getModelSet(), part -> new CitizenModel(part, true)),
                context.getEquipmentRenderer()));
        this.addLayer(new PregnancyBellyArmorLayer(this));
        this.addLayer(new ItemInHandLayer<>(this));
    }

    @Override
    public CitizenRenderState createRenderState() {
        return new CitizenRenderState();
    }

    @Override
    public void extractRenderState(CitizenEntity entity, CitizenRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.texture = textureFromPath(entity.getSkinPath());
        state.workSwing = entity.isSwinging();
        state.childNpc = entity.isChildNpc();
        state.npcAge = Math.max(1, entity.getAge());
        state.pregnancyStage = entity.getPregnancyStage();
        boolean showOverhead = shouldShowName(entity, state.distanceToCameraSq);
        state.hideOverhead = !showOverhead;
        state.overheadLines = showOverhead ? CitizenOverheadStatusRegistry.resolve(entity) : List.of();
    }

    @Override
    public Identifier getTextureLocation(CitizenRenderState state) {
        return state.texture != null ? state.texture : DEFAULT_TEXTURE;
    }

    @Override
    protected void scale(CitizenRenderState state, PoseStack poseStack) {
        float scale;
        if (state.childNpc) {
            int age = Math.max(1, state.npcAge);
            // 1岁到17岁线性从 CHILD_MIN_SCALE 渐变到 ADULT_SCALE
            float t = Math.clamp((age - 1) / 16.0f, 0.0f, 1.0f);
            scale = CHILD_MIN_SCALE + t * (ADULT_SCALE - CHILD_MIN_SCALE);
        } else {
            scale = ADULT_SCALE;
        }
        poseStack.scale(scale, scale, scale);
    }

    @Override
    protected boolean shouldShowName(CitizenEntity entity, double distanceToCameraSq) {
        if (HIDE_OVERHEAD_TEXT.get() || entity.isInvisible()) {
            return false;
        }
        return distanceToCameraSq < OVERHEAD_MAX_DISTANCE_SQ
                || entity.hasCustomName() && entity == this.entityRenderDispatcher.crosshairPickEntity;
    }

    @Override
    protected void submitNameDisplay(CitizenRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.hideOverhead) {
            return;
        }
        float yOffset = state.boundingBoxHeight + 0.82F;
        for (CitizenOverheadStatusRegistry.StatusLine line : state.overheadLines) {
            renderExtraLine(state, line.text(), line.color(), line.scale(), poseStack, submitNodeCollector, camera, yOffset);
            yOffset -= 0.23F;
        }
    }

    /**
     * withoutOverheadText：在布偶预览渲染期间屏蔽名称和工作状态文字。
     */
    public static void withoutOverheadText(Runnable renderAction) {
        boolean previous = HIDE_OVERHEAD_TEXT.get();
        HIDE_OVERHEAD_TEXT.set(true);
        try {
            renderAction.run();
        } finally {
            if (previous) {
                HIDE_OVERHEAD_TEXT.set(true);
            } else {
                HIDE_OVERHEAD_TEXT.remove();
            }
        }
    }

    private static Identifier textureFromPath(String skinPath) {
        if (skinPath == null || skinPath.isBlank()) {
            return DEFAULT_TEXTURE;
        }
        Identifier parsed = Identifier.tryParse(skinPath);
        return parsed != null ? parsed : DEFAULT_TEXTURE;
    }

    private void renderExtraLine(CitizenRenderState state, Component component, int color, float scale, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, float yOffset) {
        if (state.distanceToCameraSq > 4096.0D) {
            return;
        }
        boolean seeThrough = !state.isDiscrete;
        Font font = this.getFont();
        FormattedCharSequence text = component.getVisualOrderText();
        int argbColor = color | 0xFF000000;
        float x = -font.width(component) / 2.0F;
        poseStack.pushPose();
        poseStack.translate(0.0F, yOffset, 0.0F);
        poseStack.rotate(camera.orientation);
        poseStack.scale(scale, -scale, scale);
        int backgroundColor = (int) (Minecraft.getInstance().gameRenderer.gameRenderState().optionsRenderState.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        submitNodeCollector.submitText(poseStack, x, 0.0F, text, false, seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, state.lightCoords, argbColor, backgroundColor, 0);
        if (seeThrough) {
            submitNodeCollector.submitText(poseStack, x, 0.0F, text, false, Font.DisplayMode.NORMAL, state.lightCoords, argbColor, 0, 0);
        }
        poseStack.popPose();
    }
}
