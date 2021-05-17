package me.steven.indrevstorage.utils

import me.steven.indrevstorage.api.IRDSNetwork
import me.steven.indrevstorage.api.ItemType
import me.steven.indrevstorage.api.gui.TerminalConnection
import me.steven.indrevstorage.blockentities.HardDriveRackBlockEntity
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity

fun interactTerminalWithCursor(player: ServerPlayerEntity, network: IRDSNetwork, connection: TerminalConnection, index: Int, isCrouching: Boolean) {
    val type = if (index >= connection.serverCache.size) ItemType.EMPTY else connection.serverCache[index]
    val cursorStack = player.inventory.cursorStack
    if (cursorStack.isEmpty) {
        // EXTRACT
        val maxAmount = if (isCrouching) 64 else 1
        var extracted = 0
        val it = network.iterator(HardDriveRackBlockEntity::class)
        outer@while (it.hasNext()) {
            val rack = it.next()
            for (inv in rack.drivesInv.sortedByDescending { it?.has(type) }.filterNotNull()) {
                val c = inv[type]
                extracted += inv.extract(type, c.coerceAtMost(maxAmount))
                if (extracted >= maxAmount) break@outer
            }
        }
        if (extracted > 0) {
            player.inventory.cursorStack = type.toItemStack(extracted)
            player.updateCursorStack()
            network.markDirty()
        }
    } else {
        // INSERT
        val typeToInsert = ItemType(cursorStack.item, cursorStack.tag)
        var remaining = cursorStack.count

        val it = network.iterator(HardDriveRackBlockEntity::class)
        outer@while (it.hasNext()) {
            val rack = it.next()
            for (inv in rack.drivesInv.sortedByDescending { it?.has(typeToInsert) }.filterNotNull()) {
                remaining = inv.insert(typeToInsert, remaining)
                if (remaining <= 0)
                    break@outer
            }
        }

        player.inventory.cursorStack = ItemStack.EMPTY
        player.updateCursorStack()
        network.markDirty()
    }
}