package client.cn.kafei.simukraft.client.freecamera;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

public final class CameraMouseLock {
    private static boolean locked;

    private CameraMouseLock() {
    }

    public static void setLocked(boolean locked) {
        CameraMouseLock.locked = locked;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        minecraft.execute(() -> {
            if (minecraft.getWindow() == null) {
                return;
            }
            double x = minecraft.mouseHandler.xpos();
            double y = minecraft.mouseHandler.ypos();
            if (locked) {
                InputConstants.grabMouse(minecraft.getWindow(), x, y);
            } else {
                InputConstants.releaseMouse(minecraft.getWindow(), x, y);
            }
        });
    }

    public static boolean isLocked() {
        return locked;
    }
}
