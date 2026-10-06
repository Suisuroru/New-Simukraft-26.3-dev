package client.cn.kafei.simukraft.client.fluid;

import common.cn.kafei.simukraft.registry.ModFluidTypes;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

public final class ClientFluidExtensions {
    private ClientFluidExtensions() {
    }

    public static void register(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
        }, ModFluidTypes.MILK);
    }
}
