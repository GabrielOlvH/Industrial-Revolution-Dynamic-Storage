package me.steven.indrevstorage

import me.steven.indrevstorage.blockentities.HardDriveRackBlockEntity
import me.steven.indrevstorage.blocks.HardDriveRackBlock
import me.steven.indrevstorage.gui.HardDriveRackScreenHandler
import me.steven.indrevstorage.items.HardDriveItem
import me.steven.indrevstorage.utils.*
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.item.Items

@Suppress("MemberVisibilityCanBePrivate")
object IRDynamicStorage : ModInitializer {

    const val MOD_ID = "indrevstorage"

    val ITEM_GROUP: ItemGroup = FabricItemGroupBuilder.create(identifier("item_group")).icon { ItemStack(Items.ITEM_FRAME) }.build()

    val HARD_DRIVE = HardDriveItem()

    val HARD_DRIVE_RACK = HardDriveRackBlock()
    val HARD_DRIVE_RACK_ITEM = BlockItem(HARD_DRIVE_RACK, itemSettings())
    val HARD_DRIVE_RACK_BLOCK_ENTITY = BlockEntityType.Builder.create({ HardDriveRackBlockEntity() }, HARD_DRIVE_RACK).build(null)
    val HARD_DRIVE_RACK_SCREEN_HANDLER = ScreenHandlerRegistry.registerExtended(HardDriveRackScreenHandler.SCREEN_ID) { syncId, inv, buf -> HardDriveRackScreenHandler(syncId, inv, inv.player.world, buf.readBlockPos()) }

    override fun onInitialize() {
        identifier("hard_drive").item(HARD_DRIVE)
        identifier("hard_drive_rack").block(HARD_DRIVE_RACK).item(HARD_DRIVE_RACK_ITEM).blockEntityType(HARD_DRIVE_RACK_BLOCK_ENTITY)

        PacketHelper.registerServer()

        ServerTickEvents.START_WORLD_TICK.register { world ->
            world.players.forEach { player ->
                val screenHandler = player.currentScreenHandler as? HardDriveRackScreenHandler ?: return@forEach
                val blockEntity = world.getBlockEntity(screenHandler.pos) as? HardDriveRackBlockEntity ?: return@forEach
                if (blockEntity.dirty) {
                    screenHandler.remap()
                    blockEntity.dirty = false
                }
            }
        }
    }
}