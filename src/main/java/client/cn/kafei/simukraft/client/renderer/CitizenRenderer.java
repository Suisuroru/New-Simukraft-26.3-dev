package client.cn.kafei.simukraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import common.cn.kafei.simukraft.SimuKraft;
import common.cn.kafei.simukraft.entity.CitizenEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class CitizenRenderer extends MobRenderer<CitizenEntity, CitizenRenderState, CitizenModel> {
    public static final Identifier DEFAULT_TEXTURE = Identifier.fromNamespaceAndPath(SimuKraft.MOD_ID, "textures/entity/male/custom_male_entity_0.png");
    private static final ThreadLocal<Boolean> HIDE_OVERHEAD_TEXT = ThreadLocal.withInitial(() -> false);
    private static final float ADULT_SCALE = 0.9375F;
    private static final float CHILD_MIN_SCALE = 0.45F;
    private final CitizenModel slimModel;
    private final CitizenModel defaultModel;

    public CitizenRenderer(EntityRendererProvider.Context context) {
        super(context, new CitizenModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5F);
        this.slimModel = this.model;
        this.defaultModel = new CitizenModel(context.bakeLayer(ModelLayers.PLAYER), false);
        this.addLayer(new PregnancyBellyLayer(this));
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(),
                        part -> new HumanoidModel<>(part, RenderTypes::armorCutoutNoCull)),
                context.getEquipmentRenderer()));
        this.addLayer(new PregnancyBellyArmorLayer(this));
        this.addLayer(new ItemInHandLayer<>(this));
    }

    @Override
    public CitizenRenderState createRenderState() {
        return new CitizenRenderState();
    }

    @Override
    public Identifier getTextureLocation(CitizenRenderState state) {
        return state.texture != null ? state.texture : DEFAULT_TEXTURE;
    }

    @Override
    public void extractRenderState(CitizenEntity entity, CitizenRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTicks, this.itemModelResolver);
        state.texture = textureFromPath(entity.getSkinPath());
        state.workSwing = CitizenAnimationActions.canUseWorkSwing(entity);
        state.childNpc = entity.isChildNpc();
        state.npcAge = entity.getAge();
        state.pregnancyStage = entity.getPregnancyStage() == null ? "" : entity.getPregnancyStage();
        state.useWideModel = useDefaultModel(entity);
        state.hideOverhead = HIDE_OVERHEAD_TEXT.get();
        state.overheadLines = state.hideOverhead || entity.isInvisible()
                ? List.of()
                : CitizenOverheadStatusRegistry.resolve(entity);
        state.nameTag = null;
    }

    @Override
    public void submit(CitizenRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        this.model = state.useWideModel ? defaultModel : slimModel;
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    @Override
    protected void scale(CitizenRenderState state, PoseStack poseStack) {
        float scale;
        if (state.childNpc) {
            int age = Math.max(1, state.npcAge);
            float t = Math.clamp((age - 1) / 16.0f, 0.0f, 1.0f);
            scale = CHILD_MIN_SCALE + t * (ADULT_SCALE - CHILD_MIN_SCALE);
        } else {
            scale = ADULT_SCALE;
        }
        poseStack.scale(scale, scale, scale);
    }

    @Override
    protected void submitNameDisplay(CitizenRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.hideOverhead || state.overheadLines.isEmpty() || state.distanceToCameraSq > 45.0D * 45.0D) {
            return;
        }
        Vec3 attachment = state.nameTagAttachment != null ? state.nameTagAttachment : Vec3.ZERO;
        int offset = 0;
        for (CitizenOverheadStatusRegistry.StatusLine line : state.overheadLines) {
            submitNodeCollector.submitNameTag(poseStack, attachment, offset, line.text(), !state.isDiscrete, state.lightCoords, camera);
            offset += 1;
        }
    }

    @Override
    protected boolean shouldShowName(CitizenEntity entity, double distanceToCameraSq) {
        if (HIDE_OVERHEAD_TEXT.get() || entity.isInvisible()) {
            return false;
        }
        return distanceToCameraSq < 45.0D * 45.0D || entity.hasCustomName();
    }

    /** withoutOverheadText：在布偶预览渲染期间屏蔽名称和工作状态文字。 */
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

    private static boolean useDefaultModel(CitizenEntity entity) {
        String skinPath = entity.getSkinPath();
        if (skinPath == null) {
            return false;
        }
        String fileName = skinPath;
        int slash = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        if (slash >= 0) {
            fileName = fileName.substring(slash + 1);
        }
        if (fileName.endsWith(".png")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }
        return fileName.endsWith("_f");
    }
}
