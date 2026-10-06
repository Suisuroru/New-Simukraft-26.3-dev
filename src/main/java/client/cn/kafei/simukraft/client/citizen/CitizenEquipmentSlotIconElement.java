package client.cn.kafei.simukraft.client.citizen;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import common.cn.kafei.simukraft.citizen.CitizenInventory;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * 在 LDLib 槽位上补绘原版玩家装备空槽图标。
 */

public final class CitizenEquipmentSlotIconElement extends UIElement {
    private final CitizenInventory inventory;
    private final int inventorySlot;
    private final Identifier icon;
    private final Identifier directTexture;

    public CitizenEquipmentSlotIconElement(CitizenInventory inventory, int inventorySlot, Identifier icon) {
        this(inventory, inventorySlot, icon, null);
    }

    private CitizenEquipmentSlotIconElement(CitizenInventory inventory,
                                            int inventorySlot,
                                            Identifier icon,
                                            Identifier directTexture) {
        this.inventory = inventory;
        this.inventorySlot = inventorySlot;
        this.icon = icon;
        this.directTexture = directTexture;
        setAllowHitTest(false);
    }

    /**
     * mainHand：创建仅在主手为空时显示的指定剑形空槽贴图。
     */
    public static CitizenEquipmentSlotIconElement mainHand(CitizenInventory inventory, int inventorySlot) {
        return new CitizenEquipmentSlotIconElement(inventory, inventorySlot, null,
                Identifier.fromNamespaceAndPath("simukraft", "textures/gui/citizen_main_hand_slot.png"));
    }

    /**
     * drawBackgroundAdditional：仅在对应装备槽为空时绘制原版图集精灵。
     */
    @Override
    public void drawBackgroundAdditional(IGUIContext raw) {
        GUIContext context = (GUIContext) raw;
        if (inventory == null || !inventory.getItem(inventorySlot).isEmpty()) {
            return;
        }
        int x = Math.round(getPositionX());
        int y = Math.round(getPositionY());
        if (directTexture != null) {
            context.graphics.blit(RenderPipelines.GUI_TEXTURED, directTexture, x, y, 0, 0,
                    Math.round(getSizeWidth()), Math.round(getSizeHeight()), 16, 16);
            return;
        }
        context.graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, x, y,
                Math.round(getSizeWidth()), Math.round(getSizeHeight()));
    }
}
