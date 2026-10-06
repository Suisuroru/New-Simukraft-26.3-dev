package common.cn.kafei.simukraft.mixin;

import common.cn.kafei.simukraft.fluid.MilkBucketPlacementService;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public abstract class MixinMilkBucketItem {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void simukraft$pourMilk(Level level,
                                    Player player,
                                    InteractionHand hand,
                                    CallbackInfoReturnable<InteractionResult> callbackInfo) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!itemStack.is(Items.MILK_BUCKET)) {
            return;
        }
        InteractionResult result = MilkBucketPlacementService.tryPourMilk(level, player, hand);
        if (result != null) {
            callbackInfo.setReturnValue(result);
        }
    }
}
