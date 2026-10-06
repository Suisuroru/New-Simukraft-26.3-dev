package common.cn.kafei.simukraft.crafting;

import com.mojang.serialization.MapCodec;
import common.cn.kafei.simukraft.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;


public final class ManifestClearRecipe extends CustomRecipe {
    public static final ManifestClearRecipe INSTANCE = new ManifestClearRecipe();
    public static final MapCodec<ManifestClearRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, ManifestClearRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<ManifestClearRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public ManifestClearRecipe() {
        super();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int manifestCount = 0;
        boolean hasData = false;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (!stack.is(ModItems.MANIFEST.get())) {
                return false;
            }
            manifestCount++;
            hasData = !stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).isEmpty();
        }
        return manifestCount == 1 && hasData;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return new ItemStack(ModItems.MANIFEST.get());
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }
}
