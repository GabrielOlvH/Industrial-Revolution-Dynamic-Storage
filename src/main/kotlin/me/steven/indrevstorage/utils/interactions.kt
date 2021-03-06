package me.steven.indrevstorage.utils

import me.steven.indrev.utils.FakePlayerEntity
import me.steven.indrevstorage.IRDynamicStorage
import me.steven.indrevstorage.api.IRDSNetwork
import me.steven.indrevstorage.api.ItemType
import me.steven.indrevstorage.api.gui.TerminalConnection
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtHelper
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos

fun interactTerminalWithCursor(player: ServerPlayerEntity, network: IRDSNetwork, connection: TerminalConnection, index: Int, isCrouching: Boolean) {
    val type = if (index >= connection.serverCache.size) ItemType.EMPTY else connection.serverCache[index]
    val cursorStack = player.inventory.cursorStack
    if (cursorStack.isEmpty) {
        // EXTRACT
        val maxAmount = if (isCrouching) type.item.maxCount else 1
        val extracted = network.extract(type, maxAmount)
        if (extracted > 0) {
            player.inventory.cursorStack = type.toItemStack(extracted)
            player.updateCursorStack()
            network.markDirty()
        }
    } else {
        // INSERT
        val typeToInsert = cursorStack.item with cursorStack.tag
        val remaining = network.insert(typeToInsert, cursorStack.count)

        if (remaining != cursorStack.count) {
            player.inventory.cursorStack = if (remaining == 0) ItemStack.EMPTY else ItemStack(cursorStack.item, remaining)
            player.updateCursorStack()
            network.markDirty()
        }
    }
}

private var WORM_HOLE_DEVICE_PLAYER: FakePlayerEntity? = null

fun interactWormHoleDevice(network: IRDSNetwork, world: ServerWorld, stack: ItemStack, hand: Hand, player: PlayerEntity, context: WormHoleDeviceUsageContext): ActionResult {
    val typeTag = stack.orCreateTag.getCompound("Type")
    val itemType = ItemType.fromNbt(typeTag)

    val available = network[itemType]
    if (available >= 1) {
        val stackToUse = itemType.toItemStack()
        if (WORM_HOLE_DEVICE_PLAYER == null) {
            WORM_HOLE_DEVICE_PLAYER = FakePlayerEntity(world, BlockPos.ORIGIN)
        }
        val fakePlayer = WORM_HOLE_DEVICE_PLAYER!!
        fakePlayer.setPos(player.x, player.y, player.z)
        fakePlayer.pitch = player.pitch
        fakePlayer.yaw = player.yaw
        fakePlayer.headYaw = player.headYaw
        fakePlayer.prevHeadYaw = player.prevHeadYaw
        fakePlayer.setStackInHand(hand, stackToUse)
        val result = context.use(itemType, fakePlayer)
        if (result.result.isAccepted) {
            val extracted = network.extract(itemType, 1)
            assert(extracted == 1)
            if (!result.value.isEmpty) {
                insertOrDrop(network, world, context.blockPos, result.value)
                fakePlayer.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY)
            }
            (0 until fakePlayer.inventory.size()).forEach { slot ->
                val invStack = fakePlayer.inventory.getStack(slot)
                if (!invStack.isEmpty) {
                    insertOrDrop(network, world, context.blockPos, invStack)
                    fakePlayer.inventory.setStack(slot, ItemStack.EMPTY)
                }
            }
            return ActionResult.SUCCESS
        }
        return result.result
    }
    return ActionResult.success(world.isClient)
}

private fun insertOrDrop(network: IRDSNetwork, world: ServerWorld, blockPos: BlockPos, itemStack: ItemStack) {
    val remainder = network.insert(itemStack.item with itemStack.tag, itemStack.count)
    if (remainder > 0) {
        val unableToInsert = ItemStack(itemStack.item, remainder).also { it.tag = itemStack.tag }
        dropItem(world, blockPos, unableToInsert)
    }
}

fun interceptInsertStack(inventory: PlayerInventory, itemStack: ItemStack): Boolean {
    val world = inventory.player.world
    if (world.isClient) return false
    val cachedDeviceSlot = inventory.cachedDeviceSlot

    var stack: ItemStack? = null
    if (cachedDeviceSlot >= 0) {
        val s = inventory.getStack(cachedDeviceSlot)
        if (s.item == IRDynamicStorage.WORM_HOLE_DEVICE_ITEM) {
            stack = s
        } else inventory.cachedDeviceSlot = -1
    }
    if (stack == null) {
        for (slot in 0 until inventory.size()) {
            val s = inventory.getStack(slot)
            if (s.item == IRDynamicStorage.WORM_HOLE_DEVICE_ITEM) {
                stack = s
                inventory.cachedDeviceSlot = slot
            }
        }
    }
    val tag = stack?.tag ?: return false
    val itemType = ItemType.fromNbt(tag.getCompound("Type"))
    if (tag.contains("Pos") && itemType.matches(itemStack)) {
        val networkPos = NbtHelper.toBlockPos(tag.getCompound("Pos"))
        val network = componentOf(world, networkPos, null)?.network
        if (network != null) {
            val remainder = network.insert(itemStack)
            itemStack.count = remainder
            network.markDirty()
            return true
        }
    }

    return false
}