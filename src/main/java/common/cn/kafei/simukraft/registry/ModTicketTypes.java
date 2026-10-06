package common.cn.kafei.simukraft.registry;

import common.cn.kafei.simukraft.SimuKraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.TicketType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;


public final class ModTicketTypes {
    private static final DeferredRegister<TicketType> TICKET_TYPES =
            DeferredRegister.create(Registries.TICKET_TYPE, SimuKraft.MOD_ID);

    public static final DeferredHolder<TicketType, TicketType> CITIZEN_RECOVERY = TICKET_TYPES.register(
            "citizen_recovery",
            () -> new TicketType(0L, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION));

    public static final DeferredHolder<TicketType, TicketType> NPC_WORK_AREA = TICKET_TYPES.register(
            "npc_work_area",
            () -> new TicketType(0L, TicketType.FLAG_LOADING));

    public static final DeferredHolder<TicketType, TicketType> RTS_VIEW = TICKET_TYPES.register(
            "rts_view",
            () -> new TicketType(0L, TicketType.FLAG_LOADING));

    private ModTicketTypes() {
    }

    public static void register(IEventBus modEventBus) {
        TICKET_TYPES.register(modEventBus);
    }
}
