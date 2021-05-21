package me.steven.indrevstorage

import io.github.cottonmc.cotton.gui.client.CottonInventoryScreen
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry

object IRDynamicStorageClient : ClientModInitializer {
    override fun onInitializeClient() {
        ScreenRegistry.register(IRDynamicStorage.HARD_DRIVE_RACK_SCREEN_HANDLER) { handler, inv, title -> CottonInventoryScreen(handler, inv.player, title) }
        ScreenRegistry.register(IRDynamicStorage.TERMINAL_SCREEN_HANDLER) { handler, inv, title -> CottonInventoryScreen(handler, inv.player, title) }
        ScreenRegistry.register(IRDynamicStorage.WORM_HOLE_DEVICE_SCREEN_HANDLER) { handler, inv, title -> CottonInventoryScreen(handler, inv.player, title) }

        PacketHelper.registerClient()
    }
}