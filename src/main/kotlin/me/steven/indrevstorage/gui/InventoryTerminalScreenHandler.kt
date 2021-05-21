package me.steven.indrevstorage.gui

import me.steven.indrevstorage.IRDynamicStorage
import me.steven.indrevstorage.PacketHelper
import me.steven.indrevstorage.api.IRDSNetwork
import me.steven.indrevstorage.blockentities.TerminalBlockEntity
import me.steven.indrevstorage.utils.identifier
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.client.gui.screen.Screen
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class InventoryTerminalScreenHandler(syncId: Int, playerInventory: PlayerInventory, world: World, pos: BlockPos) : AbstractTerminalScreenHandler(
    IRDynamicStorage.TERMINAL_SCREEN_HANDLER,
    syncId,
    playerInventory,
    world,
    pos
) {

    init {
        panel.add(createPlayerInventoryPanel(), 0, 6)
        panel.validate(this)
    }

    override val terminalSlotClickAction: (Int, Int) -> Unit = { index, _ ->
        val buf = PacketByteBufs.create()
        buf.writeInt(index)
        buf.writeBoolean(Screen.hasShiftDown())
        ClientPlayNetworking.send(PacketHelper.CLICK_IRDSINV_SLOT, buf)
    }

    override fun getNetwork(): IRDSNetwork? {
        return (world.getBlockEntity(pos) as? TerminalBlockEntity)?.network
    }

    companion object {
        val SCREEN_ID = identifier("terminal")
    }
}