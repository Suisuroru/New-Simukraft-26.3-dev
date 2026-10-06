package common.cn.kafei.simukraft.exchange;

import common.cn.kafei.simukraft.SimuKraft;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

/** ExchangeReloadEvents: 注册上市公司数据包重载。 */
@EventBusSubscriber(modid = SimuKraft.MOD_ID)
public final class ExchangeReloadEvents {
    private static final Identifier LISTENER_KEY =
            Identifier.fromNamespaceAndPath(SimuKraft.MOD_ID, "exchange_companies");

    private ExchangeReloadEvents() {
    }

    /** onAddReloadListener: 加入服务器数据包重载。 */
    @SubscribeEvent
    public static void onAddReloadListener(AddServerReloadListenersEvent event) {
        event.addListener(LISTENER_KEY, ExchangeCompanyLoader.INSTANCE);
    }
}
