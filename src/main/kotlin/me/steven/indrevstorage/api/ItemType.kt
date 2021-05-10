package me.steven.indrevstorage.api

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.CompoundTag

data class ItemType(val item: Item, val tag: CompoundTag?) {

    fun matches(stack: ItemStack): Boolean = stack.item == item && stack.tag == tag

    fun toItemStack(count: Int = 1) = ItemStack(item, count).also { it.tag = this.tag }

    companion object {
        val EMPTY = ItemType(Items.AIR, null)
    }
}