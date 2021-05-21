package me.steven.indrevstorage.mixin;

import me.steven.indrevstorage.extensions.IRDSPlayerInventoryExtension;
import me.steven.indrevstorage.utils.InteractionsKt;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInventory.class)
public class MixinPlayerInventory implements IRDSPlayerInventoryExtension {

    private int lastSlot = -1;

    @Inject(method = "insertStack(Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void indrevstorage_checkWormHoleDevice(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (InteractionsKt.interceptInsertStack((PlayerInventory) (Object) this, stack)) cir.setReturnValue(true);
    }

    @Override
    public int getCachedDeviceSlot() {
        return lastSlot;
    }

    @Override
    public void setCachedDeviceSlot(int cachedDeviceSlot) {
        this.lastSlot = cachedDeviceSlot;
    }
}
