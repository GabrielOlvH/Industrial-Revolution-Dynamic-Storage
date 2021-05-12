package me.steven.indrevstorage.api

interface IRDSInventory {

    fun has(itemType: ItemType, count: Int = 1): Boolean

    fun forEach(f: (ItemType, Int) -> Unit)

    operator fun get(itemType: ItemType): Int

    //returns remainder
    fun insert(itemType: ItemType, count: Int): Int
    //returns extracted amount
    fun extract(itemType: ItemType, count: Int): Int
}