package me.steven.indrevstorage.items

import me.steven.indrevstorage.blockentities.TerminalBlockEntity
import me.steven.indrevstorage.gui.WormHoleDeviceSelectorScreenHandler
import me.steven.indrevstorage.utils.componentOf
import me.steven.indrevstorage.utils.interactWormHoleDevice
import me.steven.indrevstorage.utils.itemSettings
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.nbt.NbtHelper
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class WormHoleDeviceItem : Item(itemSettings()) {
    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val pos = context.blockPos
        val world = context.world
        val stack = context.stack
        val player = context.player

        if (world is ServerWorld) {
            val tag = stack.orCreateTag

            if (tag.contains("Pos")) {
                val networkPos = NbtHelper.toBlockPos(tag.getCompound("Pos"))
                val network = componentOf(world, networkPos, null)?.network
                if (network != null) {
                    return interactWormHoleDevice(network, world, context)
                }
            }

            val component = componentOf(world, pos, null) ?: return ActionResult.PASS
            if (component.network != null) {
                tag.put("Pos", NbtHelper.fromBlockPos(pos))
                player?.sendMessage(TranslatableText("item.indrevstorage.worm_hole_device.linked"), true)
                return ActionResult.success(world.isClient)
            }
        }
        return ActionResult.CONSUME
    }

    override fun use(world: World, player: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val stack = player.getStackInHand(hand)
        if (!world.isClient) {
            val tag = stack.orCreateTag

            if (tag.contains("Pos")) {
                val pos = NbtHelper.toBlockPos(tag.getCompound("Pos"))

                player.openHandledScreen(object : ExtendedScreenHandlerFactory {
                    override fun createMenu(syncId: Int, inv: PlayerInventory, player: PlayerEntity?): ScreenHandler {
                        return WormHoleDeviceSelectorScreenHandler(syncId, inv, world, pos)
                    }

                    override fun getDisplayName(): Text = LiteralText.EMPTY

                    override fun writeScreenOpeningData(player: ServerPlayerEntity?, buf: PacketByteBuf) {
                        buf.writeBlockPos(pos)
                    }
                })
                postOpenScreen(world, pos)
            }
        }
        return TypedActionResult.success(stack, world.isClient)
    }

    private fun postOpenScreen(world: World, pos: BlockPos) {
        val terminal = componentOf(world, pos, null)!!.convert(TerminalBlockEntity::class)
        val network = terminal?.network ?: return
        network.markDirty()
    }
}