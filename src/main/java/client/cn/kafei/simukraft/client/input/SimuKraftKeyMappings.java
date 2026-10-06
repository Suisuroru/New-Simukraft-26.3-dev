package client.cn.kafei.simukraft.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import common.cn.kafei.simukraft.SimuKraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public final class SimuKraftKeyMappings {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(SimuKraft.MOD_ID, "simukraft"));

    public static final KeyMapping RTS_TOGGLE = key("key.simukraft.rts.toggle", InputConstants.KEY_F12);
    public static final KeyMapping RTS_DELETE = key("key.simukraft.rts.delete", InputConstants.KEY_DELETE);
    public static final KeyMapping RTS_MINIMAP_TOGGLE = key("key.simukraft.rts.minimap_toggle", InputConstants.KEY_M);

    public static final KeyMapping SELECTION_POINT_1 = mouse("key.simukraft.selection.point1", InputConstants.MOUSE_BUTTON_LEFT);
    public static final KeyMapping SELECTION_POINT_2 = mouse("key.simukraft.selection.point2", InputConstants.MOUSE_BUTTON_RIGHT);
    public static final KeyMapping SELECTION_CONFIRM = key("key.simukraft.selection.confirm", InputConstants.KEY_RETURN);
    public static final KeyMapping SELECTION_CANCEL = key("key.simukraft.selection.cancel", InputConstants.KEY_ESCAPE);

    public static final KeyMapping PREVIEW_MOVE_FORWARD = key("key.simukraft.preview.move_forward", InputConstants.KEY_UP);
    public static final KeyMapping PREVIEW_MOVE_BACKWARD = key("key.simukraft.preview.move_backward", InputConstants.KEY_DOWN);
    public static final KeyMapping PREVIEW_MOVE_LEFT = key("key.simukraft.preview.move_left", InputConstants.KEY_LEFT);
    public static final KeyMapping PREVIEW_MOVE_RIGHT = key("key.simukraft.preview.move_right", InputConstants.KEY_RIGHT);
    public static final KeyMapping PREVIEW_MOVE_UP = key("key.simukraft.preview.move_up", InputConstants.KEY_EQUALS);
    public static final KeyMapping PREVIEW_MOVE_DOWN = key("key.simukraft.preview.move_down", InputConstants.KEY_MINUS);
    public static final KeyMapping PREVIEW_ROTATE = key("key.simukraft.preview.rotate", InputConstants.KEY_R);
    public static final KeyMapping PREVIEW_CONFIRM = key("key.simukraft.preview.confirm", InputConstants.KEY_RETURN);
    public static final KeyMapping PREVIEW_CANCEL = key("key.simukraft.preview.cancel", InputConstants.KEY_ESCAPE);
    public static final KeyMapping PREVIEW_TOGGLE_HUD = key("key.simukraft.preview.toggle_hud", InputConstants.KEY_TAB);

    private SimuKraftKeyMappings() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(RTS_TOGGLE);
        event.register(RTS_DELETE);
        event.register(RTS_MINIMAP_TOGGLE);
        event.register(SELECTION_POINT_1);
        event.register(SELECTION_POINT_2);
        event.register(SELECTION_CONFIRM);
        event.register(SELECTION_CANCEL);
        event.register(PREVIEW_MOVE_FORWARD);
        event.register(PREVIEW_MOVE_BACKWARD);
        event.register(PREVIEW_MOVE_LEFT);
        event.register(PREVIEW_MOVE_RIGHT);
        event.register(PREVIEW_MOVE_UP);
        event.register(PREVIEW_MOVE_DOWN);
        event.register(PREVIEW_ROTATE);
        event.register(PREVIEW_CONFIRM);
        event.register(PREVIEW_CANCEL);
        event.register(PREVIEW_TOGGLE_HUD);
    }

    public static boolean matches(KeyMapping mapping, KeyEvent event) {
        return mapping != null && mapping.matches(event);
    }

    public static boolean matches(KeyMapping mapping, int keyCode, int scanCode) {
        return matches(mapping, new KeyEvent(keyCode, scanCode, 0));
    }

    public static boolean matchesMouse(KeyMapping mapping, int button) {
        return mapping != null && mapping.matches(InputConstants.Type.MOUSE.getOrCreate(button));
    }

    public static Component display(KeyMapping mapping) {
        if (mapping == null) return Component.literal("?");
        InputConstants.Key key = mapping.getKey();
        if (key.getType() == InputConstants.Type.KEYBOARD) {
            String arrow = switch (key.getValue()) {
                case InputConstants.KEY_UP -> "↑";
                case InputConstants.KEY_DOWN -> "↓";
                case InputConstants.KEY_LEFT -> "←";
                case InputConstants.KEY_RIGHT -> "→";
                default -> null;
            };
            if (arrow != null) return Component.literal(arrow);
        }
        return mapping.getTranslatedKeyMessage();
    }

    private static KeyMapping key(String name, int keyCode) {
        return new KeyMapping(name, InputConstants.Type.KEYBOARD, keyCode, CATEGORY);
    }

    private static KeyMapping mouse(String name, int button) {
        return new KeyMapping(name, InputConstants.Type.MOUSE, button, CATEGORY);
    }
}
