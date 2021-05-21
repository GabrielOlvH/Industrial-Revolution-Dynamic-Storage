package me.steven.indrevstorage.api

import me.steven.indrevstorage.api.gui.CountedItemType
import me.steven.indrevstorage.utils.with
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

data class ItemType(val item: Item, val tag: CompoundTag?) {

    fun matches(stack: ItemStack): Boolean = stack.item == item && stack.tag == tag

    fun toItemStack(count: Int = 1) = ItemStack(item, count).also { it.tag = this.tag }

    fun withCount(count: Int): CountedItemType = CountedItemType(this, count)

    fun toNbt(): CompoundTag {
        val tag = CompoundTag()
        tag.putString("id", Registry.ITEM.getId(item).toString())
        if (this.tag != null)
            tag.put("tag", this.tag)
        return tag
    }

    companion object {
        val EMPTY = ItemType(Items.AIR, null)

        fun fromNbt(tag: CompoundTag): ItemType {
            val item = Registry.ITEM.get(Identifier(tag.getString("id")))
            val tag = if (tag.contains("tag")) tag.getCompound("tag") else null
            return item with tag
        }
    }
}