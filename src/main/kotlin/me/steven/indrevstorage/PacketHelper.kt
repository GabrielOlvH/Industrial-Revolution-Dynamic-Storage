package me.steven.indrevstorage

import me.steven.indrevstorage.api.ItemType
import me.steven.indrevstorage.api.MappedItemType
import me.steven.indrevstorage.blockentities.HardDriveRackBlockEntity
import me.steven.indrevstorage.gui.HardDriveRackScreenHandler
import me.steven.indrevstorage.utils.identifier
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.item.ItemStack

object PacketHelper {

    val CLICK_IRDSINV_SLOT = identifier("click_irdsinv_slot")

    fun registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(CLICK_IRDSINV_SLOT) { server, player, _, buf, _ ->
            val index = buf.readByte().toInt()
            val isCrouching = buf.readBoolean()

            server.execute {
                val screenHandler = player.currentScreenHandler as? HardDriveRackScreenHandler ?: return@execute
                val blockEntity = player.world.getBlockEntity(screenHandler.pos) as? HardDriveRackBlockEntity ?: return@execute
                val (type, invs) = if (index >= screenHandler.mappedTypes.size) MappedItemType.EMPTY else screenHandler.mappedTypes[index]
                val cursorStack = player.inventory.cursorStack
                if (cursorStack.isEmpty) {
                    // EXTRACT
                    val maxAmount = if (isCrouching) 64 else 1
                    var extracted = 0
                    for (inv in invs) {
                        val c = inv[type]
                        extracted += c.coerceAtMost(maxAmount)
                        val before = inv.map.addTo(type, -extracted)
                        if (extracted >= before) inv.map.removeInt(type)
                        if (extracted >= maxAmount) break
                    }
                    if (extracted > 0) {
                        player.inventory.cursorStack = type.toItemStack(extracted)
                        player.updateCursorStack()
                        blockEntity.markDirty()
                        blockEntity.sync()
                    }
                } else {
                    // INSERT
                    val typeToInsert = ItemType(cursorStack.item, cursorStack.tag)
                    val remaining = cursorStack.count
                    for (inv in blockEntity.drivesInv.sortedByDescending { it?.has(typeToInsert) }.filterNotNull()) {
                        inv.map.addTo(typeToInsert, remaining)
                        break
                    }
                    player.inventory.cursorStack = ItemStack.EMPTY
                    player.updateCursorStack()
                    blockEntity.markDirty()
                    blockEntity.sync()
                }
            }
        }
    }
}