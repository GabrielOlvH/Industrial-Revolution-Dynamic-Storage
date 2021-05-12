package me.steven.indrevstorage.gui

import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WScrollPanel
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import me.steven.indrevstorage.IRDynamicStorage
import me.steven.indrevstorage.api.IRDSInventory
import me.steven.indrevstorage.api.ItemType
import me.steven.indrevstorage.api.MappedItemType
import me.steven.indrevstorage.blockentities.HardDriveRackBlockEntity
import me.steven.indrevstorage.blockentities.TerminalBlockEntity
import me.steven.indrevstorage.gui.widgets.WIRDSInventorySlot
import me.steven.indrevstorage.utils.componentOf
import me.steven.indrevstorage.utils.identifier
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.util.TriState
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

    private val nRows: Int get() = ceil((mappedTypes.size / 9.0)).toInt().coerceAtLeast(5)

    var mappedTypes = emptyList<MappedItemType>()

    @Environment(EnvType.CLIENT)
    val clientPositionsToRemap = hashSetOf<BlockPos>()

    var slotsPanel = WGridPanel()
    var scrollPanel = WScrollPanel(slotsPanel)
    val panel = object : WGridPanel(){
        override fun tick() {
            if (clientPositionsToRemap.isNotEmpty()) {
                remap(clientPositionsToRemap)
                clientPositionsToRemap.clear()
            }
        }
    }

    init {
        buildPanel()
    }

    fun remap() {
        val terminal = componentOf(world, pos, null)!!.convert(TerminalBlockEntity::class)
        val positions = hashSetOf<BlockPos>()
        terminal?.network?.forEach(HardDriveRackBlockEntity::class) { be -> if (be != null) positions.add(be.pos) }
        remap(positions)
    }

    fun remap(positions: Set<BlockPos>) {
        val before = nRows
        val map = Object2ObjectOpenHashMap<ItemType, HashSet<IRDSInventory>>()

        positions.forEach { pos ->
            componentOf(world, pos, null)?.convert(HardDriveRackBlockEntity::class)?.drivesInv?.forEach { inv ->
                inv?.forEach { type, _ -> map.computeIfAbsent(type) { HashSet() }.add(inv) }
            }
        }
        mappedTypes = map.map { (type, invs) -> MappedItemType(type, invs) }
            .sortedWith(compareByDescending<MappedItemType> { it.invs.sumBy { inv -> inv[it.type] } }
                .then(compareBy { Registry.ITEM.getRawId(it.type.item) }))

        val after = nRows

        if (after != before) {
            rebuildTerminalSlots()
        }
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

        panel.add(createPlayerInventoryPanel(), 0, 6)

        panel.validate(this)
    }

    companion object {
        val SCREEN_ID = identifier("terminal")
    }
}