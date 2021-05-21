package me.steven.indrevstorage.mixin;

import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemUsageContext.class)
public interface AccessorItemUsageContext {
    @Invoker("getHitResult")
    BlockHitResult indrevstorage_getHitResult();
}
