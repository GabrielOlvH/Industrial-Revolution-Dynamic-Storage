package me.steven.indrevstorage.gui

import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WScrollPanel
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import me.steven.indrevstorage.IRDynamicStorage
import me.steven.indrevstorage.PacketHelper
import me.steven.indrevstorage.api.IRDSInventory
import me.steven.indrevstorage.api.ItemType
import me.steven.indrevstorage.api.gui.CountedItemType
import me.steven.indrevstorage.api.gui.StoredItemType
import me.steven.indrevstorage.blockentities.HardDriveRackBlockEntity
import me.steven.indrevstorage.blockentities.TerminalBlockEntity
import me.steven.indrevstorage.gui.widgets.WIRDSInventorySlot
import me.steven.indrevstorage.gui.widgets.WTerminalSearchBar
import me.steven.indrevstorage.utils.componentOf
import me.steven.indrevstorage.utils.identifier
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.util.TriState
import net.minecraft.client.resource.language.I18n
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.Registry
import net.minecraft.world.World
import kotlin.math.ceil

class TerminalScreenHandler(syncId: Int, playerInventory: PlayerInventory, world: World, val pos: BlockPos) : SyncedGuiDescription(
    IRDynamicStorage.TERMINAL_SCREEN_HANDLER,
    syncId,
    playerInventory,
    getBlockInventory(ScreenHandlerContext.create(world, pos)),
    getBlockPropertyDelegate(ScreenHandlerContext.create(world, pos))
) {

    var sortedByIdTypes = emptyList<ItemType>()

    var serverCache = emptyList<StoredItemType>()
    var clientCache = emptyList<CountedItemType>()
    var filteredClientCache = emptyList<CountedItemType>()

    private var lastSearch = ""
    private val searchText = WTerminalSearchBar()
    private var slotsPanel = WGridPanel()
    private var scrollPanel = WScrollPanel(slotsPanel)
    private val panel = TerminalBasePanel()

    init {
        buildPanel()
    }

    fun remapServer() {
        val map = Object2ObjectOpenHashMap<ItemType, HashSet<IRDSInventory>>()
        val terminal = componentOf(world, pos, null)!!.convert(TerminalBlockEntity::class)
        terminal?.network?.forEach(HardDriveRackBlockEntity::class) { rack ->
            rack?.drivesInv?.forEach { inv ->
                inv?.forEach { type, _ -> map.computeIfAbsent(type) { HashSet() }.add(inv) }
            }
        }

        sortedByIdTypes = map.map { it.key }
            .sortedWith(compareBy { Registry.ITEM.getRawId(it.item) })

        val before = getRows(false)
        serverCache = map.map { StoredItemType(it.key, it.value) }
            .sortedWith(compareBy { Registry.ITEM.getRawId(it.type.item) })
        val after = getRows(false)

        rebuildTerminalSlots(before, after)
    }

    fun remapClient(map: Object2IntOpenHashMap<ItemType>) {
        val before = getRows(true)

        sortedByIdTypes = map.map { it.key }
            .sortedWith(compareBy { Registry.ITEM.getRawId(it.item) })

        this.clientCache = map.object2IntEntrySet()
            .map { (key, value) -> key.withCount(value) }
            .sortedWith(compareBy { Registry.ITEM.getRawId(it.type.item) })

        if (world.isClient) {
            applyFilter()
        }

        val after = getRows(true)

        rebuildTerminalSlots(before, after)
    }

    private fun applyFilter() {
        filteredClientCache = clientCache
            .sortedWith(compareByDescending { it.count })
            .filter { lastSearch.isEmpty() || I18n.translate(it.type.item.translationKey).toLowerCase().startsWith(lastSearch.toLowerCase()) }

        val buf = PacketByteBufs.create()
        buf.writeInt(filteredClientCache.size)
        filteredClientCache.forEach {
            val indexOf = sortedByIdTypes.indexOf(it.type)
            if (indexOf >= 0) buf.writeInt(indexOf)
        }
        ClientPlayNetworking.send(PacketHelper.UPDATE_FILTER_TERMINAL, buf)
    }

    private fun getRows(client: Boolean) = ceil((if (client) filteredClientCache else serverCache).size / 9.0).coerceAtLeast(5.0).toInt()

    private fun rebuildTerminalSlots(before: Int, after: Int) {
        if (before == after) return
        var index = 0
        panel.remove(scrollPanel)
        slotsPanel = WGridPanel()
        scrollPanel = WScrollPanel(slotsPanel)
        for (y in 0 until after)
            for (x in 0 until 9)
                slotsPanel.add(WIRDSInventorySlot(this, index++), x, y)
        scrollPanel.isScrollingVertically = TriState.TRUE
        scrollPanel.isScrollingHorizontally = TriState.FALSE
        panel.add(scrollPanel, 0, 1)
        scrollPanel.setSize(9 * 18 + 8, 5*18 - 9)
        scrollPanel.validate(this)
    }

    private fun buildPanel() {

        this.rootPanel = panel

        rebuildTerminalSlots(0, getRows(world.isClient))

        panel.add(searchText, 0, 0)
        searchText.setSize(9 * 18 + 8, 16)

        panel.add(createPlayerInventoryPanel(), 0, 6)

        panel.validate(this)
    }

    inner class TerminalBasePanel : WGridPanel() {
        override fun tick() {
            if (lastSearch != searchText.text) {
                lastSearch = searchText.text
                applyFilter()
            }
        }
    }

    companion object {
        val SCREEN_ID = identifier("terminal")
    }
}