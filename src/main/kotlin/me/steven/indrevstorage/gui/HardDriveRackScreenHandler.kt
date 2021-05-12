package me.steven.indrevstorage.gui

import alexiil.mc.lib.attributes.item.compat.SlotFixedItemInv
import io.github.cottonmc.cotton.gui.GuiDescription
import io.github.cottonmc.cotton.gui.SyncedGuiDescription
import io.github.cottonmc.cotton.gui.widget.WGridPanel
import me.steven.indrevstorage.IRDynamicStorage
import me.steven.indrevstorage.blockentities.HardDriveRackBlockEntity
import me.steven.indrevstorage.utils.identifier
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class HardDriveRackScreenHandler(syncId: Int, playerInventory: PlayerInventory, world: World, val pos: BlockPos) : SyncedGuiDescription(
    IRDynamicStorage.HARD_DRIVE_RACK_SCREEN_HANDLER,
    syncId,
    playerInventory,
    getBlockInventory(ScreenHandlerContext.create(world, pos)),
    getBlockPropertyDelegate(ScreenHandlerContext.create(world, pos))
) {

    init {
        val panel = object : WGridPanel() {
            override fun validate(c: GuiDescription?) {
                super.validate(c)
                val blockEntity = world.getBlockEntity(pos) as HardDriveRackBlockEntity
                addSlot(SlotFixedItemInv(this@HardDriveRackScreenHandler, blockEntity.inv, !world.isClient, 0, 2 * 18, 2 * 18))
                addSlot(SlotFixedItemInv(this@HardDriveRackScreenHandler, blockEntity.inv, !world.isClient, 1, 3 * 18, 2 * 18))
                addSlot(SlotFixedItemInv(this@HardDriveRackScreenHandler, blockEntity.inv, !world.isClient, 2, 2 * 18, 3 * 18))
                addSlot(SlotFixedItemInv(this@HardDriveRackScreenHandler, blockEntity.inv, !world.isClient, 3, 3 * 18, 3 * 18))
            }
        }
        this.rootPanel = panel

        panel.add(createPlayerInventoryPanel(), 0, 6)

        panel.validate(this)
    }

    companion object {
        val SCREEN_ID = identifier("hard_drive_rack")
    }
}