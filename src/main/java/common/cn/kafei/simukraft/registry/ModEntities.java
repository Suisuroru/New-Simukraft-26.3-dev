package common.cn.kafei.simukraft.registry;

import common.cn.kafei.simukraft.SimuKraft;
import common.cn.kafei.simukraft.entity.CitizenEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;


public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, SimuKraft.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<CitizenEntity>> CITIZEN = ENTITIES.register("citizen", () -> EntityType.Builder.of(CitizenEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.8F)
            .clientTrackingRange(10)
            .updateInterval(3)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(SimuKraft.MOD_ID, "citizen"))));

    private ModEntities() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
    }
}
