package me.steven.indrevstorage.events

import me.steven.indrevstorage.blockentities.TerminalBlockEntity
import me.steven.indrevstorage.gui.TerminalScreenHandler
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld

object IRDSStartWorldTick : ServerTickEvents.StartWorldTick {
    override fun onStartTick(world: ServerWorld) {
        world.players.forEach { player ->
            val screenHandler = player.currentScreenHandler as? TerminalScreenHandler ?: return@forEach
            val blockEntity = world.getBlockEntity(screenHandler.pos) as? TerminalBlockEntity ?: return@forEach
            val network = blockEntity.network ?: return@forEach
            if (network.dirty) {
                network.syncHDRacks(screenHandler, player as ServerPlayerEntity)
                network.dirty = false
            }
        }
    }
}