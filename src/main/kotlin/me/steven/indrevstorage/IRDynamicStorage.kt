package me.steven.indrevstorage

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import me.steven.indrevstorage.api.StorageNetworkComponent
import me.steven.indrevstorage.blockentities.HardDriveRackBlockEntity
import me.steven.indrevstorage.blockentities.TerminalBlockEntity
import me.steven.indrevstorage.blocks.HardDriveRackBlock
import me.steven.indrevstorage.blocks.StorageNetworkCable
import me.steven.indrevstorage.blocks.TerminalBlock
import me.steven.indrevstorage.events.IRDSStartWorldTick
import me.steven.indrevstorage.gui.HardDriveRackScreenHandler
import me.steven.indrevstorage.gui.InventoryTerminalScreenHandler
import me.steven.indrevstorage.gui.WormHoleDeviceSelectorScreenHandler
import me.steven.indrevstorage.items.HardDriveItem
import me.steven.indrevstorage.items.WormHoleDeviceItem
import me.steven.indrevstorage.utils.*
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import java.util.*

@Suppress("MemberVisibilityCanBePrivate")
object IRDynamicStorage : ModInitializer {

    const val MOD_ID = "indrevstorage"

    val ITEM_GROUP: ItemGroup = FabricItemGroupBuilder.create(identifier("item_group")).icon { ItemStack(Items.ITEM_FRAME) }.build()

    val HARD_DRIVE = HardDriveItem()

    val HARD_DRIVE_RACK = HardDriveRackBlock()
    val HARD_DRIVE_RACK_ITEM = BlockItem(HARD_DRIVE_RACK, itemSettings())
    val HARD_DRIVE_RACK_BLOCK_ENTITY = BlockEntityType.Builder.create({ HardDriveRackBlockEntity() }, HARD_DRIVE_RACK).build(null)
    val HARD_DRIVE_RACK_SCREEN_HANDLER = ScreenHandlerRegistry.registerExtended(HardDriveRackScreenHandler.SCREEN_ID) { syncId, inv, buf -> HardDriveRackScreenHandler(syncId, inv, inv.player.world, buf.readBlockPos()) }

    val STORAGE_NETWORK_CABLE = StorageNetworkCable()
    val STORAGE_NETWORK_CABLE_ITEM = BlockItem(STORAGE_NETWORK_CABLE, itemSettings())

    val TERMINAL = TerminalBlock()
    val TERMINAL_ITEM = BlockItem(TERMINAL, itemSettings())
    val TERMINAL_BLOCK_ENTITY = BlockEntityType.Builder.create({ TerminalBlockEntity() }, TERMINAL).build(null)
    val TERMINAL_SCREEN_HANDLER =  ScreenHandlerRegistry.registerExtended(InventoryTerminalScreenHandler.SCREEN_ID) { syncId, inv, buf -> InventoryTerminalScreenHandler(syncId, inv, inv.player.world, buf.readBlockPos()) }

    val WORM_HOLE_DEVICE_ITEM = WormHoleDeviceItem()
    val WORM_HOLE_DEVICE_SCREEN_HANDLER = ScreenHandlerRegistry.registerExtended(WormHoleDeviceSelectorScreenHandler.SCREEN_ID) { syncId, inv, buf -> WormHoleDeviceSelectorScreenHandler(syncId, inv, inv.player.world, buf.readBlockPos()) }

    val STORAGE_CONNECTABLE: BlockApiLookup<StorageNetworkComponent, Direction?> = BlockApiLookup.get(identifier("connectable"), StorageNetworkComponent::class.java, Direction::class.java)
    val CONNECTABLE = WeakHashMap<World, Long2ObjectOpenHashMap<BlockApiCache<StorageNetworkComponent, Direction?>>>()

    override fun onInitialize() {
        identifier("hard_drive").item(HARD_DRIVE)
        identifier("hard_drive_rack").block(HARD_DRIVE_RACK).item(HARD_DRIVE_RACK_ITEM).blockEntityType(HARD_DRIVE_RACK_BLOCK_ENTITY)

        identifier("terminal").block(TERMINAL).blockEntityType(TERMINAL_BLOCK_ENTITY).item(TERMINAL_ITEM)

        identifier("cable").block(STORAGE_NETWORK_CABLE).item(STORAGE_NETWORK_CABLE_ITEM)

        identifier("worm_hole_device").item(WORM_HOLE_DEVICE_ITEM)

        STORAGE_CONNECTABLE.registerForBlockEntities({ be, _ -> be as? HardDriveRackBlockEntity }, HARD_DRIVE_RACK_BLOCK_ENTITY)
        STORAGE_CONNECTABLE.registerForBlockEntities({ be, _ -> be as? TerminalBlockEntity }, TERMINAL_BLOCK_ENTITY)

        PacketHelper.registerServer()

        ServerTickEvents.START_WORLD_TICK.register(IRDSStartWorldTick)
    }
}