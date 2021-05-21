package me.steven.indrevstorage.utils

import me.steven.indrevstorage.api.ItemType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

interface WormHoleDeviceUsageContext {

    val blockPos: BlockPos

    fun use(itemType: ItemType, player: PlayerEntity): TypedActionResult<ItemStack>

    companion object {
        class WormHoleDeviceUseOnBlockContext(val ctx: ItemUsageContext) : WormHoleDeviceUsageContext {
            override val blockPos: BlockPos = ctx.blockPos

            override fun use(itemType: ItemType, player: PlayerEntity): TypedActionResult<ItemStack> {
                return TypedActionResult(itemType.item.useOnBlock(ItemUsageContext(player, ctx.hand, ctx.blockHitResult)), ItemStack.EMPTY)
            }
        }

        fun of(ctx: ItemUsageContext) = WormHoleDeviceUseOnBlockContext(ctx)

        class WormHoleDeviceUseContext(val world: World, val hand: Hand, override val blockPos: BlockPos) : WormHoleDeviceUsageContext {
            override fun use(itemType: ItemType, player: PlayerEntity): TypedActionResult<ItemStack> {
                return itemType.item.use(world, player, hand)
            }
        }

        fun of(world: World, hand: Hand, blockPos: BlockPos) = WormHoleDeviceUseContext(world, hand, blockPos)
    }
}