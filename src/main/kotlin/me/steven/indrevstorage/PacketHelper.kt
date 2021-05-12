package me.steven.indrevstorage

import me.steven.indrevstorage.api.ItemType
import me.steven.indrevstorage.api.MappedItemType
import me.steven.indrevstorage.blockentities.HardDriveRackBlockEntity
import me.steven.indrevstorage.blockentities.TerminalBlockEntity
import me.steven.indrevstorage.gui.TerminalScreenHandler
import me.steven.indrevstorage.utils.identifier
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos

object PacketHelper {

    val CLICK_IRDSINV_SLOT = identifier("click_irdsinv_slot")
    val REMAP_SCREEN_HANDLER = identifier("remap_screen_handler")

    fun registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(CLICK_IRDSINV_SLOT) { server, player, _, buf, _ ->
            val index = buf.readByte().toInt()
            val isCrouching = buf.readBoolean()

            server.execute {
                val screenHandler = player.currentScreenHandler as? TerminalScreenHandler ?: return@execute
                val blockEntity = player.world.getBlockEntity(screenHandler.pos) as? TerminalBlockEntity ?: return@execute
                val network = blockEntity.network ?: return@execute
                val (type, invs) = if (index >= screenHandler.mappedTypes.size) MappedItemType.EMPTY else screenHandler.mappedTypes[index]
                val cursorStack = player.inventory.cursorStack
                if (cursorStack.isEmpty) {
                    // EXTRACT
                    val maxAmount = if (isCrouching) 64 else 1
                    var extracted = 0
                    for (inv in invs) {
                        val c = inv[type]
                        extracted += inv.extract(type, c.coerceAtMost(maxAmount))
                        if (extracted >= maxAmount) break
                    }
                    if (extracted > 0) {
                        player.inventory.cursorStack = type.toItemStack(extracted)
                        player.updateCursorStack()
                        network.syncHDRacks()
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
                    blockEntity.network?.syncHDRacks()
                }
            }
        }
    }

    fun registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(REMAP_SCREEN_HANDLER) { client, _, buf, _ ->
            val positions = hashSetOf<BlockPos>()
            val size = buf.readInt()
            for (x in 0 until size) {
                positions.add(buf.readBlockPos())
            }
            client.execute {
                val screenHandler = client.player?.currentScreenHandler as? TerminalScreenHandler ?: return@execute
                screenHandler.remap(positions)
            }
        }
    }

}