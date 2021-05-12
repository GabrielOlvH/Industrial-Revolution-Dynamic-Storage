package me.steven.indrevstorage.blocks

import me.steven.indrev.api.machines.Tier
import me.steven.indrev.blocks.machine.pipes.BasePipeBlock
import me.steven.indrevstorage.api.IRDSNetwork
import me.steven.indrevstorage.utils.blockSettings
import me.steven.indrevstorage.utils.componentOf
import net.minecraft.block.BlockState
import net.minecraft.block.Material
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes

class StorageNetworkCable : BasePipeBlock(blockSettings(Material.METAL), Tier.MK1, IRDSNetwork.STORAGE) {
    override fun getShape(blockState: BlockState): VoxelShape {
        return VoxelShapes.fullCube()
    }

    override fun isConnectable(world: ServerWorld, pos: BlockPos, dir: Direction): Boolean {
        return componentOf(world, pos, dir) != null || world.getBlockState(pos).isOf(this)
    }
}