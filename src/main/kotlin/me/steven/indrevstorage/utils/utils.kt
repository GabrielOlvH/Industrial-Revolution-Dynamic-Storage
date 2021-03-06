package me.steven.indrevstorage.utils

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import me.steven.indrev.utils.component1
import me.steven.indrev.utils.component2
import me.steven.indrev.utils.component3
import me.steven.indrev.utils.toVec3d
import me.steven.indrevstorage.IRDynamicStorage
import me.steven.indrevstorage.api.ItemType
import me.steven.indrevstorage.api.StorageNetworkComponent
import me.steven.indrevstorage.extensions.IRDSPlayerInventoryExtension
import me.steven.indrevstorage.mixin.AccessorItemUsageContext
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache
import net.minecraft.block.Block
import net.minecraft.block.Material
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.ItemScatterer
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.registry.Registry
import net.minecraft.world.World
import java.util.function.LongFunction

fun identifier(path: String) = Identifier(IRDynamicStorage.MOD_ID, path)

fun itemSettings(): Item.Settings = Item.Settings().group(IRDynamicStorage.ITEM_GROUP)

fun blockSettings(material: Material) = FabricBlockSettings.of(material)

val ItemUsageContext.blockHitResult: BlockHitResult get() = (this as AccessorItemUsageContext).indrevstorage_getHitResult()

fun Identifier.item(item: Item): Identifier {
    Registry.register(Registry.ITEM, this, item)
    return this
}

fun Identifier.block(block: Block): Identifier {
    Registry.register(Registry.BLOCK, this, block)
    return this
}

fun Identifier.blockEntityType(blockEntityType: BlockEntityType<*>): Identifier {
    Registry.register(Registry.BLOCK_ENTITY_TYPE, this, blockEntityType)
    return this
}

infix fun Item.with(tag: CompoundTag?) = ItemType(this, tag?.copy())

fun componentOf(world: ServerWorld, pos: BlockPos, direction: Direction?): StorageNetworkComponent? {
    return IRDynamicStorage.CONNECTABLE.computeIfAbsent(world) { Long2ObjectOpenHashMap() }.computeIfAbsent(
        pos.asLong(),
        LongFunction { BlockApiCache.create(IRDynamicStorage.STORAGE_CONNECTABLE, world, pos) }).find(direction)
}

fun componentOf(world: World, pos: BlockPos, direction: Direction?): StorageNetworkComponent? {
    return if (world is ServerWorld) componentOf(world, pos, direction) else IRDynamicStorage.STORAGE_CONNECTABLE.find(world, pos, direction)
}

fun dropItem(world: World, blockPos: BlockPos, itemStack: ItemStack) {
    val (x, y, z) = blockPos.toVec3d()
    ItemScatterer.spawn(world, x, y, z, itemStack)
}

var PlayerInventory.cachedDeviceSlot
get() = (this as IRDSPlayerInventoryExtension).cachedDeviceSlot
set(value) { (this as IRDSPlayerInventoryExtension).cachedDeviceSlot = value }