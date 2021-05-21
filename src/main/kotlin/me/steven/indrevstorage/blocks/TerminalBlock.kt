package me.steven.indrevstorage.blocks

import me.steven.indrevstorage.blockentities.TerminalBlockEntity
import me.steven.indrevstorage.gui.InventoryTerminalScreenHandler
import me.steven.indrevstorage.utils.blockSettings
import me.steven.indrevstorage.utils.componentOf
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.block.Block
import net.minecraft.block.BlockEntityProvider
import net.minecraft.block.BlockState
import net.minecraft.block.Material
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.BlockView
import net.minecraft.world.World

class TerminalBlock : Block(blockSettings(Material.GLASS)), BlockEntityProvider {

    override fun createBlockEntity(world: BlockView?): BlockEntity = TerminalBlockEntity()

    override fun onUse(state: BlockState?, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand?, hit: BlockHitResult?): ActionResult {
        if (!world.isClient) {
            player.openHandledScreen(object : ExtendedScreenHandlerFactory {
                override fun createMenu(syncId: Int, inv: PlayerInventory, player: PlayerEntity?): ScreenHandler {
                    return InventoryTerminalScreenHandler(syncId, inv, world, pos)
                }

                override fun getDisplayName(): Text = LiteralText.EMPTY

                override fun writeScreenOpeningData(player: ServerPlayerEntity?, buf: PacketByteBuf) {
                    buf.writeBlockPos(pos)
                }
            })
            postOpenScreen(world, pos)
        }
        return ActionResult.success(world.isClient)
    }

    private fun postOpenScreen(world: World, pos: BlockPos) {
        val terminal = componentOf(world, pos, null)!!.convert(TerminalBlockEntity::class)
        val network = terminal?.network ?: return
        network.markDirty()
    }
}