package me.steven.indrevstorage.api.impl

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import me.steven.indrevstorage.api.IRDSInventory
import me.steven.indrevstorage.api.ItemType
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag

class IRDSHardDriveInventory(private val map: Object2IntOpenHashMap<ItemType> = Object2IntOpenHashMap()) : IRDSInventory {

    override operator fun get(itemType: ItemType) = map.getInt(itemType)

    override fun has(itemType: ItemType, count: Int) = map.getInt(itemType) >= count

    override fun forEach(f: (ItemType, Int) -> Unit) {
        map.forEach { (type, count) ->  f(type, count) }
    }

    override fun extract(itemType: ItemType, count: Int): Int {
        val has = map.getInt(itemType)
        val max = count.coerceAtMost(has)
        if (has == max)
            map.removeInt(itemType)
        else
            map.addTo(itemType, -max)
        return max
    }

    override fun insert(itemType: ItemType, count: Int): Int {
        map.addTo(itemType, count)
        return 0
    }

    fun toNbt(): ListTag {
        val list = ListTag()
        map.forEach { (type, count) ->
            val t = type.toNbt()
            t.putInt("c", count)
            list.add(t)
        }
        return list
    }

    companion object {
        fun fromNbt(inv: IRDSHardDriveInventory, list: ListTag) {
            list.forEach { entry ->
                if (entry !is CompoundTag) return@forEach
                val itemType = ItemType.fromNbt(entry)
                val count = entry.getInt("c")
                inv.map.addTo(itemType, count)
            }
        }
    }
}