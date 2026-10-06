package client.cn.kafei.simukraft.mixin;

import client.cn.kafei.simukraft.client.freecamera.FreeCameraManager;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class MixinCamera {
    @Shadow
    private @org.jspecify.annotations.Nullable Entity entity;

    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot, float roll);

    /**
     * simukraft$update: 原版相机对齐实体后覆盖为独立 RTS/自由相机姿态。
     */
    @Inject(method = "update", at = @At("TAIL"))
    private void simukraft$update(DeltaTracker deltaTracker, CallbackInfo callbackInfo) {
        if (FreeCameraManager.isActive() && entity instanceof LocalPlayer) {
            setPosition(FreeCameraManager.getPosition());
            setRotation(FreeCameraManager.getYaw(), FreeCameraManager.getPitch(), 0.0F);
        }
    }

    /**
     * simukraft$applyRtsProjection: 用 RTS 正交矩阵替换提取后的透视投影。
     */
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void simukraft$applyRtsProjection(CameraRenderState cameraState, DeltaTracker deltaTracker, CallbackInfo callbackInfo) {
        if (FreeCameraManager.isRtsActive()) {
            cameraState.projectionMatrix.set(FreeCameraManager.rtsProjectionMatrix());
        }
    }
}
