package me.steven.indrevstorage.api

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import me.steven.indrev.networks.Network
import me.steven.indrevstorage.IRDynamicStorage
import me.steven.indrevstorage.PacketHelper
import me.steven.indrevstorage.blockentities.HardDriveRackBlockEntity
import me.steven.indrevstorage.gui.AbstractTerminalScreenHandler
import me.steven.indrevstorage.utils.componentOf
import me.steven.indrevstorage.utils.with
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.block.BlockState
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.registry.Registry
import kotlin.reflect.KClass

class IRDSNetwork(world: ServerWorld) : Network(STORAGE, world) {

    // used for syncing when a terminal is open
    var dirty = true

    override fun tick(world: ServerWorld) {
    }

    override fun appendContainer(blockPos: BlockPos, direction: Direction) {
        super.appendContainer(blockPos, direction)
        val component = componentOf(world, blockPos, direction)
        component?.network = this
    }

    fun <T : Any> iterator(kclass: KClass<T>) : Iterator<T> {
        val list = ArrayList(containers.keys)
        return object : Iterator<T> {
            override fun hasNext(): Boolean {
                var value: T? = null
                while (value == null && list.isNotEmpty()) {
                    value = componentOf(world, list[0], null)?.convert(kclass)
                    if (value != null) return true
                    else list.removeFirst()
                }

                return false
            }

            override fun next(): T = componentOf(world, list.removeAt(0), null)!!.convert(kclass)!!

        }
    }

    fun markDirty() {
        this.dirty = true
    }

    inline fun <T : Any> forEach(kclass: KClass<T>, f: (T?) -> Unit) {
        containers.forEach { (pos, _) ->
            val rack = componentOf(world, pos, null)?.convert(kclass)
            f(rack)
        }
    }

    fun syncHDRacks(screenHandler: AbstractTerminalScreenHandler, player: ServerPlayerEntity) {
        screenHandler.connection.updateServer()
        val map = Object2IntOpenHashMap<ItemType>()


        val buf = PacketByteBufs.create()
        forEach(HardDriveRackBlockEntity::class) { rack ->
            rack?.drivesInv?.forEach { inv ->
                inv?.forEach { type, count -> map.addTo(type, count) }
            }
        }
        buf.writeInt(map.size)
        map.forEach { (type, count) ->
            buf.writeInt(Registry.ITEM.getRawId(type.item))
            buf.writeInt(count)
            buf.writeBoolean(type.tag != null)
            if (type.tag != null)
                buf.writeCompoundTag(type.tag)
        }
        ServerPlayNetworking.send(player, PacketHelper.REMAP_SCREEN_HANDLER, buf)
    }

    fun insert(type: ItemType, count: Int): Int {
        var remaining = count
        val it = iterator(HardDriveRackBlockEntity::class)
        outer@while (it.hasNext()) {
            val rack = it.next()
            for (inv in rack.drivesInv.sortedByDescending { it?.has(type) }.filterNotNull()) {
                val result = inv.insert(type, remaining)
                if (result < remaining) rack.markDirty()
                remaining = result
                if (remaining <= 0)
                    break@outer
            }
        }
        return remaining
    }

    fun insert(itemStack: ItemStack) = insert(itemStack.item with itemStack.tag, itemStack.count)

    fun extract(type: ItemType, count: Int): Int {
        var extracted = 0
        val it = iterator(HardDriveRackBlockEntity::class)
        outer@while (it.hasNext()) {
            val rack = it.next()
            for (inv in rack.drivesInv.filterNotNull()) {
                val c = inv[type]
                val result = inv.extract(type, c.coerceAtMost(count))
                if (result > 0) rack.markDirty()
                extracted += result
                if (extracted >= count) break@outer
            }
        }
        return extracted
    }

    operator fun get(type: ItemType): Int {
        var total = 0
        forEach(HardDriveRackBlockEntity::class) { rack ->
            if (rack != null)
                for (inv in rack.drivesInv.filterNotNull()) {
                    total += inv[type]
                }
        }
        return total
    }

    companion object {
        val STORAGE = object : Network.Type<IRDSNetwork>("indrev_storage") {
            override fun createEmpty(world: ServerWorld): IRDSNetwork = IRDSNetwork(world)

            override fun isContainer(world: ServerWorld, pos: BlockPos, direction: Direction): Boolean = componentOf(world, pos, direction) != null

            override fun isPipe(blockState: BlockState): Boolean = blockState.isOf(IRDynamicStorage.STORAGE_NETWORK_CABLE)
        }
    }
}