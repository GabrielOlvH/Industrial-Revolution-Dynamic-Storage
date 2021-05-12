package me.steven.indrevstorage

import me.steven.indrevstorage.blockentities.TerminalBlockEntity
import me.steven.indrevstorage.gui.TerminalScreenHandler
import me.steven.indrevstorage.utils.identifier
import me.steven.indrevstorage.utils.interactTerminalWithCursor
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.util.math.BlockPos

object PacketHelper {

    val CLICK_IRDSINV_SLOT = identifier("click_irdsinv_slot")
    val REMAP_SCREEN_HANDLER = identifier("remap_screen_handler")

    fun registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(CLICK_IRDSINV_SLOT) { server, player, _, buf, _ ->
            val index = buf.readByte().toInt()
            val isCrouching = buf.readBoolean()

            server.execute {
                val screenHandler = player.currentScreenHandler as? TerminalScreenHandler ?: return@execute
                val blockEntity = player.world.getBlockEntity(screenHandler.pos) as? TerminalBlockEntity ?: return@execute
                val network = blockEntity.network ?: return@execute
                interactTerminalWithCursor(player, network, blockEntity, screenHandler, index, isCrouching)
            }
        }
    }

    fun registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(REMAP_SCREEN_HANDLER) { client, _, buf, _ ->
            val positions = hashSetOf<BlockPos>()
            val size = buf.readInt()
            for (x in 0 until size) positions.add(buf.readBlockPos())
            client.execute {
                val screenHandler = client.player?.currentScreenHandler as? TerminalScreenHandler ?: return@execute
                screenHandler.remap(positions)
            }
        }
    }

}