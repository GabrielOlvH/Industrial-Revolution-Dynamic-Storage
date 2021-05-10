package me.steven.indrevstorage.api

data class MappedItemType(val type: ItemType, val invs: Set<IRDSInventory>) {

    companion object {
        val EMPTY = MappedItemType(ItemType.EMPTY, emptySet())
    }
}