package me.steven.indrevstorage.gui

import me.steven.indrevstorage.IRDynamicStorage
import me.steven.indrevstorage.PacketHelper
import me.steven.indrevstorage.api.IRDSNetwork
import me.steven.indrevstorage.blockentities.TerminalBlockEntity
import me.steven.indrevstorage.mixin.AccessorSyncedGuiDescription
import me.steven.indrevstorage.utils.identifier
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.client.gui.screen.Screen
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.screen.slot.SlotActionType
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

    override fun onSlotClick(slotNumber: Int, button: Int, action: SlotActionType?, player: PlayerEntity): ItemStack {
        return if (action == SlotActionType.QUICK_MOVE) {
            if (slotNumber < 0) {
                return ItemStack.EMPTY
            }
            if (slotNumber >= slots.size) return ItemStack.EMPTY
            val slot = slots[slotNumber]
            if (slot == null || !slot.canTakeItems(player)) {
                return ItemStack.EMPTY
            }
            var remaining = ItemStack.EMPTY
            if (slot.hasStack()) {
                val toTransfer = slot.stack
                remaining = toTransfer.copy()
                if (!this.insert(slotNumber, toTransfer, player)) {
                    return ItemStack.EMPTY
                } else if (!swapHotbar(toTransfer, slotNumber, playerInventory, player)) {
                    return ItemStack.EMPTY
                }
                if (toTransfer.isEmpty) {
                    slot.stack = ItemStack.EMPTY
                } else {
                    slot.markDirty()
                }
            }
            remaining!!
        } else {
            super.onSlotClick(slotNumber, button, action, player)
        }
    }

    fun insert(slotNumber: Int, toInsert: ItemStack, player: PlayerEntity): Boolean {
        if (!player.world.isClient) {
            val network = getNetwork() ?: return false
            val rem = network.insert(toInsert)
            val slot = slots[slotNumber]
            if (rem > 0)
                slot.stack = toInsert.copy().also { it.count = rem }
            else
                slot.stack = ItemStack.EMPTY
            onContentChanged(playerInventory)
            network.markDirty()
        }
        return false
    }

    private fun swapHotbar(toInsert: ItemStack?, slotNumber: Int, inventory: Inventory?, player: PlayerEntity?): Boolean {
        return (this as AccessorSyncedGuiDescription).indrevstorage_swapHotbar(toInsert, slotNumber, inventory, player)
    }

    companion object {
        val SCREEN_ID = identifier("terminal")
    }
}