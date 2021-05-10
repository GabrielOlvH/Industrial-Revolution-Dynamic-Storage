package me.steven.indrevstorage.utils

import me.steven.indrevstorage.IRDynamicStorage
import me.steven.indrevstorage.api.ItemType
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.Block
import net.minecraft.block.Material
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.item.Item
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

fun identifier(path: String) = Identifier(IRDynamicStorage.MOD_ID, path)

fun itemSettings(): Item.Settings = Item.Settings().group(IRDynamicStorage.ITEM_GROUP)

fun blockSettings(material: Material) = FabricBlockSettings.of(material)

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

infix fun Item.with(tag: CompoundTag?) = ItemType(this, tag)