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
import me.steven.indrevstorage.gui.widgets.WIRDSInventorySlot
import me.steven.indrevstorage.utils.identifier
import net.fabricmc.fabric.api.util.TriState
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.Registry
import net.minecraft.world.World
import kotlin.math.ceil

class HardDriveRackScreenHandler(syncId: Int, playerInventory: PlayerInventory, world: World, val pos: BlockPos) : SyncedGuiDescription(
    IRDynamicStorage.HARD_DRIVE_RACK_SCREEN_HANDLER,
    syncId,
    playerInventory,
    getBlockInventory(ScreenHandlerContext.create(world, pos)),
    getBlockPropertyDelegate(ScreenHandlerContext.create(world, pos))
) {
    var mappedTypes = emptyList<MappedItemType>()

    var index = 0
    val slotsPanel = WGridPanel()

    val children = arrayListOf<WIRDSInventorySlot>()

    init {
        remap()
        buildPanel()
    }

    fun remap() {
        val before = ceil((mappedTypes.size / 9.0)).toInt().coerceAtLeast(5)
        val blockEntity = world.getBlockEntity(pos) as HardDriveRackBlockEntity
        val map = Object2ObjectOpenHashMap<ItemType, HashSet<IRDSInventory>>()
        blockEntity.drivesInv.forEach { inv ->
            inv?.forEach { type, _ -> map.computeIfAbsent(type) { HashSet() }.add(inv) }
        }
        mappedTypes = map.map { (type, invs) -> MappedItemType(type, invs) }
            .sortedWith(compareByDescending<MappedItemType> { it.invs.sumBy { inv -> inv[it.type] } }
                .then(compareBy { Registry.ITEM.getRawId(it.type.item) }))
        val after = ceil((mappedTypes.size / 9.0)).toInt().coerceAtLeast(5)

        if (after > before && index > 0) {
            for (y in before until after)
                for (x in 0 until 9)
                    slotsPanel.add(WIRDSInventorySlot(this, index++), x, y)
            rootPanel.validate(this)
        }
    }

    private fun buildPanel() {
        val panel = WGridPanel()
        this.rootPanel = panel

        for (y in 0 until ceil((mappedTypes.size / 9.0)).toInt().coerceAtLeast(5))
            for (x in 0 until 9)
                slotsPanel.add(WIRDSInventorySlot(this, index++), x, y)

        val scrollPanel = WScrollPanel(slotsPanel)
        scrollPanel.isScrollingVertically = TriState.TRUE
        scrollPanel.isScrollingHorizontally = TriState.FALSE
        panel.add(scrollPanel, 0, 1)
        scrollPanel.setSize(9 * 18 + 8, 5*18 - 9)

        panel.add(createPlayerInventoryPanel(), 0, 6)

        panel.validate(this)
    }

    companion object {
        val SCREEN_ID = identifier("hard_drive_rack")
    }
}