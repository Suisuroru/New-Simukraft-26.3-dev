package client.cn.kafei.simukraft.mixin;

import client.cn.kafei.simukraft.client.freecamera.FreeCameraManager;
import client.cn.kafei.simukraft.client.freecamera.FreeCameraScreen;
import client.cn.kafei.simukraft.client.path.NpcPathDebugRenderer;
import client.cn.kafei.simukraft.client.rts.RtsSelectionManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public final class MixinKeyboardHandler {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void simukraft$keyPress(long window, int action, KeyEvent keyEvent, CallbackInfo callbackInfo) {
        int key = keyEvent.key();
        int scanCode = keyEvent.keycode();
        int modifiers = keyEvent.modifiers();
        if (NpcPathDebugRenderer.handleToggleShortcut(window, key, action, modifiers)) {
            callbackInfo.cancel();
            return;
        }
        if (RtsSelectionManager.handleEscapeKey(key, action)) {
            callbackInfo.cancel();
            return;
        }
        if (RtsSelectionManager.handlePreviewMovementKey(key, scanCode, action)) {
            callbackInfo.cancel();
            return;
        }
        if (!FreeCameraManager.isActive()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.gui.screen() instanceof FreeCameraScreen) && minecraft.gui.screen() != null) {
            return;
        }
        boolean pressed = action == InputConstants.PRESS;
        boolean released = action == InputConstants.RELEASE;
        if (!pressed && !released) {
            return;
        }
        boolean state = pressed;
        if (minecraft.options.keyUp.matches(keyEvent)) {
            FreeCameraManager.setMovingForward(state);
            callbackInfo.cancel();
        } else if (minecraft.options.keyDown.matches(keyEvent)) {
            FreeCameraManager.setMovingBackward(state);
            callbackInfo.cancel();
        } else if (minecraft.options.keyLeft.matches(keyEvent)) {
            FreeCameraManager.setMovingLeft(state);
            callbackInfo.cancel();
        } else if (minecraft.options.keyRight.matches(keyEvent)) {
            FreeCameraManager.setMovingRight(state);
            callbackInfo.cancel();
        } else if (minecraft.options.keyJump.matches(keyEvent)) {
            FreeCameraManager.setMovingUp(state);
            callbackInfo.cancel();
        } else if (minecraft.options.keyShift.matches(keyEvent)) {
            FreeCameraManager.setMovingDown(state);
            callbackInfo.cancel();
        } else if (minecraft.options.keySprint.matches(keyEvent)) {
            FreeCameraManager.setSprinting(state);
            callbackInfo.cancel();
        }
    }
}
