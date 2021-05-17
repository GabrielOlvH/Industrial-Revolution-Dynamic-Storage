package me.steven.indrevstorage.api.gui

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import me.steven.indrevstorage.PacketHelper
import me.steven.indrevstorage.api.ItemType
import me.steven.indrevstorage.blockentities.HardDriveRackBlockEntity
import me.steven.indrevstorage.blockentities.TerminalBlockEntity
import me.steven.indrevstorage.gui.TerminalScreenHandler
import me.steven.indrevstorage.utils.componentOf
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.client.resource.language.I18n
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.Registry
import net.minecraft.world.World
import kotlin.math.ceil

/**
 * Quick sum so I don't forget how things work:
 *
 * TerminalConnection is an utility class for cleaner code. It keeps track of ItemTypes and manages filtering.
 *
 * - Operations
 *      When an operation is made or a terminal is opened, IRDSNetwork#markDirty is called which
 *      which causes the network to update the TerminalConnection on the server-side and then sync to the client
 *      Both server-side and client-side will have the connection sorted by raw id
 *      The client will reorder the indexes to be ordered by ItemType count, apply the filter and then send the new indexes to the server
 *
 * - Filtering
 *      When the search bar is updated, the client will send a packet with the new order and indexes based off of the sortedByIds lists
 *      The server will update the serverCache using these indexes
 *
 * - Inserting and extracting
 *      When clicking on a terminal slot, a packet with the slot index will be sent to the server
 *      If the cursor stack is empty, the server will try to extract the ItemType from that index
 *      If the cursor stack is not empty, the server will try to insert the ItemType matching the cursor stack
 */
class TerminalConnection(val world: World, val pos: BlockPos, val screenHandler: TerminalScreenHandler) {
    /**
     * Used by both client and server as sorting base
     *
     * This is only updated when there's a network resync
     */
    var sortedByIdTypes = emptyList<ItemType>()

    /**
     * Used by the server to keep track of the order of the ItemTypes being displayed on the terminal
     *
     * This is updated when there's a network resync or search update by the client
     */
    var serverCache = emptyList<ItemType>()

    /**
     * Used by the client as sorting based.
     * This is only updated when there's an operation, which means
     * filtering does not need resyncing contents of the network
     *
     * This is only updated when there's a network resync
     */
    var clientCacheSortedById = emptyList<CountedItemType>()

    /**
     * Used by the client to display and keep track of the order of the ItemTypes being displayed on the terminal
     *
     * This is updated when there's a network resync or search update
     */
    var clientCache = emptyList<CountedItemType>()

    fun updateServer() {
        val types = hashSetOf<ItemType>()
        val terminal = componentOf(world, pos, null)!!.convert(TerminalBlockEntity::class)
        terminal?.network?.forEach(HardDriveRackBlockEntity::class) { rack ->
            rack?.drivesInv?.forEach { inv ->
                inv?.forEach { type, _ -> types.add(type) }
            }
        }

        sortedByIdTypes = types
            .sortedWith(compareBy { Registry.ITEM.getRawId(it.item) })

        val before = getRowCount(false)
        serverCache = types
            .sortedWith(compareBy { Registry.ITEM.getRawId(it.item) })
        val after = getRowCount(false)

        screenHandler.rebuildTerminalSlots(before, after)
    }

    fun updateClient(map: Object2IntOpenHashMap<ItemType>) {
        sortedByIdTypes = map.map { it.key }
            .sortedWith(compareBy { Registry.ITEM.getRawId(it.item) })

        val before = getRowCount(true)

        this.clientCacheSortedById = map.object2IntEntrySet()
            .map { (key, value) -> key.withCount(value) }
            .sortedWith(compareBy { Registry.ITEM.getRawId(it.type.item) })

        applyFilter()

        val after = getRowCount(true)

        screenHandler.rebuildTerminalSlots(before, after)
    }

    fun applyFilter() {
        clientCache = clientCacheSortedById
            .sortedWith(compareByDescending { it.count })
        if (screenHandler.currentSearch.isNotEmpty()) {
            val search = screenHandler.currentSearch.toLowerCase()
            clientCache = clientCache
                .filter { I18n.translate(it.type.item.translationKey).toLowerCase().startsWith(search) }
        }

        val buf = PacketByteBufs.create()
        buf.writeInt(clientCache.size)
        clientCache.forEach {
            val indexOf = sortedByIdTypes.indexOf(it.type)
            if (indexOf >= 0) buf.writeInt(indexOf)
        }
        ClientPlayNetworking.send(PacketHelper.UPDATE_FILTER_TERMINAL, buf)
    }

    fun getRowCount(client: Boolean) = ceil((if (client) clientCache else serverCache).size / 9.0).coerceAtLeast(5.0).toInt()

}