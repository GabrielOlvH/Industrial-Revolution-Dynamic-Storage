package me.steven.indrevstorage

import io.github.cottonmc.cotton.gui.client.CottonInventoryScreen
import me.steven.indrevstorage.blockentities.HardDriveRackBlockEntity
import me.steven.indrevstorage.gui.HardDriveRackScreenHandler
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry

object IRDynamicStorageClient : ClientModInitializer {
    override fun onInitializeClient() {
        ScreenRegistry.register(IRDynamicStorage.HARD_DRIVE_RACK_SCREEN_HANDLER) { handler, inv, title -> CottonInventoryScreen(handler, inv.player, title) }

        ClientTickEvents.START_WORLD_TICK.register { world ->
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