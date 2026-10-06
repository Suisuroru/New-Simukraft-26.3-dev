package net.minecraft.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.BlockModelFeatureRenderer;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
import net.minecraft.client.renderer.feature.GizmoFeatureRenderer;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.feature.LeashFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.MovingBlockFeatureRenderer;
import net.minecraft.client.renderer.feature.QuadParticleFeatureRenderer;
import net.minecraft.client.renderer.feature.ShadowFeatureRenderer;
import net.minecraft.client.renderer.feature.ShapeOutlineFeatureRenderer;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.TranslucentFeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SubmitNodeCollection implements OrderedSubmitNodeCollector {
    public final SimpleFeatureRenderPhase solid = new SimpleFeatureRenderPhase();
    public final SimpleFeatureRenderPhase waterMask = new SimpleFeatureRenderPhase();
    public final SimpleFeatureRenderPhase outline = new SimpleFeatureRenderPhase();
    public final SimpleFeatureRenderPhase alwaysOnTopGizmos = new SimpleFeatureRenderPhase();
    public final TranslucentFeatureRenderPhase seeThrough;
    public final SimpleFeatureRenderPhase oitTranslucent = new SimpleFeatureRenderPhase();
    public final SimpleFeatureRenderPhase shadows;
    public final SimpleFeatureRenderPhase nameTags;
    public final SimpleFeatureRenderPhase texts;
    public final SimpleFeatureRenderPhase shapeOutlines;
    public final FeatureRenderPhase<? super TranslucentSubmit> translucentBlocksAndItems;
    public final FeatureRenderPhase<? super TranslucentSubmit> translucentModels;
    public final SimpleFeatureRenderPhase translucentCustomGeometry;
    public final SimpleFeatureRenderPhase breakingOverlay;
    public final SimpleFeatureRenderPhase afterTerrain;
    public final SimpleFeatureRenderPhase translucentGizmos;
    private final List<FeatureRenderPhase<?>> allPhases;

    public SubmitNodeCollection(boolean useImprovedTransparency, TranslucentFeatureRenderPhase seeThrough) {
        this.seeThrough = seeThrough;
        if (useImprovedTransparency) {
            this.shadows = this.oitTranslucent;
            this.nameTags = this.oitTranslucent;
            this.texts = this.oitTranslucent;
            this.shapeOutlines = this.oitTranslucent;
            this.translucentBlocksAndItems = this.oitTranslucent;
            this.translucentModels = this.oitTranslucent;
            this.translucentCustomGeometry = this.oitTranslucent;
            this.breakingOverlay = this.oitTranslucent;
            this.afterTerrain = this.oitTranslucent;
            this.translucentGizmos = this.oitTranslucent;
            this.allPhases = List.of(this.solid, this.waterMask, this.outline, this.alwaysOnTopGizmos, this.oitTranslucent);
        } else {
            this.shadows = new SimpleFeatureRenderPhase();
            this.nameTags = new SimpleFeatureRenderPhase();
            this.texts = new SimpleFeatureRenderPhase();
            this.shapeOutlines = new SimpleFeatureRenderPhase();
            this.translucentBlocksAndItems = new TranslucentFeatureRenderPhase();
            this.translucentModels = new TranslucentFeatureRenderPhase();
            this.translucentCustomGeometry = new SimpleFeatureRenderPhase();
            this.breakingOverlay = new SimpleFeatureRenderPhase();
            this.afterTerrain = new SimpleFeatureRenderPhase();
            this.translucentGizmos = new SimpleFeatureRenderPhase();
            this.allPhases = List.of(
                this.solid,
                this.shadows,
                this.nameTags,
                this.texts,
                this.shapeOutlines,
                this.translucentBlocksAndItems,
                this.translucentModels,
                this.translucentCustomGeometry,
                this.translucentGizmos,
                this.breakingOverlay,
                this.waterMask,
                this.afterTerrain,
                this.alwaysOnTopGizmos,
                this.outline
            );
        }
    }

    @Override
    public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {
        PoseStack.Pose pose = poseStack.last();
        this.shadows.submit(new ShadowFeatureRenderer.Submit(new Matrix4f(pose.pose()), radius, pieces));
    }

    @Override
    public void submitNameTag(
        PoseStack poseStack, @Nullable Vec3 nameTagAttachment, int offset, Component name, boolean seeThrough, int lightCoords, CameraRenderState camera
    ) {
        if (nameTagAttachment != null) {
            Minecraft minecraft = Minecraft.getInstance();
            poseStack.pushPose();
            poseStack.translate(nameTagAttachment.x, nameTagAttachment.y + 0.5, nameTagAttachment.z);
            poseStack.rotate(camera.orientation);
            poseStack.scale(0.025F, -0.025F, 0.025F);
            Matrix4f pose = new Matrix4f(poseStack.last().pose());
            float x = -minecraft.font.width(name) / 2.0F;
            float backgroundAlpha = minecraft.gameRenderer.gameRenderState().optionsRenderState.getBackgroundOpacity(0.25F);
            int backgroundColor = ARGB.color(backgroundAlpha, -16777216);
            float textAlpha = Math.max((backgroundAlpha + 0.75F) * 0.5F, 0.5F);
            int textColor = ARGB.color(textAlpha, -1);
            FormattedCharSequence visualOrderName = name.getVisualOrderText();
            if (seeThrough) {
                this.submitNameTagPart(
                    nameTag(pose, x, offset, visualOrderName, LightCoordsUtil.lightCoordsWithEmission(lightCoords, 2), -1, 0, Font.DisplayMode.NORMAL)
                );
                this.seeThrough.submit(nameTag(pose, x, offset, visualOrderName, lightCoords, textColor, backgroundColor, Font.DisplayMode.SEE_THROUGH));
            } else {
                this.submitNameTagPart(nameTag(pose, x, offset, visualOrderName, lightCoords, textColor, backgroundColor, Font.DisplayMode.NORMAL));
            }

            poseStack.popPose();
        }
    }

    private static TextFeatureRenderer.Submit nameTag(
        Matrix4f pose, float x, float y, FormattedCharSequence name, int lightCoords, int color, int backgroundColor, Font.DisplayMode displayMode
    ) {
        TextFeatureRenderer.Content content = new TextFeatureRenderer.Content.Text(x, y, name, false, color, backgroundColor, 0);
        return new TextFeatureRenderer.Submit(pose, displayMode, lightCoords, content);
    }

    private void submitNameTagPart(TextFeatureRenderer.Submit nameTag) {
        if (canRenderAsSolid(nameTag.content())) {
            this.solid.submit(nameTag);
        } else {
            this.nameTags.submit(nameTag);
        }
    }

    private static boolean canRenderAsSolid(TextFeatureRenderer.Content content) {
        return content instanceof TextFeatureRenderer.Content.Text text
            && ARGB.alpha(text.color()) == 255
            && ARGB.alpha(text.backgroundColor()) == 0
            && RenderSystem.isRenderingLevel;
    }

    @Override
    public void submitText(
        PoseStack poseStack,
        float x,
        float y,
        FormattedCharSequence string,
        boolean dropShadow,
        Font.DisplayMode displayMode,
        int lightCoords,
        int color,
        int backgroundColor,
        int outlineColor
    ) {
        this.submitTextPart(
            poseStack, displayMode, lightCoords, new TextFeatureRenderer.Content.Text(x, y, string, dropShadow, color, backgroundColor, outlineColor)
        );
    }

    @Override
    public void submitTextBackground(PoseStack poseStack, float x0, float y0, float x1, float y1, int color, Font.DisplayMode displayMode, int lightCoords) {
        this.submitTextPart(poseStack, displayMode, lightCoords, new TextFeatureRenderer.Content.StandaloneBackground(x0, y0, x1, y1, color));
    }

    private void submitTextPart(PoseStack poseStack, Font.DisplayMode displayMode, int lightCoords, TextFeatureRenderer.Content content) {
        TextFeatureRenderer.Submit submit = new TextFeatureRenderer.Submit(new Matrix4f(poseStack.last().pose()), displayMode, lightCoords, content);
        if (displayMode == Font.DisplayMode.SEE_THROUGH) {
            this.seeThrough.submit(submit);
        } else if (canRenderAsSolid(content)) {
            this.solid.submit(submit);
        } else {
            this.texts.submit(submit);
        }
    }

    @Override
    public void submitFlame(PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {
        this.solid.submit(new FlameFeatureRenderer.Submit(poseStack.last().copy(), renderState, rotation));
    }

    @Override
    public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
        this.solid.submit(new LeashFeatureRenderer.Submit(new Matrix4f(poseStack.last().pose()), leashState));
    }

    @Override
    public <S> void submitModel(
        Model<? super S> model,
        S state,
        PoseStack poseStack,
        RenderType renderType,
        int lightCoords,
        int overlayCoords,
        int tintedColor,
        @Nullable UvMapping uvMapping,
        int outlineColor
    ) {
        PoseStack.Pose pose = poseStack.last().copy();
        if (!renderType.isOutline()) {
            ModelFeatureRenderer.Submit<S> submit = new ModelFeatureRenderer.Submit<>(
                renderType, pose, model, state, lightCoords, overlayCoords, tintedColor, uvMapping, null
            );
            if (renderType == RenderTypes.waterMask()) {
                this.waterMask.submit(submit);
            } else if (!renderType.forceSolidModelPhase() && renderType.hasBlending()) {
                this.translucentModels.submit(submit);
            } else {
                this.solid.submit(submit);
            }
        }

        if (outlineColor != 0) {
            RenderType outlineRenderType = getOutlineRenderType(renderType);
            if (outlineRenderType != null) {
                this.outline
                    .submit(
                        new ModelFeatureRenderer.Submit<>(
                            outlineRenderType, pose, model, state, 15728880, OverlayTexture.NO_OVERLAY, outlineColor, uvMapping, null
                        )
                    );
            }
        }
    }

    @Override
    public <S> void submitCrumblingOverlay(
        Model<? super S> model,
        S state,
        PoseStack poseStack,
        RenderType renderType,
        int lightCoords,
        int overlayCoords,
        int tintedColor,
        ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        if (renderType.affectsCrumbling()) {
            boolean translucent = renderType.hasBlending();
            List<RenderType> destroyTypes;
            if (Minecraft.getInstance().gameRenderer.useImprovedTransparency() && translucent) {
                destroyTypes = ModelBakery.DESTROY_TYPES_OIT;
            } else {
                destroyTypes = ModelBakery.DESTROY_TYPES;
            }

            RenderType crumblingRenderType = destroyTypes.get(crumblingOverlay.progress());
            SimpleFeatureRenderPhase targetPhase = translucent ? this.breakingOverlay : this.solid;
            targetPhase.submit(
                new ModelFeatureRenderer.Submit<>(
                    crumblingRenderType, poseStack.last().copy(), model, state, lightCoords, overlayCoords, tintedColor, null, crumblingOverlay.cameraPose()
                )
            );
        }
    }

    @Override
    public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState, int outlineColor) {
        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(movingBlockRenderState.blockState);
        boolean isTranslucent = model.hasMaterialFlag(movingBlockRenderState, movingBlockRenderState.blockPos, movingBlockRenderState.blockState, 1);
        MovingBlockFeatureRenderer.Submit submit = new MovingBlockFeatureRenderer.Submit(
            new Matrix4f(poseStack.last().pose()), movingBlockRenderState, 0, isTranslucent
        );
        if (isTranslucent) {
            this.translucentBlocksAndItems.submit(submit);
        } else {
            this.solid.submit(submit);
        }

        if (outlineColor != 0) {
            this.outline.submit(new MovingBlockFeatureRenderer.Submit(new Matrix4f(poseStack.last().pose()), movingBlockRenderState, outlineColor, false));
        }
    }

    @Override
    public void submitBlockModel(
        PoseStack poseStack,
        RenderType renderType,
        List<BlockStateModelPart> modelParts,
        int[] tintLayers,
        int lightCoords,
        int overlayCoords,
        int outlineColor
    ) {
        PoseStack.Pose pose = poseStack.last().copy();
        if (!renderType.isOutline()) {
            BlockModelFeatureRenderer.Submit submit = new BlockModelFeatureRenderer.Submit(
                pose, renderType, modelParts, tintLayers, lightCoords, overlayCoords, -1, null
            );
            if (renderType.hasBlending()) {
                this.translucentBlocksAndItems.submit(submit);
            } else {
                this.solid.submit(submit);
            }
        }

        if (outlineColor != 0) {
            RenderType outlineRenderType = getOutlineRenderType(renderType);
            if (outlineRenderType != null) {
                this.outline
                    .submit(
                        new BlockModelFeatureRenderer.Submit(
                            pose, outlineRenderType, modelParts, BlockModelRenderState.EMPTY_TINTS, 15728880, OverlayTexture.NO_OVERLAY, outlineColor, null
                        )
                    );
            }
        }
    }

    @Override
    public void submitMultiLayerBlockModel(
            PoseStack poseStack,
            List<BlockStateModelPart> modelParts,
            boolean translucent,
            int[] tintLayers,
            int lightCoords,
            int overlayCoords,
            int outlineColor
    ) {
        PoseStack.Pose pose = poseStack.last().copy();
        var submit = new net.neoforged.neoforge.client.submit.ExtendedBlockModelFeatureRenderer.Submit(
                pose, modelParts, translucent, tintLayers, lightCoords, overlayCoords, -1
        );
        if (translucent) {
            this.translucentBlocksAndItems.submit(submit);
        } else {
            this.solid.submit(submit);
        }
    }

    private static @Nullable RenderType getOutlineRenderType(RenderType renderType) {
        if (renderType.isOutline()) {
            return renderType;
        } else {
            return renderType.outline().isPresent() ? renderType.outline().get() : null;
        }
    }

    @Override
    public void submitBreakingBlockModel(PoseStack poseStack, List<BlockStateModelPart> parts, int progress, boolean isBlockTranslucent) {
        PoseStack.Pose pose = poseStack.last().copy();
        List<RenderType> destroyTypes = Minecraft.getInstance().gameRenderer.useImprovedTransparency() && isBlockTranslucent
            ? ModelBakery.DESTROY_TYPES_OIT
            : ModelBakery.DESTROY_TYPES;
        SimpleFeatureRenderPhase targetPhase = isBlockTranslucent ? this.breakingOverlay : this.solid;
        targetPhase.submit(
            new BlockModelFeatureRenderer.Submit(
                pose, destroyTypes.get(progress), List.copyOf(parts), BlockModelRenderState.EMPTY_TINTS, 15728880, OverlayTexture.NO_OVERLAY, 0, pose
            )
        );
    }

    @Override
    public void submitShapeOutline(PoseStack poseStack, VoxelShape shape, RenderType renderType, int color, float width, boolean afterTerrain) {
        ShapeOutlineFeatureRenderer.Submit submit = new ShapeOutlineFeatureRenderer.Submit(poseStack.last().copy(), shape, renderType, color, width);
        if (ARGB.alpha(color) == 255) {
            this.solid.submit(submit);
        } else if (afterTerrain) {
            this.afterTerrain.submit(submit);
        } else {
            this.shapeOutlines.submit(submit);
        }
    }

    @Override
    public void submitItem(
        PoseStack poseStack,
        ItemDisplayContext displayContext,
        int lightCoords,
        int overlayCoords,
        int outlineColor,
        int[] tintLayers,
        ItemQuads quads,
        ItemStackRenderState.FoilType foilType
    ) {
        PoseStack.Pose pose = poseStack.last().copy();
        if (!quads.translucent().isEmpty()) {
            this.translucentBlocksAndItems
                .submit(new ItemFeatureRenderer.Submit(pose, displayContext, lightCoords, overlayCoords, 0, tintLayers, quads.translucent(), foilType));
        }

        if (!quads.solid().isEmpty()) {
            this.solid.submit(new ItemFeatureRenderer.Submit(pose, displayContext, lightCoords, overlayCoords, 0, tintLayers, quads.solid(), foilType));
        }

        if (outlineColor != 0) {
            this.outline
                .submit(
                    new ItemFeatureRenderer.Submit(
                        pose,
                        displayContext,
                        15728880,
                        OverlayTexture.NO_OVERLAY,
                        outlineColor,
                        ItemStackRenderState.LayerRenderState.EMPTY_TINTS,
                        quads.all(),
                        ItemStackRenderState.FoilType.NONE
                    )
                );
        }
    }

    @Override
    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer) {
        CustomFeatureRenderer.Submit submit = new CustomFeatureRenderer.Submit(poseStack.last().copy(), renderType, customGeometryRenderer);
        if (renderType.isOutline()) {
            this.outline.submit(submit);
        } else if (renderType.hasBlending()) {
            this.translucentCustomGeometry.submit(submit);
        } else {
            this.solid.submit(submit);
        }
    }

    @Override
    public void submitQuadParticleGroup(QuadParticleRenderState particles) {
        this.solid.submit(new QuadParticleFeatureRenderer.Submit(particles, false));
        this.afterTerrain.submit(new QuadParticleFeatureRenderer.Submit(particles, true));
    }

    @Override
    public void submitGizmoPrimitives(DrawableGizmoPrimitives.Group group, CameraRenderState camera, boolean onTop) {
        GizmoFeatureRenderer.Submit submit = new GizmoFeatureRenderer.Submit(group, camera);
        if (onTop) {
            this.alwaysOnTopGizmos.submit(submit);
        } else if (group.opaque()) {
            this.solid.submit(submit);
        } else {
            this.translucentGizmos.submit(submit);
        }
    }

    @Override
    public <T extends net.minecraft.client.renderer.feature.submit.SubmitNode, S extends T> void submitSpecial(
        net.neoforged.neoforge.client.submit.RenderPhaseKey<T> phaseKey, S submitNode
    ) {
        phaseKey.resolve(this).submit(submitNode);
    }

    public List<FeatureRenderPhase<?>> allPhases() {
        return this.allPhases;
    }
}
