package me.steven.indrevstorage.gui

import me.steven.indrevstorage.IRDynamicStorage
import me.steven.indrevstorage.PacketHelper
import me.steven.indrevstorage.api.IRDSNetwork
import me.steven.indrevstorage.utils.componentOf
import me.steven.indrevstorage.utils.identifier
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.nbt.NbtHelper
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class WormHoleDeviceSelectorScreenHandler(syncId: Int, playerInventory: PlayerInventory, world: World, pos: BlockPos) : AbstractTerminalScreenHandler(
    IRDynamicStorage.WORM_HOLE_DEVICE_SCREEN_HANDLER,
    syncId,
    playerInventory,
    world,
    pos
) {

    init {
        panel.validate(this)
    }

    override val terminalSlotClickAction: (Int, Int) -> Unit = { index, _ ->
        val buf = PacketByteBufs.create()
        buf.writeInt(index)
        ClientPlayNetworking.send(PacketHelper.WORM_HOLE_DEVICE_SELECT, buf)
    }

    override fun getNetwork(): IRDSNetwork? {
        val stack = playerInventory.mainHandStack
        return componentOf(world, NbtHelper.toBlockPos(stack.tag?.getCompound("Pos") ?: return null), null)?.network
    }

    companion object {
        val SCREEN_ID = identifier("wormhole_device_selector")
    }
}