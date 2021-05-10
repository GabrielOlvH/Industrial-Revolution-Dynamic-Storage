package me.steven.indrevstorage.blocks

import me.steven.indrev.utils.component1
import me.steven.indrev.utils.component2
import me.steven.indrev.utils.component3
import me.steven.indrev.utils.toVec3d
import me.steven.indrevstorage.IRDynamicStorage
import me.steven.indrevstorage.api.IRDSInventory
import me.steven.indrevstorage.blockentities.HardDriveRackBlockEntity
import me.steven.indrevstorage.utils.blockSettings
import net.minecraft.block.Block
import net.minecraft.block.BlockEntityProvider
import net.minecraft.block.BlockState
import net.minecraft.block.Material
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.ItemScatterer
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.BlockView
import net.minecraft.world.World

class HardDriveRackBlock : Block(blockSettings(Material.METAL)), BlockEntityProvider {
    override fun createBlockEntity(world: BlockView?): BlockEntity = HardDriveRackBlockEntity()

    override fun onUse(state: BlockState?, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand?, hit: BlockHitResult?): ActionResult {
        if (!world.isClient) {
            val blockEntity = world.getBlockEntity(pos) as? HardDriveRackBlockEntity ?: return ActionResult.PASS
            val stack = player.mainHandStack
            if (stack.item == IRDynamicStorage.HARD_DRIVE) {
                player.setStackInHand(Hand.MAIN_HAND, blockEntity.inv.insert(stack))
                blockEntity.drivesInv[blockEntity.drivesInv.indexOfFirst { it == null }] = IRDSInventory()
                blockEntity.updateInventories()
                blockEntity.markDirty()
                blockEntity.sync()
            }
            else
                player.openHandledScreen(blockEntity)
        }
        return ActionResult.success(world.isClient)
    }

    override fun onStateReplaced(state: BlockState, world: World, pos: BlockPos, newState: BlockState, moved: Boolean) {
        if (!newState.isOf(this)) {
            val blockEntity = world.getBlockEntity(pos) as? HardDriveRackBlockEntity ?: return
            blockEntity.updateStacks()
            val (x, y, z) = pos.toVec3d()
            blockEntity.inv.stackIterable().forEach { stack -> ItemScatterer.spawn(world, x, y, z, stack) }
        }
        super.onStateReplaced(state, world, pos, newState, moved)
    }

}