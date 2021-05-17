package me.steven.indrevstorage.gui

import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WScrollPanel
import me.steven.indrevstorage.IRDynamicStorage
import me.steven.indrevstorage.api.gui.TerminalConnection
import me.steven.indrevstorage.gui.widgets.WIRDSInventorySlot
import me.steven.indrevstorage.gui.widgets.WTerminalSearchBar
import me.steven.indrevstorage.utils.identifier
import net.fabricmc.fabric.api.util.TriState
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class TerminalScreenHandler(syncId: Int, playerInventory: PlayerInventory, world: World, val pos: BlockPos) : SyncedGuiDescription(
    IRDynamicStorage.TERMINAL_SCREEN_HANDLER,
    syncId,
    playerInventory,
    getBlockInventory(ScreenHandlerContext.create(world, pos)),
    getBlockPropertyDelegate(ScreenHandlerContext.create(world, pos))
) {

    val connection = TerminalConnection(world, pos, this)

    var currentSearch = ""
    private val searchText = WTerminalSearchBar()
    private var slotsPanel = WGridPanel()
    private var scrollPanel = WScrollPanel(slotsPanel)
    private val panel = TerminalBasePanel()

    init {
        buildPanel()
    }

    fun rebuildTerminalSlots(before: Int, after: Int) {
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

        rebuildTerminalSlots(0, connection.getRowCount(world.isClient))

        panel.add(searchText, 0, 0)
        searchText.setSize(9 * 18 + 8, 16)

        panel.add(createPlayerInventoryPanel(), 0, 6)

        panel.validate(this)
    }

    inner class TerminalBasePanel : WGridPanel() {
        override fun tick() {
            if (currentSearch != searchText.text) {
                currentSearch = searchText.text
                connection.applyFilter()
            }
        }
    }

    companion object {
        val SCREEN_ID = identifier("terminal")
    }
}