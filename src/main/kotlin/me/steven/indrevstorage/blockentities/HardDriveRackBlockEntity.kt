package me.steven.indrevstorage.blockentities

import alexiil.mc.lib.attributes.item.AbstractItemInvView
import alexiil.mc.lib.attributes.item.InvMarkDirtyListener
import alexiil.mc.lib.attributes.item.impl.DirectFixedItemInv
import me.steven.indrevstorage.IRDynamicStorage
import me.steven.indrevstorage.api.IRDSInventory
import me.steven.indrevstorage.gui.HardDriveRackScreenHandler
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
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
    BlockEntityClientSerializable,
    ExtendedScreenHandlerFactory,
    InvMarkDirtyListener {

    val inv = DirectFixedItemInv(8)

    val drivesInv = Array<IRDSInventory?>(8) { null }

    var dirty = false

    init {
        inv.addListener(this) {}
    }

    override fun onMarkDirty(inv: AbstractItemInvView) {
        (0 until 8).forEach { slot ->
            val current = this.inv.getInvStack(slot)
            if (current.item != IRDynamicStorage.HARD_DRIVE) return@forEach
            val newInv = IRDSInventory()
            IRDSInventory.fromNbt(newInv, current.tag?.getList("Items", 10) ?: return@forEach)
            drivesInv[slot] = newInv
        }
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
            if (stack.isEmpty) return@forEach
            val newInv = IRDSInventory()
            IRDSInventory.fromNbt(newInv, stack.tag?.getList("Items", 10) ?: return@forEach)
            drivesInv[slot] = newInv
        }
        dirty = true
    }

    override fun createMenu(syncId: Int, inv: PlayerInventory, player: PlayerEntity): ScreenHandler {
        return HardDriveRackScreenHandler(syncId, inv, world!!, pos)
    }

    override fun getDisplayName(): Text = LiteralText("Hard Drive Rack")

    override fun writeScreenOpeningData(player: ServerPlayerEntity?, buf: PacketByteBuf) {
        buf.writeBlockPos(pos)
    }

    override fun markDirty() {
        super.markDirty()
        this.dirty = true
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

    override fun fromClientTag(tag: CompoundTag) {
        inv.fromTag(tag.getCompound("Items"))
        updateInventories()
    }

    override fun toClientTag(tag: CompoundTag): CompoundTag {
        updateStacks()
        tag.put("Items", inv.toTag())
        return tag
    }
}