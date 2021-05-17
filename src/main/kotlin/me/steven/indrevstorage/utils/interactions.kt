package me.steven.indrevstorage.utils

import me.steven.indrevstorage.api.IRDSNetwork
import me.steven.indrevstorage.api.ItemType
import me.steven.indrevstorage.api.gui.TerminalConnection
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity

fun interactTerminalWithCursor(player: ServerPlayerEntity, network: IRDSNetwork, connection: TerminalConnection, index: Int, isCrouching: Boolean) {
    val type = if (index >= connection.serverCache.size) ItemType.EMPTY else connection.serverCache[index]
    val cursorStack = player.inventory.cursorStack
    if (cursorStack.isEmpty) {
        // EXTRACT
        val maxAmount = if (isCrouching) 64 else 1
        val extracted = network.extract(type, maxAmount)
        if (extracted > 0) {
            player.inventory.cursorStack = type.toItemStack(extracted)
            player.updateCursorStack()
            network.markDirty()
        }
    } else {
        // INSERT
        val typeToInsert = ItemType(cursorStack.item, cursorStack.tag)
        val remaining = network.insert(typeToInsert, cursorStack.count)

        if (remaining != cursorStack.count) {
            player.inventory.cursorStack = if (remaining == 0) ItemStack.EMPTY else ItemStack(cursorStack.item, remaining)
            player.updateCursorStack()
            network.markDirty()
        }
    }
}