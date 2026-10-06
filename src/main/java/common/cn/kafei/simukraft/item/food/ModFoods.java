package common.cn.kafei.simukraft.item.food;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.List;


public final class ModFoods {

    private ModFoods() {
    }

    private static MobEffectInstance effect(Holder<MobEffect> effect, int duration, int amplifier) {
        return new MobEffectInstance(effect, duration, amplifier);
    }

    public static final BuffFoodItem.FoodDefinition HAMBURGER = BuffFoodItem.createFood(
            8, 0.8f,
            List.of(
                    new BuffFoodItem.EffectEntry(() -> effect(MobEffects.SATURATION, 1, 0), 1.0f),
                    new BuffFoodItem.EffectEntry(() -> effect(MobEffects.STRENGTH, 600, 0), 0.8f)
            )
    );

    public static final BuffFoodItem.FoodDefinition FRENCH_FRIES = BuffFoodItem.createFood(
            4, 0.4f,
            List.of(
                    new BuffFoodItem.EffectEntry(() -> effect(MobEffects.SPEED, 400, 0), 1.0f),
                    new BuffFoodItem.EffectEntry(() -> effect(MobEffects.HASTE, 300, 0), 0.6f)
            )
    );

    public static final BuffFoodItem.FoodDefinition CHEESE_CHUNK = BuffFoodItem.createFood(
            2, 0.3f,
            List.of(
                    new BuffFoodItem.EffectEntry(() -> effect(MobEffects.REGENERATION, 200, 0), 1.0f),
                    new BuffFoodItem.EffectEntry(() -> effect(MobEffects.RESISTANCE, 300, 0), 0.5f)
            )
    );

    public static final BuffFoodItem.FoodDefinition CHEESE_BURGER = BuffFoodItem.createFood(
            12, 1.0f,
            List.of(
                    new BuffFoodItem.EffectEntry(() -> effect(MobEffects.SATURATION, 1, 0), 1.0f),
                    new BuffFoodItem.EffectEntry(() -> effect(MobEffects.STRENGTH, 1200, 1), 1.0f),
                    new BuffFoodItem.EffectEntry(() -> effect(MobEffects.REGENERATION, 300, 0), 1.0f),
                    new BuffFoodItem.EffectEntry(() -> effect(MobEffects.ABSORPTION, 1200, 0), 0.8f)
            )
    );
}
