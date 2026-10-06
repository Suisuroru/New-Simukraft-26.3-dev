package client.cn.kafei.simukraft.client.bridge;

import client.cn.kafei.simukraft.client.buildbox.BuildBoxScreenOpener;
import client.cn.kafei.simukraft.client.manifest.ManifestScreen;
import common.cn.kafei.simukraft.clientbridge.ClientInteractionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * ClientInteractionHandlerImpl: 客户端交互实现，二次封装具体界面调用。
 */
public final class ClientInteractionHandlerImpl implements ClientInteractionHandler {
    public static final ClientInteractionHandlerImpl INSTANCE = new ClientInteractionHandlerImpl();

    private ClientInteractionHandlerImpl() {
    }

    @Override
    public void openBuildBox(BlockPos pos) {
        BuildBoxScreenOpener.open(pos);
    }

    @Override
    public void openManifest(ItemStack stack, InteractionHand hand) {
        ManifestScreen.open(stack, hand);
    }
}
