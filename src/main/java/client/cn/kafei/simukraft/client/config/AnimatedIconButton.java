package client.cn.kafei.simukraft.client.config;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

import java.util.Objects;

/**
 * AnimatedIconButton：带旋转角标动画的图标按钮，用于打开配置选择界面。
 */

public final class AnimatedIconButton extends Button {

    // 主图标：模组 logo（居中显示）
    private static final Identifier MAIN_ICON =
            Objects.requireNonNull(Identifier.fromNamespaceAndPath("simukraft", "textures/gui/logo.png"));
    // 角标图标：齿轮（右下角旋转）
    private static final Identifier CORNER_ICON =
            Objects.requireNonNull(Identifier.fromNamespaceAndPath("simukraft", "textures/gui/setting_icon.png"));
    // 按钮背景纹理
    private static final Identifier WIDGETS_TEXTURE =
            Objects.requireNonNull(Identifier.fromNamespaceAndPath("simukraft", "textures/gui/widgets.png"));

    private float rotationAngle = 0.0f;
    private static final float ROTATION_SPEED = 3.0f; // 悬停旋转速度（度/帧）

    public AnimatedIconButton(int x, int y, int width, int height, OnPress onPress) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 悬停时旋转角标，松开时还原
        if (this.isHovered()) {
            rotationAngle += ROTATION_SPEED * partialTick;
            if (rotationAngle >= 360.0f) rotationAngle -= 360.0f;
        } else {
            if (rotationAngle > 0) {
                rotationAngle -= ROTATION_SPEED * 2 * partialTick;
                if (rotationAngle < 0) rotationAngle = 0;
            }
        }

        renderBackground(guiGraphics);
        renderMainIcon(guiGraphics);
        renderCornerIcon(guiGraphics);
    }

    /**
     * renderBackground：拉伸 widgets.png 作为按钮背景，悬停时加白色边框。
     */
    private void renderBackground(GuiGraphicsExtractor guiGraphics) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, WIDGETS_TEXTURE, this.getX(), this.getY(), 0, 0,
                this.width, this.height, this.width, this.height);
        if (this.isHovered()) {
            int white = 0xFFFFFFFF;
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, white);
            guiGraphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, white);
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.height, white);
            guiGraphics.fill(this.getX() + this.width - 1, this.getY(), this.getX() + this.width, this.getY() + this.height, white);
        }
    }

    /**
     * renderMainIcon：居中渲染 logo 主图标（80% 按钮尺寸）。
     */
    private void renderMainIcon(GuiGraphicsExtractor guiGraphics) {
        int size = Math.min((int) (Math.min(this.width, this.height) * 0.8f), 20);
        int x = this.getX() + (this.width - size) / 2;
        int y = this.getY() + (this.height - size) / 2;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, MAIN_ICON, x, y, 0, 0, size, size, size, size);
    }

    /**
     * renderCornerIcon：右下角渲染旋转齿轮角标（50% 按钮尺寸，悬停时旋转）。
     */
    private void renderCornerIcon(GuiGraphicsExtractor guiGraphics) {
        int size = Math.min((int) (Math.min(this.width, this.height) * 0.5f), 14);
        int cx = this.getX() + this.width - size / 2 - Math.max(2, this.width / 12);
        int cy = this.getY() + this.height - size / 2 - Math.max(2, this.height / 12);

        Matrix3x2fStack pose = guiGraphics.pose();
        pose.pushMatrix();
        pose.translate(cx, cy);
        pose.rotate((float) Math.toRadians(rotationAngle));
        pose.translate(-size / 2.0F, -size / 2.0F);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CORNER_ICON, 0, 0, 0, 0, size, size, size, size);
        pose.popMatrix();
    }
}
