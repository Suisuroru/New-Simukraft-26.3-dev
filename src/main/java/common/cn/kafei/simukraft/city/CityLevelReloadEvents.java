package common.cn.kafei.simukraft.city;

import common.cn.kafei.simukraft.SimuKraft;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

/** CityLevelReloadEvents: 将城市等级定义加入数据包重载流程。 */

@EventBusSubscriber(modid = SimuKraft.MOD_ID)
public final class CityLevelReloadEvents {
    private static final Identifier LISTENER_KEY =
            Identifier.fromNamespaceAndPath(SimuKraft.MOD_ID, "city_level_definitions");

    private CityLevelReloadEvents() {
    }

    /** onAddReloadListener: 注册城市等级重载监听器。 */
    @SubscribeEvent
    public static void onAddReloadListener(AddServerReloadListenersEvent event) {
        event.addListener(LISTENER_KEY, CityLevelDefinitionLoader.INSTANCE);
    }
}
