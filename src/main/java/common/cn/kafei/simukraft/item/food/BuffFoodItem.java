package common.cn.kafei.simukraft.item.food;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class BuffFoodItem extends Item {
    public BuffFoodItem(Properties properties) {
        super(properties);
    }

    public static FoodDefinition createFood(
            int nutrition,
            float saturation,
            @NotNull List<EffectEntry> effects) {
        FoodProperties.Builder builder = new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationModifier(saturation);

        Consumable.Builder consumable = Consumable.builder();
        for (EffectEntry entry : effects) {
            consumable.onConsume(new ApplyStatusEffectsConsumeEffect(
                    Objects.requireNonNull(entry.effectSupplier().get()), entry.probability()));
        }

        return new FoodDefinition(builder.build(), consumable.build());
    }

    public record FoodDefinition(FoodProperties food, Consumable consumable) {
    }

    public record EffectEntry(@NotNull Supplier<MobEffectInstance> effectSupplier, float probability) {
    }
}
