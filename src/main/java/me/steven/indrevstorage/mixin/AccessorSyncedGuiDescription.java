package me.steven.indrevstorage.mixin;

import io.github.cottonmc.cotton.gui.SyncedGuiDescription;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SyncedGuiDescription.class)
public interface AccessorSyncedGuiDescription {

    @Invoker("swapHotbar")
    boolean indrevstorage_swapHotbar(ItemStack toInsert, int slotNumber, Inventory inventory, PlayerEntity player);
}
