package me.steven.indrevstorage.api.gui

import me.steven.indrevstorage.api.ItemType

data class CountedItemType(val type: ItemType, val count: Int) {

    companion object {
        val EMPTY = CountedItemType(ItemType.EMPTY, 0)
    }
}