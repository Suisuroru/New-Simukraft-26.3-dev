package client.cn.kafei.simukraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import common.cn.kafei.simukraft.SimuKraft;
import common.cn.kafei.simukraft.entity.CitizenEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class CitizenRenderer extends MobRenderer<CitizenEntity, CitizenRenderState, CitizenModel> {
    public static final Identifier DEFAULT_TEXTURE = Identifier.fromNamespaceAndPath(SimuKraft.MOD_ID, "textures/entity/male/custom_male_entity_0.png");
    private static final ThreadLocal<Boolean> HIDE_OVERHEAD_TEXT = ThreadLocal.withInitial(() -> false);
    private static final float ADULT_SCALE = 0.9375F;
    private static final float CHILD_MIN_SCALE = 0.45F;
    private static final float OVERHEAD_HEAD_CLEARANCE = 0.20F;
    private static final float OVERHEAD_LINE_GAP = 0.05F;
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
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        float backgroundAlpha = minecraft.gameRenderer.gameRenderState().optionsRenderState.getBackgroundOpacity(0.25F);
        int backgroundColor = ARGB.color(backgroundAlpha, -16777216);
        boolean seeThrough = !state.isDiscrete;
        List<CitizenOverheadStatusRegistry.StatusLine> lines = state.overheadLines;
        float[] heights = new float[lines.size()];
        float totalHeight = 0.0F;
        for (int i = 0; i < lines.size(); i++) {
            float scale = lines.get(i).scale() > 0.0F ? lines.get(i).scale() : 0.025F;
            heights[i] = font.lineHeight * 1.15F * scale;
            totalHeight += heights[i];
            if (i > 0) {
                totalHeight += OVERHEAD_LINE_GAP;
            }
        }
        float y = state.boundingBoxHeight + OVERHEAD_HEAD_CLEARANCE + totalHeight;
        for (int i = 0; i < lines.size(); i++) {
            submitOverheadLine(poseStack, submitNodeCollector, camera, font, lines.get(i), y, backgroundColor, seeThrough, state.lightCoords);
            y -= heights[i] + OVERHEAD_LINE_GAP;
        }
    }

    private static void submitOverheadLine(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera,
            Font font,
            CitizenOverheadStatusRegistry.StatusLine line,
            float y,
            int backgroundColor,
            boolean seeThrough,
            int lightCoords) {
        Component text = line.text();
        if (text == null || text.getString().isBlank()) {
            return;
        }
        float scale = line.scale() > 0.0F ? line.scale() : 0.025F;
        FormattedCharSequence visual = text.getVisualOrderText();
        float x = -font.width(visual) / 2.0F;
        int color = ARGB.color(1.0F, line.color() | 0xFF000000);
        poseStack.pushPose();
        poseStack.translate(0.0F, y, 0.0F);
        poseStack.rotate(camera.orientation);
        poseStack.scale(scale, -scale, scale);
        submitNodeCollector.submitText(
                poseStack, x, 0.0F, visual, false,
                seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL,
                lightCoords, color, backgroundColor, 0);
        if (seeThrough) {
            submitNodeCollector.submitText(
                    poseStack, x, 0.0F, visual, false,
                    Font.DisplayMode.NORMAL, lightCoords, color, 0, 0);
        }
        poseStack.popPose();
    }

    @Override
    protected boolean shouldShowName(CitizenEntity entity, double distanceToCameraSq) {
        if (HIDE_OVERHEAD_TEXT.get() || entity.isInvisible()) {
            return false;
        }
        return distanceToCameraSq < 45.0D * 45.0D || entity.hasCustomName();
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

    private static boolean useDefaultModel(CitizenEntity entity) {
        String skinPath = entity.getSkinPath();
        if (skinPath == null || skinPath.isBlank()) {
            return true;
        }
        String path = skinPath.replace('\\', '/').toLowerCase();
        if (path.contains("/female/") || path.contains("female_entity")) {
            return false;
        }
        if (path.contains("/male/") || path.contains("male_entity")) {
            return true;
        }
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        if (fileName.endsWith(".png")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }
        return !fileName.endsWith("_f");
    }
}
