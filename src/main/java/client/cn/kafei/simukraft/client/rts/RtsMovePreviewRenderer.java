package client.cn.kafei.simukraft.client.rts;

import client.cn.kafei.simukraft.client.buildbox.BuildingPreviewRenderer;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/** RTS 移动预览渲染器：复用建筑预览的几何提交。 */
public final class RtsMovePreviewRenderer {
    private RtsMovePreviewRenderer() {
    }

    /** onRender: 提交当前抓取物的预览方块。 */
    public static void onRender(SubmitCustomGeometryEvent event) {
        if (RtsMovePreviewManager.isActive()) {
            BuildingPreviewRenderer.submitMesh(RtsMovePreviewManager.mesh(), event);
        }
    }
}
