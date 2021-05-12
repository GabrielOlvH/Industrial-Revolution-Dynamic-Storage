package me.steven.indrevstorage.api

import me.steven.indrev.networks.Network
import me.steven.indrevstorage.IRDynamicStorage
import me.steven.indrevstorage.blockentities.HardDriveRackBlockEntity
import me.steven.indrevstorage.utils.componentOf
import net.minecraft.block.BlockState
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
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

    inline fun <T : Any> forEach(kclass: KClass<T>, f: (T?) -> Unit) {
        containers.forEach { (pos, _) ->
            val rack = componentOf(world, pos, null)?.convert(kclass)
            f(rack)
        }
    }

    fun syncHDRacks() {
       forEach(HardDriveRackBlockEntity::class) { rack ->
            rack?.markDirty()
            rack?.sync()
            dirty = true
        }
    }

    companion object {
        val STORAGE = object : Network.Type<IRDSNetwork>("indrev_storage") {
            override fun createEmpty(world: ServerWorld): IRDSNetwork = IRDSNetwork(world)

            override fun isContainer(world: ServerWorld, pos: BlockPos, direction: Direction): Boolean = componentOf(world, pos, direction) != null

            override fun isPipe(blockState: BlockState): Boolean = blockState.isOf(IRDynamicStorage.STORAGE_NETWORK_CABLE)
        }
    }
}