package me.steven.indrevstorage.blockentities

import alexiil.mc.lib.attributes.item.AbstractItemInvView
import alexiil.mc.lib.attributes.item.InvMarkDirtyListener
import alexiil.mc.lib.attributes.item.impl.DirectFixedItemInv
import me.steven.indrevstorage.IRDynamicStorage
import me.steven.indrevstorage.api.IRDSNetwork
import me.steven.indrevstorage.api.StorageNetworkComponent
import me.steven.indrevstorage.api.impl.IRDSHardDriveInventory
import me.steven.indrevstorage.gui.HardDriveRackScreenHandler
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.LiteralText
import net.minecraft.text.Text

class HardDriveRackBlockEntity :
    BlockEntity(IRDynamicStorage.HARD_DRIVE_RACK_BLOCK_ENTITY),
    ExtendedScreenHandlerFactory,
    InvMarkDirtyListener,
    StorageNetworkComponent {

    val inv = DirectFixedItemInv(4)

    val drivesInv = Array<IRDSHardDriveInventory?>(4) { null }

    override var network: IRDSNetwork? = null

    init {
        inv.addListener(this) {}
    }

    override fun onMarkDirty(inv: AbstractItemInvView) {
        updateInventories()
        network?.markDirty()
        markDirty()
    }

    fun updateStacks() {
        drivesInv.forEachIndexed { index, driveInv ->
            if (driveInv != null) {
                val invStack = inv.getInvStack(index)
                if (!invStack.isEmpty)
                    invStack.orCreateTag.put("Items", driveInv.toNbt())
            }
        }
    }

    fun updateInventories() {
        (0 until inv.slotCount).forEach { slot ->
            val stack = inv.getInvStack(slot)
            if (stack.item != IRDynamicStorage.HARD_DRIVE) {
                drivesInv[slot] = null
                return@forEach
            }
            val newInv = IRDSHardDriveInventory()
            val tag = stack.tag
            if (tag != null)
                IRDSHardDriveInventory.fromNbt(newInv, tag.getList("Items", 10))
            drivesInv[slot] = newInv
        }

        network?.markDirty()
    }

    override fun createMenu(syncId: Int, inv: PlayerInventory, player: PlayerEntity): ScreenHandler {
        return HardDriveRackScreenHandler(syncId, inv, world!!, pos)
    }

    override fun getDisplayName(): Text = LiteralText("Hard Drive Rack")

    override fun writeScreenOpeningData(player: ServerPlayerEntity?, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
    }

    override fun fromTag(state: BlockState, tag: CompoundTag) {
        super.fromTag(state, tag)
        inv.fromTag(tag.getCompound("Items"))
        updateInventories()
    }

    override fun toTag(tag: CompoundTag): CompoundTag {
        updateStacks()
        tag.put("Items", inv.toTag())
        return super.toTag(tag)
    }

    fun fromClientTag(tag: CompoundTag) {
        inv.fromTag(tag.getCompound("Items"))
        updateInventories()
    }

    fun toClientTag(tag: CompoundTag): CompoundTag {
        updateStacks()
        tag.put("Items", inv.toTag())
        return tag
    }
}