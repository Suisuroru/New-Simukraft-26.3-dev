package client.cn.kafei.simukraft.client.mineraldrilling;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import common.cn.kafei.simukraft.network.mineraldrilling.MineralDrillingControlBoxOpenRequestPacket;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;

/** MineralDrillingControlBoxScreenOpener: 从客户端辅助界面请求重新打开钻井容器。 */
public final class MineralDrillingControlBoxScreenOpener {
    private MineralDrillingControlBoxScreenOpener() {
    }

    /** request: 向服务端发送经过权威校验的控制箱打开请求。 */
    public static void request(BlockPos boxPos) {
        if (boxPos != null) {
            ClientPacketDistributor.sendToServer(new MineralDrillingControlBoxOpenRequestPacket(boxPos));
        }
    }
}
