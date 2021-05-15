package me.steven.indrevstorage.gui

import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WScrollPanel
import io.github.cottonmc.cotton.gui.widget.WTextField
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import me.steven.indrevstorage.IRDynamicStorage
import me.steven.indrevstorage.PacketHelper
import me.steven.indrevstorage.api.IRDSInventory
import me.steven.indrevstorage.api.ItemType
import me.steven.indrevstorage.api.MappedItemType
import me.steven.indrevstorage.blockentities.HardDriveRackBlockEntity
import me.steven.indrevstorage.blockentities.TerminalBlockEntity
import me.steven.indrevstorage.gui.widgets.WIRDSInventorySlot
import me.steven.indrevstorage.utils.componentOf
import me.steven.indrevstorage.utils.identifier
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.util.TriState
import net.minecraft.client.resource.language.I18n
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.text.LiteralText
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

    private val nRows: Int get() = ceil((mappedTypes.size / 9.0)).toInt().coerceAtLeast(5)

    var sortedByIdTypes = emptyList<ItemType>()
    var mappedTypes = emptyList<MappedItemType>()
    var clientThing = emptyList<Pair<ItemType, Int>>()

    var lastSearch = ""
    val searchText = object : WTextField(LiteralText("Search...")) {
        override fun setSize(x: Int, y: Int) {
            this.width = x
            this.height = y
        }
    }
    var slotsPanel = WGridPanel()
    var scrollPanel = WScrollPanel(slotsPanel)
    val panel = object : WGridPanel() { override fun tick() = clientTick() }

    init {
        buildPanel()
    }

    fun remap() {
        val terminal = componentOf(world, pos, null)!!.convert(TerminalBlockEntity::class)
        val positions = hashSetOf<BlockPos>()
        terminal?.network?.forEach(HardDriveRackBlockEntity::class) { be -> if (be != null) positions.add(be.pos) }
        val map = Object2ObjectOpenHashMap<ItemType, HashSet<IRDSInventory>>()
        positions.forEach { pos ->
            componentOf(world, pos, null)?.convert(HardDriveRackBlockEntity::class)?.drivesInv?.forEach { inv ->
                inv?.forEach { type, _ -> map.computeIfAbsent(type) { HashSet() }.add(inv) }
            }
        }
        sortedByIdTypes = map.map { it.key }
            .sortedWith(compareBy { Registry.ITEM.getRawId(it.item) })
        val before = nRows
        mappedTypes = map.map { MappedItemType(it.key, it.value) }
            .sortedWith(compareBy { Registry.ITEM.getRawId(it.type.item) })
        val after = nRows

        if (after != before) {
            rebuildTerminalSlots()
        }
    }

    fun remap(map: Object2IntOpenHashMap<ItemType>) {
        val before = nRows

        sortedByIdTypes = map.map { it.key }
            .sortedWith(compareBy { Registry.ITEM.getRawId(it.item) })

        this.clientThing = map.object2IntEntrySet()
            .map { (key, value) -> key to value }
            .sortedWith(compareBy { Registry.ITEM.getRawId(it.first.item) })

        if (world.isClient) {
            applyFilter()
        }

        val after = nRows

        if (after != before) {
            rebuildTerminalSlots()
        }
    }

    private fun applyFilter() {
        clientThing = clientThing
            .sortedWith(compareByDescending { it.second })
            .filter { I18n.translate(it.first.item.translationKey).toLowerCase().startsWith(lastSearch.toLowerCase()) }

        val buf = PacketByteBufs.create()
        buf.writeInt(clientThing.size)
        clientThing.forEach {
            val indexOf = sortedByIdTypes.indexOf(it.first)
            if (indexOf >= 0) buf.writeInt(indexOf)
        }
        ClientPlayNetworking.send(PacketHelper.UPDATE_FILTER_TERMINAL, buf)
    }

    private fun rebuildTerminalSlots() {
        var index = 0
        panel.remove(scrollPanel)
        slotsPanel = WGridPanel()
        scrollPanel = WScrollPanel(slotsPanel)
        for (y in 0 until nRows)
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

        rebuildTerminalSlots()

        panel.add(searchText, 0, 0)
        searchText.setSize(9 * 18 + 8, 16)

        panel.add(createPlayerInventoryPanel(), 0, 6)

        panel.validate(this)
    }

    private fun clientTick() {
        if (lastSearch != searchText.text) {
            lastSearch = searchText.text
            applyFilter()
        }

    }

    companion object {
        val SCREEN_ID = identifier("terminal")
    }
}