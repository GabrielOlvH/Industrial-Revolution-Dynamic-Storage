package me.steven.indrevstorage

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import me.steven.indrevstorage.api.ItemType
import me.steven.indrevstorage.api.gui.StoredItemType
import me.steven.indrevstorage.blockentities.TerminalBlockEntity
import me.steven.indrevstorage.gui.TerminalScreenHandler
import me.steven.indrevstorage.utils.identifier
import me.steven.indrevstorage.utils.interactTerminalWithCursor
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.util.registry.Registry

object PacketHelper {

    val CLICK_IRDSINV_SLOT = identifier("click_irdsinv_slot")
    val REMAP_SCREEN_HANDLER = identifier("remap_screen_handler")
    val UPDATE_FILTER_TERMINAL = identifier("update_terminal_filter")

    fun registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(CLICK_IRDSINV_SLOT) { server, player, _, buf, _ ->
            val index = buf.readByte().toInt()
            val isCrouching = buf.readBoolean()

            server.execute {
                val screenHandler = player.currentScreenHandler as? TerminalScreenHandler ?: return@execute
                val blockEntity = player.world.getBlockEntity(screenHandler.pos) as? TerminalBlockEntity ?: return@execute
                val network = blockEntity.network ?: return@execute
                interactTerminalWithCursor(player, network, blockEntity, screenHandler, index, isCrouching)
            }
        }

        ServerPlayNetworking.registerGlobalReceiver(UPDATE_FILTER_TERMINAL) { server, player, _, buf, _ ->
            val size = buf.readInt()
            val order = IntArray(size)
            repeat(size) { index -> order[index] = buf.readInt() }
            server.execute {
                val screenHandler = player.currentScreenHandler as? TerminalScreenHandler ?: return@execute
                // causes mappedTypes list to resort itself
                screenHandler.remapServer()
                val newList = ArrayList<StoredItemType>(size)
                order.forEach { i -> newList.add(screenHandler.serverCache[i]) }
                screenHandler.serverCache = newList
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
                val screenHandler = client.player?.currentScreenHandler as? TerminalScreenHandler ?: return@execute
                screenHandler.remapClient(map)
            }
        }
    }

}