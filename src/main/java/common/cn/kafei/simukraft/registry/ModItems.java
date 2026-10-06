package common.cn.kafei.simukraft.registry;

import common.cn.kafei.simukraft.SimuKraft;
import common.cn.kafei.simukraft.item.ManifestItem;
import common.cn.kafei.simukraft.item.GeologicalHammerItem;
import common.cn.kafei.simukraft.item.PortableCityCoreItem;
import common.cn.kafei.simukraft.item.food.BuffFoodItem;
import common.cn.kafei.simukraft.item.food.ModFoods;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;


public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SimuKraft.MOD_ID);

    public static final DeferredHolder<Item, Item> MANIFEST = ITEMS.registerItem("manifest", ManifestItem::new);
    public static final DeferredHolder<Item, Item> PORTABLE_CITY_CORE = ITEMS.registerItem("portable_city_core", PortableCityCoreItem::new);
    public static final DeferredHolder<Item, Item> COPPER_COIN = ITEMS.registerSimpleItem("copper_coin");
    public static final DeferredHolder<Item, Item> SILVER_COIN = ITEMS.registerSimpleItem("silver_coin");
    public static final DeferredHolder<Item, Item> GOLD_COIN = ITEMS.registerSimpleItem("gold_coin");
    public static final DeferredHolder<Item, Item> HAMBURGER = ITEMS.registerItem("hamburger", BuffFoodItem::new,
            p -> p.food(ModFoods.HAMBURGER.food(), ModFoods.HAMBURGER.consumable()));
    public static final DeferredHolder<Item, Item> FRENCH_FRIES = ITEMS.registerItem("french_fries", BuffFoodItem::new,
            p -> p.food(ModFoods.FRENCH_FRIES.food(), ModFoods.FRENCH_FRIES.consumable()));
    public static final DeferredHolder<Item, Item> CHEESE_CHUNK = ITEMS.registerItem("cheese_chunk", BuffFoodItem::new,
            p -> p.food(ModFoods.CHEESE_CHUNK.food(), ModFoods.CHEESE_CHUNK.consumable()));
    public static final DeferredHolder<Item, Item> CHEESE_BURGER = ITEMS.registerItem("cheese_burger", BuffFoodItem::new,
            p -> p.food(ModFoods.CHEESE_BURGER.food(), ModFoods.CHEESE_BURGER.consumable()));
    public static final DeferredHolder<Item, Item> GEOLOGICAL_HAMMER = ITEMS.registerItem("geological_hammer", GeologicalHammerItem::new);
    public static final DeferredHolder<Item, Item> DRILL_ROD_SEGMENT = ITEMS.registerSimpleItem("drill_rod_segment");
    public static final DeferredHolder<Item, Item> SHALLOW_DRILL_BIT = ITEMS.registerItem("shallow_drill_bit", Item::new, p -> p.durability(500));
    public static final DeferredHolder<Item, Item> DEEP_DRILL_BIT = ITEMS.registerItem("deep_drill_bit", Item::new, p -> p.durability(900));

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
