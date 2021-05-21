package me.steven.indrevstorage.events

import me.steven.indrevstorage.gui.AbstractTerminalScreenHandler
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld

object IRDSStartWorldTick : ServerTickEvents.StartWorldTick {
    override fun onStartTick(world: ServerWorld) {
        world.players.forEach { player ->
            val screenHandler = player.currentScreenHandler as? AbstractTerminalScreenHandler ?: return@forEach
            val network = screenHandler.getNetwork() ?: return@forEach
            if (network.dirty) {
                network.syncHDRacks(screenHandler, player as ServerPlayerEntity)
                network.dirty = false
            }
        }
    }
}