package me.steven.indrevstorage.utils

import me.steven.indrev.utils.*
import me.steven.indrevstorage.api.IRDSNetwork
import me.steven.indrevstorage.api.ItemType
import me.steven.indrevstorage.api.gui.TerminalConnection
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.ActionResult
import net.minecraft.util.ItemScatterer
import net.minecraft.util.math.BlockPos

fun interactTerminalWithCursor(player: ServerPlayerEntity, network: IRDSNetwork, connection: TerminalConnection, index: Int, isCrouching: Boolean) {
    val type = if (index >= connection.serverCache.size) ItemType.EMPTY else connection.serverCache[index]
    val cursorStack = player.inventory.cursorStack
    if (cursorStack.isEmpty) {
        // EXTRACT
        val maxAmount = if (isCrouching) 64 else 1
        val extracted = network.extract(type, maxAmount)
        if (extracted > 0) {
            player.inventory.cursorStack = type.toItemStack(extracted)
            player.updateCursorStack()
            network.markDirty()
        }
    } else {
        // INSERT
        val typeToInsert = ItemType(cursorStack.item, cursorStack.tag)
        val remaining = network.insert(typeToInsert, cursorStack.count)

        if (remaining != cursorStack.count) {
            player.inventory.cursorStack = if (remaining == 0) ItemStack.EMPTY else ItemStack(cursorStack.item, remaining)
            player.updateCursorStack()
            network.markDirty()
        }
    }
}

private var WORM_HOLE_DEVICE_PLAYER: FakePlayerEntity? = null

fun interactWormHoleDevice(network: IRDSNetwork, world: ServerWorld, context: ItemUsageContext): ActionResult {
    val typeTag = context.stack.orCreateTag.getCompound("Type")
    val itemType = ItemType.fromNbt(typeTag)

    val available = network[itemType]
    if (available >= 1) {
        val stackToUse = itemType.toItemStack()
        if (WORM_HOLE_DEVICE_PLAYER == null) {
            WORM_HOLE_DEVICE_PLAYER = FakePlayerEntity(world, BlockPos.ORIGIN)
        }
        val player = WORM_HOLE_DEVICE_PLAYER!!
        player.setStackInHand(context.hand, stackToUse)
        val result = itemType.item.useOnBlock(ItemUsageContext(player, context.hand, context.blockHitResult))
        if (result.isAccepted) {
            val extracted = network.extract(itemType, 1)
            assert(extracted == 1)
            (0 until player.inventory.size()).forEach { slot ->
                val invStack = player.inventory.getStack(slot)
                if (!invStack.isEmpty) {
                    val remainder = network.insert(invStack.item with invStack.tag, invStack.count)
                    if (remainder > 0) {
                        val (x, y, z) = context.blockPos.toVec3d()
                        ItemScatterer.spawn(world, x, y, z, ItemStack(invStack.item, remainder).also { it.tag = invStack.tag })
                    }
                    player.inventory.setStack(slot, ItemStack.EMPTY)
                }
            }
            return ActionResult.SUCCESS
        }
        return result
    }
    return ActionResult.success(world.isClient)
}