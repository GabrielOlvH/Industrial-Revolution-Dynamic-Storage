package me.steven.indrevstorage.blockentities

import me.steven.indrevstorage.IRDynamicStorage
import me.steven.indrevstorage.api.IRDSNetwork
import me.steven.indrevstorage.api.StorageNetworkComponent
import net.minecraft.block.entity.BlockEntity

class TerminalBlockEntity : BlockEntity(IRDynamicStorage.TERMINAL_BLOCK_ENTITY), StorageNetworkComponent {
    override var network: IRDSNetwork? = null
}