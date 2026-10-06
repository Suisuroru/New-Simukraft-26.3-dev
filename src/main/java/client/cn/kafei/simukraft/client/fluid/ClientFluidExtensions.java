package client.cn.kafei.simukraft.client.fluid;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import common.cn.kafei.simukraft.SimuKraft;
import common.cn.kafei.simukraft.registry.ModFluidTypes;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@OnlyIn(Dist.CLIENT)
public final class ClientFluidExtensions {
    private static final Identifier MILK_STILL = Identifier.fromNamespaceAndPath(SimuKraft.MOD_ID, "block/milk_still");
    private static final Identifier MILK_FLOWING = Identifier.fromNamespaceAndPath(SimuKraft.MOD_ID, "block/milk_flow");

    private ClientFluidExtensions() {
    }

    public static void register(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public Identifier getStillTexture() {
                return MILK_STILL;
            }

            @Override
            public Identifier getFlowingTexture() {
                return MILK_FLOWING;
            }

            @Override
            public int getTintColor() {
                return 0xFFFFFFFF;
            }
        }, ModFluidTypes.MILK);
    }
}
