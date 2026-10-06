package common.cn.kafei.simukraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import common.cn.kafei.simukraft.item.CoinItems;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStackResourceHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 单格处理器按资源算容量，取的是 min(物品上限, Item.ABSOLUTE_MAX_STACK_SIZE)，26.3 里后者是 99。
 */
@Mixin(ItemStackResourceHandler.class)
public abstract class MixinCoinSingleSlotLimit {
    /**
     * simukraft$raiseCoinSingleSlotCapacity：单格存储处理器放行钱币那一档截断。
     */
    @ModifyReturnValue(method = "getCapacity(Lnet/neoforged/neoforge/transfer/item/ItemResource;)I", at = @At("RETURN"))
    private int simukraft$raiseCoinSingleSlotCapacity(int original, @Local(argsOnly = true) ItemResource resource) {
        return CoinItems.raiseCapacity(original, resource.getMaxStackSize());
    }
}
