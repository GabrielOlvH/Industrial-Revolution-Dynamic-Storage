package me.steven.indrevstorage.api.gui

import me.steven.indrevstorage.api.IRDSInventory
import me.steven.indrevstorage.api.ItemType

data class StoredItemType(val type: ItemType, val invs: Set<IRDSInventory>) {

    companion object {
        val EMPTY = StoredItemType(ItemType.EMPTY, emptySet())
    }
}