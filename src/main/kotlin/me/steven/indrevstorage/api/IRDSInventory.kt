package me.steven.indrevstorage.api

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import me.steven.indrevstorage.utils.with
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

class IRDSInventory(val map: Object2IntOpenHashMap<ItemType> = Object2IntOpenHashMap()) {

    operator fun get(itemType: ItemType) = map.getInt(itemType)

    operator fun set(itemType: ItemType, count: Int) {
        map[itemType] = count
    }

    fun has(itemType: ItemType) = map.containsKey(itemType)

    inline fun forEach(f: (ItemType, Int) -> Unit) {
        map.forEach { (type, count) ->  f(type, count) }
    }

    fun toNbt(): ListTag {
        val list = ListTag()
        map.forEach { (type, count) ->
            val t = CompoundTag()
            t.putString("id", Registry.ITEM.getId(type.item).toString())
            if (type.tag != null)
                t.put("tag", type.tag)
            t.putInt("c", count)
            list.add(t)
        }
        return list
    }

    companion object {
        fun fromNbt(inv: IRDSInventory, list: ListTag) {
            list.forEach { entry ->
                if (entry !is CompoundTag) return@forEach
                val item = Registry.ITEM.get(Identifier(entry.getString("id")))
                val tag = if (entry.contains("tag")) entry.getCompound("tag") else null
                val count = entry.getInt("c")
                val itemType = item with tag
                inv.map.addTo(itemType, count)
            }
        }
    }
}