package me.steven.indrevstorage

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import me.steven.indrevstorage.api.ItemType
import me.steven.indrevstorage.blockentities.TerminalBlockEntity
import me.steven.indrevstorage.gui.AbstractTerminalScreenHandler
import me.steven.indrevstorage.gui.InventoryTerminalScreenHandler
import me.steven.indrevstorage.utils.identifier
import me.steven.indrevstorage.utils.interactTerminalWithCursor
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.util.registry.Registry

object PacketHelper {

    val CLICK_IRDSINV_SLOT = identifier("click_irdsinv_slot")
    val REMAP_SCREEN_HANDLER = identifier("remap_screen_handler")
    val UPDATE_FILTER_TERMINAL = identifier("update_terminal_filter")
    val WORM_HOLE_DEVICE_SELECT = identifier("worm_hole_selected")

    fun registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(CLICK_IRDSINV_SLOT) { server, player, _, buf, _ ->
            val index = buf.readInt()
            val isCrouching = buf.readBoolean()

            server.execute {
                val screenHandler = player.currentScreenHandler as? InventoryTerminalScreenHandler ?: return@execute
                val blockEntity = player.world.getBlockEntity(screenHandler.pos) as? TerminalBlockEntity ?: return@execute
                val network = blockEntity.network ?: return@execute
                interactTerminalWithCursor(player, network, screenHandler.connection, index, isCrouching)
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(UPDATE_FILTER_TERMINAL) { server, player, _, buf, _ ->
            val size = buf.readInt()
            val order = IntArray(size)
            repeat(size) { index -> order[index] = buf.readInt() }
            server.execute {
                val screenHandler = player.currentScreenHandler as? AbstractTerminalScreenHandler ?: return@execute
                // causes mappedTypes list to resort itself
                val conn = screenHandler.connection
                conn.updateServer()
                val newList = ArrayList<ItemType>(size)
                order.forEach { i -> newList.add(conn.serverCache[i]) }
                conn.serverCache = newList
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(WORM_HOLE_DEVICE_SELECT) { server, player, _, buf, _ ->
            val index = buf.readInt()

            server.execute {
                val screenHandler = player.currentScreenHandler as? AbstractTerminalScreenHandler ?: return@execute
                val connection = screenHandler.connection
                val type = if (index >= connection.serverCache.size) ItemType.EMPTY else connection.serverCache[index]
                player.mainHandStack.orCreateTag.put("Type", type.toNbt())
                screenHandler.close(player)
            }
        }
    }

    fun registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(REMAP_SCREEN_HANDLER) { client, _, buf, _ ->
            val map = Object2IntOpenHashMap<ItemType>()
            val size = buf.readInt()
            for (x in 0 until size) {
                val itemId = buf.readInt()
                val count = buf.readInt()
                val hasTag = buf.readBoolean()
                val tag = if(hasTag) buf.readCompoundTag() else null
                val item = Registry.ITEM.get(itemId)
                map[ItemType(item, tag)] = count
            }
            client.execute {
                val screenHandler = client.player?.currentScreenHandler as? AbstractTerminalScreenHandler ?: return@execute
                screenHandler.connection.updateClient(map)
            }
        }
    }

}