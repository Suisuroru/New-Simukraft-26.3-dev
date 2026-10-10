package client.cn.kafei.simukraft.client.city.map;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

/**
 * 立体沙盘的自定义 GUI 渲染状态：MC 26.3 移除了 {@code RenderSystem} 的即时状态开关，
 * 需要把网格顶点封装成 {@link GuiElementRenderState} 交给渲染管线统一提交。
 */
public record CityMap3DRenderState(
        SimuMap3DMesh mesh,
        Matrix3x2fc pose,
        float yaw,
        float scale,
        double centerScreenX,
        double centerScreenY,
        int liveCenterX,
        int liveCenterZ,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    public CityMap3DRenderState(
            SimuMap3DMesh mesh, Matrix3x2fc pose, float yaw, float scale,
            double centerScreenX, double centerScreenY, int liveCenterX, int liveCenterZ,
            int areaX, int areaY, int areaWidth, int areaHeight,
            @Nullable ScreenRectangle scissorArea
    ) {
        this(mesh, pose, yaw, scale, centerScreenX, centerScreenY, liveCenterX, liveCenterZ,
                scissorArea, computeBounds(areaX, areaY, areaWidth, areaHeight, pose, scissorArea));
    }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer) {
        this.mesh.emit(vertexConsumer, this.pose, this.yaw, this.scale,
                this.centerScreenX, this.centerScreenY, this.liveCenterX, this.liveCenterZ);
    }

    @Override
    public RenderPipeline pipeline() {
        return RenderPipelines.GUI;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    private static @Nullable ScreenRectangle computeBounds(int x, int y, int width, int height,
                                                           Matrix3x2fc pose, @Nullable ScreenRectangle scissorArea) {
        ScreenRectangle bounds = new ScreenRectangle(x, y, width, height).transformMaxBounds(pose);
        return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
    }
}
